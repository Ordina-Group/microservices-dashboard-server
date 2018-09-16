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
package be.ordina.msdashboard.nodes.aggregators.index;

import be.ordina.msdashboard.nodes.model.Node;
import be.ordina.msdashboard.nodes.model.NodeBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * @author Andreas Evers
 * @author Tim Ysewyn
 */
public class IndexToNodeConverter {

	private static final Logger logger = LoggerFactory.getLogger(IndexToNodeConverter.class);

	private static final String LINKS = "_links";
	private static final String CURIES = "curies";
	private static final String HREF = "href";
	private static final String CURIE_NAME = "name";
	private static final String RESOURCE = "RESOURCE";
	private static final String UP = "UP";

	// TODO: Use these properties in the aggregator
	private IndexProperties indexProperties;

	public IndexToNodeConverter(IndexProperties indexProperties) {
		this.indexProperties = indexProperties;
	}

	public Observable<Node> convert(String serviceId, String serviceUri, String source) {
		try {
			return convert(serviceId, serviceUri, new JSONObject(source));
		} catch (JSONException je) {
			logger.error("Could not parse JSON: {}", je);
            return Observable.empty();
		}
	}

    public Observable<Node> convert(String serviceId, String serviceUri, JSONObject index) {
        if (index.has(LINKS)) {
            return Observable.from(getNodesFromJSON(serviceId, serviceUri, index));
        } else {
            logger.error("Index deserialization fails because no HAL _links was found at the root");
            return Observable.empty();
        }
    }

	@SuppressWarnings("unchecked")
	private List<Node> getNodesFromJSON(String serviceId, String serviceUri, JSONObject index) {

		NodeBuilder serviceNode = NodeBuilder.node().withId(serviceId).withLane(2);

		JSONObject links = index.getJSONObject(LINKS);
		final boolean hasCuries = links.has(CURIES);

		List<Node> nodes = links.keySet().stream()
		                        .filter(linkKey -> !CURIES.equals(linkKey))
		                        .peek(serviceNode::withLinkedFromNodeId)
		                        .map(linkKey -> toNode(serviceId, serviceUri, links, hasCuries, linkKey))
		                        .collect(Collectors.toList());
		nodes.add(0, serviceNode.build());
		return nodes;
	}

	private Node toNode(String serviceId, String serviceUri, JSONObject links, boolean hasCuries, String linkKey) {
		JSONObject link = links.getJSONObject(linkKey);
		Node linkedNode = convertLinkToNodes(linkKey, link, serviceId);
		if (hasCuries) {
			String docs = resolveDocs(linkKey, links.getJSONArray(CURIES), serviceUri);
			if (docs != null) {
				linkedNode.addDetail("docs", docs);
			}
		}
		return linkedNode;
	}

	private Node convertLinkToNodes(String linkKey, JSONObject link, String linkToServiceId) {
		return NodeBuilder.node()
		                  .withId(linkKey)
		                  .withLane(1)
		                  .withLinkedToNodeId(linkToServiceId)
		                  .withDetail("url", link.getString(HREF))
		                  .withDetail("type", RESOURCE)
		                  .withDetail("status", UP)
		                  .build();
	}

	private String resolveDocs(String linkKey, JSONArray curies, String serviceUri) {
		String namespace = linkKey.substring(0, linkKey.indexOf(':'));
		return IntStream.range(0, curies.length())
		                .mapToObj(curies::getJSONObject)
		                .filter(curie -> curie.has(CURIE_NAME))
		                .filter(curie -> curie.getString(CURIE_NAME).equals(namespace))
		                .findFirst()
		                .map(curie -> {
			                String rel = substringAfter(linkKey,":");
			                return serviceUri + curie.getString(HREF)
			                                         .replace("{rel}", rel);
		                })
		                .orElse(null);

	}
}
