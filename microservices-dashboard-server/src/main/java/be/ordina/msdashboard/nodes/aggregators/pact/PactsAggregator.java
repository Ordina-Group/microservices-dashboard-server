/*
 * Copyright 2012-2016 the original author or authors.
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
package be.ordina.msdashboard.nodes.aggregators.pact;

import be.ordina.msdashboard.nodes.aggregators.NodeAggregator;
import be.ordina.msdashboard.nodes.model.Node;
import be.ordina.msdashboard.nodes.model.SystemEvent;
import com.jayway.jsonpath.JsonPath;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import rx.Observable;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * @author Andreas Evers
 */
public class PactsAggregator implements NodeAggregator {

	private static final Logger logger = LoggerFactory.getLogger(PactsAggregator.class);

	private final PactProperties properties;
	private final ApplicationEventPublisher publisher;
	private final PactToNodeConverter pactToNodeConverter;

	@Value("${pact-broker.url:'http://localhost:8089'}")
	protected String pactBrokerUrl;
	// TODO: is latest going to return all pacts?
	@Value("${pact-broker.latest-url:'/pacts/latest'}")
	protected String latestPactsUrl;
	@Value("${pact-broker.self-href-jsonPath:'$.pacts[*]._links.self[0].href'}")
	protected String selfHrefJsonPath;

	public PactsAggregator(final PactToNodeConverter pactToNodeConverter,
			final PactProperties properties, final ApplicationEventPublisher publisher) {
		this.properties = properties;
		this.publisher = publisher;
		this.pactToNodeConverter = pactToNodeConverter;
	}

	@Override
	public Observable<Node> aggregateNodes() {
		Observable<String> urls = getPactUrlsFromBroker();
		return urls.map(url -> getNodesFromPacts(url))
				.flatMap(el -> el)
				.doOnNext(el -> logger.info("Merged pact node! " + el.getId()));
	}

	private Observable<String> getPactUrlsFromBroker() {
		logger.info("Discovering pact urls");
		final String url = pactBrokerUrl + latestPactsUrl;
		HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);
		for (Map.Entry<String, String> header : properties.getRequestHeaders().entrySet()) {
			request.withHeader(header.getKey(), header.getValue());
		}

		return RxNetty.createHttpRequest(request)
				.doOnError(el -> {
					String error = MessageFormat.format("Error retrieving pacts in url {0} with headers {1}: {2}",
							request.getUri(), request.getHeaders().entries(), el);
					logger.error(error);
					publisher.publishEvent(new SystemEvent(error, el));
				})
				.filter(r -> {
					if (r.getStatus().code() < 400) {
						return true;
					} else {
						String warning = "Exception " + r.getStatus() + " for call " + url + " with headers " + r.getHeaders().entries();
						logger.warn(warning);
						publisher.publishEvent(new SystemEvent(warning));
						return false;
					}
				})
				.flatMap(response -> response.getContent())
				.map(data -> data.toString(Charset.defaultCharset()))
				.onErrorReturn(Throwable::toString)
				.map(response -> JsonPath.<List<String>> read(response, selfHrefJsonPath))
				.map(jsonList -> Observable.from(jsonList))
				.flatMap(el -> (Observable<String>) el.map(obj -> (String) obj))
				.doOnNext(pactUrl -> logger.info("Pact url discovered: " + pactUrl));
	}

	private Observable<Node> getNodesFromPacts(final String url) {logger.info("Discovering pact urls");
		HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);
		for (Map.Entry<String, String> header : properties.getRequestHeaders().entrySet()) {
			request.withHeader(header.getKey(), header.getValue());
		}

		return RxNetty.createHttpRequest(request)
				.doOnError(el -> {
					String error = MessageFormat.format("Error retrieving pacts in url {0} with headers {1}: {2}",
							request.getUri(), request.getHeaders().entries(), el);
					logger.error(error);
					publisher.publishEvent(new SystemEvent(error, el));
				})
				.filter(r -> {
					if (r.getStatus().code() < 400) {
						return true;
					} else {
						String warning = "Exception " + r.getStatus() + " for call " + url + " with properties " + r.getHeaders().entries();
						logger.warn(warning);
						publisher.publishEvent(new SystemEvent(warning));
						return false;
					}
				})
				.flatMap(response -> response.getContent())
				.map(data -> data.toString(Charset.defaultCharset()))
				.onErrorReturn(Throwable::toString)
				.map(response -> pactToNodeConverter.convert(response, url))
				.filter(node -> !properties.getFilteredServices().contains(node.getId()))
				.doOnNext(node -> logger.info("Pact node discovered in url: " + url));
	}
}
