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

import java.util.*;

import static be.ordina.msdashboard.nodes.aggregators.Constants.*;
import static be.ordina.msdashboard.nodes.model.Node.TYPE;
import static be.ordina.msdashboard.nodes.model.NodeTypes.MICROSERVICE;

/**
 * Groups microservice nodes retrieved by the {@link HealthIndicatorsAggregator}
 * together in groups.
 * By default a group is created for Spring Cloud microservices.
 *
 * @author Andreas Evers
 */
public class MicroserviceGrouper {

	public static final String GROUP = "group";
	public static final String SPRING_CLOUD = "SPRING-CLOUD";
	private Map<String, List<String>> services = new HashMap<>();

	public MicroserviceGrouper() {
		services.put(SPRING_CLOUD,
				Arrays.asList(DISCOVERY, CONFIG_SERVER, HYSTRIX, TURBINE, ZUUL));
	}

	public Collection<Node> enrich(final Collection<Node> source) {
		Collection<Node> modified = new HashSet<>();
		for (Node node : source) {
			for (String group : services.keySet()) {
				if (services.get(group).contains(node.getId())) {
					node.getDetails().put(TYPE, MICROSERVICE);
					node.getDetails().put(GROUP, group);
				}
				modified.add(node);
			}
		}
		return modified;
	}

	public void setServices(Map<String, List<String>> services) {
		this.services = services;
	}
}
