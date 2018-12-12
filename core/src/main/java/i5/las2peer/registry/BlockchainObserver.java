package i5.las2peer.registry;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.data.ServiceDeploymentData;
import i5.las2peer.registry.data.ServiceReleaseData;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.*;

/**
 * Observes blockchain events related to the registry and updates its
 * state accordingly.
 */
class BlockchainObserver {
	private static Map<Contracts.ContractsConfig, BlockchainObserver> instances = new HashMap<>();

	private Contracts contracts;

	/** Tags to their description */
	Map<String, String> tags;

	/** Service names to name of their author/owner */
	Map<String, String> serviceNameToAuthor;

	/** Service names to list of releases */
	Map<String, List<ServiceReleaseData>> releases;

	/**
	 * Nested map from service name and version components to that
	 * exact release.
	 *
	 * E.g., "com.example.service" -> 1 -> 0 -> 2 -> release 1.0.2.
	 */
	Map<String, Map<Integer, Map<Integer, Map<Integer, ServiceReleaseData>>>> releasesByVersion;

	/**
	 * Service name to service deployment announcements.
	 *
	 * The value type of this map should really be a set, but we want
	 * the ability to update entries (specifically their timestamps).
	 * So instead we use the awkward workaround of mapping the
	 * deployments to themself. Crucially, <b>the value is the definitive
	 * one</b>, i.e., the one with updated timestamp.
	 */
	// Java's Set is too limited; this is exactly the use case we have:
	// https://stackoverflow.com/questions/7283338/getting-an-element-from-a-set
	// so far, this is ugly but fine. maybe hide this under sane accessors
	Map<String, Map<ServiceDeploymentData, ServiceDeploymentData>> deployments;

	private static final L2pLogger logger = L2pLogger.getInstance(BlockchainObserver.class);

	/**
	 * Returns a BlockchainObserver instance for the given contracts
	 * configuration.
	 *
	 * The intent is to prevent redundant observers by returning an
	 * already existing instance when possible.
	 * @param contractsConfig registry contract addresses and Ethereum
	 *                        client endpoint
	 * @return BlockchainObserver reflecting the contracts' state
	 */
	public static BlockchainObserver getInstance(Contracts.ContractsConfig contractsConfig) {
		logger.fine("Blockchain observer instance requested, looking up ...");
		instances.computeIfAbsent(contractsConfig, BlockchainObserver::new);
		return instances.get(contractsConfig);
	}

	private BlockchainObserver(Contracts.ContractsConfig contractsConfig) {
		logger.fine("Creating new blockchain observer");
		contracts = new Contracts.ContractsBuilder(contractsConfig).build();

		tags = new HashMap<>();
		serviceNameToAuthor = new HashMap<>();
		releases = new HashMap<>();
		releasesByVersion = new HashMap<>();
		deployments = new HashMap<>();

		observeTagCreations();
		observeServiceRegistrations();
		observeServiceReleases();
		observeServiceDeployments();
	}

	private void observeTagCreations() {
		contracts.communityTagIndex.communityTagCreatedEventFlowable(DefaultBlockParameterName.EARLIEST,
		DefaultBlockParameterName.LATEST).subscribe(tag -> {
			String tagName = Util.recoverString(tag.name);
			String tagDescription = contracts.communityTagIndex.viewDescription(
					Util.padAndConvertString(tagName, 32)).send();
			tags.put(tagName, tagDescription);
		}, e -> logger.severe("Error observing tag event: " + e.toString()));
	}

	private void observeServiceRegistrations() {
		contracts.serviceRegistry.serviceCreatedEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST).subscribe(service -> {
			String serviceName = contracts.serviceRegistry.hashToName(service.nameHash).send();

			if (serviceName.equals("")) {
				// I've seen this; this must be a race condition, but, uhhh, it *cannot* happen sooo ... ???
				// reasoning: the ServiceCreated event is emitted *after* the hash entry has been set
				// so what's going on!?
				logger.severe("FIXME service name is empty, which definitely should not happen");
				logger.severe("--> hash: " + service.nameHash);
				logger.severe("--> log entry: " + service.log);
			}

			serviceNameToAuthor.put(serviceName, Util.recoverString(service.author));
		}, e -> logger.severe("Error observing service registration event: " + e.toString()));
	}

	private void observeServiceReleases() {
		contracts.serviceRegistry.serviceReleasedEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST).subscribe(release -> {
			String serviceName = contracts.serviceRegistry.hashToName(release.nameHash).send();
			ServiceReleaseData releaseData = new ServiceReleaseData(serviceName,
					release.versionMajor, release.versionMinor, release.versionPatch, new byte[]{});

			releases.computeIfAbsent(releaseData.getServiceName(), k -> new ArrayList<>());

			if (releases.get(releaseData.getServiceName()).contains(releaseData)) {
				logger.warning("Duplicate release event received. This *can* happen due to race-condition, but shouldn't happen often. Release: " + releaseData);
				// TODO: decide whether to ignore duplicates or allow them
				// TODO: investigate why there are lots of duplicates
			}
			releases.get(releaseData.getServiceName()).add(releaseData);

			storeReleaseByVersion(releaseData);
		}, e -> logger.severe("Error observing service release event: " + e.toString()));
	}

	private void observeServiceDeployments() {
		// service deployment announcements and re-announcements
		contracts.serviceRegistry.serviceDeploymentEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST).subscribe(deployment -> {
			String serviceName = contracts.serviceRegistry.hashToName(deployment.nameHash).send();
			ServiceDeploymentData deploymentData = new ServiceDeploymentData(serviceName, deployment.className,
					deployment.versionMajor, deployment.versionMinor, deployment.versionPatch, deployment.timestamp,
					deployment.nodeId);
			addOrUpdateDeployment(deploymentData);
		}, e -> logger.severe("Error observing service deployment event: " + e.toString()));

		// *end* of service deployment announcements
		// FIXME: this should work almost always, but it would be far safer to actually add a timestamp
		// to the end event too, so that if (for whatever reason) the events are very out of order, we
		// don't accidentally kill a newer deployment
		// (this would only last until the re-announcement anyway. still.)
		// TODO: yeah, this is definitely needed
		contracts.serviceRegistry.serviceDeploymentEndEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST).subscribe(stopped -> {
			String serviceName = contracts.serviceRegistry.hashToName(stopped.nameHash).send();

			// for comparison only; remember: this event signifies the END of a deployment, not actually a deployment
			ServiceDeploymentData deploymentThatEnded = new ServiceDeploymentData(serviceName, stopped.className,
					stopped.versionMajor, stopped.versionMinor, stopped.versionPatch, null,
					stopped.nodeId);
			deployments.get(serviceName).remove(deploymentThatEnded);
		}, e -> logger.severe("Error observing service deployment event: " + e.toString()));
	}

	/**
	 * If an older entry (identical except for lower timestamp) exists,
	 * update the timestamp. Otherwise just add the entry.
	 * @param deployment deployment data to be added
	 */
	private void addOrUpdateDeployment(ServiceDeploymentData deployment) {
		deployments.computeIfAbsent(deployment.getServicePackageName(), k -> new HashMap<>());

		Map<ServiceDeploymentData, ServiceDeploymentData> existingDeployments =
				deployments.get(deployment.getServicePackageName());

		if (existingDeployments.containsKey(deployment)) {
			// other entry -- older or newer (or identical) -- already exists
			if (existingDeployments.get(deployment).getTimestamp().compareTo(deployment.getTimestamp()) < 0) {
				// existing entry is older => update
				existingDeployments.replace(deployment, deployment);
			}
		} else {
			existingDeployments.put(deployment, deployment);
		}
	}

	private void storeReleaseByVersion(ServiceReleaseData release) {
		// store release under "name -> x -> y -> z -> release" (i.e. essentially a map of the name and the version triple)
		releasesByVersion.computeIfAbsent(release.getServiceName(), k -> new HashMap<>());
		releasesByVersion.get(release.getServiceName()).computeIfAbsent(release.getVersionMajor(), k -> new HashMap<>());
		releasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).computeIfAbsent(release.getVersionMinor(), k -> new HashMap<>());
		if (releasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).containsKey(release.getVersionPatch())) {
			logger.warning("Tried to store duplicate release. Ignoring. FIXME: someone is misbehaving.");
		} else {
			releasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).put(release.getVersionPatch(), release);
		}
	}

	/**
	 * Safely accesses the nested map, returning null if the entry
	 * does not exist.
	 */
	ServiceReleaseData getReleaseByVersion(String serviceName, int versionMajor, int versionMinor, int versionPatch) {
		try {
			return releasesByVersion.get(serviceName).get(versionMajor).get(versionMinor).get(versionPatch);
		} catch (NullPointerException e) {
			return null;
		}
	}
}
