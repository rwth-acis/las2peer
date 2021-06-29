package i5.las2peer.registry.data;

import i5.las2peer.api.Configurable;

public class RegistryConfiguration extends Configurable {
	private String endpoint;
	private long gasPrice;
	private long gasLimit;

	public static float Faucet_baseFaucetAmount = 1f;
	public static float Faucet_minRatingPerService = 0f;
	public static float Faucet_maxRatingPerService = 5f;

	public static float Faucet_serviceHostingScoreMultiplier = 1f;
	public static float Faucet_serviceDevelopScoreMultiplier = 1f;
	public static float Faucet_userScoreMultiplier = 1f;

	// max amount for service payout, applied after the multipler
	public static float Faucet_serviceMaxScore = -1f; // -1 is unlimited
	public static float Faucet_developMaxScore = -1f; // -1 is unlimited

	public static float Faucet_minTimeFrame = 6f; // in hours
	public static float Faucet_TimeFrame = 24f; // in hours

	public static float UserRating_minRatingValue = 0f;
	public static float UserRating_maxRatingValue = 5f;

	public static String MobSOS_SuccessMeasure_Hosting_Label = "CommunityServiceHostingValue";
	public static String MobSOS_SuccessMeasure_Develop_Label = "CommunityServiceDevelopValue";


	private String communityTagIndexAddress;
	private String userRegistryAddress;
	private String groupRegistryAddress;
	private String serviceRegistryAddress;
	private String reputationRegistryAddress;
	
	public RegistryConfiguration() {
		setFieldValues();
		if (endpoint == null || communityTagIndexAddress == null || userRegistryAddress == null || groupRegistryAddress == null
				|| serviceRegistryAddress == null || reputationRegistryAddress == null) {
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

	public String getGroupRegistryAddress() {
		return groupRegistryAddress;
	}

	public String getServiceRegistryAddress() {
		return serviceRegistryAddress;
	}
	
	public String getReputationRegistryAddress() {
		return reputationRegistryAddress;
	}
}
