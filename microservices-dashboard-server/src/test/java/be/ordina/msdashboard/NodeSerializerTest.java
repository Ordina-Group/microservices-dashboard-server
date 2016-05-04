package be.ordina.msdashboard;

import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.converters.JsonToObjectConverter;
import be.ordina.msdashboard.converters.ObjectToJsonConverter;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import org.junit.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.UnsupportedEncodingException;

import static be.ordina.msdashboard.constants.Constants.MICROSERVICE;
import static be.ordina.msdashboard.constants.Constants.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

public class NodeSerializerTest {

	@Test
	public void serialize() {
		Node node = NodeBuilder.node()
							   .withId("key1")
							   .withDetail("type", Constants.MICROSERVICE)
							   .withDetail(Constants.STATUS, "UP")
							   .withLinkedNode(NodeBuilder.node().withId("1a").withDetail(Constants.STATUS, "DOWN").withDetail("type", "REST").build())
							   .build();

		String nodeAsJson = "{\"id\":\"key1\",\"details\":{\"type\":\"MICROSERVICE\",\"status\":\"UP\"},\"linkedNodes\":[{\"id\":\"1a\",\"details\":{\"type\":\"REST\",\"status\":\"DOWN\"},\"linkedNodes\":[]}]}";

		NodeSerializer nodeSerializer = new NodeSerializer();

		byte[] bytes = nodeSerializer.serialize(node);
		String out = "";
		try {
			out = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println(out);
		assertThat(bytes).isEqualTo(nodeAsJson.getBytes());
	}

	@Test
	public void deserialize() {
		String nodeAsJson = "{\"id\":\"key1\",\"details\":{\"type\":\"MICROSERVICE\",\"status\":\"UP\"},\"linkedNodes\":[{\"id\":\"1a\",\"details\":{\"type\":\"REST\",\"status\":\"DOWN\"}}]}";
		NodeSerializer nodeSerializer = new NodeSerializer();

		Node node = nodeSerializer.deserialize(nodeAsJson.getBytes());

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
}
