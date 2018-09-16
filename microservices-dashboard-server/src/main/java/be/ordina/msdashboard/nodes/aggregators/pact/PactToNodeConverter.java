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
import be.ordina.msdashboard.nodes.model.NodeBuilder;
import be.ordina.msdashboard.nodes.model.NodeTypes;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Andreas Evers
 */
public class PactToNodeConverter {

	private static final String UP = "UP";
	private static final String relPath = "rel://";

	private static final Logger logger = LoggerFactory.getLogger(PactToNodeConverter.class);

	public Node convert(final String source, final String pactUrl) {
		List<String> paths = JsonPath.read(source, "$.interactions[*].request.path");
		String provider = JsonPath.read(source, "$.provider.name");
		String consumer = JsonPath.read(source, "$.consumer.name");

		logger.info("Retrieved UI Component for consumer {} and producer {} with rels {}", consumer, provider, paths);

		NodeBuilder node = new NodeBuilder();
		node.withId(consumer);
		node.withLane(0);
		paths.forEach(path -> node.withLinkedToNodeId(convertPathToRel(path)));
		Map<String, Object> details = new HashMap<>();
		details.put("url", pactUrl);
		//details.put("docs", pactUrl);
		details.put("type", NodeTypes.UI_COMPONENT);
		details.put("status", UP);
		node.havingDetails(details);

		return node.build();
	}

	private String convertPathToRel(String path) {
		String rel;
		if (path.startsWith(relPath) || path.startsWith("/" + relPath)) {
			rel = path.substring(path.indexOf(relPath) + relPath.length());
		} else {
			rel = path;
		}
		logger.info("Path '{}' resolved to rel '{}'", path, rel);
		return rel;
	}
}

