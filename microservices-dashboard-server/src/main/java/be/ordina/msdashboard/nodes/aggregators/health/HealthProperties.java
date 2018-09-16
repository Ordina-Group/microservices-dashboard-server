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
package be.ordina.msdashboard.nodes.aggregators.health;

import be.ordina.msdashboard.nodes.aggregators.AbstractAggregatorProperties;

import static be.ordina.msdashboard.nodes.aggregators.Constants.*;

/**
 * Properties for health aggregation.
 *
 * @author Andreas Evers
 * @author Kevin van Houtte
 * @author Nikita Guchakov
 */
public class HealthProperties extends AbstractAggregatorProperties {

    public static final String DISK_SPACE = "diskSpace";

    public HealthProperties() {
        super(HYSTRIX, TURBINE, DISK_SPACE, CONFIG_SERVER, DISCOVERY, ZUUL);
    }
}
