package be.ordina.msdashboard.services;

import be.ordina.msdashboard.model.Node;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RedisServiceTest {

	private static final String nodeAsJson= "{\"id\":\"key1\",\"details\":{\"type\":\"MICROSERVICE\",\"status\":\"UP\"},\"linkedNodes\":[{\"id\":\"1a\",\"details\":{\"type\":\"REST\",\"status\":\"DOWN\"}}]}";

	@InjectMocks
	private RedisService redisService;

	@Mock
	private RedisTemplate<String, Node> redisTemplate;

	@Mock
	private JedisConnectionFactory redisConnectionFactory;

	@Test
	public void getAllNodes() {
		Set<String> keys = Collections.singleton("nodeId");

		doReturn(keys).when(redisTemplate).keys("virtual:*");

		ValueOperations opsForValue = mock(ValueOperations.class);

		doReturn(opsForValue).when(redisTemplate).opsForValue();

		Node node = new Node();

		doReturn(node).when(opsForValue).get("nodeId");

		List<Node> nodes = redisService.getAllNodes();

		verify(redisTemplate).keys("virtual:*");
		verify(redisTemplate).opsForValue();
		verify(opsForValue).get("nodeId");

		assertThat(nodes).isNotEmpty();
		assertThat(nodes.get(0)).isEqualTo(node);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void saveNode() {
		ValueOperations opsForValue = mock(ValueOperations.class);

		doReturn(opsForValue).when(redisTemplate).opsForValue();

		redisService.saveNode(nodeAsJson);

		verify(redisTemplate).opsForValue();
		verify(opsForValue).set(eq("virtual:key1"), any(Node.class));
	}

	@Test
	public void deleteNode() {
		redisService.deleteNode("nodeId");

		verify(redisTemplate).delete("nodeId");
	}

	@Test
	public void deleteAllNodes() {
		Set<String> keys = Collections.singleton("nodeId");

		doReturn(keys).when(redisTemplate).keys("*");

		redisService.deleteAllNodes();

		verify(redisTemplate).keys("*");
		verify(redisTemplate).delete(keys);
	}

	@Test
	public void flushDB() {
		JedisConnection redisConnection = mock(JedisConnection.class);

		doReturn(redisConnection).when(redisConnectionFactory).getConnection();

		redisService.flushDB();

		verify(redisConnectionFactory).getConnection();
		verify(redisConnection).flushDb();
	}

	@Test
	public void evictHealthsCache() {
		redisService.evictHealthsCache();
	}

	@Test
	public void evictIndexesCache() {
		redisService.evictIndexesCache();
	}
}