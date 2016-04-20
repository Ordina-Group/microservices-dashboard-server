package be.ordina.msdashboard.aggregator.pact;

import be.ordina.msdashboard.aggregator.EurekaBasedAggregator;
import be.ordina.msdashboard.aggregator.PactBrokerBasedAggregator;
import be.ordina.msdashboard.aggregator.index.SingleServiceIndexCollectorTask;
import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.NodeBuilder;
import be.ordina.msdashboard.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;

@Component
public class PactsAggregator extends PactBrokerBasedAggregator<Node> {

	private static final Logger LOG = LoggerFactory.getLogger(PactsAggregator.class);

	private static final long TIMEOUT = 17000L;

	@Cacheable(value = Constants.PACTS_CACHE_NAME, keyGenerator = "simpleKeyGenerator")
	public Node fetchUIComponents() {
		NodeBuilder uiNode = new NodeBuilder();
		for (FutureTask<Node> task : getFutureTasks()) {
			String key = null;
			try {
				key = ((IdentifiableFutureTask) task).getId();
				Node value = task.get(TIMEOUT, TimeUnit.MILLISECONDS);
				LOG.debug("Task {} is done {}", key, task.isDone());
				uiNode.withLinkedNode(value);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				LOG.debug("Problem getting results for task: {} caused by: {}", key, e.toString());
			}
		}
		LOG.debug("Finished fetching combined indexes");
		return uiNode.build();
	}

	@Override
	protected Callable<Node> instantiateAggregatorTask(String pactUrl) {
		return new SinglePactCollectorTask(pactUrl);
	}
}
