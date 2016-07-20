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
import be.ordina.msdashboard.model.NodeBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andreas Evers
 * @author Tim Ysewyn
 */
public class IndexToNodeConverterHateoasImpl implements IndexToNodeConverter {

	private static final Logger logger = LoggerFactory.getLogger(IndexToNodeConverterHateoasImpl.class);

	private static final String LINKS = "_links";
	private static final String CURIES = "curies";
	private static final String HREF = "href";
	private static final String CURIE_NAME = "name";
	private static final String RESOURCE = "RESOURCE";
	private static final String UP = "UP";

	@Override
	public Observable<Node> convert(String serviceId, String serviceUri, String source) {
		Observable<Node> observable;

		try {
			observable = convert(serviceId, serviceUri, new JSONObject(source));
		} catch (JSONException je) {
			logger.error("Could not parse JSON: {}", je);
			observable = Observable.empty();
		}
		return observable;
	}

	@Override
	public Observable<Node> convert(String serviceId, String serviceUri, JSONObject index) {
		Observable<Node> observable;

		if (index.has(LINKS)) {
			observable = Observable.from(getNodesFromJSON(serviceId, serviceUri, index));
		} else {
			logger.error("Index deserialization fails because no HAL _links was found at the root");
			observable = Observable.empty();
		}

		return observable;
	}

	@SuppressWarnings("unchecked")
	private List<Node> getNodesFromJSON(String serviceId, String serviceUri, JSONObject index) {
		List<Node> nodes = new ArrayList<>();

		NodeBuilder serviceNode = NodeBuilder.node().withId(serviceId).withLane(2);

		JSONObject links = index.getJSONObject(LINKS);
		final boolean hasCuries = links.has(CURIES);

		Set<String> keyset = new HashSet<>(links.keySet());

		keyset.stream()
			.filter(linkKey -> !CURIES.equals(linkKey))
			.forEach(linkKey -> {
				serviceNode.withLinkedFromNodeId(linkKey);

				JSONObject link = links.getJSONObject(linkKey);

				Node linkedNode = convertLinkToNodes(linkKey, link, serviceId);
				if (hasCuries) {
					String docs = resolveDocs(linkKey, links.getJSONArray(CURIES), serviceUri);

					if (docs != null) {
						linkedNode.addDetail("docs", docs);
					}
				}

				nodes.add(linkedNode);
			});

		nodes.add(0, serviceNode.build());

		return nodes;
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
		String namespace = linkKey.substring(0, linkKey.indexOf(":"));

		for (int i = 0; i < curies.length(); ) {
			JSONObject curie = curies.getJSONObject(i);

			if (curie.has(CURIE_NAME) && curie.getString(CURIE_NAME).equals(namespace)) {
				return serviceUri + curie.getString(HREF).replace("{rel}", linkKey.substring(linkKey.indexOf(":") + 1));
			}

			++i;
		}

		return null;
	}
}
