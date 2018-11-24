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

package be.ordina.msdashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import be.ordina.msdashboard.events.NewServiceDiscovered;
import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;
import be.ordina.msdashboard.events.ServiceDeregistered;
import be.ordina.msdashboard.events.ServiceInstanceDeregistered;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.mockito.BDDMockito.when;

/**
 * @author Tim Ysewyn
 */
@RunWith(MockitoJUnitRunner.class)
public class LandscapeWatcherTest {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Mock
	private DiscoveryClient discoveryClient;

	@Mock
	private ApplicationEventPublisher publisher;

	@InjectMocks
	private LandscapeWatcher landscapeWatcher;

	@Captor
	private ArgumentCaptor<ApplicationEvent> applicationEventArgumentCaptor;

	@Test
	public void shouldDispatchAllEvents() {
		when(this.discoveryClient.getServices()).thenReturn(Arrays.asList("a", "c"));
		when(this.discoveryClient.getInstances("a")).thenReturn(Collections.singletonList(new DefaultServiceInstance("a", "host", 8080, false)));
		when(this.discoveryClient.getInstances("c")).thenReturn(Collections.singletonList(new DefaultServiceInstance("c", "host1", 8080, false)));

		this.landscapeWatcher.discoverLandscape();

		when(this.discoveryClient.getServices()).thenReturn(Arrays.asList("b", "c"));
		when(this.discoveryClient.getInstances("b")).thenReturn(Collections.singletonList(new DefaultServiceInstance("b", "host", 8080, false)));
		when(this.discoveryClient.getInstances("c")).thenReturn(Collections.singletonList(new DefaultServiceInstance("c", "host2", 8080, false)));

		this.landscapeWatcher.checkForChangesInLandscape();

		verify(this.discoveryClient, times(2)).getServices();
		verify(this.discoveryClient).getInstances("a");
		verify(this.discoveryClient).getInstances("b");
		verify(this.discoveryClient, times(2)).getInstances("c");
		verifyNoMoreInteractions(this.discoveryClient);

		verify(this.publisher, times(10)).publishEvent(this.applicationEventArgumentCaptor.capture());
		verifyNoMoreInteractions(this.publisher);

		List<ApplicationEvent> applicationEvents = this.applicationEventArgumentCaptor.getAllValues();

		assertThat(applicationEvents).hasSize(10);

		List<NewServiceDiscovered> newServiceDiscoveredEvents = applicationEvents.stream().filter(e -> e instanceof NewServiceDiscovered)
				.map(e -> (NewServiceDiscovered) e)
				.collect(toList());
		List<NewServiceInstanceDiscovered> newServiceInstanceDiscoveredEvents = applicationEvents.stream().filter(e -> e instanceof NewServiceInstanceDiscovered)
				.map(e -> (NewServiceInstanceDiscovered) e)
				.collect(toList());
		List<ServiceDeregistered> serviceDeregisteredEvents = applicationEvents.stream().filter(e -> e instanceof ServiceDeregistered)
				.map(e -> (ServiceDeregistered) e)
				.collect(toList());
		List<ServiceInstanceDeregistered> serviceInstanceDeregisteredEvents = applicationEvents.stream().filter(e -> e instanceof ServiceInstanceDeregistered)
				.map(e -> (ServiceInstanceDeregistered) e)
				.collect(toList());

		assertThat(newServiceDiscoveredEvents).hasSize(3);
		assertThat(newServiceInstanceDiscoveredEvents).hasSize(4);
		assertThat(serviceDeregisteredEvents).hasSize(1);
		assertThat(serviceInstanceDeregisteredEvents).hasSize(2);

		this.outputCapture.expect(containsString("Discovering landscape"));
		this.outputCapture.expect(containsString("Discovering new services"));
		this.outputCapture.expect(containsString("Registering new service 'a'"));
		this.outputCapture.expect(containsString("Registering new service instance for 'a'"));
		this.outputCapture.expect(containsString("Registering new service 'c'"));
		this.outputCapture.expect(containsString("Registering new service instance for 'c'"));
		this.outputCapture.expect(containsString("Checking for changes in landscape"));
		this.outputCapture.expect(containsString("Registering new service 'b'"));
		this.outputCapture.expect(containsString("Registering new service instance for 'b'"));
		this.outputCapture.expect(containsString("Checking deregistered services"));
		this.outputCapture.expect(containsString("Deregistering service 'a'"));
		this.outputCapture.expect(containsString("Deregistering service instance for 'a'"));
		this.outputCapture.expect(containsString("Checking for changes in known services"));
		this.outputCapture.expect(containsString("Registering new service instance for 'c'"));
		this.outputCapture.expect(containsString("Deregistering service instance for 'c'"));
	}

}
