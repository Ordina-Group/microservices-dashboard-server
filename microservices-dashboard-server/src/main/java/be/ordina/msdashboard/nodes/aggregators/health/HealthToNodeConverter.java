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
package be.ordina.msdashboard.nodes.aggregators.health;

import be.ordina.msdashboard.nodes.model.Node;
import rx.Observable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static be.ordina.msdashboard.nodes.model.Node.TYPE;
import static be.ordina.msdashboard.nodes.model.NodeTypes.MICROSERVICE;

/**
 * @author Andreas Evers
 */
public class HealthToNodeConverter {

	private static final String STATUS = "status";

	private HealthProperties properties;
	
	public HealthToNodeConverter(final HealthProperties properties){
		this.properties = properties;
	}
	
	public Observable<Node> convertToNodes(final String serviceId, final Map<String, Object> source) {
		if (!source.containsKey(STATUS)) {
			throw new IllegalStateException("Health deserialization fails because no status was found at the root");
		}

		Node topLevelNode = new Node(serviceId);
		Map<String, Object> ownDetails = topLevelNode.getDetails();
		ownDetails.put(STATUS, source.get(STATUS));
		ownDetails.put(TYPE, MICROSERVICE);

		Set<Node> nodes = new HashSet<>();
		nodes.add(topLevelNode);
		source.entrySet()
		      .stream()
              .filter(entry -> !STATUS.equals(entry.getKey()))
		      .filter(entry -> !properties.getFilteredServices().contains(entry.getKey()))
		      .forEachOrdered(entry -> {
			      Object nested = source.get(entry.getKey());
			      if (nested instanceof Map && ((Map) nested).containsKey(STATUS)) {
				      Node nestedNode = new Node(entry.getKey());
				      copyDetails((Map<String, Object>) source.get(entry.getKey()), nestedNode.getDetails());
				      topLevelNode.getLinkedToNodeIds().add(nestedNode.getId());
				      nestedNode.getLinkedFromNodeIds().add(topLevelNode.getId());
				      nodes.add(nestedNode);
			      } else {
				      ownDetails.put(entry.getKey(), source.get(entry.getKey()));
			      }
		      });
		return Observable.from(nodes);
	}

	private static void copyDetails(Map<String, Object> source,
	                                Map<String, Object> target) {
		source.forEach(target::put);
	}
}

