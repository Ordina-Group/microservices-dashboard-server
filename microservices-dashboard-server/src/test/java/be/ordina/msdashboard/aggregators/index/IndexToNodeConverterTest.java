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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import rx.observers.TestSubscriber;
import be.ordina.msdashboard.model.Node;

/**
 * Tests for {@link IndexToNodeConverter}
 *
 * @author Tim Ysewyn
 */
public class IndexToNodeConverterTest {

    private IndexToNodeConverter indexToNodeConverter;

    @Before
    public void setUp() {
        indexToNodeConverter = new IndexToNodeConverterHateoasImpl();
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

    @Test
    public void linksWithoutCuriesShouldReturnThreeSimpleNodes() throws InterruptedException {
        List<Node> nodes = convertSource("{" +
                "  \"_links\": {\n" +
                "    \"svc1:svc1rsc1\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc1\",\n" +
                "      \"templated\": true\n" +
                "    },\n" +
                "    \"svc1:svc1rsc2\": {\n" +
                "      \"href\": \"http://host0015.local:8301/svc1rsc2\",\n" +
                "      \"templated\": true\n" +
                "    }\n" +
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
        assertThat(nodes).hasSize(3);

        Iterator<Node> iterator = nodes.iterator();
        Node serviceNode = iterator.next();
        assertThat(serviceNode.getId()).isEqualTo("service");
        assertThat(serviceNode.getLinkedFromNodeIds()).contains("svc1:svc1rsc1", "svc1:svc1rsc2");

        checkResource(iterator.next(), "svc1:svc1rsc1", "http://host0015.local:8301/svc1rsc1");
        checkResource(iterator.next(), "svc1:svc1rsc2", "http://host0015.local:8301/svc1rsc2");
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
        indexToNodeConverter.convert("service", "serviceUri", source).toBlocking().subscribe(testSubscriber);
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
