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
package be.ordina.msdashboard.config;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Defers our {@code @Configuration}-classes imports to process after normal @Configuration-classes
 *
 * @author Andreas Evers
 */
public class MicroservicesDashboardServerImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] { WebConfiguration.class.getCanonicalName(),
                RedisConfiguration.class.getCanonicalName(),
                DiscoveryClientConfiguration.class.getCanonicalName(),
                HealthAggregatorConfiguration.class.getCanonicalName(),
                IndexAggregatorConfiguration.class.getCanonicalName(),
                MappingsAggregatorConfiguration.class.getCanonicalName(),
                PactAggregatorConfiguration.class.getCanonicalName()
        };
    }

}
