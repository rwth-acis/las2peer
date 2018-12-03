package i5.las2peer.registryGateway;

import i5.las2peer.api.Configurable;

public class RegistryConfiguration extends Configurable {
	private String endpoint;
	private long gasPrice;
	private long gasLimit;

	private String communityTagIndexAddress;
	private String userRegistryAddress;
	private String serviceRegistryAddress;

	public RegistryConfiguration() {
		setFieldValues();
	}

	public String getEndpoint() {
		return endpoint;
	}

	public long getGasPrice() {
		return gasPrice;
	}

	public long getGasLimit() {
		return gasLimit;
	}

	public String getCommunityTagIndexAddress() {
		return communityTagIndexAddress;
	}

	public String getUserRegistryAddress() {
		return userRegistryAddress;
	}

	public String getServiceRegistryAddress() {
		return serviceRegistryAddress;
	}
}
