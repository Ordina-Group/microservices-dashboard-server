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

import be.ordina.msdashboard.model.Node;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexToNodeConverterActuatorMappingsTest {

    private IndexToNodeConverter indexToNodeConverter;

    @Before
    public void setUp() {
        indexToNodeConverter = new IndexToNodeConverterActuatorMappingsImpl();
    }

    @Test
    public void malformedJsonShouldReturnZeroNodes() throws InterruptedException {
        List<Node> nodes = convertSource("");
        assertThat(nodes).hasSize(0);
    }

    @Test
    public void noHALLinksShouldReturnZeroNodes() throws InterruptedException {
        List<Node> nodes = convertSource("{}");
        assertThat(nodes).hasSize(0);
    }

    public static final String PAYLOAD ="{\"/webjars/**\":{\"bean\":\"resourceHandlerMapping\"},\"/**\":{\"bean\":\"resourceHandlerMapping\"},\"/**/favicon.ico\":{\"bean\":\"faviconHandlerMapping\"},\"{[/error]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String, java.lang.Object>> org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)\"},\"{[/error],produces=[text/html]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public org.springframework.web.servlet.ModelAndView org.springframework.boot.autoconfigure.web.BasicErrorController.errorHtml(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)\"},\"{[/events],methods=[GET]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public java.util.Collection<be.ordina.msdashboard.events.SystemEvent> be.ordina.msdashboard.controllers.EventsController.getAllNodes()\"},\"{[/events],methods=[DELETE]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.EventsController.deleteAllNodes()\"},\"{[/flush],methods=[DELETE]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.NodesController.flushAll()\"},\"{[/node],methods=[POST]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.NodesController.saveNode(java.lang.String)\"},\"{[/graph],produces=[application/json]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public org.springframework.http.HttpEntity<java.util.Map<java.lang.String, java.lang.Object>> be.ordina.msdashboard.controllers.NodesController.getDependenciesGraphJson()\"},\"{[/node/{nodeId}],methods=[DELETE]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.NodesController.deleteNode(java.lang.String)\"},\"{[/evictCache],methods=[POST]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.NodesController.evictCache()\"},\"{[/node],methods=[GET]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public java.util.Collection<be.ordina.msdashboard.model.Node> be.ordina.msdashboard.controllers.NodesController.getAllNodes()\"},\"{[/node],methods=[DELETE]}\":{\"bean\":\"requestMappingHandlerMapping\",\"method\":\"public void be.ordina.msdashboard.controllers.NodesController.deleteAllNodes()\"},\"{[/env],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.cloud.context.environment.EnvironmentManagerMvcEndpoint.value(java.util.Map<java.lang.String, java.lang.String>)\"},\"{[/env/reset],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.util.Map<java.lang.String, java.lang.Object> org.springframework.cloud.context.environment.EnvironmentManagerMvcEndpoint.reset()\"},\"{[/pause || /pause.json],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.cloud.endpoint.GenericPostableMvcEndpoint.invoke()\"},\"{[/beans || /beans.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/jolokia/**]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public org.springframework.web.servlet.ModelAndView org.springframework.boot.actuate.endpoint.mvc.JolokiaMvcEndpoint.handle(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse) throws java.lang.Exception\"},\"{[/trace || /trace.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/dump || /dump.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/metrics/{name:.*}],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint.value(java.lang.String)\"},\"{[/metrics || /metrics.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/resume || /resume.json],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.cloud.endpoint.GenericPostableMvcEndpoint.invoke()\"},\"{[/env/{name:.*}],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint.value(java.lang.String)\"},\"{[/env || /env.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/configprops || /configprops.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/autoconfig || /autoconfig.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/archaius || /archaius.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/health || /health.json],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint.invoke(java.security.Principal)\"},\"{[/refresh || /refresh.json],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.cloud.endpoint.GenericPostableMvcEndpoint.invoke()\"},\"{[/mappings || /mappings.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/restart || /restart.json],methods=[POST]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.cloud.context.restart.RestartMvcEndpoint.invoke()\"},\"{[/info || /info.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"},\"{[/features || /features.json],methods=[GET],produces=[application/json]}\":{\"bean\":\"endpointHandlerMapping\",\"method\":\"public java.lang.Object org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter.invoke()\"}}";

    @Test
    public void linksWithoutCuriesShouldReturnThreeSimpleNodes() throws InterruptedException {
        List<Node> nodes = convertSource(PAYLOAD);
        assertThat(nodes).hasSize(36);

        Iterator<Node> iterator = nodes.iterator();
        Node serviceNode = iterator.next();
        assertThat(serviceNode.getId()).isEqualTo("service");
        assertThat(serviceNode.getLinkedFromNodeIds()).contains("service:pause", "service:configprops");

        checkResource(iterator.next(), "service:events", "http://host0015.local:8301/events");
        checkResource(iterator.next(), "service:metrics", "http://host0015.local:8301/metrics");
    }

    @Test
    public void linksWithEmptyCuriesArrayShouldReturnThreeSimpleNodes() throws InterruptedException {
        List<Node> nodes = convertSource("{" +
                "  \"_links\": {\n" +
                "    \"svc1:svc1rsc1\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc1\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"svc1:svc1rsc2\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc2\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"curies\": [\n" +
                "    ]\n" +
                "  }\n" +
                "}");
        assertThat(nodes).hasSize(3);

        Iterator<Node> iterator = nodes.iterator();
        Node serviceNode = iterator.next();
        assertThat(serviceNode.getId()).isEqualTo("service");
        assertThat(serviceNode.getLinkedFromNodeIds()).contains("svc1:svc1rsc1", "svc1:svc1rsc2");

        checkResource(iterator.next(), "svc1:svc1rsc1", "http://host0015.local:8301/svc1rsc1");
        checkResource(iterator.next(), "svc1:svc1rsc2", "http://host0015.local:8301/svc1rsc2");

    }

    @Test
    public void linksWithCuriesWithMissingNamespaceShouldReturnThreeSimpleNodes() throws InterruptedException {
        List<Node> nodes = convertSource("{" +
                "  \"_links\": {\n" +
                "    \"svc1:svc1rsc1\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc1\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"svc1:svc1rsc2\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc2\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"curies\": [\n" +
                "      {\n" +
                "        \"href\": \"/generated-docs/api-guide.html#resources-{rel}\",\n" +
                "        \"name\": \"svc2\",\n" +
                "        \"templated\": true\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
        assertThat(nodes).hasSize(1);

        Iterator<Node> iterator = nodes.iterator();
        Node serviceNode = iterator.next();
        assertThat(serviceNode.getId()).isEqualTo("service");
        assertThat(serviceNode.getLinkedFromNodeIds()).isEmpty();

    }

    @Test
    public void linksWithCuriesShouldReturnTwoNodesWithExtraDetails() throws InterruptedException {
        List<Node> nodes = convertSource("{" +
                "  \"_links\": {\n" +
                "    \"svc1:svc1rsc1\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc1\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"svc1:svc1rsc2\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc2\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"curies\": [\n" +
                "      {\n" +
                "        \"href\": \"/generated-docs/api-guide.html#resources-{rel}\",\n" +
                "        \"name\": \"svc1\",\n" +
                "        \"templated\": true\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
        assertThat(nodes).hasSize(3);

        Iterator<Node> iterator = nodes.iterator();
        Node serviceNode = iterator.next();
        assertThat(serviceNode.getId()).isEqualTo("service");
        assertThat(serviceNode.getLinkedFromNodeIds()).contains("svc1:svc1rsc1", "svc1:svc1rsc2");

        checkResource(iterator.next(), "svc1:svc1rsc1", "http://host0015.local:8301/svc1rsc1", "serviceUri/generated-docs/api-guide.html#resources-svc1rsc1");
        checkResource(iterator.next(), "svc1:svc1rsc2", "http://host0015.local:8301/svc1rsc2", "serviceUri/generated-docs/api-guide.html#resources-svc1rsc2");
    }

    private List<Node> convertSource(String source) {
        TestSubscriber<Node> testSubscriber = new TestSubscriber<>();
        indexToNodeConverter.convert("service", "http://host0015.local:8301", source).toBlocking().subscribe(testSubscriber);
        testSubscriber.assertNoErrors();

        return testSubscriber.getOnNextEvents();
    }

    private void checkResource(Node resource, String id, String url) {
        assertThat(resource.getId()).isEqualTo(id);
        assertThat(resource.getLinkedToNodeIds()).contains("service");
        assertThat(resource.getLane()).isEqualTo(1);
        assertThat(resource.getDetails()).isNotEmpty();

        Map<String, Object> details = resource.getDetails();
        assertThat(details.get("status")).isEqualTo("UP");
        assertThat(details.get("type")).isEqualTo("RESOURCE");
        assertThat(details.get("url")).isEqualTo(url);
    }

    private void checkResource(Node resource, String id, String url, String docs) {
        checkResource(resource, id, url);

        Map<String, Object> details = resource.getDetails();
        assertThat(details.get("docs")).isEqualTo(docs);
    }


}
