package be.ordina.msdashboard.aggregator.health;

public class HealthInfo {

	private String status;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public static HealthInfo withStatus(String status) {
		HealthInfo healthInfo = new HealthInfo();
		healthInfo.setStatus(status);

		return healthInfo;
	}
}
