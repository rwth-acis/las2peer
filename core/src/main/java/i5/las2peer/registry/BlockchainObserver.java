package i5.las2peer.registry;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.data.ServiceDeploymentData;
import i5.las2peer.registry.data.ServiceReleaseData;
import i5.las2peer.registry.exceptions.EthereumException;
import io.reactivex.schedulers.Schedulers;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Observes blockchain events related to the registry and updates its state
 * accordingly.
 *
 * The JavaRx Flowables use extra threads so that look-ups etc. don't block
 * the main thread.
 */
class BlockchainObserver {
	/** Tags to their description */
	ConcurrentMap<String, String> tags;

	/** Service names to name of their author/owner */
	ConcurrentMap<String, String> serviceNameToAuthor;

	/** Service names to list of releases */
	ConcurrentMap<String, List<ServiceReleaseData>> releases;

	/**
	 * Nested map from service name and version components to that
	 * exact release.
	 *
	 * E.g., "com.example.service" -> 1 -> 0 -> 2 -> release 1.0.2.
	 */
	ConcurrentMap<String, Map<Integer, Map<Integer, Map<Integer, ServiceReleaseData>>>> releasesByVersion;

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
	ConcurrentMap<String, Map<ServiceDeploymentData, ServiceDeploymentData>> deployments;

	private static final ConcurrentMap<String, String> hashToNameCache = new ConcurrentHashMap<>();

	private static final Map<Contracts.ContractsConfig, BlockchainObserver> instances = new HashMap<>();

	private Contracts contracts;

	/**
	 * Hashes of transactions that have already been handled.
	 *
	 * The reason for this is that due to chain reorganizations, a transaction
	 * can be mined in several different blocks which are (temporarily)
	 * accepted as being part of the longest chain.
	 *
	 * Deduplicating them in this way is a simple, but ugly way out. It would
	 * be better to have a threshold for confirmed blocks (e.g., latest minus
	 * six), and even better to handle the reorganizations (as mentioned in
	 * the issue, geth has a "removed" field for this exact purpose).
	 *
	 * As long as we don't have truly orphaned blocks / txs, i.e., as long as
	 * all tx are mined in main chain blocks eventually, this approach should
	 * be fine.
	 *
	 * @see <a href="web3.js issue describing the same problem">https://github.com/ethereum/web3.js/issues/398#issuecomment-189163101</a>
	 */
	private final Set<String> observedEventTxHashes = new HashSet<>();

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

		tags = new ConcurrentHashMap<>();
		serviceNameToAuthor = new ConcurrentHashMap<>();
		releases = new ConcurrentHashMap<>();
		releasesByVersion = new ConcurrentHashMap<>();
		deployments = new ConcurrentHashMap<>();

		observeTagCreations();
		observeServiceRegistrations();
		observeServiceReleases();
		observeServiceDeployments();
	}

	private boolean txHasAlreadyBeenHandled(String txHash) {
		if (observedEventTxHashes.contains(txHash)) {
			return true;
		} else {
			observedEventTxHashes.add(txHash);
			return false;
		}
	}

	private void observeTagCreations() {
		contracts.communityTagIndex.communityTagCreatedEventFlowable(DefaultBlockParameterName.EARLIEST,
		DefaultBlockParameterName.LATEST)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(tag -> {
			if (!txHasAlreadyBeenHandled(tag.log.getTransactionHash())) {
			String tagName = Util.recoverString(tag.name);

				// same issue as lookUpServiceName; let's just retry
				String tagDescription = "";
				for (int i = 0; i < 5; i++) {
					tagDescription = contracts.communityTagIndex.viewDescription(
					Util.padAndConvertString(tagName, 32)).send();
					if (!tagDescription.isEmpty()) {
						break;
					}
					logger.warning("Tag description returned empty, retrying");
				}

			tags.put(tagName, tagDescription);
			}
		}, e -> logger.severe("Error observing tag event: " + e.toString()));
	}

	/**
	 * Looks up service name from hash with caching and retry if unavailable.
	 *
	 * Result is cached because this is an RPC call and thus expensive (very
	 * roughly ~100ms, too expensive to call thousands of times).
	 *
	 * We retry because a chain reorganization can mean that we try to query
	 * the name in the brief window where the most recent block(s) becomes
	 * orphaned by a slightly longer chain segment, but before its
	 * transactions are included in one of those main-chain blocks.
	 * (As of this writing, that window is less than a second long, so one or
	 * two retries should do the trick.)
	 *
	 * This method is hopefully thread-safe, since it only modifies the cache
	 * using {@link java.util.concurrent.ConcurrentHashMap#putIfAbsent}.
	 *
	 * @param hashOfName raw hash bytes as returned by the smart contract
	 * @return service name
	 */
	private String lookupServiceName(byte[] hashOfName) throws EthereumException {
		String hashAsString = new String(hashOfName); // this is not human readable
		if (hashToNameCache.containsKey(hashAsString)) {
			return hashToNameCache.get(hashAsString);
		}
		logger.fine("Cache miss for " + Util.bytesToHexString(hashOfName) + ", querying Eth client ...");

		int retries = 120;
		int wait = 500;
		for (int i = 0; i < retries ; i++) {
			try {
				logger.finer("Lookup attempt " + i + "  querying Eth client ...");
				String serviceName = contracts.serviceRegistry.hashToName(hashOfName).send();
				if (!serviceName.isEmpty()) {
					logger.finer("... succeeded.");
					hashToNameCache.putIfAbsent(hashAsString, serviceName);
					return serviceName;
				}
				logger.finer("... still not available, retrying.");
				Thread.sleep(wait);
			} catch (Exception e) {
				logger.severe("Error while looking up service name: " + e.getMessage());
			}
		}
		throw new EthereumException("Could not look up service name");
	}

	private void observeServiceRegistrations() {
		contracts.serviceRegistry.serviceCreatedEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(service -> {
			if (!txHasAlreadyBeenHandled(service.log.getTransactionHash())) {
				String serviceName = lookupServiceName(service.nameHash);
				serviceNameToAuthor.put(serviceName, Util.recoverString(service.author));
			}
		}, e -> logger.severe("Error observing service registration event: " + e.toString()));
	}

	private void observeServiceReleases() {
		contracts.serviceRegistry.serviceReleasedEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(release -> {
			if (!txHasAlreadyBeenHandled(release.log.getTransactionHash())) {
				String serviceName = lookupServiceName(release.nameHash);
				ServiceReleaseData releaseData = new ServiceReleaseData(serviceName,
						release.versionMajor, release.versionMinor, release.versionPatch, new byte[]{});

				releases.computeIfAbsent(releaseData.getServiceName(), k -> new ArrayList<>());
				releases.get(releaseData.getServiceName()).add(releaseData);
				storeReleaseByVersion(releaseData);
			}
		}, e -> logger.severe("Error observing service release event: " + e.toString()));
	}

	private void observeServiceDeployments() {
		// service deployment announcements and re-announcements
		contracts.serviceRegistry.serviceDeploymentEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(deployment -> {
			if (!txHasAlreadyBeenHandled(deployment.log.getTransactionHash())) {
				String serviceName = lookupServiceName(deployment.nameHash);
				ServiceDeploymentData deploymentData = new ServiceDeploymentData(serviceName, deployment.className,
						deployment.versionMajor, deployment.versionMinor, deployment.versionPatch, deployment.timestamp,
						deployment.nodeId);
				addOrUpdateDeployment(deploymentData);
			}
		}, e -> logger.severe("Error observing service deployment event: " + e.toString()));

		// *end* of service deployment announcements
		// FIXME: this should work almost always, but it would be far safer to actually add a timestamp
		// to the end event too, so that if (for whatever reason) the events are very out of order, we
		// don't accidentally kill a newer deployment
		// (this would only last until the re-announcement anyway. still.)
		// TODO: yeah, this is definitely needed
		contracts.serviceRegistry.serviceDeploymentEndEventFlowable(DefaultBlockParameterName.EARLIEST,
				DefaultBlockParameterName.LATEST)
				.observeOn(Schedulers.io())
				.subscribeOn(Schedulers.io())
				.subscribe(stopped -> {
			if (!txHasAlreadyBeenHandled(stopped.log.getTransactionHash())) {
				String serviceName = lookupServiceName(stopped.nameHash);

				// for comparison only; remember: this event signifies the END of a deployment, not actually a deployment
				ServiceDeploymentData deploymentThatEnded = new ServiceDeploymentData(serviceName, stopped.className,
						stopped.versionMajor, stopped.versionMinor, stopped.versionPatch, null,
						stopped.nodeId);
				deployments.get(serviceName).remove(deploymentThatEnded);
			}
		}, e -> logger.severe("Error observing service deployment end event: " + e.toString()));
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
