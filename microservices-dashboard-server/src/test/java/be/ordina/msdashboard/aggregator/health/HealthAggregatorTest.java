package be.ordina.msdashboard.aggregator.health;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.ordina.msdashboard.LandscapeWatcher;
import be.ordina.msdashboard.aggregator.health.events.HealthInfoFailed;
import be.ordina.msdashboard.aggregator.health.events.HealthInfoRetrieved;
import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;
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

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	public void setup() {
		when(this.webClient.get()).thenReturn(this.requestHeadersUriSpec);
		when(this.requestHeadersUriSpec.uri(any(URI.class))).thenReturn(this.requestHeadersSpec);
		when(this.requestHeadersSpec.retrieve()).thenReturn(this.responseSpec);
	}

	@Test
	public void shouldHandleApplicationInstanceEvent() {
		NewServiceInstanceDiscovered newServiceInstanceDiscovered = NewServiceInstanceDiscoveredMother.defaultNewServiceInstanceDiscovered();

		when(this.responseSpec.bodyToMono(HealthInfo.class)).thenReturn(Mono.just(HealthInfo.withStatus("UP")));

		healthAggregator.handleApplicationInstanceEvent(newServiceInstanceDiscovered);

		assertHealthInfoRetrievalSucceeded((ServiceInstance) newServiceInstanceDiscovered.getSource());
	}

	@Test
	public void shouldHandleApplicationInstanceEventHandlesError() {
		NewServiceInstanceDiscovered newServiceInstanceDiscovered = NewServiceInstanceDiscoveredMother.defaultNewServiceInstanceDiscovered();

		when(this.responseSpec.bodyToMono(HealthInfo.class)).thenReturn(Mono.error(new RuntimeException("OOPSIE!")));

		healthAggregator.handleApplicationInstanceEvent(newServiceInstanceDiscovered);

		assertHealthInfoRetrievalFailed((ServiceInstance) newServiceInstanceDiscovered.getSource());
	}

	private void assertHealthInfoRetrievalSucceeded(ServiceInstance serviceInstance) {
		verify(this.applicationEventPublisher).publishEvent(this.applicationEventArgumentCaptor.capture());

		assertThat(this.outputCapture.toString()).contains(String.format("Found health information for service [%s]", serviceInstance.getServiceId()));

		HealthInfoRetrieved healthInfoRetrieved = (HealthInfoRetrieved) this.applicationEventArgumentCaptor.getValue();
		assertThat(healthInfoRetrieved).isNotNull();
		assertThat(healthInfoRetrieved.getHealthInfo()).isNotNull();
		assertThat(healthInfoRetrieved.getHealthInfo().getStatus()).isEqualTo("UP");
		assertThat(healthInfoRetrieved.getSource()).isEqualTo(serviceInstance);
	}

	private void assertHealthInfoRetrievalFailed(ServiceInstance serviceInstance) {
		verify(this.applicationEventPublisher).publishEvent(this.applicationEventArgumentCaptor.capture());

		assertThat(this.outputCapture.toString()).contains(String.format("Could not retrieve health information for [http://%s:%d/actuator/health]", serviceInstance.getHost(), serviceInstance.getPort()));

		HealthInfoFailed healthInfoFailed = (HealthInfoFailed) this.applicationEventArgumentCaptor.getValue();
		assertThat(healthInfoFailed).isNotNull();
		assertThat(healthInfoFailed.getSource()).isEqualTo(serviceInstance);
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
		when(this.responseSpec.bodyToMono(HealthInfo.class)).thenReturn(Mono.just(HealthInfo.withStatus("UP")));

		healthAggregator.aggregateHealthInformation();

		assertHealthInfoRetrievalSucceeded(services);
	}

	private void assertHealthInfoRetrievalSucceeded(Map<String, List<ServiceInstance>> serviceInstances) {
		List<ServiceInstance> allServiceInstances = serviceInstances.entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(toList());

		assertHealthInfoRetrievalSucceeded(allServiceInstances);
	}

	private void assertHealthInfoRetrievalSucceeded(List<ServiceInstance> serviceInstances) {
		assertThat(this.outputCapture.toString()).contains("Aggregating [HEALTH] information");

		verify(this.applicationEventPublisher, times(serviceInstances.size())).publishEvent(this.applicationEventArgumentCaptor.capture());

		List<HealthInfoRetrieved> healthInfoRetrievals = (List) this.applicationEventArgumentCaptor.getAllValues();

		healthInfoRetrievals.forEach(healthInfoRetrieved -> {
			ServiceInstance serviceInstance = (ServiceInstance) healthInfoRetrieved.getSource();

			assertThat(healthInfoRetrieved).isNotNull();
			assertThat(healthInfoRetrieved.getHealthInfo()).isNotNull();
			assertThat(healthInfoRetrieved.getHealthInfo().getStatus()).isEqualTo("UP");
			assertThat(serviceInstances).contains(serviceInstance);
		});
	}
}