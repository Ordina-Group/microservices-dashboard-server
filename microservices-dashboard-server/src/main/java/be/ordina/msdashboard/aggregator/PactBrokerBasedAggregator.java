package be.ordina.msdashboard.aggregator;

import be.ordina.msdashboard.model.Service;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public abstract class PactBrokerBasedAggregator<T> {

	private static final Logger LOG = LoggerFactory.getLogger(PactBrokerBasedAggregator.class);
	private static final String ZUUL_ID = "zuul";
	private static final int CORE_POOL_SIZE = 30;
	private static final int MAX_POOL_SIZE = 50;

	@Value("${pact-broker.url}")
	private String pactBrokerUrl;
	@Value("${pact-broker.latest-url}")
	private String latestPactsUrl;
	@Value("${pact-broker.self-href-jsonPath}")
	private String selfHrefJsonPath;
	@Autowired
	private DiscoveryClient discoveryClient;
	private TaskExecutor taskExecutor;

	private RestTemplate restTemplate = new RestTemplate();

	public PactBrokerBasedAggregator() {
		taskExecutor = createTaskExecutor();
	}

	private ThreadPoolTaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setCorePoolSize(CORE_POOL_SIZE);
		executor.afterPropertiesSet();
		return executor;
	}

	public List<FutureTask<T>> getFutureTasks() {
		LOG.debug("Starting collecting remote info");
		List<String> pacts = getPactUrlsFromOnlineServices();
		return launchSinglePactCollectorTasks(pacts);
	}

	private List<String> getPactUrlsFromOnlineServices() {
		String latestPacts = null;
		try {
			latestPacts = restTemplate.getForObject(new URI(pactBrokerUrl + latestPactsUrl), String.class);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Bad Pact broker URI", e);
		}
		return JsonPath.read(latestPacts, selfHrefJsonPath);
	}

	private List<FutureTask<T>> launchSinglePactCollectorTasks(final List<String> pactUrls) {
		List<FutureTask<T>> tasks = new ArrayList<FutureTask<T>>();
		HttpServletRequest originRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		for (String pactUrl : pactUrls) {
			FutureTask<T> task = new IdentifiableFutureTask(instantiateAggregatorTask(pactUrl), pactUrl);
			tasks.add(task);
			taskExecutor.execute(task);
		}

		return tasks;
	}

	protected abstract Callable<T> instantiateAggregatorTask(final String pactUrl);

	protected class IdentifiableFutureTask extends FutureTask<T> {

		private String id;

		public IdentifiableFutureTask(final Callable<T> callable, final String id) {
			super(callable);
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
}
