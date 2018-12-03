package i5.las2peer.registryGateway;

import i5.las2peer.logging.L2pLogger;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.*;

class BlockchainObserver {
	private static Map<Contracts.ContractsConfig, BlockchainObserver> instances = new HashMap<>();

	private Contracts contracts;

	Map<String, String> tags;
	Map<String, String> serviceNameToAuthor;
	Map<String, List<ServiceReleaseData>> serviceReleases;
	Map<String, Map<Integer, Map<Integer, Map<Integer, ServiceReleaseData>>>> serviceReleasesByVersion;
	Map<String, List<ServiceDeploymentData>> serviceDeployments;

	private static final L2pLogger logger = L2pLogger.getInstance(BlockchainObserver.class);

	public static BlockchainObserver getInstance(Contracts.ContractsConfig contractsConfig) {
		logger.fine("Blockchain observer instance requested, looking up ...");
		instances.computeIfAbsent(contractsConfig, BlockchainObserver::new);
		return instances.get(contractsConfig);
	}

	private BlockchainObserver(Contracts.ContractsConfig contractsConfig) {
		logger.fine("Creating new blockchain observer");
		contracts = new Contracts.ContractsBuilder(contractsConfig).build();
		observeTagCreations();
		observeServiceRegistrations();
		observeServiceReleases();
		observeServiceDeployments();
	}

	/**
	 * Create a blockchain observer that reacts to all (past and future)
	 * tag creation events by putting them in the map.
	 */
	private void observeTagCreations() {
		tags = new HashMap<>();

		contracts.communityTagIndex.communityTagCreatedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(tag -> {
			String tagName = Util.recoverString(tag.name);
			String tagDescription = contracts.communityTagIndex.viewDescription(Util.padAndConvertString(tagName, 32)).send();
			tags.put(tagName, tagDescription);
		}, e -> logger.severe("Error observing tag event: " + e.toString()));
	}

	private void observeServiceRegistrations() {
		serviceNameToAuthor = new HashMap<>();

		contracts.serviceRegistry.serviceCreatedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(service -> {
			String serviceName = contracts.serviceRegistry.hashToName(service.nameHash).send();
			serviceNameToAuthor.put(serviceName, Util.recoverString(service.author));
		}, e -> logger.severe("Error observing service registration event: " + e.toString()));
	}

	private void observeServiceReleases() {
		serviceReleases = new HashMap<>();
		serviceReleasesByVersion = new HashMap<>();

		contracts.serviceRegistry.serviceReleasedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(release -> {
			String serviceName = contracts.serviceRegistry.hashToName(release.nameHash).send();
			ServiceReleaseData releaseData = new ServiceReleaseData(serviceName, release.versionMajor, release.versionMinor, release.versionPatch, new byte[]{});

			serviceReleases.computeIfAbsent(releaseData.getServiceName(), k -> new ArrayList<>());
			serviceReleases.get(releaseData.getServiceName()).add(releaseData);

			storeReleaseByVersion(releaseData);
		}, e -> logger.severe("Error observing service release event: " + e.toString()));
	}

	private void observeServiceDeployments() {
		serviceDeployments = new HashMap<>();

		contracts.serviceRegistry.serviceDeploymentEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(deployment -> {
			String serviceName = contracts.serviceRegistry.hashToName(deployment.nameHash).send();
			ServiceDeploymentData deploymentData = new ServiceDeploymentData(serviceName, deployment.className,
					deployment.versionMajor, deployment.versionMinor, deployment.versionPatch, deployment.timestamp,
					deployment.nodeId);
			serviceDeployments.computeIfAbsent(deploymentData.getServiceName(), k -> new ArrayList<>());
			serviceDeployments.get(deploymentData.getServiceName()).add(deploymentData);
		}, e -> logger.severe("Error observing service deployment event: " + e.toString()));
	}

	// TODO: it's questionable whether this should be here rather than in Registry
	private void storeReleaseByVersion(ServiceReleaseData release) {
		// store release under "name -> x -> y -> z -> release" (i.e. essentially a map of the name and the version triple)
		serviceReleasesByVersion.computeIfAbsent(release.getServiceName(), k -> new HashMap<>());
		serviceReleasesByVersion.get(release.getServiceName()).computeIfAbsent(release.getVersionMajor(), k -> new HashMap<>());
		serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).computeIfAbsent(release.getVersionMinor(), k -> new HashMap<>());
		if (serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).containsKey(release.getVersionPatch())) {
			logger.warning("Tried to store duplicate release. Ignoring. FIXME: someone is misbehaving.");
		} else {
			serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).put(release.getVersionPatch(), release);
		}
	}

	ServiceReleaseData getReleaseByVersion(String serviceName, int versionMajor, int versionMinor, int versionPatch) {
		try {
			return serviceReleasesByVersion.get(serviceName).get(versionMajor).get(versionMinor).get(versionPatch);
		} catch (NullPointerException e) {
			return null;
		}
	}
}
