/*
 * Copyright 2015-2018 the original author or authors.
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

package be.ordina.msdashboard.aggregator.health;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ordina.msdashboard.LandscapeWatcher;
import be.ordina.msdashboard.events.HealthInfoRetrievalFailed;
import be.ordina.msdashboard.events.HealthInfoRetrieved;
import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Aggregator for the /health endpoint of a regular Spring Boot application.
 *
 * @author Dieter Hubau
 */
public class HealthAggregator {

	private static final Logger logger = LoggerFactory.getLogger(HealthAggregator.class);

	private final WebClient webClient;
	private final LandscapeWatcher landscapeWatcher;
	private final ApplicationEventPublisher publisher;
	private final UriComponentsBuilder uriComponentsBuilder;

	public HealthAggregator(LandscapeWatcher landscapeWatcher, WebClient webClient, ApplicationEventPublisher publisher) {
		this.landscapeWatcher = landscapeWatcher;
		this.webClient = webClient;
		this.publisher = publisher;
		this.uriComponentsBuilder = UriComponentsBuilder.newInstance().path("/actuator/health");
	}

	@EventListener({NewServiceInstanceDiscovered.class})
	public void handleApplicationInstanceEvent(NewServiceInstanceDiscovered event) {
		ServiceInstance serviceInstance = (ServiceInstance) event.getSource();
		checkHealthInformation(serviceInstance);
	}

	@Scheduled(fixedRateString = "${aggregator.health.rate:10000}")
	public void aggregateHealthInformation() {
		logger.debug("Aggregating [HEALTH] information");

		this.landscapeWatcher.getServiceInstances()
				.entrySet()
				.parallelStream()
				.forEach(entry -> entry.getValue().parallelStream().forEach(this::checkHealthInformation));
	}

	private void checkHealthInformation(ServiceInstance instance) {
		URI uri = this.uriComponentsBuilder.uri(instance.getUri()).build().toUri();

		this.webClient.get().uri(uri).retrieve().bodyToMono(HealthWrapper.class)
				.defaultIfEmpty(new HealthWrapper(Status.DOWN, new HashMap<>()))
				.map(HealthWrapper::getHealth)
				.doOnError(exception -> {
					logger.debug("Could not retrieve health information for [" + uri + "]", exception);

					this.publisher.publishEvent(new HealthInfoRetrievalFailed(instance));
				})
				.subscribe(healthInfo -> {
					logger.debug("Found health information for service [{}]", instance.getServiceId());

					this.publisher.publishEvent(new HealthInfoRetrieved(instance, healthInfo));
				});
	}

	/**
	 * Wrapper for the Health class since it doesn't have correct constructors for Jackson.
	 */
	static class HealthWrapper {

		private Health health;

		@JsonCreator
		HealthWrapper(@JsonProperty("status") Status status, @JsonProperty("details") Map<String, Object> details) {
			this.health = Health.status(status).withDetails(details == null ? new HashMap<>() : details).build();
		}

		protected Health getHealth() {
			return this.health;
		}
	}
}
