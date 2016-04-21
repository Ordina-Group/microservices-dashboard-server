package be.ordina.msdashboard.aggregator.pact;

import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PactToNodeConverter {

	private static final Logger LOG = LoggerFactory.getLogger(PactToNodeConverter.class);
	private static final String UI_COMPONENT = "UI_COMPONENT";
	private static final String UP = "UP";

	public Node convert(final String source, final String pactUrl) {
		List<String> rels = JsonPath.read(source, "$.interactions[*].request.path");
		String provider = JsonPath.read(source, "$.provider.name");
		String consumer = JsonPath.read(source, "$.consumer.name");

		LOG.debug("Retrieved UI Component for consumer {} and producer {} with rels {}", consumer, provider, rels);

		NodeBuilder node = new NodeBuilder();
		node.withId(consumer);
		node.withLane(0);
		rels.stream().forEach(rel -> {
				Node linkNode = NodeBuilder.node().withId(rel).build();
				node.withLinkedNode(linkNode);
		});
		Map<String, Object> details = new HashMap<>();
		details.put("url", pactUrl);
		details.put("docs", pactUrl);
		details.put("type", UI_COMPONENT);
		details.put("status", UP);
		node.havingDetails(details);

		return node.build();
	}
}

