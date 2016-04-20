package be.ordina.msdashboard.aggregator.pact;

import be.ordina.msdashboard.aggregator.health.DependenciesListFilterPredicate;
import be.ordina.msdashboard.aggregator.health.ToolBoxDependenciesModifier;
import be.ordina.msdashboard.aggregator.index.IndexToNodeConverter;
import be.ordina.msdashboard.model.Node;
import be.ordina.msdashboard.model.Service;
import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.Callable;

public class SinglePactCollectorTask implements Callable<Node> {

	private static final Logger LOG = LoggerFactory.getLogger(SinglePactCollectorTask.class);
	private final String pactUrl;
	private PactToNodeConverter pactToNodeConverter;

	public SinglePactCollectorTask(final String pactUrl) {
		this.pactUrl = pactUrl;
		pactToNodeConverter = new PactToNodeConverter();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Node call() throws Exception {
		long startTime = 0;
		if (LOG.isDebugEnabled()) {
			startTime = new DateTime().getMillis();
		}
		RestTemplate restTemplate = getRestTemplate();

		LOG.debug("Calling URI: {}", pactUrl);

		String pact = restTemplate.getForObject(pactUrl, String.class);
		Node node = pactToNodeConverter.convert(pact, pactUrl);
		if (LOG.isDebugEnabled()) {
			long totalTime = new DateTime().getMillis() - startTime;
			LOG.debug("URI: {} Total time: {}", pactUrl, totalTime);
		}
		return node;
	}

	@VisibleForTesting
	RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			protected boolean hasError(final HttpStatus statusCode) {
				if (HttpStatus.SERVICE_UNAVAILABLE.equals(statusCode)) {
					return false;
				} else {
					return (statusCode.series() == HttpStatus.Series.CLIENT_ERROR ||
							statusCode.series() == HttpStatus.Series.SERVER_ERROR);
				}
			}
		});
		return restTemplate;
	}
}
