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
import be.ordina.msdashboard.nodes.aggregators.mappings.MappingsAggregator;
import be.ordina.msdashboard.nodes.aggregators.mappings.MappingsProperties;
import be.ordina.msdashboard.nodes.uriresolvers.UriResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for the mappings aggregator.
 *
 * @author Andreas Evers
 */
@Configuration
@ConditionalOnSingleCandidate(DiscoveryClient.class)
@ConditionalOnProperty(value = "msdashboard.mappings.enabled", matchIfMissing = true)
@AutoConfigureAfter(DiscoveryClientConfiguration.class)
public class MappingsAggregatorConfiguration {

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
    public MappingsAggregator mappingsAggregator() {
        return new MappingsAggregator(discoveryClient, uriResolver, mappingsProperties(), caller, errorHandler);
    }

    @ConfigurationProperties("msdashboard.mappings")
    @Bean
    public MappingsProperties mappingsProperties() {
        return new MappingsProperties();
    }
}
