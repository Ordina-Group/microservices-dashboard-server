package be.ordina.msdashboard.autoconfigure;

import org.junit.Test;

import be.ordina.msdashboard.LandscapeWatcher;
import be.ordina.msdashboard.aggregator.health.HealthAggregator;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 */
public class AutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					MicroservicesDashboardServerAutoConfiguration.class,
					CompositeDiscoveryClientAutoConfiguration.class));

	@Test
	public void shouldLoadContext() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(LandscapeWatcher.class)).isNotNull();
			assertThat(context.getBean(WebClient.class)).isNotNull();
			assertThat(context.getBean(HealthAggregator.class)).isNotNull();
		});
	}

}
