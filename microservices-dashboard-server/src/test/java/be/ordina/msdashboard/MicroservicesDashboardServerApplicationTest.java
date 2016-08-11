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
package be.ordina.msdashboard;

import be.ordina.msdashboard.MicroservicesDashboardServerApplicationTest.TestMicroservicesDashboardServerApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import rx.plugins.DebugHook;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;
import rx.plugins.RxJavaPlugins;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static be.ordina.msdashboard.JsonHelper.load;
import static be.ordina.msdashboard.JsonHelper.removeBlankNodes;
import static be.ordina.msdashboard.graph.GraphRetriever.LINKS;
import static be.ordina.msdashboard.graph.GraphRetriever.NODES;
import static be.ordina.msdashboard.nodes.model.Node.ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for the Microservices Dashboard server application
 *
 * @author Andreas Evers
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = { "spring.cloud.config.enabled=false" },
        classes = { TestMicroservicesDashboardServerApplication.class, InMemoryMockedConfiguration.class })
public class MicroservicesDashboardServerApplicationTest {

    private static final Logger logger = LoggerFactory.getLogger(MicroservicesDashboardServerApplicationTest.class);

    @Value("${local.server.port}")
    private int port = 0;

    @Test
    public void contextLoads() {
        // Intentionally left empty
    }

    @Test
    public void exposesGraph() throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        @SuppressWarnings("rawtypes")
        ResponseEntity<String> graph = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/graph", String.class);
        long totalTime = System.currentTimeMillis() - startTime;
        assertThat(HttpStatus.OK).isEqualTo(graph.getStatusCode());
        String body = removeBlankNodes(graph.getBody());
        // logger.info("BODY: " + body);
        logger.info("Time spent waiting for /graph: " + totalTime);

        JSONAssert.assertEquals(removeBlankNodes(load("src/test/resources/MicroservicesDashboardServerApplicationTestGraphResponse.json")),
                body, JSONCompareMode.LENIENT);

        ObjectMapper m = new ObjectMapper();
        Map<String, List> r = m.readValue(body, Map.class);
        // printLinks(r);
        assertLinkBetweenIds(r, "svc1:svc1rsc1", "service1");
        assertLinkBetweenIds(r, "svc1:svc1rsc2", "service1");
        assertLinkBetweenIds(r, "svc1:svc1rsc3", "service1");
        assertLinkBetweenIds(r, "svc3:svc3rsc1", "service3");
        assertLinkBetweenIds(r, "svc3:svc3rsc2", "service3");
        assertLinkBetweenIds(r, "svc4:svc4rsc1", "service4");
        assertLinkBetweenIds(r, "svc4:svc4rsc2", "service4");
        assertLinkBetweenIds(r, "/endpoint1/", "service1");
        assertLinkBetweenIds(r, "/endpoint2", "service1");
        assertLinkBetweenIds(r, "/endpoint3/", "service3");
        assertLinkBetweenIds(r, "/endpoint4", "service3");
        assertLinkBetweenIds(r, "/endpoint5/", "service4");
        assertLinkBetweenIds(r, "service1", "backend2");
        assertLinkBetweenIds(r, "service1", "discoveryComposite");
        assertLinkBetweenIds(r, "service1", "backend1");
        assertLinkBetweenIds(r, "service1", "svc3:svc3rsc1");
        assertLinkBetweenIds(r, "service1", "svc4:svc4rsc1");
        assertLinkBetweenIds(r, "service3", "discoveryComposite");
        assertLinkBetweenIds(r, "service3", "backend1");
        assertLinkBetweenIds(r, "service3", "backend3");
        assertLinkBetweenIds(r, "service3", "backend4");
        assertLinkBetweenIds(r, "service4", "discoveryComposite");
        assertLinkBetweenIds(r, "service4", "backend4");
        assertLinkBetweenIds(r, "service4", "backend10");
        assertLinkBetweenIds(r, "service4", "loyalty-program");
        assertLinkBetweenIds(r, "service4", "backend9");
        assertLinkBetweenIds(r, "service4", "db");
        assertThat(((List<Map>) r.get(LINKS)).size()).isEqualTo(27);

        ResponseEntity<String> errors = new TestRestTemplate()
                .getForEntity("http://localhost:" + port + "/events", String.class);

        assertThat(HttpStatus.OK).isEqualTo(errors.getStatusCode());
        body = errors.getBody();
        body = body.replaceAll(", [c,C]ontent-[l,L]ength=[0-9]*", "");
        logger.info("BODY: " + body);
        JSONAssert.assertEquals(load("src/test/resources/MicroservicesDashboardServerApplicationTestEventsResponse.json"),
                body, JSONCompareMode.LENIENT);
    }

    private void printLinks(Map<String, List> r) {
        List<Object> nodes = (List<Object>) r.get(NODES);
        List<Map<String, Integer>> links = (List<Map<String, Integer>>) r.get(LINKS);
        for (Map<String, Integer> link : links) {
            int sourceIndex = link.get("source");
            int targetIndex = link.get("target");
            String sourceNodeId = (String) ((Map) nodes.get(sourceIndex)).get(ID);
            String targetNodeId = (String) ((Map) nodes.get(targetIndex)).get(ID);
            logger.info("Graph contains links between: " + sourceNodeId + " and " + targetNodeId);
        }
    }

    private static void assertLinkBetweenIds(Map<String, List> r, String source, String target) throws IOException {
        List<Object> nodes = (List<Object>) r.get(NODES);
        List<Map<String, Integer>> links = (List<Map<String, Integer>>) r.get(LINKS);
        int sourceId = -1;
        int targetId = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (((Map) nodes.get(i)).get(ID).equals(source)) {
                sourceId = i;
            } else if (((Map) nodes.get(i)).get(ID).equals(target)) {
                targetId = i;
            }
        }
        final int s = sourceId;
        final int t = targetId;
        assertThat(links.stream().anyMatch(link -> link.get("source") == s && link.get("target") == t)).isTrue();
    }

    @Configuration
    @EnableDiscoveryClient
    @EnableAutoConfiguration
    @EnableMicroservicesDashboardServer
    public static class TestMicroservicesDashboardServerApplication {

        private static final Logger logger = LoggerFactory.getLogger(TestMicroservicesDashboardServerApplication.class);

        public static void main(String[] args) {
            RxJavaPlugins.getInstance().registerObservableExecutionHook(new DebugHook(new DebugNotificationListener() {
                public Object onNext(DebugNotification n) {
                    logger.info("onNext on " + n);
                    return super.onNext(n);
                }

                public Object start(DebugNotification n) {
                    logger.info("start on " + n);
                    return super.start(n);
                }

                public void complete(Object context) {
                    logger.info("complete on " + context);
                }

                public void error(Object context, Throwable e) {
                    logger.error("error on " + context);
                }
            }));
        }
    }
}
