package be.ordina.msdashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class InMemoryMockedConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryMockedConfiguration.class);

    @Bean
    protected DiscoveryClient discoveryClient() {
        DiscoveryClient discoveryClient = new DiscoveryClient() {

            private List<String> services = Arrays.asList("service1",
                    "service2", "service3", "service4");

            @Override
            public String description() {
                return null;
            }

            @Override
            public ServiceInstance getLocalServiceInstance() {
                return null;
            }

            @Override
            public List<ServiceInstance> getInstances(String serviceId) {
                return Arrays.asList(createServiceInstance(serviceId),
                        createServiceInstance(serviceId));
            }

            private ServiceInstance createServiceInstance(final String name) {
                return new ServiceInstance() {
                    @Override
                    public String getServiceId() {
                        return name;
                    }

                    @Override
                    public String getHost() {
                        return "localhost";
                    }

                    @Override
                    public int getPort() {
                        return 8089;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public URI getUri() {
                        return URI.create(DefaultServiceInstance.getUri(this).toString() + "/" + getServiceId());
                    }

                    @Override
                    public Map<String, String> getMetadata() {
                        return null;
                    }
                };
            }

            @Override
            public List<String> getServices() {
                //TODO: Make sure all calls to getServices and getServiceInstances are as observable in their own thread
                /*try {
                    LOG.info("Getting services from DiscoveryClient");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
                return services;
            }
        };
        return discoveryClient;
    }

    @Bean
    protected InMemoryWireMock inMemoryWireMock() {
        return new InMemoryWireMock();
    }
}