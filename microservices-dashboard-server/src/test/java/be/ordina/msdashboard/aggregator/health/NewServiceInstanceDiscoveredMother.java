package be.ordina.msdashboard.aggregator.health;

import be.ordina.msdashboard.events.NewServiceInstanceDiscovered;

import org.springframework.cloud.client.DefaultServiceInstance;

public class NewServiceInstanceDiscoveredMother {
	private NewServiceInstanceDiscoveredMother() {}

	public static NewServiceInstanceDiscovered defaultNewServiceInstanceDiscovered() {
		DefaultServiceInstance serviceInstance = new DefaultServiceInstance("a", "host", 8080, false);
		NewServiceInstanceDiscovered newServiceInstanceDiscovered = new NewServiceInstanceDiscovered(serviceInstance);

		HealthInfo healthInfo = new HealthInfo();
		healthInfo.setStatus("UP");

		return newServiceInstanceDiscovered;
	}
}
