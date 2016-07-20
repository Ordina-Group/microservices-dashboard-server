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

import be.ordina.msdashboard.aggregators.ErrorHandler;
import be.ordina.msdashboard.aggregators.NettyServiceCaller;
import be.ordina.msdashboard.aggregators.NodeAggregator;
import be.ordina.msdashboard.aggregators.VirtualAndRealDependencyIntegrator;
import be.ordina.msdashboard.aggregators.health.HealthIndicatorsAggregator;
import be.ordina.msdashboard.aggregators.health.HealthProperties;
import be.ordina.msdashboard.aggregators.index.IndexProperties;
import be.ordina.msdashboard.aggregators.index.IndexToNodeConverterActuatorMappingsImpl;
import be.ordina.msdashboard.aggregators.index.IndexToNodeConverterHateoasImpl;
import be.ordina.msdashboard.aggregators.index.IndexesAggregator;
import be.ordina.msdashboard.aggregators.pact.PactProperties;
import be.ordina.msdashboard.aggregators.pact.PactsAggregator;
import be.ordina.msdashboard.cache.CacheCleaningBean;
import be.ordina.msdashboard.controllers.EventsController;
import be.ordina.msdashboard.controllers.NodesController;
import be.ordina.msdashboard.events.EventListener;
import be.ordina.msdashboard.graph.GraphRetriever;
import be.ordina.msdashboard.properties.Labels;
import be.ordina.msdashboard.stores.NodeStore;
import be.ordina.msdashboard.stores.SimpleStore;
import be.ordina.msdashboard.uriresolvers.ActuatorUriResolver;
import be.ordina.msdashboard.uriresolvers.DefaultUriResolver;
import be.ordina.msdashboard.uriresolvers.EurekaUriResolver;
import be.ordina.msdashboard.uriresolvers.UriResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Evers
 */
@Configuration
@EnableConfigurationProperties
@AutoConfigureAfter({ RedisConfiguration.class })
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    @ConditionalOnMissingBean
    public Labels labels() {
        return new Labels();
    }

    @Configuration
    @AutoConfigureAfter({ HealthConfiguration.class, IndexConfiguration.class, PactConfiguration.class })
    public static class GraphConfiguration {

        @Autowired
        private NodeStore nodeStore;
        @Autowired
        private CacheCleaningBean cacheCleaningBean;

        @Autowired(required = false)
        private List<NodeAggregator> aggregators = new ArrayList<>();

        @Bean
        @ConditionalOnMissingBean
        public GraphRetriever graphRetriever() {
            return new GraphRetriever(aggregators, nodeStore);
        }

        @Bean
        @ConditionalOnMissingBean
        public NodesController nodesController() {
            return new NodesController(graphRetriever(), nodeStore, cacheCleaningBean);
        }
    }

    @Configuration
    public static class EventsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public EventsController eventsController() {
            return new EventsController(eventListener());
        }

        @Bean
        @ConditionalOnMissingBean
        public EventListener eventListener() {
            return new EventListener();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorHandler errorHandler(ApplicationEventPublisher publisher) {
        return new ErrorHandler(publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public NettyServiceCaller nettyServiceCaller(ApplicationEventPublisher publisher) {
        return new NettyServiceCaller(errorHandler(publisher));
    }

    @Configuration
    @ConditionalOnProperty("eureka.client.serviceUrl.defaultZone")
    public static class IndexConfiguration {

        @Autowired
        private DiscoveryClient discoveryClient;
        @Autowired
        private NettyServiceCaller caller;

//        @Bean
//        @ConditionalOnMissingBean
//        public IndexesAggregator indexesAggregator(ApplicationEventPublisher publisher) {
//            return new IndexesAggregator(new IndexToNodeConverterHateoasImpl(), discoveryClient,
//                    new EurekaUriResolver(),
//                    indexProperties(), publisher, caller);
//        }

        @Bean
        @ConditionalOnMissingBean
        public IndexesAggregator indexesAggregatorUrlMappings(ApplicationEventPublisher publisher) {
            return new IndexesAggregator(new IndexToNodeConverterActuatorMappingsImpl(),
                    discoveryClient, new ActuatorUriResolver(), indexProperties(), publisher, caller);
        }

        @ConfigurationProperties("msdashboard.index")
        @Bean
        public IndexProperties indexProperties() {
            return new IndexProperties();
        }

        @Bean
        @ConditionalOnProperty("eureka.client.serviceUrl.defaultZone")
        @ConditionalOnMissingBean
        public UriResolver uriResolver() {
            return new EurekaUriResolver();
        }
    }

    @Configuration
    @ConditionalOnProperty("eureka.client.serviceUrl.defaultZone")
    public static class HealthConfiguration {

        @Autowired
        private DiscoveryClient discoveryClient;
        @Autowired
        private NettyServiceCaller caller;
        @Autowired
        private ErrorHandler errorHandler;

        @Bean
        @ConditionalOnMissingBean
        public HealthIndicatorsAggregator healthIndicatorsAggregator(ApplicationEventPublisher publisher) {
            return new HealthIndicatorsAggregator(discoveryClient, uriResolver(), healthProperties(), caller, errorHandler);
        }

        @ConfigurationProperties("msdashboard.health")
        @Bean
        public HealthProperties healthProperties() {
            return new HealthProperties();
        }

        @Bean
        @ConditionalOnProperty("eureka.client.serviceUrl.defaultZone")
        @ConditionalOnMissingBean
        public UriResolver uriResolver() {
            return new EurekaUriResolver();
        }
    }

    @Configuration
    public static class PactConfiguration {

        @ConfigurationProperties("msdashboard.pact")
        @Bean
        public PactProperties pactProperties() {
            return new PactProperties();
        }

        @Bean
        @ConditionalOnProperty("pact-broker.url")
        @ConditionalOnMissingBean
        public PactsAggregator pactsAggregator(ApplicationEventPublisher publisher) {
            return new PactsAggregator(pactProperties(), publisher);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public VirtualAndRealDependencyIntegrator virtualAndRealDependencyIntegrator() {
        return new VirtualAndRealDependencyIntegrator();
    }

    @Bean
    @ConditionalOnMissingBean
    public NodeStore nodeStore() {
        return new SimpleStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheCleaningBean cacheCleaningBean() {
        return null;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "eureka.client.serviceUrl.defaultZone", matchIfMissing = true)
    public UriResolver uriResolver() {
        return new DefaultUriResolver();
    }

}
