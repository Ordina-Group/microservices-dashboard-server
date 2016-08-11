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

import be.ordina.msdashboard.nodes.uriresolvers.EurekaUriResolver;
import be.ordina.msdashboard.nodes.uriresolvers.UriResolver;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for discoveryclient specific beans.
 *
 * @author Andreas Evers
 */
@Configuration
@ConditionalOnSingleCandidate(DiscoveryClient.class)
@AutoConfigureAfter({ NoopDiscoveryClientAutoConfiguration.class })
public class DiscoveryClientConfiguration {

    @Bean
    @ConditionalOnClass(name = "com.netflix.discovery.EurekaClient")
    @ConditionalOnMissingBean
    public UriResolver uriResolver() {
        return new EurekaUriResolver();
    }

}
