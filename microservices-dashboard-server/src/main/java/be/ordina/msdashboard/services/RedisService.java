package be.ordina.msdashboard.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import be.ordina.msdashboard.converters.JsonToObjectConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;

@Service
public class RedisService {

	public static final String REDIS_KEY_PREFIX = "virtual:";
	public static final String _VIRTUAL_FLAG = "virtual";

	private RedisTemplate<String, Node> redisTemplate;

	private RedisConnectionFactory redisConnectionFactory;

	@Autowired
	public RedisService(final RedisTemplate<String, Node> redisTemplate,
			final RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate = redisTemplate;
		((JedisConnectionFactory) redisConnectionFactory).setTimeout(10000);
		this.redisConnectionFactory = redisConnectionFactory;
	}

	public List<Node> getAllNodes() {
		List<Node> results = new ArrayList<>();
		Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
		for (String key : keys) {
			Node node = redisTemplate.opsForValue().get(key);
			results.add(node);
		}
		return results;
	}

	public void saveNode(final String nodeData) {
		Node node = getNode(nodeData);
		String nodeId = node.getId();
		node.getDetails().put(_VIRTUAL_FLAG, true);
		redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + nodeId, node);
	}

	public void deleteNode(final String nodeId) {
		redisTemplate.delete(nodeId);
	}

	public void deleteAllNodes() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}

	private Node getNode(String nodeData) {
		JsonToObjectConverter<Node> converter = new JsonToObjectConverter<>(Node.class);
		return converter.convert(nodeData);
	}

	public void flushDB() {
		redisConnectionFactory.getConnection().flushDb();
	}

	@CacheEvict(value = Constants.HEALTH_CACHE_NAME, allEntries = true)
	public void evictHealthsCache() {
	}

	@CacheEvict(value = Constants.INDEX_CACHE_NAME, allEntries = true)
	public void evictIndexesCache() {
	}
}