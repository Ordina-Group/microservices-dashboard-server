package be.ordina.msdashboard.aggregator.health;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import be.ordina.msdashboard.aggregator.EurekaBasedAggregator;
import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import be.ordina.msdashboard.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class HealthIndicatorsAggregator extends EurekaBasedAggregator<Node> {

	private static final Logger LOG = LoggerFactory.getLogger(HealthIndicatorsAggregator.class);

	private static final long TIMEOUT = 17000L;

	@Autowired
	private Environment environment;

	@Cacheable(value = Constants.HEALTH_CACHE_NAME, keyGenerator = "simpleKeyGenerator")
	public Node fetchCombinedDependencies() {
		Node taskResponses = buildAggregatedDependenciesFromTaskResponses(getFutureTasks());
		return taskResponses;
	}

	private Node buildAggregatedDependenciesFromTaskResponses(final List<FutureTask<Node>> tasks) {
		NodeBuilder nodeBuilder = NodeBuilder.node();
		for (FutureTask<Node> task : tasks) {
			String key = null;
			try {
				key = ((IdentifiableFutureTask) task).getId();
				Node value = task.get(TIMEOUT, TimeUnit.MILLISECONDS);
				LOG.debug("Task {} is done {}", key, task.isDone());
				value.setId(key);
				nodeBuilder.withLinkedNode(value);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOG.debug("Problem getting results for task: {} caused by: {}", key, e.toString());
			}
		}
		LOG.debug("Finished fetching combined dependencies");
		return nodeBuilder.build();
	}

	@Override
	protected Callable<Node> instantiateAggregatorTask(final HttpServletRequest originRequest, final Service service) {
		return new SingleServiceHealthCollectorTask(service, originRequest, environment.getProperty("management.context-path"));
	}
}
