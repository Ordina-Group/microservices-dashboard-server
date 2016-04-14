package be.ordina.msdashboard.converters;

import be.ordina.msdashboard.model.Node;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static be.ordina.msdashboard.constants.Constants.MICROSERVICE;
import static be.ordina.msdashboard.constants.Constants.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class JsonToObjectConverterTest {

    @Test
    public void testJsonToObject() {
        String nodeAsJson = "{\"id\":\"key1\",\"details\":{\"type\":\"MICROSERVICE\",\"status\":\"UP\"},\"linkedNodes\":[{\"id\":\"1a\",\"details\":{\"type\":\"REST\",\"status\":\"DOWN\"}}]}";
        JsonToObjectConverter<Node> converter = new JsonToObjectConverter<>(Node.class);

        Node node = converter.convert(nodeAsJson);

        assertThat(node).isNotNull();
        assertThat(node.getId()).isEqualTo("key1");
        assertThat(node.getDetails().size()).isEqualTo(2);
        assertThat(node.getDetails().get("type")).isEqualTo(MICROSERVICE);
        assertThat(node.getDetails().get(STATUS)).isEqualTo("UP");
        assertThat(node.getLinkedNodes().size()).isEqualTo(1);

        assertThat(node.getLinkedNodes().get(0).getId()).isEqualTo("1a");
        assertThat(node.getLinkedNodes().get(0).getDetails().size()).isEqualTo(2);
        assertThat(node.getLinkedNodes().get(0).getDetails().get("type")).isEqualTo("REST");
        assertThat(node.getLinkedNodes().get(0).getDetails().get(STATUS)).isEqualTo("DOWN");
    }

    @Test
    public void testEmptyString() {
        String nodeAsJson = "";
        JsonToObjectConverter<Node> converter = new JsonToObjectConverter<>(Node.class);

        Node node = converter.convert(nodeAsJson);

        assertThat(node).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedJson() {
        String nodeAsJson = "{\"id\":\"key1\",}";
        JsonToObjectConverter<Node> converter = new JsonToObjectConverter<>(Node.class);

        converter.convert(nodeAsJson);
    }

}
