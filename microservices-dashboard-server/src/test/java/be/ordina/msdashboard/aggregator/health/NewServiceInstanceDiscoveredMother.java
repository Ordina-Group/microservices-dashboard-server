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

import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;

import org.springframework.cloud.client.DefaultServiceInstance;

/**
 * Mother class to simplify instantiation of NewServiceInstanceDiscovered events for testing purposes.
 *
 * @author Steve De Zitter
 */
public final class NewServiceInstanceDiscoveredMother {
	private NewServiceInstanceDiscoveredMother() {
	}

	public static NewServiceInstanceDiscovered defaultNewServiceInstanceDiscovered() {
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance("a", "host", 8080, false);

		return new NewServiceInstanceDiscovered(serviceInstance);
	}
}
