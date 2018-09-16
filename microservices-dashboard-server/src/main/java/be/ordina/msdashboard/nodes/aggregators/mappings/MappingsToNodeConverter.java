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
package be.ordina.msdashboard.nodes.aggregators.mappings;

import be.ordina.msdashboard.nodes.model.Node;
import rx.Observable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static be.ordina.msdashboard.nodes.model.Node.STATUS;
import static be.ordina.msdashboard.nodes.model.Node.TYPE;
import static be.ordina.msdashboard.nodes.model.NodeTypes.MICROSERVICE;
import static be.ordina.msdashboard.nodes.model.NodeTypes.RESOURCE;

/**
 * @author Andreas Evers
 * @author Nikita Guchakov
 */
public class MappingsToNodeConverter {

    private static final Object METHOD = "method";

    public static Observable<Node> convertToNodes(final String serviceId, final Map<String, Object> source) {
        Set<Node> nodes = new HashSet<>();
        Node topLevelNode = new Node(serviceId);
        topLevelNode.setLane(2);
        Map<String, Object> ownDetails = topLevelNode.getDetails();
        ownDetails.put(TYPE, MICROSERVICE);
        ownDetails.put(STATUS, "UP");
        nodes.add(topLevelNode);
        source.keySet()
              .stream()
              .filter(MappingsToNodeConverter::validMappingKey)
              .forEachOrdered(key -> {
                  Object mapping = source.get(key);
                  if (mapping instanceof Map && isNonSpringMapping((Map) mapping)) {
                      nodes.add(toNode(topLevelNode, key));
                  }
              });
        return Observable.from(nodes);
    }

    private static Node toNode(Node topLevelNode, String key) {
        String url = extractUrl(key);
        Node nestedNode = new Node(url);
        nestedNode.setLane(1);
        nestedNode.addDetail("url", url);
        nestedNode.addDetail("type", RESOURCE);
        nestedNode.addDetail("status", "UP");
        extractMethods(key).ifPresent(methods -> nestedNode.addDetail("methods", methods));
        topLevelNode.getLinkedFromNodeIds().add(nestedNode.getId());
        nestedNode.getLinkedToNodeIds().add(topLevelNode.getId());
        return nestedNode;
    }

    protected static String extractUrl(String key) {
        Pattern pattern = Pattern.compile("\\{.*\\[(\\/[^\\]]*)\\].*\\}");
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException("No url found for mapping " + key);
        }
    }

    protected static Optional<String> extractMethods(String key) {
        Pattern pattern = Pattern.compile("\\{.*methods=\\[([^\\]]*)\\].*\\}");
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }

    protected static boolean isNonSpringMapping(Map<String, String> mapping) {
        return !mapping.containsKey(METHOD) || !mapping.get(METHOD).matches("[a-z]* .* org\\.springframework.*");
    }

    protected static boolean validMappingKey(String key) {
        return key.matches("(\\{.*\\[\\/.*].*\\})");
    }
}