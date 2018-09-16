package be.ordina.msdashboard.config;

import be.ordina.msdashboard.nodes.aggregators.health.HealthProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;


import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {HealthPropertiesTest.HealthPropertiesTestConfiguration.class},
        properties = {
                "msdashboard.health.filteredServices[0]: a1",
                "msdashboard.health.filteredServices[1]: a2",
                "msdashboard.health.filteredServices[2]: a3",
                "msdashboard.health.filteredServices[3]: a4",
                "msdashboard.health.filteredServices[4]: a5",
                "msdashboard.health.filteredServices[5]: a6",
                "msdashboard.health.filteredServices[6]: a7"
        }
)
@EnableConfigurationProperties
public class HealthPropertiesTest {

    @Autowired
    private HealthProperties healthProperties;

    @Test
    public void should_set_properties_when_properties_list_size_is_greater_that_default() throws Exception {
        assertThat(healthProperties.getFilteredServices())
                .isEqualTo(asList("a1", "a2", "a3", "a4", "a5", "a6", "a7"));
    }

    @Configuration
    public static class HealthPropertiesTestConfiguration {
        @Bean
        @ConfigurationProperties(prefix = "msdashboard.health")
        public HealthProperties healthProperties() {
            return new HealthProperties();
        }
    }
}
