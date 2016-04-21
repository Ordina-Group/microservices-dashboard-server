package be.ordina.msdashboard.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;

import static be.ordina.msdashboard.constants.Constants.DETAILS;
import static be.ordina.msdashboard.constants.Constants.ID;
import static be.ordina.msdashboard.constants.Constants.LANE;
import static be.ordina.msdashboard.constants.Constants.MICROSERVICE;
import static be.ordina.msdashboard.constants.Constants.STATUS;
import static be.ordina.msdashboard.constants.Constants.TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.ordina.msdashboard.aggregator.health.HealthIndicatorsAggregator;
import be.ordina.msdashboard.aggregator.index.IndexesAggregator;
import be.ordina.msdashboard.aggregator.pact.PactsAggregator;
import be.ordina.msdashboard.model.NodeBuilder;
import be.ordina.msdashboard.services.RedisService;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import be.ordina.msdashboard.model.Node;

@RunWith(MockitoJUnitRunner.class)
public class DependenciesGraphResourceJsonBuilderTest {

	@InjectMocks
	private DependenciesGraphResourceJsonBuilder dependenciesGraphResourceJsonBuilder;

	@Spy
	private ObjectMapper primaryObjectMapper;

	@Mock
	private HealthIndicatorsAggregator healthIndicatorsAggregator;

	@Mock
	private IndexesAggregator indexesAggregator;

	@Mock
	private RedisService redisService;

	@Mock
	private PactsAggregator pactsAggregator;

	@Mock
	private VirtualAndRealDependencyIntegrator virtualAndRealDependencyIntegrator;

	@SuppressWarnings("unchecked")
	@Test
	public void testBuild() throws Exception {
		List<Node> microservicesAndBackends = Lists.newArrayList(
				NodeBuilder.node().withId("key1")
						.withDetail("type", MICROSERVICE)
						.withDetail(STATUS, "UP")
						.withLinkedNode(NodeBuilder.node().withId("1a").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("1b").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("1c").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.build(),

				NodeBuilder.node().withId("key2")
						.withDetail("type", MICROSERVICE)
						.withDetail(STATUS, "UP")
						.withLinkedNode(NodeBuilder.node().withId("2a").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("2b").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("2c").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.build(),
				NodeBuilder.node().withId("key3")
						.withDetail("type", MICROSERVICE)
						.withDetail(STATUS, "UP")
						.withLinkedNode(NodeBuilder.node().withId("3a").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("3b").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.withLinkedNode(NodeBuilder.node().withId("3c").withDetail(STATUS, "DOWN").withDetail("type", "SOAP").build())
						.build()
		);
	Node dependencies = NodeBuilder.node().havingLinkedNodes(microservicesAndBackends).build();
		doReturn(Lists.newArrayList(new Node())).when(redisService).getAllNodes();
		doReturn(dependencies.getLinkedNodes()).when(virtualAndRealDependencyIntegrator).integrateVirtualNodesWithReal(anyListOf(Node.class), anyListOf(Node.class), anyListOf(Node.class));
		doReturn(dependencies).when(healthIndicatorsAggregator).fetchCombinedDependencies();
		doReturn(new Node()).when(indexesAggregator).fetchIndexes();
		doReturn(new Node()).when(pactsAggregator).fetchUIComponents();


		Map<String, Object> returnedMap = dependenciesGraphResourceJsonBuilder.build();

		assertThat(((boolean) returnedMap.get("directed"))).isEqualTo(true);
		assertThat(((boolean) returnedMap.get("multigraph"))).isEqualTo(false);
		assertThat(((String[]) returnedMap.get("graph")).length).isEqualTo(0);

		List<Map<String, Object>> expectedNodeList = getExpectedNodesList();

		List<Map<String, String>> returnedNodeList = (List<Map<String, String>>) returnedMap.get("nodes");

		assertThat(CollectionUtils.isEqualCollection(expectedNodeList, returnedNodeList)).isTrue();

		List<Map<String, Integer>> expectedLinks = getExpectedLinks();
		List<Map<String, Integer>> returnedLinks = (List<Map<String, Integer>>) returnedMap.get("links");
		assertThat(CollectionUtils.isEqualCollection(expectedLinks, returnedLinks)).isTrue();

	}

	@Test
	public void shouldDetermineCorrectLine() {
		Map<String, Object> details = new HashMap<>();
		details.put(TYPE, MICROSERVICE);
		assertThat(dependenciesGraphResourceJsonBuilder.determineLane(details)).isEqualTo(2);
		details.put(TYPE, "SOAP");
		assertThat(dependenciesGraphResourceJsonBuilder.determineLane(details)).isEqualTo(3);
	}

	@Test
	public void shouldCreateCorrectNode() {
		Node node = new Node();
		node.getDetails().put(STATUS, "UP");
		node.getDetails().put(TYPE, MICROSERVICE);
		Map<String, Object> microserviceNode = dependenciesGraphResourceJsonBuilder.createMicroserviceNode("Awards", node);
		assertThat(microserviceNode.get(ID)).isEqualTo("Awards");
		assertThat(microserviceNode.get(LANE)).isEqualTo(2);
		assertThat(((Map<String, Object>) microserviceNode.get(DETAILS)).get(STATUS)).isEqualTo("UP");
		assertThat(((Map<String, Object>) microserviceNode.get(DETAILS)).get(TYPE)).isEqualTo(MICROSERVICE);
	}

	private List<Map<String, Object>> getExpectedNodesList() {
		List<Map<String, Object>> expectedNodeList = new ArrayList<>();
		Map<String, Object> microserviceDetails = new HashMap<>();
		microserviceDetails.put("type", MICROSERVICE);
		microserviceDetails.put(STATUS, "UP");

		Map<String, Object> backendDetails = new HashMap<>();
		backendDetails.put(STATUS, "DOWN");
		backendDetails.put("type", "SOAP");

		Map<String, Object> node1 = new HashMap<>();
		node1.put(ID, "key1");
		node1.put(LANE, 2);
		node1.put(DETAILS, microserviceDetails);
		Map<String, Object> node2 = new HashMap<>();
		node2.put(ID, "1c");
		node2.put(LANE, 3);
		node2.put(DETAILS, backendDetails);
		Map<String, Object> node3 = new HashMap<>();
		node3.put(ID, "1b");
		node3.put(LANE, 3);
		node3.put(DETAILS, backendDetails);
		Map<String, Object> node4 = new HashMap<>();
		node4.put(ID, "1a");
		node4.put(LANE, 3);
		node4.put(DETAILS, backendDetails);
		Map<String, Object> node5 = new HashMap<>();
		node5.put(ID, "key2");
		node5.put(LANE, 2);
		node5.put(DETAILS, microserviceDetails);
		Map<String, Object> node6 = new HashMap<>();
		node6.put(ID, "2a");
		node6.put(LANE, 3);
		node6.put(DETAILS, backendDetails);
		Map<String, Object> node7 = new HashMap<>();
		node7.put(ID, "2c");
		node7.put(LANE, 3);
		node7.put(DETAILS, backendDetails);
		Map<String, Object> node8 = new HashMap<>();
		node8.put(ID, "2b");
		node8.put(LANE, 3);
		node8.put(DETAILS, backendDetails);
		Map<String, Object> node9 = new HashMap<>();
		node9.put(ID, "key3");
		node9.put(LANE, 2);
		node9.put(DETAILS, microserviceDetails);
		Map<String, Object> node10 = new HashMap<>();
		node10.put(ID, "3b");
		node10.put(LANE, 3);
		node10.put(DETAILS, backendDetails);
		Map<String, Object> node11 = new HashMap<>();
		node11.put(ID, "3a");
		node11.put(LANE, 3);
		node11.put(DETAILS, backendDetails);
		Map<String, Object> node12 = new HashMap<>();
		node12.put(ID, "3c");
		node12.put(LANE, 3);
		node12.put(DETAILS, backendDetails);
		expectedNodeList.add(node1);
		expectedNodeList.add(node2);
		expectedNodeList.add(node3);
		expectedNodeList.add(node4);
		expectedNodeList.add(node5);
		expectedNodeList.add(node6);
		expectedNodeList.add(node7);
		expectedNodeList.add(node8);
		expectedNodeList.add(node9);
		expectedNodeList.add(node10);
		expectedNodeList.add(node11);
		expectedNodeList.add(node12);
		return expectedNodeList;
	}

	private List<Map<String, Integer>> getExpectedLinks() {
		List<Map<String, Integer>> expectedLinks = new ArrayList<>();
		Map<String, Integer> link1 = new HashMap<>();
		link1.put("source", 0);
		link1.put("target", 1);
		Map<String, Integer> link2 = new HashMap<>();
		link2.put("source", 0);
		link2.put("target", 2);
		Map<String, Integer> link3 = new HashMap<>();
		link3.put("source", 0);
		link3.put("target", 3);
		Map<String, Integer> link4 = new HashMap<>();
		link4.put("source", 4);
		link4.put("target", 5);
		Map<String, Integer> link5 = new HashMap<>();
		link5.put("source", 4);
		link5.put("target", 6);
		Map<String, Integer> link6 = new HashMap<>();
		link6.put("source", 4);
		link6.put("target", 7);
		Map<String, Integer> link7 = new HashMap<>();
		link7.put("source", 8);
		link7.put("target", 9);
		Map<String, Integer> link8 = new HashMap<>();
		link8.put("source", 8);
		link8.put("target", 10);
		Map<String, Integer> link9 = new HashMap<>();
		link9.put("source", 8);
		link9.put("target", 11);
		expectedLinks.add(link1);
		expectedLinks.add(link2);
		expectedLinks.add(link3);
		expectedLinks.add(link4);
		expectedLinks.add(link5);
		expectedLinks.add(link6);
		expectedLinks.add(link7);
		expectedLinks.add(link8);
		expectedLinks.add(link9);
		return expectedLinks;
	}
}
