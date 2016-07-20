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
package be.ordina.msdashboard.aggregators.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationEventPublisher;

import rx.Observable;
import rx.observers.TestSubscriber;
import be.ordina.msdashboard.aggregators.NettyServiceCaller;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import be.ordina.msdashboard.uriresolvers.DefaultUriResolver;

/**
 * Tests for {@link IndexesAggregator}
 *
 * @author Tim Ysewyn
 */
@RunWith(PowerMockRunner.class)
public class IndexesAggregatorTest {

    private DiscoveryClient discoveryClient;
    private IndexToNodeConverter indexToNodeConverter;
    private IndexProperties indexProperties;
    private ApplicationEventPublisher publisher;
    private NettyServiceCaller caller;
    private IndexesAggregator indexesAggregator;

    @Before
    public void setUp() {
        discoveryClient = mock(DiscoveryClient.class);
        indexToNodeConverter = mock(IndexToNodeConverterHateoasImpl.class);
        indexProperties = mock(IndexProperties.class);
        caller = mock(NettyServiceCaller.class);
        publisher = mock(ApplicationEventPublisher.class);
        indexesAggregator = new IndexesAggregator(indexToNodeConverter, discoveryClient, new DefaultUriResolver(), indexProperties, publisher, caller);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnThreeNodes() throws InterruptedException {
        when(discoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
        ServiceInstance instance = mock(ServiceInstance.class);
        when(discoveryClient.getInstances("service")).thenReturn(Collections.singletonList(instance));

        when(instance.getServiceId()).thenReturn("service");
        when(instance.getUri()).thenReturn(URI.create("http://localhost:8089/service"));

        Map<String, String> properties = new HashMap<>();
        properties.put("header", "value");
        when(indexProperties.getRequestHeaders()).thenReturn(properties);

        Map<String, Object> indexCallResult = Collections.singletonMap("", "");
        when(caller.retrieveJsonFromRequest(eq("service"), any(HttpClientRequest.class))).thenReturn(Observable.just(indexCallResult));

        Node node = new NodeBuilder().withId("service").build();
        when(indexToNodeConverter.convert(eq("service"), eq("http://localhost:8089/service"), any(JSONObject.class))).thenReturn(Observable.just(node));

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        indexesAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        //testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(1);

        assertThat(nodes.get(0).getId()).isEqualTo("service");
    }

    @Test
    public void noServicesShouldReturnZeroNodes() throws InterruptedException {
        when(discoveryClient.getServices()).thenReturn(Collections.emptyList());

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        indexesAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(0);
    }

    @Test
    public void noInstancesShouldReturnZeroNodes() throws InterruptedException {
        when(discoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
        when(discoveryClient.getInstances("service")).thenReturn(Collections.emptyList());

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        indexesAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void emptyObservableFromIndexCallShouldReturnZeroNodes() throws InterruptedException {
        when(discoveryClient.getServices()).thenReturn(Collections.singletonList("service"));
        ServiceInstance instance = mock(ServiceInstance.class);
        when(discoveryClient.getInstances("service")).thenReturn(Collections.singletonList(instance));

        when(instance.getServiceId()).thenReturn("service");
        when(instance.getUri()).thenReturn(URI.create("http://localhost:8089/service"));

        when(caller.retrieveJsonFromRequest(eq("service"), any(HttpClientRequest.class))).thenReturn(Observable.empty());

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        indexesAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(0);
    }

}
