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

import be.ordina.msdashboard.nodes.aggregators.pact.PactProperties;
import be.ordina.msdashboard.nodes.aggregators.pact.PactToNodeConverter;
import be.ordina.msdashboard.nodes.aggregators.pact.PactsAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for the pact aggregator.
 *
 * @author Andreas Evers
 */
@Configuration
public class PactAggregatorConfiguration {

    @Bean
    @ConditionalOnProperty("pact-broker.url")
    @ConditionalOnMissingBean
    public PactsAggregator pactsAggregator(PactToNodeConverter pactToNodeConverter, ApplicationEventPublisher publisher) {
        return new PactsAggregator(pactToNodeConverter, pactProperties(), publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public PactToNodeConverter pactToNodeConverter() {
        return new PactToNodeConverter();
    }

    @ConfigurationProperties("msdashboard.pact")
    @Bean
    public PactProperties pactProperties() {
        return new PactProperties();
    }
}
