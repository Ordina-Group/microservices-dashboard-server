/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ordina.msdashboard.nodes.aggregators.health;

import be.ordina.msdashboard.nodes.aggregators.ErrorHandler;
import be.ordina.msdashboard.nodes.aggregators.NettyServiceCaller;
import be.ordina.msdashboard.nodes.aggregators.NodeAggregator;
import be.ordina.msdashboard.nodes.model.Node;
import be.ordina.msdashboard.nodes.uriresolvers.UriResolver;
import be.ordina.msdashboard.security.outbound.SecurityStrategyFactory;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.List;

import static be.ordina.msdashboard.nodes.aggregators.Constants.ZUUL;

/**
 * Aggregates nodes from health information exposed by Spring Boot's Actuator.
 * <p>
 * In case Spring Boot is not used for a microservice, your service must comply to
 * the health format exposed by Spring Boot under the <code>/health</code> endpoint.
 * Example of a health response:
 * <p>
 * <pre>
 * {
 *   "status": "UP",
 *   "foo": "bar",
 *   "aServiceWhichThisServiceCalls": {
 *     "status": "UNKNOWN",
 *     "type": "SOAP",
 *     "group": "SVCGROUP"
 *   },
 *   "anotherServiceWhichThisServiceCalls": {
 *     "status": "DOWN",
 *     "type": "REST",
 *     "group": "SVCGROUP",
 *     "foo": "bar"
 *   }
 * }
 * </pre>
 *
 * @author Andreas Evers
 * @author Kevin van Houtte
 * @see <a href="http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready">
 * Spring Boot Actuator</a>
 */
public class HealthIndicatorsAggregator implements NodeAggregator {

	private static final Logger logger = LoggerFactory.getLogger(HealthIndicatorsAggregator.class);
	private static final String AGGREGATOR_KEY = "health";
	private DiscoveryClient discoveryClient;

	private UriResolver uriResolver;
	private HealthProperties properties;
	private NettyServiceCaller caller;
	private ErrorHandler errorHandler;
	private HealthToNodeConverter healthToNodeConverter;
	private SecurityStrategyFactory securityStrategyFactory;

	@Deprecated
	public HealthIndicatorsAggregator(final DiscoveryClient discoveryClient,
	                                  final UriResolver uriResolver,
									  final HealthProperties properties,
									  final NettyServiceCaller caller,
									  final ErrorHandler errorHandler,
									  final HealthToNodeConverter healthToNodeConverter) {
		this.discoveryClient = discoveryClient;
		this.uriResolver = uriResolver;
		this.properties = properties;
		this.caller = caller;
		this.errorHandler = errorHandler;
		this.healthToNodeConverter = healthToNodeConverter;
	}

	public HealthIndicatorsAggregator(final DiscoveryClient discoveryClient,
	                                  final UriResolver uriResolver,
									  final HealthProperties properties,
									  final NettyServiceCaller caller,
									  final ErrorHandler errorHandler,
									  final HealthToNodeConverter healthToNodeConverter,
									  final SecurityStrategyFactory securityStrategyFactory) {
		this(discoveryClient, uriResolver, properties, caller, errorHandler, healthToNodeConverter);
		this.securityStrategyFactory = securityStrategyFactory;
	}

	@Override
	public Observable<Node> aggregateNodes() {
		final Object outboundSecurityObject = getOutboundSecurityObject();
		Observable<Observable<Node>> observableObservable = getServiceIdsFromDiscoveryClient()
				.map(id -> new ImmutablePair<>(id, resolveHealthCheckUrl(id)))
				.doOnNext(pair -> logger.info("Creating health observable: {}", pair))
				.map(pair -> outboundSecurityObject != null
				             ? getHealthNodesFromService(pair.getLeft(), pair.getRight(), outboundSecurityObject)
				             : getHealthNodesFromService(pair.getLeft(), pair.getRight())
				)
				.doOnNext(el -> logger.debug("Unmerged health observable: {}",  el))
				.doOnError(e -> errorHandler.handleSystemError("Error filtering services: " + e.getMessage(), e))
				.doOnCompleted(() -> logger.info("Completed getting all health observables"))
				.retry();
		return Observable.merge(observableObservable)
				.doOnNext(el -> logger.debug("Merged health node: {}", el.getId()))
				.doOnError(e -> errorHandler.handleSystemError("Error filtering services: " + e.getMessage(), e))
				.doOnCompleted(() -> logger.info("Completed merging all health observables"));
	}

	private String resolveHealthCheckUrl(String id) {
		List<ServiceInstance> instances = discoveryClient.getInstances(id);
		if (instances.isEmpty()) {
			throw new IllegalStateException("No instances found for service " + id);
		} else {
			return uriResolver.resolveHealthCheckUrl(instances.get(0));
		}
	}

	protected Observable<String> getServiceIdsFromDiscoveryClient() {
        logger.info("Discovering services for health");
        return Observable.from(discoveryClient.getServices())
                         .subscribeOn(Schedulers.io())
                         .publish()
                         .autoConnect()
                         .map(String::toLowerCase)
                         .filter(id -> !id.equals(ZUUL))
                         .doOnNext(serviceId -> logger.debug("Service discovered: {}", serviceId))
                         .doOnError(e -> errorHandler.handleSystemError("Error filtering services: " + e.getMessage(), e))
                         .retry();
    }

	protected Observable<Node> getHealthNodesFromService(String serviceId, String url) {
		return getHealthNodesFromService(serviceId, url, null);
	}

	protected Observable<Node> getHealthNodesFromService(String serviceId, String url, final Object outboundSecurityObject) {
		HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);
		applyOutboundSecurityStrategyOnRequest(request, outboundSecurityObject);
		properties.getRequestHeaders()
		          .forEach(request::withHeader);
		return caller.retrieveJsonFromRequest(serviceId, request)
		             .flatMap(el -> healthToNodeConverter.convertToNodes(serviceId, el))
		             .filter(node -> !properties.getFilteredServices().contains(node.getId()))
		             //TODO: .map(node -> springCloudEnricher.enrich(node))
		             .doOnNext(node -> logger.info("Health node {} discovered in url: {}", node.getId(), url))
		             .doOnError(e -> logger.error("Error during healthnode fetching: ", e))
		             .doOnCompleted(() -> logger.info("Completed emission of a health node observable from url: {}", url))
		             .onErrorResumeNext(Observable.empty());
	}


	private Object getOutboundSecurityObject() {
		if (securityStrategyFactory != null) {
			return securityStrategyFactory.getStrategy(AGGREGATOR_KEY).getOutboundSecurityObjectProvider().getOutboundSecurityObject();
		} else {
			return null;
		}
	}

	private void applyOutboundSecurityStrategyOnRequest(HttpClientRequest<ByteBuf> request, Object outboundSecurityObject) {
		if (outboundSecurityObject != null) {
			securityStrategyFactory.getStrategy(AGGREGATOR_KEY).getOutboundSecurityStrategy().apply(request, outboundSecurityObject);
		}
	}
}
