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

package be.ordina.msdashboard.autoconfigure;

import be.ordina.msdashboard.LandscapeWatcher;
import be.ordina.msdashboard.aggregator.health.HealthAggregator;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for the microservices dashboard server.
 *
 * @author Tim Ysewyn
 */
@Configuration
@AutoConfigureAfter({ CompositeDiscoveryClientAutoConfiguration.class })
public class MicroservicesDashboardServerAutoConfiguration {

	@Bean
	LandscapeWatcher landscapeWatcher(DiscoveryClient discoveryClient, ApplicationEventPublisher publisher) {
		return new LandscapeWatcher(discoveryClient, publisher);
	}

	@Bean
	public WebClient webClient() {
		return WebClient.create();
	}

	@Bean
	public HealthAggregator msHealthAggregator(LandscapeWatcher landscapeWatcher, WebClient webClient, ApplicationEventPublisher publisher) {
		return new HealthAggregator(landscapeWatcher, webClient, publisher);
	}

}
