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
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.*;

/**
 * @author Souris Stathis
 */
public class IndexToNodeConverterActuatorMappingsImpl implements IndexToNodeConverter {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(IndexToNodeConverterActuatorMappingsImpl.class);

	private static final String RESOURCE = "RESOURCE";
	private static final String UP = "UP";
	public static final String SPRING_FRAMEWORK_PACKAGE_PREFIX = "org.springframework";
	public static final String METHOD_KEY = "method";

	@Override
	public Observable<Node> convert(String serviceId, String serviceUri, String source) {
		Observable<Node> observable;

		try {
			observable = convert(serviceId, serviceUri, new JSONObject(source));
		} catch (JSONException je) {
			LOGGER.error("Could not parse JSON: {}", je);
			observable = Observable.empty();
		}
		return observable;
	}

	@Override
	public Observable<Node> convert(String serviceId, String serviceUri, JSONObject index) {
		return Observable.from(getNodesFromJSON(serviceId, serviceUri, index));
	}

	private List<Node> getNodesFromJSON(String serviceId, String serviceUri, JSONObject index) {
		List<Node> nodes = new ArrayList<>();

		if (index.keySet().isEmpty()) {
			return Collections.emptyList();
		}

		NodeBuilder serviceNode = NodeBuilder.node().withId(serviceId).withLane(2);

		index.keySet().stream()
				.filter(StringUtils::isNotBlank)
				.filter(mappingKey ->
						index.getJSONObject(mappingKey).has(METHOD_KEY) &&
						!index.getJSONObject(mappingKey).getString(METHOD_KEY).contains(SPRING_FRAMEWORK_PACKAGE_PREFIX))
			.forEach(mappingKey -> {
				String urlMapping = parseMappingsKey(mappingKey);
				String link = serviceUri.endsWith("/") ? serviceUri : (serviceUri+"/") + urlMapping;
				String namespacedUrlMapping = serviceId + ":" + urlMapping;

				serviceNode.withLinkedFromNodeId(namespacedUrlMapping);

				Node linkedNode = convertLinkToNodes(namespacedUrlMapping, link, serviceId);
				nodes.add(linkedNode);
			});

		nodes.add(0, serviceNode.build());

		return nodes;
	}

	private String parseMappingsKey(String mappingKey) {
		return mappingKey.replace("{", "")
				.replace("}", "")
				.replace("[", "")
				.replace("]", "")
				.split(",")[0];
	}

	private Node convertLinkToNodes(String linkKey, String link, String linkToServiceId) {
		return NodeBuilder.node()
				.withId(linkKey)
				.withLane(1)
				.withLinkedToNodeId(linkToServiceId)
				.withDetail("url", link)
				.withDetail("type", RESOURCE)
				.withDetail("status", UP)
				.build();
	}

}
