package be.ordina.msdashboard.nodes.aggregators.mappings;

import be.ordina.msdashboard.nodes.aggregators.AbstractAggregatorProperties;

import static be.ordina.msdashboard.nodes.aggregators.Constants.*;

/**
 * @author Andreas Evers
 * @author Kevin van Houtte
 */
public class MappingsProperties extends AbstractAggregatorProperties {

    public MappingsProperties() {
        super(HYSTRIX, TURBINE, CONFIG_SERVER, DISCOVERY, ZUUL);
    }
}
