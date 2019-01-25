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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import be.ordina.msdashboard.LandscapeWatcher;
import be.ordina.msdashboard.events.HealthInfoRetrievalFailed;
import be.ordina.msdashboard.events.HealthInfoRetrieved;
import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;

/**
 * Unit test for the HealthAggregator.
 *
 * @author Steve De Zitter
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthAggregatorTest {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Mock
	private WebClient webClient;
	@Mock
	private LandscapeWatcher landscapeWatcher;
	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
	@Mock
	private WebClient.RequestHeadersSpec requestHeadersSpec;
	@Mock
	private WebClient.ResponseSpec responseSpec;

	@Captor
	private ArgumentCaptor<ApplicationEvent> applicationEventArgumentCaptor;

	@InjectMocks
	private HealthAggregator healthAggregator;

	@Before
	public void setupMocks() {
		when(this.webClient.get()).thenReturn(this.requestHeadersUriSpec);
		when(this.requestHeadersUriSpec.uri(any(URI.class))).thenReturn(this.requestHeadersSpec);
		when(this.requestHeadersSpec.retrieve()).thenReturn(this.responseSpec);
	}

	@Test
	public void shouldHandleApplicationInstanceEvent() {
		NewServiceInstanceDiscovered newServiceInstanceDiscovered =
				NewServiceInstanceDiscoveredMother.defaultNewServiceInstanceDiscovered();

		when(this.responseSpec.bodyToMono(HealthAggregator.HealthWrapper.class)).thenReturn(Mono.just(new HealthAggregator.HealthWrapper(Status.UP, new HashMap<>())));

		this.healthAggregator.handleApplicationInstanceEvent(newServiceInstanceDiscovered);

		assertHealthInfoRetrievalSucceeded((ServiceInstance) newServiceInstanceDiscovered.getSource());
	}

	@Test
	public void shouldHandleApplicationInstanceEventHandlesError() {
		NewServiceInstanceDiscovered newServiceInstanceDiscovered =
				NewServiceInstanceDiscoveredMother.defaultNewServiceInstanceDiscovered();

		when(this.responseSpec.bodyToMono(HealthAggregator.HealthWrapper.class)).thenReturn(Mono.error(new RuntimeException("OOPSIE!")));

		this.healthAggregator.handleApplicationInstanceEvent(newServiceInstanceDiscovered);

		assertHealthInfoRetrievalFailed((ServiceInstance) newServiceInstanceDiscovered.getSource());
	}

	private void assertHealthInfoRetrievalSucceeded(ServiceInstance serviceInstance) {
		verify(this.applicationEventPublisher).publishEvent(this.applicationEventArgumentCaptor.capture());

		assertThat(this.outputCapture.toString())
				.contains(String.format("Found health information for service [%s]", serviceInstance.getServiceId()));

		HealthInfoRetrieved healthInfoRetrieved = (HealthInfoRetrieved) this.applicationEventArgumentCaptor.getValue();
		assertThat(healthInfoRetrieved).isNotNull();
		assertThat(healthInfoRetrieved.getHealth()).isNotNull();
		assertThat(healthInfoRetrieved.getHealth().getStatus()).isEqualTo(Status.UP);
		assertThat(healthInfoRetrieved.getSource()).isEqualTo(serviceInstance);
	}

	private void assertHealthInfoRetrievalFailed(ServiceInstance serviceInstance) {
		verify(this.applicationEventPublisher).publishEvent(this.applicationEventArgumentCaptor.capture());

		assertThat(this.outputCapture.toString()).contains(
				String.format("Could not retrieve health information for [http://%s:%d/actuator/health]",
						serviceInstance.getHost(), serviceInstance.getPort()));

		HealthInfoRetrievalFailed healthInfoRetrievalFailed = (HealthInfoRetrievalFailed) this.applicationEventArgumentCaptor.getValue();
		assertThat(healthInfoRetrievalFailed).isNotNull();
		assertThat(healthInfoRetrievalFailed.getSource()).isEqualTo(serviceInstance);
	}

	@Test
	public void shouldAggregateHealthInformation() {
		DefaultServiceInstance serviceInstanceA = new DefaultServiceInstance("a", "hosta", 8080, false);
		DefaultServiceInstance serviceInstanceB = new DefaultServiceInstance("b", "hostb", 8080, false);

		DefaultServiceInstance serviceInstanceC = new DefaultServiceInstance("c", "hostc", 8080, false);
		DefaultServiceInstance serviceInstanceD = new DefaultServiceInstance("d", "hostd", 8080, false);

		Map<String, List<ServiceInstance>> services = new HashMap<>();
		services.put("MovieService", Arrays.asList(serviceInstanceA, serviceInstanceB));
		services.put("OtherService", Arrays.asList(serviceInstanceC, serviceInstanceD));

		when(this.landscapeWatcher.getServiceInstances()).thenReturn(services);
		when(this.responseSpec.bodyToMono(HealthAggregator.HealthWrapper.class)).thenReturn(Mono.just(new HealthAggregator.HealthWrapper(Status.UP, new HashMap<>())));

		this.healthAggregator.aggregateHealthInformation();

		assertHealthInfoRetrievalSucceeded(services);
	}

	private void assertHealthInfoRetrievalSucceeded(Map<String, List<ServiceInstance>> serviceInstances) {
		List<ServiceInstance> allServiceInstances =
				serviceInstances.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(toList());

		assertHealthInfoRetrievalSucceeded(allServiceInstances);
	}

	private void assertHealthInfoRetrievalSucceeded(List<ServiceInstance> serviceInstances) {
		assertThat(this.outputCapture.toString()).contains("Aggregating [HEALTH] information");

		verify(this.applicationEventPublisher, times(serviceInstances.size()))
				.publishEvent(this.applicationEventArgumentCaptor.capture());

		List<HealthInfoRetrieved> healthInfoRetrievals = (List) this.applicationEventArgumentCaptor.getAllValues();

		healthInfoRetrievals.forEach(healthInfoRetrieved -> {
			ServiceInstance serviceInstance = (ServiceInstance) healthInfoRetrieved.getSource();

			assertThat(healthInfoRetrieved).isNotNull();
			assertThat(healthInfoRetrieved.getHealth()).isNotNull();
			assertThat(healthInfoRetrieved.getHealth().getStatus()).isEqualTo(Status.UP);
			assertThat(serviceInstances).contains(serviceInstance);
		});
	}

}
