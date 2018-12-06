package i5.las2peer.registry;

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
		if (endpoint == null || communityTagIndexAddress == null || userRegistryAddress == null
				|| serviceRegistryAddress == null) {
			throw new RuntimeException("Registry configuration file incomplete! This is almost certainly unintended.");
		}
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
