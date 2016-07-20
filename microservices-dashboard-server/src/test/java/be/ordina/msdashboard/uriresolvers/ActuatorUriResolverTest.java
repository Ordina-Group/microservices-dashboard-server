package be.ordina.msdashboard.uriresolvers;

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActuatorUriResolverTest {

	private UriResolver actuatorUriResolver = new ActuatorUriResolver();
	@Test
	public void resolveMappingsUrl() {
		EurekaDiscoveryClient.EurekaServiceInstance instance = mock(EurekaDiscoveryClient.EurekaServiceInstance.class);
		InstanceInfo instanceInfo = mock(InstanceInfo.class);
		when(instance.getInstanceInfo()).thenReturn(instanceInfo);
		when(instanceInfo.getHomePageUrl()).thenReturn("http://homepage:1000");
		assertThat(actuatorUriResolver.resolveHomePageUrl(instance)).isEqualTo("http://homepage:1000/mappings");
	}
}
