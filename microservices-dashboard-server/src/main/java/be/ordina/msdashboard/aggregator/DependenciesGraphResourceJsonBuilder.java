package be.ordina.msdashboard.aggregator;

import static com.google.common.collect.Maps.newHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.ordina.msdashboard.aggregator.health.HealthIndicatorsAggregator;
import be.ordina.msdashboard.aggregator.index.IndexesAggregator;
import be.ordina.msdashboard.aggregator.pact.PactsAggregator;
import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.services.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;

@Component
public class DependenciesGraphResourceJsonBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(DependenciesGraphResourceJsonBuilder.class);

	private static final String DIRECTED = "directed";
	private static final String MULTIGRAPH = "multigraph";
	private static final String GRAPH = "graph";
	private static final String LANES = "lanes";
	private static final String TYPES = "types";
	private static final String NODES = "nodes";
	private static final String LINKS = "links";
	private static final String UI = "UI Components";
	private static final String RESOURCES = "Resources";
	private static final String MICROSERVICES = "Microservices";
	private static final String BACKEND = "Backends";
	private static final String DESCRIPTION = "description";
	private static final String DB = "DB";
	private static final String MICROSERVICE = "MICROSERVICE";
	private static final String REST = "REST";
	private static final String SOAP = "SOAP";
	private static final String JMS = "JMS";
	private static final String RESOURCE = "RESOURCE";

	private HealthIndicatorsAggregator healthIndicatorsAggregator;
	private IndexesAggregator indexesAggregator;
	private PactsAggregator pactsAggregator;

	private RedisService redisService;

	private VirtualAndRealDependencyIntegrator virtualAndRealDependencyIntegrator;

	private final Map<String, Object> graph;

	@Autowired
	public DependenciesGraphResourceJsonBuilder(final HealthIndicatorsAggregator healthIndicatorsAggregator, final IndexesAggregator indexesAggregator,
												final PactsAggregator pactsAggregator, final RedisService redisService,
												final VirtualAndRealDependencyIntegrator virtualAndRealDependencyIntegrator) {
		this.healthIndicatorsAggregator = healthIndicatorsAggregator;
		this.indexesAggregator = indexesAggregator;
		this.pactsAggregator = pactsAggregator;
		this.redisService = redisService;
		this.virtualAndRealDependencyIntegrator = virtualAndRealDependencyIntegrator;
		graph = new HashMap<>();
		initGraph(graph);
	}

	public Map<String, Object> build() {
		Node healthNode = healthIndicatorsAggregator.fetchCombinedDependencies();
		Node indexNode = indexesAggregator.fetchIndexes();
		Node uiNode = pactsAggregator.fetchUIComponents();
		List<Node> microservicesAndBackends = healthNode.getLinkedNodes();
		List<Node> resources = indexNode.getLinkedNodes();
		List<Node> uiComponents = uiNode.getLinkedNodes();
		List<Node> virtualNodes = redisService.getAllNodes();
		if (!virtualNodes.isEmpty()) {
			virtualAndRealDependencyIntegrator.integrateVirtualNodesWithReal(microservicesAndBackends, resources, virtualNodes);
		}
		return createGraph(microservicesAndBackends, resources, uiComponents);
	}

	private Map<String, Object> createGraph(final List<Node> microservicesAndBackends, List<Node> resources, List<Node> uiComponents) {
		List<Map<String, Object>> nodes = new ArrayList<>();
		List<Map<String, Integer>> links = new ArrayList<>();

		for (Node microservice : microservicesAndBackends) {
			String microserviceName = microservice.getId();
			Map<String, Object> microserviceNode = createMicroserviceNode(microserviceName, microservice);
			if (!isNodeAlreadyThere(nodes, microserviceName)) {
				nodes.add(microserviceNode);
			}
			int microserviceNodeId = nodes.size() - 1;
			List<Node> dependencyNodes = microservice.getLinkedNodes();
			removeEurekaDescription(dependencyNodes);
			for (Node dependencyNode : dependencyNodes) {
				int dependencyNodeId = findNode(dependencyNode.getId(), nodes);
				if (dependencyNodeId == -1) {
					Integer lane = determineLane(dependencyNode.getDetails());
					nodes.add(createNode(dependencyNode.getId(), lane, dependencyNode.getDetails()));
					dependencyNodeId = nodes.size() - 1;
				}
				links.add(createLink(microserviceNodeId, dependencyNodeId));
			}
		}
		for (Node resource : resources) {
			Map<String, Object> resourceNode = createResourceNode(resource);
			nodes.add(resourceNode);
			String linkedMicroservice = resource.getLinkedNodes().get(0).getId();
			int resourceNodeId = nodes.size() - 1;
			int linkedMicroserviceNodeId = findNode(linkedMicroservice, nodes);
			if (linkedMicroserviceNodeId != -1) {
				links.add(createLink(resourceNodeId, linkedMicroserviceNodeId));
			} else {
				LOG.warn("Unable to resolve a link from {} to {} (destination not found)", resource.getId(), linkedMicroservice);
			}
		}
		for (Node uiComponent : uiComponents) {
			Map<String, Object> uiComponentNode = createUiComponentNode(uiComponent);
			nodes.add(uiComponentNode);
			int uiComponentNodeId = nodes.size() - 1;
			uiComponent.getLinkedNodes().stream()
					.map(linkedResource -> linkedResource.getId())
					.forEach(linkedResourceId -> {
						int linkedResourceNodeId = findNode(linkedResourceId, nodes);
						if (linkedResourceNodeId != -1) {
							links.add(createLink(uiComponentNodeId, linkedResourceNodeId));
						} else {
							LOG.warn("Unable to resolve a link from {} to {} (destination not found)", uiComponent.getId(), linkedResourceId);
						}
					});
		}
		graph.put(NODES, nodes);
		graph.put(LINKS, links);
		return graph;
	}

	@VisibleForTesting
	Integer determineLane(Map<String, Object> details) {
		if (Constants.MICROSERVICE.equals(details.get(Constants.TYPE))) {
			return new Integer("2");
		}
		return new Integer("3");
	}

	private int findNode(final String dependency, final List<Map<String, Object>> nodes) {
		for (Map<String, Object> map : nodes) {
			if (dependency.equals(map.get(Constants.ID))) {
				return nodes.indexOf(map);
			}
		}
		return -1;
	}

	@VisibleForTesting
	Map<String, Object> createMicroserviceNode(final String microServicename, final Node node) {
		Map<String, Object> details = new HashMap<>();
		for (Map.Entry<String, Object> detail : node.getDetails().entrySet()) {
			if (!(detail.getValue() instanceof Node)) {
				details.put(detail.getKey(), detail.getValue());
			}
		}
		Integer lane = determineLane(details);
		return createNode(microServicename, lane, details);
	}

	private Map<String, Object> createResourceNode(final Node node) {
		return createNode(node.getId(), node.getLane(), node.getDetails());
	}

	private Map<String, Object> createUiComponentNode(final Node node) {
		return createNode(node.getId(), node.getLane(), node.getDetails());
	}

	private Map<String, Object> createNode(final String id, final Integer lane, Map<String, Object> details) {
		Map<String, Object> node = new HashMap<>();
		node.put(Constants.ID, id);
		node.put(Constants.LANE, lane);
		node.put(Constants.DETAILS, details);
		return node;
	}

	private Map<String, Integer> createLink(final int source, final int target) {
		Map<String, Integer> link = new HashMap<>();
		link.put("source", source);
		link.put("target", target);
		return link;
	}

	private List<Map<Object, Object>> constructLanes() {
		List<Map<Object, Object>> lanes = new ArrayList<>();
		lanes.add(constructLane(0, UI));
		lanes.add(constructLane(1, RESOURCES));
		lanes.add(constructLane(2, MICROSERVICES));
		lanes.add(constructLane(3, BACKEND));
		return lanes;
	}

	private List<String> constructTypes() {
		List<String> types = new ArrayList<>();
		types.add(DB);
		types.add(MICROSERVICE);
		types.add(REST);
		types.add(SOAP);
		types.add(JMS);
		types.add(RESOURCE);
		return types;
	}

	private Map<Object, Object> constructLane(final int lane, final String type) {
		Map<Object, Object> laneMap = newHashMap();
		laneMap.put(Constants.LANE, lane);
		laneMap.put(Constants.TYPE, type);
		return laneMap;
	}

	private void removeEurekaDescription(final List<Node> dependencyNodes) {
		for (Node dependencyNode : dependencyNodes) {
			if (DESCRIPTION.equals(dependencyNode.getId())) {
				dependencyNodes.remove(dependencyNode);
				break;
			}
		}
	}

	private boolean isNodeAlreadyThere(final List<Map<String, Object>> nodes, final String microserviceId) {
		for (Map<String, Object> node : nodes) {
			if (microserviceId.equals(node.get(Constants.ID))) {
				return true;
			}
		}
		return false;
	}

	private void initGraph(final Map<String, Object> graph) {
		graph.put(DIRECTED, true);
		graph.put(MULTIGRAPH, false);
		graph.put(GRAPH, new String[0]);
		graph.put(LANES, constructLanes());
		graph.put(TYPES, constructTypes());
	}
}
