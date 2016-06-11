package be.ordina.msdashboard.aggregator.pact;

import be.ordina.msdashboard.aggregator.NodeAggregator;
import be.ordina.msdashboard.model.Node;
import com.jayway.jsonpath.JsonPath;
import io.reactivex.netty.RxNetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Andreas Evers
 */
public class PactsAggregator implements NodeAggregator {

	private static final Logger LOG = LoggerFactory.getLogger(PactsAggregator.class);

	@Value("${pact-broker.url:'http://localhost:8089'}")
	protected String pactBrokerUrl;
	@Value("${pact-broker.latest-url:'/pacts/latest'}")
	protected String latestPactsUrl;
	@Value("${pact-broker.self-href-jsonPath:'test'}")
	protected String selfHrefJsonPath;

	//TODO: Caching
	//@Cacheable(value = Constants.PACTS_CACHE_NAME, keyGenerator = "simpleKeyGenerator")
	@Override
	public Observable<Node> aggregateNodes() {
		Observable<String> urls = getPactUrlsFromBroker();
		return urls.map(url -> getNodesFromPacts(url))
				.flatMap(el -> el)
				.doOnNext(el -> LOG.info("Merged pact node! " + el.getId()));
	}

	private Observable<String> getPactUrlsFromBroker() {
		LOG.info("Discovering pact urls");
		return RxNetty.createHttpGet(pactBrokerUrl + latestPactsUrl)
				.filter(r -> {
					if (r.getStatus().code() < 400) {
						return true;
					} else {
						LOG.warn("Exception {} for call {} with headers {}", r.getStatus(), pactBrokerUrl + latestPactsUrl, r.getHeaders().entries());
						return false;
					}
				})
				.flatMap(response -> response.getContent())
				.map(data -> data.toString(Charset.defaultCharset()))
				.onErrorReturn(Throwable::toString)
				.map(response -> (List<String>) JsonPath.read(response, selfHrefJsonPath))
				.map(jsonList -> Observable.from(jsonList))
				.flatMap(el -> el.map(obj -> (String) obj))
				.doOnNext(url -> LOG.info("Pact url discovered: " + url));
	}

	private Observable<Node> getNodesFromPacts(String url) {
		return RxNetty.createHttpGet(url)
				.filter(r -> {
					if (r.getStatus().code() < 400) {
						return true;
					} else {
						LOG.warn("Exception {} for call {} with headers {}", r.getStatus(), url, r.getHeaders().entries());
						return false;
					}
				})
				.flatMap(response -> response.getContent())
				.map(data -> data.toString(Charset.defaultCharset()))
				.onErrorReturn(Throwable::toString)
				.map(response -> {
					PactToNodeConverter pactToNodeConverter = new PactToNodeConverter();
					return pactToNodeConverter.convert(response, url);
				})
				.doOnNext(node -> LOG.info("Pact node discovered in url: " + url));
	}
}
