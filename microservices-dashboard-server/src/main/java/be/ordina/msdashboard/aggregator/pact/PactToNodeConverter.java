package be.ordina.msdashboard.aggregator.pact;

import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import be.ordina.msdashboard.model.Service;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PactToNodeConverter {

	private static final String LINKS = "_links";
	private static final String CURIES = "curies";
	private static final String HREF = "href";
	private static final String CURIE_NAME = "name";
	private static final String RESOURCE = "RESOURCE";
	private static final String UP = "UP";

	public Node convert(final Map<String, Object> source, final Service service) {
		if (source.containsKey(LINKS)) {
			return convertLinksToNodes((Map<String, Object>) source.get(LINKS), service);
		} else {
			throw new IllegalStateException("Index deserialization fails because no HAL _links was found at the root");
		}
	}

	private Node convertLinksToNodes(final Map<String, Object> links, final Service service) {
		Node masterNode = new Node();
		links.keySet().stream()
				.filter(linkKey -> !CURIES.equals(linkKey))
				.forEach(linkKey -> {
					NodeBuilder node = new NodeBuilder();
					Map <String, Object> link = (Map<String, Object>) links.get(linkKey);

					node.withId(linkKey);
					Node linkNode = NodeBuilder.node().withId(service.getId()).build();
					node.withLinkedNode(linkNode);
					node.withLane(1);
					Map<String, Object> details = new HashMap<>();
					details.put("url", (String) link.get(HREF));
					details.put("docs", resolveDocs((List<Map<String, Object>>) links.get(CURIES), linkKey, service));
					details.put("type", RESOURCE);
					details.put("status", UP);
					node.havingDetails(details);

					masterNode.getLinkedNodes().add(node.build());

				});
		return masterNode;
	}

	private String resolveDocs(List<Map<String, Object>> curies, String link, Service service) {
		String namespace = link.substring(0, link.indexOf(":"));
		String rel = link.substring(link.indexOf(":") + 1);
		List<String> list = curies.stream()
				.filter(curie ->
						((String) curie.get(CURIE_NAME)).equals(namespace))
				.map(curie -> ((String) curie.get(HREF)).replace("{rel}", rel))
				.map(docs -> {
					StringBuilder builder = new StringBuilder("http://");
					builder.append(service.getHost())
							.append(":")
							.append(service.getPort())
							.append(docs);
					return builder.toString();
				})
				.collect(Collectors.toList());
		return list.get(0);
	}
}

