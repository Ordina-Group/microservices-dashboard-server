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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ordina.msdashboard.events.NewServiceDiscovered;
import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;
import be.ordina.msdashboard.events.ServiceDeregistered;
import be.ordina.msdashboard.events.ServiceInstanceDeregistered;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.intersection;
import static org.apache.commons.collections4.CollectionUtils.subtract;

/**
 * Watcher that will dispatch events whenever there is a change in the landscape.
 *
 * @author Tim Ysewyn
 */
public class LandscapeWatcher {

	private static final Logger logger = LoggerFactory.getLogger(LandscapeWatcher.class);

	private final DiscoveryClient discoveryClient;
	private final ApplicationEventPublisher publisher;

	private final Map<String, List<ServiceInstance>> serviceInstances = new HashMap<>();

	private List<String> services = new ArrayList<>();

	public LandscapeWatcher(DiscoveryClient discoveryClient, ApplicationEventPublisher publisher) {
		this.discoveryClient = discoveryClient;
		this.publisher = publisher;
	}

	@EventListener({ApplicationStartedEvent.class})
	public void discoverLandscape() {
		logger.debug("Discovering landscape");
		this.services = this.discoveryClient.getServices();
		processNewServices(this.services);
	}

	@EventListener({HeartbeatEvent.class})
	public void checkForChangesInLandscape() {
		logger.debug("Checking for changes in landscape");
		List<String> retrievedServices = this.discoveryClient.getServices();
		processNewServices(subtract(retrievedServices, this.services));
		processDeletedServices(subtract(this.services, retrievedServices));
		processExistingServices(intersection(this.services, retrievedServices));
		this.services = retrievedServices;
	}

	private List<ServiceInstance> getInstances(String service) {
		return this.discoveryClient.getInstances(service)
				.stream()
				.map(ServiceInstanceWrapper::new)
				.collect(toList());
	}

	private void processNewServices(Collection<String> newServices) {
		logger.debug("Discovering new services");
		newServices.forEach(service -> {
			logger.debug("Registering new service '{}'", service);
			List<ServiceInstance> instances = this.getInstances(service);
			processNewServiceInstances(instances);
			this.serviceInstances.put(service, instances);
			this.publisher.publishEvent(new NewServiceDiscovered(service));
		});
	}

	private void processDeletedServices(Collection<String> deletedServices) {
		logger.debug("Checking deregistered services");
		deletedServices.forEach(service -> {
			logger.debug("Deregistering service '{}'", service);
			List<ServiceInstance> serviceInstances = this.serviceInstances.remove(service);
			processDeletedServiceInstances(serviceInstances);
			this.publisher.publishEvent(new ServiceDeregistered(service));
		});
	}

	private void processExistingServices(Collection<String> existingServices) {
		logger.debug("Checking for changes in known services");
		existingServices.forEach(service -> {
			List<ServiceInstance> retrievedInstances = this.getInstances(service);
			List<ServiceInstance> knownServiceInstances = this.serviceInstances.getOrDefault(service, emptyList());
			processNewServiceInstances(subtract(retrievedInstances, knownServiceInstances));
			processDeletedServiceInstances(subtract(knownServiceInstances, retrievedInstances));
			this.serviceInstances.put(service, retrievedInstances);
		});
	}

	private void processNewServiceInstances(Collection<ServiceInstance> newServiceInstances) {
		newServiceInstances.forEach(serviceInstance -> {
			logger.debug("Registering new service instance for '{}'", serviceInstance.getServiceId());
			this.publisher.publishEvent(new NewServiceInstanceDiscovered(serviceInstance));
		});
	}

	private void processDeletedServiceInstances(Collection<ServiceInstance> deletedServiceInstances) {
		deletedServiceInstances.forEach(serviceInstance -> {
			logger.debug("Deregistering service instance for '{}'", serviceInstance.getServiceId());
			this.publisher.publishEvent(new ServiceInstanceDeregistered(serviceInstance));
		});
	}

	public Map<String, List<ServiceInstance>> getServiceInstances() {
		return this.serviceInstances;
	}

	private static final class ServiceInstanceWrapper implements ServiceInstance {

		private final ServiceInstance delegate;

		private ServiceInstanceWrapper(ServiceInstance delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getServiceId() {
			return this.delegate.getServiceId();
		}

		@Override
		public String getHost() {
			return this.delegate.getHost();
		}

		@Override
		public int getPort() {
			return this.delegate.getPort();
		}

		@Override
		public boolean isSecure() {
			return this.delegate.isSecure();
		}

		@Override
		public URI getUri() {
			return this.delegate.getUri();
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.delegate.getMetadata();
		}

		@Override
		public String getScheme() {
			return this.delegate.getScheme();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ServiceInstance that = (ServiceInstance) o;
			return this.delegate.getPort() == that.getPort() &&
					this.delegate.isSecure() == that.isSecure() &&
					Objects.equals(this.delegate.getServiceId(), that.getServiceId()) &&
					Objects.equals(this.delegate.getHost(), that.getHost());
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.delegate.getServiceId(),
					this.delegate.getHost(),
					this.delegate.getPort(),
					this.delegate.isSecure());
		}
	}
}
