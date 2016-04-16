package be.ordina.msdashboard.converters;

import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static be.ordina.msdashboard.constants.Constants.MICROSERVICE;
import static be.ordina.msdashboard.constants.Constants.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ObjectToJsonConverterTest {

    @Test
    public void testJsonToObject() {
        Node node = NodeBuilder.node()
                               .withId("key1")
                               .withDetail("type", Constants.MICROSERVICE)
                               .withDetail(Constants.STATUS, "UP")
                               .withLinkedNode(NodeBuilder.node().withId("1a").withDetail(Constants.STATUS, "DOWN").withDetail("type", "REST").build())
                               .build();

        ObjectToJsonConverter<Node> converter = new ObjectToJsonConverter<>();

        String nodeAsJson = converter.convert(node);

        assertThat(nodeAsJson).isNotNull();
        assertThat(nodeAsJson).isEqualTo("{\"id\":\"key1\",\"details\":{\"type\":\"MICROSERVICE\",\"status\":\"UP\"},\"linkedNodes\":[{\"id\":\"1a\",\"details\":{\"type\":\"REST\",\"status\":\"DOWN\"}}]}");
    }

    @Test
    public void testNullObject() {
        ObjectToJsonConverter<Node> converter = new ObjectToJsonConverter<>();

        String nodeAsJson = converter.convert(null);

        assertThat(nodeAsJson).isNull();
    }

}
