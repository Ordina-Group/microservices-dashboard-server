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

import be.ordina.msdashboard.cache.CacheProperties;
import be.ordina.msdashboard.cache.NodeCache;
import be.ordina.msdashboard.controllers.CacheController;
import be.ordina.msdashboard.controllers.EventsController;
import be.ordina.msdashboard.controllers.GraphController;
import be.ordina.msdashboard.graph.GraphProperties;
import be.ordina.msdashboard.graph.GraphRetriever;
import be.ordina.msdashboard.nodes.aggregators.ErrorHandler;
import be.ordina.msdashboard.nodes.aggregators.NettyServiceCaller;
import be.ordina.msdashboard.nodes.aggregators.NodeAggregator;
import be.ordina.msdashboard.nodes.stores.EventStore;
import be.ordina.msdashboard.nodes.stores.NodeStore;
import be.ordina.msdashboard.nodes.stores.SimpleStore;
import be.ordina.msdashboard.nodes.uriresolvers.DefaultUriResolver;
import be.ordina.msdashboard.nodes.uriresolvers.UriResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration for the main functionality of the microservices dashboard.
 *
 * @author Andreas Evers
 * @author Tim Ysewyn
 */
@Configuration
@EnableConfigurationProperties
@AutoConfigureAfter({ RedisConfiguration.class })
public class WebConfiguration extends WebMvcConfigurerAdapter {

    @ConditionalOnMissingBean
    @ConfigurationProperties("msdashboard.cache")
    @Bean
    public CacheProperties cacheProperties() {
        return new CacheProperties();
    }

    @Configuration
    public static class CacheConfiguration {

        @Autowired
        private CacheProperties cacheProperties;

        @Autowired
        private NodeCache nodeCache;

        @Bean
        @ConditionalOnMissingBean
        public CacheController cacheController() {
                return new CacheController(cacheProperties, nodeCache);
            }
    }

    @Configuration
    @AutoConfigureAfter({ HealthAggregatorConfiguration.class, IndexAggregatorConfiguration.class, PactAggregatorConfiguration.class })
    public static class GraphConfiguration {

        @Autowired
        private NodeStore nodeStore;

        @Autowired(required = false)
        private List<NodeAggregator> aggregators = new ArrayList<>();

        @Bean
        @ConditionalOnMissingBean
        public GraphRetriever graphRetriever() {
            return new GraphRetriever(aggregators, nodeStore, graphProperties());
        }

        @Bean
        @ConditionalOnMissingBean
        public GraphController graphController() {
            return new GraphController(graphRetriever(), nodeStore);
        }

        @ConfigurationProperties("msdashboard.graph")
        @Bean
        public GraphProperties graphProperties() {
            return new GraphProperties();
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
        public EventStore eventListener() {
            return new EventStore();
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

    @Bean
    @ConditionalOnMissingBean
    public NodeStore nodeStore() {
        return new SimpleStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public NodeCache nodeCache() {
        return null;
    }

    @Configuration
    @AutoConfigureOrder
    public static class UriResolverFallbackConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public UriResolver uriResolver() {
            return new DefaultUriResolver();
        }
    }

}
