/*
 * Copyright 2012-2018 the original author or authors.
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
package be.ordina.msdashboard.nodes.aggregators;

import be.ordina.msdashboard.security.outbound.SecurityStrategyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Base class for common aggregator properties
 * @author Nikita Guchakov
 */
public abstract class AbstractAggregatorProperties {

    private Map<String, String> requestHeaders = new HashMap<>();

    private List<String> filteredServices;

    private String security = SecurityStrategyFactory.NONE;

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getSecurity() {
        return security;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public List<String> getFilteredServices() {
        return filteredServices;
    }

    public AbstractAggregatorProperties(String...filteredServices) {
        this.filteredServices = new ArrayList<>(asList(filteredServices));
    }
}
