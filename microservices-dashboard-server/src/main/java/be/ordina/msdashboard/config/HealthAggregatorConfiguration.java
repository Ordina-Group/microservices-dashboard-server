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
package be.ordina.msdashboard.config;

import be.ordina.msdashboard.nodes.aggregators.ErrorHandler;
import be.ordina.msdashboard.nodes.aggregators.NettyServiceCaller;
import be.ordina.msdashboard.nodes.aggregators.health.HealthIndicatorsAggregator;
import be.ordina.msdashboard.nodes.aggregators.health.HealthProperties;
import be.ordina.msdashboard.nodes.aggregators.health.HealthToNodeConverter;
import be.ordina.msdashboard.nodes.aggregators.health.MicroserviceGrouper;
import be.ordina.msdashboard.nodes.uriresolvers.EurekaUriResolver;
import be.ordina.msdashboard.nodes.uriresolvers.UriResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for the health aggregator.
 *
 * @author Andreas Evers
 */
@Configuration
@AutoConfigureAfter(DiscoveryClientConfiguration.class)
public class HealthAggregatorConfiguration {

    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private NettyServiceCaller caller;
    @Autowired
    private ErrorHandler errorHandler;
    @Autowired
    private UriResolver uriResolver;

    @Bean
    @ConditionalOnMissingBean
    public HealthIndicatorsAggregator healthIndicatorsAggregator(HealthToNodeConverter healthToNodeConverter) {
        return new HealthIndicatorsAggregator(discoveryClient, uriResolver, healthProperties(), caller, errorHandler, healthToNodeConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public HealthToNodeConverter healthToNodeConverter() {
        return new HealthToNodeConverter(healthProperties());
    }

    @ConfigurationProperties("msdashboard.health")
    @Bean
    public HealthProperties healthProperties() {
        return new HealthProperties();
    }

    // TODO: Incorporate this
    @ConfigurationProperties("msdashboard.health.toolbox")
    @Bean
    public MicroserviceGrouper springCloudEnricher() {
        return new MicroserviceGrouper();
    }
}
