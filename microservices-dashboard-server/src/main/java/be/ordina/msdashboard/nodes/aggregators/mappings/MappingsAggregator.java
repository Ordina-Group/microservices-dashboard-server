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
package be.ordina.msdashboard.nodes.aggregators.mappings;

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
 * Aggregates nodes from mappings information exposed by Spring Boot's Actuator.
 * <p>
 * In case Spring Boot is not used for a microservice, your service must comply to
 * the mappings format exposed by Spring Boot under the <code>/mappings</code> endpoint.
 * Example of a mappings response:
 * <p>
 * <pre>
 * {
 *   "{[/endpoint1],methods=[GET],produces=[application/json]}" : {
 *     "bean" : "requestMappingHandlerMapping",
 *     "method" : "public java.util.Date be.ordina.controllers.Endpoint1Controller.retrieveDate(java.util.Date)"
 *   },
 *   "{[/endpoint2],methods=[GET, POST]}" : {
 *     "bean" : "requestMappingHandlerMapping",
 *     "method" : "public void be.ordina.controllers.Endpoint2Controller.createInformation(be.ordina.model.Information)"
 *   }
 * }
 * </pre>
 * <p>
 * Any endpoint with a method signature containing <code>org.springframework</code> will be ignored.
 *
 * @author Andreas Evers
 * @author Kevin van Houtte
 * @see <a href="http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready">
 * Spring Boot Actuator</a>
 */
public class MappingsAggregator implements NodeAggregator {

	private static final Logger logger = LoggerFactory.getLogger(MappingsAggregator.class);
	private static final String AGGREGATOR_KEY = "mappings";
	private final DiscoveryClient discoveryClient;

	private final UriResolver uriResolver;
	private final MappingsProperties properties;
	private final NettyServiceCaller caller;
	private final ErrorHandler errorHandler;
	private SecurityStrategyFactory securityStrategyFactory;

	@Deprecated
	public MappingsAggregator(final DiscoveryClient discoveryClient,
	                          final UriResolver uriResolver,
							  final MappingsProperties properties,
							  final NettyServiceCaller caller,
							  final ErrorHandler errorHandler) {
		this.discoveryClient = discoveryClient;
		this.uriResolver = uriResolver;
		this.properties = properties;
		this.caller = caller;
		this.errorHandler = errorHandler;
	}

	public MappingsAggregator(final DiscoveryClient discoveryClient,
	                          final UriResolver uriResolver,
							  final MappingsProperties properties,
							  final NettyServiceCaller caller,
							  final ErrorHandler errorHandler,
							  final SecurityStrategyFactory securityStrategyFactory) {
		this(discoveryClient, uriResolver, properties, caller, errorHandler);
		this.securityStrategyFactory = securityStrategyFactory;
	}

	@Override
	public Observable<Node> aggregateNodes() {
		final Object outboundSecurityObject = getOutboundSecurityObject();
		Observable<Observable<Node>> observableObservable = getServiceIdsFromDiscoveryClient()
				.map(id -> new ImmutablePair<>(id, resolveMappingsUrl(id)))
				.doOnNext(pair -> logger.info("Creating mappings observable: {}", pair))
				.map(pair -> outboundSecurityObject != null
				             ? getMappingNodesFromService(pair.getLeft(), pair.getRight(), outboundSecurityObject)
				             : getMappingNodesFromService(pair.getLeft(), pair.getRight())
				)
				.doOnNext(el -> logger.debug("Unmerged mappings observable: {}", el))
				.doOnError(e -> errorHandler.handleSystemError("Error filtering services: " + e.getMessage(), e))
				.doOnCompleted(() -> logger.info("Completed getting all mappings observables"))
				.retry();
		return Observable.merge(observableObservable)
				.doOnNext(el -> logger.debug("Merged health node: {}", el.getId()))
				.doOnError(e -> errorHandler.handleSystemError("Error filtering services: " + e.getMessage(), e))
				.doOnCompleted(() -> logger.info("Completed merging all mappings observables"));
	}

	private String resolveMappingsUrl(String id) {
		List<ServiceInstance> instances = discoveryClient.getInstances(id);
		if (instances.isEmpty()) {
			throw new IllegalStateException("No instances found for service " + id);
		} else {
			return uriResolver.resolveMappingsUrl(instances.get(0));
		}
	}

	protected Observable<String> getServiceIdsFromDiscoveryClient() {
		logger.info("Discovering services for mappings");
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

	@Deprecated
	protected Observable<Node> getMappingNodesFromService(String serviceId, String url) {
		return getMappingNodesFromService(serviceId, url, null);
	}

	protected Observable<Node> getMappingNodesFromService(String serviceId, String url, final Object outboundSecurityObject) {
		HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);
		applyOutboundSecurityStrategyOnRequest(request, outboundSecurityObject);
		properties.getRequestHeaders()
		          .forEach(request::withHeader);
		return caller.retrieveJsonFromRequest(serviceId, request)
				.map(source -> MappingsToNodeConverter.convertToNodes(serviceId, source))
				.flatMap(nodes -> nodes)
				.filter(node -> !properties.getFilteredServices().contains(node.getId()))
				.doOnNext(el -> logger.info("Mapping node {} discovered in url: {}", el.getId(), url))
				.doOnError(e -> logger.error("Error during mapping node fetching: ", e))
				.doOnCompleted(() -> logger.info("Completed emission of a mapping node observable from url: {}", url))
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
