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
package be.ordina.msdashboard.nodes.aggregators.pact;

import be.ordina.msdashboard.nodes.model.Node;
import be.ordina.msdashboard.nodes.model.NodeTypes;
import be.ordina.msdashboard.nodes.model.SystemEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.client.HttpResponseHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PactsAggregator}
 *
 * @author Tim De Bruyn
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RxNetty.class})
public class PactsAggregatorTest {

    @Mock
    private PactProperties pactProperties;
    @Mock
    private ApplicationEventPublisher publisher;

	private PactsAggregator pactsAggregator;

    String onePactSource = "{\"pacts\": [{\"_links\": {\"self\":"
			+ " {\"title\": \"Pact\",\"name\": \"Pact between consumer2 (v1.0.0) and provider2\","
			+ "\"href\": \"http://someserver.be:7000/pacts/provider/provider2/consumer/consumer2/version/1.0.0\"}}}]}";

	String twoPactsSource = "{\"pacts\": [{\"_links\": {\"self\": {\"title\": \"Pact\",\"name\": \"Pact between consumer1 (v1.0.0) and provider1\","
			+ "\"href\": \"http://someserver.be:7000/pacts/provider/provider1/consumer/consumer1/version/1.0.0\"}}},{\"_links\": {\"self\":"
			+ " {\"title\": \"Pact\",\"name\": \"Pact between consumer2 (v1.0.0) and provider2\","
			+ "\"href\": \"http://someserver.be:7000/pacts/provider/provider2/consumer/consumer2/version/1.0.0\"}}}]}";

	String pactOne = "{\"provider\":{\"name\":\"provider1\"},\"consumer\":{\"name\":\"consumer1\"},\"interactions\":[{\"description\":"
			+ "\"A request to get the data of a customer\",\"request\":{\"method\":\"GET\",\"path\":"
			+ "\"/rel://pn:provider1\",\"headers\":{\"globalid\":\"12345\",\"accept\":"
			+ "\"application/vnd.pxs.provider1.v1+json;charset=UTF-8\"}},\"response\":"
			+ "{\"status\":200,\"headers\":{\"Content-Type\":\"application/vnd.pxs.provider1.v1+json;charset=UTF-8\"},\"body\":"
			+ "{\"currentPoints\":264355,\"_embedded\":{\"pn:provider1\":{\"providerNodeSpecificationId\":\"GLP_ID\"}}}}}],\"metadata\":"
			+ "{\"pact-specification\":{\"version\":\"3.0.0\"},\"pact-jvm\":{\"version\":\"2.3.3\"}},\"_links\":{\"self\":{\"title\":\"Pact\",\"name\":"
			+ "\"Pact between provider1 (v1.0.0) and consumer1\",\"href\":"
			+ "\"http://someServer.be:7000/pacts/provider/provider1/consumer/consumer1/version/1.0.0\"},\"curies\":[{\"name\":\"pb\",\"href\":"
			+ "\"http://el3101.bc:7000/doc/{rel}\",\"templated\":true}]}}";

	String pactTwo = "{\"provider\":{\"name\":\"provider2\"},\"consumer\":{\"name\":\"consumer2\"},\"interactions\":[{\"description\":"
			+ "\"A request to get the data of a customer\",\"request\":{\"method\":\"GET\",\"path\":"
			+ "\"/rel://pn:provider2\",\"headers\":{\"globalid\":\"12345\",\"accept\":"
			+ "\"application/vnd.pxs.provider2.v1+json;charset=UTF-8\"}},\"response\":"
			+ "{\"status\":200,\"headers\":{\"Content-Type\":\"application/vnd.pxs.provider2.v1+json;charset=UTF-8\"},\"body\":"
			+ "{\"currentPoints\":264355,\"_embedded\":{\"pn:provider2\":{\"providerNodeSpecificationId\":\"GLP_ID\"}}}}}],\"metadata\":"
			+ "{\"pact-specification\":{\"version\":\"3.0.0\"},\"pact-jvm\":{\"version\":\"2.3.3\"}},\"_links\":{\"self\":{\"title\":\"Pact\",\"name\":"
			+ "\"Pact between provider2 (v1.0.0) and consumer2\",\"href\":"
			+ "\"http://someServer.be:7000/pacts/provider/provider2/consumer/consumer2/version/1.0.0\"},\"curies\":[{\"name\":\"pb\",\"href\":"
			+ "\"http://el3101.bc:7000/doc/{rel}\",\"templated\":true}]}}";


    @Before
    public void setUp() {
        when(pactProperties.getRequestHeaders()).thenReturn(requestHeaders());
        pactsAggregator = new PactsAggregator(new PactToNodeConverter(), pactProperties, publisher);
        ReflectionTestUtils.setField(pactsAggregator, "selfHrefJsonPath", "$.pacts[*]._links.self.href");
        ReflectionTestUtils.setField(pactsAggregator, "pactBrokerUrl", "http://localhost:8089");
        ReflectionTestUtils.setField(pactsAggregator, "latestPactsUrl", "/pacts/latest");
        PowerMockito.mockStatic(RxNetty.class);
    }

    private Map<String,String> requestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/hal+json");
        headers.put("Accept-Language", "en-us,en;q=0.5");
        return headers;
    }

    @SuppressWarnings("unchecked")
	@Test
    public void shouldReturnTwoNodes() throws InterruptedException {

        HttpClientResponse<ByteBuf> urlsResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf, twoPactsSource);
        when(urlsResponse.getContent()).thenReturn(Observable.just(byteBuf));
        when(urlsResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        HttpClientResponse<ByteBuf> pactOneResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf2 = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf2, pactOne);
        when(pactOneResponse.getContent()).thenReturn(Observable.just(byteBuf2));
        when(pactOneResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        HttpClientResponse<ByteBuf> pactTwoResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf3 = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf3, pactTwo);
        when(pactTwoResponse.getContent()).thenReturn(Observable.just(byteBuf3));
        when(pactTwoResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        when(RxNetty.createHttpRequest(any(HttpClientRequest.class)))
        	.thenReturn(Observable.just(urlsResponse), Observable.just(pactOneResponse), Observable.just(pactTwoResponse));


        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(2);

		assertThat(nodes.get(0).getId()).isEqualTo("consumer1");
		assertThat(nodes.get(0).getLane()).isEqualTo(0);
		assertThat(nodes.get(0).getLinkedToNodeIds()).contains("pn:provider1");
		assertThat(nodes.get(0).getDetails().get("url")).isEqualTo("http://someserver.be:7000/pacts/provider/provider1/consumer/consumer1/version/1.0.0");
		assertThat(nodes.get(0).getDetails().get("type")).isEqualTo(NodeTypes.UI_COMPONENT);
		assertThat(nodes.get(0).getDetails().get("status")).isEqualTo("UP");

		assertThat(nodes.get(1).getId()).isEqualTo("consumer2");
		assertThat(nodes.get(1).getLane()).isEqualTo(0);
		assertThat(nodes.get(1).getLinkedToNodeIds()).contains("pn:provider2");
		assertThat(nodes.get(1).getDetails().get("url")).isEqualTo("http://someserver.be:7000/pacts/provider/provider2/consumer/consumer2/version/1.0.0");
		assertThat(nodes.get(1).getDetails().get("type")).isEqualTo(NodeTypes.UI_COMPONENT);
		assertThat(nodes.get(1).getDetails().get("status")).isEqualTo("UP");
    }

    @SuppressWarnings("unchecked")
 	@Test
     public void shouldReturnOneNode() throws InterruptedException {

        HttpClientResponse<ByteBuf> urlsResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf, onePactSource);
        when(urlsResponse.getContent()).thenReturn(Observable.just(byteBuf));
        when(urlsResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        HttpClientResponse<ByteBuf> pactTwoResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf3 = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf3, pactTwo);
        when(pactTwoResponse.getContent()).thenReturn(Observable.just(byteBuf3));
        when(pactTwoResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        when(RxNetty.createHttpRequest(any(HttpClientRequest.class)))
         	.thenReturn(Observable.just(urlsResponse), Observable.just(pactTwoResponse));


        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).hasSize(1);

 		assertThat(nodes.get(0).getId()).isEqualTo("consumer2");
 		assertThat(nodes.get(0).getLane()).isEqualTo(0);
 		assertThat(nodes.get(0).getLinkedToNodeIds()).contains("pn:provider2");
 		assertThat(nodes.get(0).getDetails().get("url")).isEqualTo("http://someserver.be:7000/pacts/provider/provider2/consumer/consumer2/version/1.0.0");
 		assertThat(nodes.get(0).getDetails().get("type")).isEqualTo(NodeTypes.UI_COMPONENT);
 		assertThat(nodes.get(0).getDetails().get("status")).isEqualTo("UP");
     }


    @SuppressWarnings("unchecked")
  	@Test
    public void getPactUrlsNotFound() throws InterruptedException {

         HttpClientResponse<ByteBuf> urlsNotFoundResponse = mock(HttpClientResponse.class);
         when(urlsNotFoundResponse.getContent()).thenReturn(null);
         when(urlsNotFoundResponse.getStatus()).thenReturn(HttpResponseStatus.NOT_FOUND);
         HttpResponseHeaders httpResponseHeaders = mock(HttpResponseHeaders.class);
         when(httpResponseHeaders.entries()).thenReturn(newArrayList());
         when(urlsNotFoundResponse.getHeaders()).thenReturn(httpResponseHeaders);

         when(RxNetty.createHttpRequest(any(HttpClientRequest.class))).thenReturn(Observable.just(urlsNotFoundResponse));

         TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
         pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
         testSubscriber.assertNoErrors();

         List<Node> nodes = testSubscriber.getOnNextEvents();
         assertThat(nodes).isEmpty();

         verify(publisher).publishEvent(any(SystemEvent.class));
    }

    @SuppressWarnings("unchecked")
 	@Test
     public void nodeOneNotFound() throws InterruptedException {

        HttpClientResponse<ByteBuf> urlsResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf, onePactSource);
        when(urlsResponse.getContent()).thenReturn(Observable.just(byteBuf));
        when(urlsResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        HttpClientResponse<ByteBuf> pactNotFoundResponse = mock(HttpClientResponse.class);
        when(pactNotFoundResponse.getContent()).thenReturn(null);
        when(pactNotFoundResponse.getStatus()).thenReturn(HttpResponseStatus.NOT_FOUND);
        HttpResponseHeaders httpResponseHeaders = mock(HttpResponseHeaders.class);
        when(httpResponseHeaders.entries()).thenReturn(newArrayList());
        when(pactNotFoundResponse.getHeaders()).thenReturn(httpResponseHeaders);

        when(RxNetty.createHttpRequest(any(HttpClientRequest.class)))
         	.thenReturn(Observable.just(urlsResponse), Observable.just(pactNotFoundResponse));


        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        List<Node> nodes = testSubscriber.getOnNextEvents();
        assertThat(nodes).isEmpty();

        verify(publisher).publishEvent(any(SystemEvent.class));
     }

    @SuppressWarnings("unchecked")
	@Test
    public void onErrorWhenGettingPactsUrl(){
        when(RxNetty.createHttpRequest(any(HttpClientRequest.class)))
     		.thenReturn(Observable.error(new RuntimeException()));

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertError(RuntimeException.class);

        verify(publisher).publishEvent(any(SystemEvent.class));
    }

    @SuppressWarnings("unchecked")
	@Test
    public void onErrorWhenGettingNodeOne(){
        HttpClientResponse<ByteBuf> urlsResponse = mock(HttpClientResponse.class);
        ByteBuf byteBuf = (new PooledByteBufAllocator()).directBuffer();
        ByteBufUtil.writeUtf8(byteBuf, onePactSource);
        when(urlsResponse.getContent()).thenReturn(Observable.just(byteBuf));
        when(urlsResponse.getStatus()).thenReturn(HttpResponseStatus.OK);

        when(RxNetty.createHttpRequest(any(HttpClientRequest.class)))
         	.thenReturn(Observable.just(urlsResponse), Observable.error(new RuntimeException()));

        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        pactsAggregator.aggregateNodes().toBlocking().subscribe(testSubscriber);
        testSubscriber.assertError(RuntimeException.class);

        verify(publisher).publishEvent(any(SystemEvent.class));
    }
}
