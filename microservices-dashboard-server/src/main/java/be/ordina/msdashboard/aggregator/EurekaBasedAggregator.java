package be.ordina.msdashboard.aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.servlet.http.HttpServletRequest;

import be.ordina.msdashboard.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public abstract class EurekaBasedAggregator<T> {

	private static final Logger LOG = LoggerFactory.getLogger(EurekaBasedAggregator.class);
	private static final String ZUUL_ID = "zuul";
	private static final int CORE_POOL_SIZE = 30;
	private static final int MAX_POOL_SIZE = 50;

	@Autowired
	private DiscoveryClient discoveryClient;
	private TaskExecutor taskExecutor;

	public EurekaBasedAggregator() {
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
		List<String> serviceIds = getIdsFromOnlineServices();
		return launchSingleServiceRootLinksCollectorTasks(serviceIds);
	}

	private List<String> getIdsFromOnlineServices() {
		List<String> serviceIds = discoveryClient.getServices();
		serviceIds.remove(ZUUL_ID);
		return serviceIds;
	}

	private List<FutureTask<T>> launchSingleServiceRootLinksCollectorTasks(final List<String> serviceIds) {
		List<FutureTask<T>> tasks = new ArrayList<FutureTask<T>>();
		HttpServletRequest originRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		for (String serviceId : serviceIds) {
			List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
			ServiceInstance serviceInstance = serviceInstances.get(0);
			int servicePort = serviceInstance.getPort();
			String serviceHost = serviceInstance.getHost();
			FutureTask<T> task = new IdentifiableFutureTask(instantiateAggregatorTask(originRequest, new Service(serviceId, serviceHost, servicePort)), serviceId);
			tasks.add(task);
			taskExecutor.execute(task);
		}

		return tasks;
	}

	protected abstract Callable<T> instantiateAggregatorTask(final HttpServletRequest originRequest, final Service service);

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
