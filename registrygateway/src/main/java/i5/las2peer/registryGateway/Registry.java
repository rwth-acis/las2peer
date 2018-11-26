package i5.las2peer.registryGateway;

import i5.las2peer.api.Configurable;
import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.ServiceRegistry;
import i5.las2peer.registryGateway.contracts.UserRegistry;

import i5.las2peer.logging.L2pLogger;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.web3j.utils.Numeric.cleanHexPrefix;

public class Registry extends Configurable {
	private Map<String, String> tags;
	private Map<String, String> serviceNameToAuthor;
	private Map<String, List<ServiceReleaseData>> serviceReleases;
	private Map<String, Map<Integer, Map<Integer, Map<Integer, ServiceReleaseData>>>> serviceReleasesByVersion;
	private Map<String, List<ServiceDeploymentData>> serviceDeployments;

	// injected from config file
	private String endpoint;
	private long gasPrice;
	private long gasLimit;
	private boolean removeHexPrefixFromAddresses;

	private String account;
	private String privateKey;
	private String walletFile;
	private String password;

	private String communityTagIndexAddress;
	private String userRegistryAddress;
	private String serviceRegistryAddress;
	// end config values

	private Web3j web3j;
	private Credentials credentials;
	private ContractGasProvider gasProvider;

	private CommunityTagIndex communityTagIndex;
	private UserRegistry userRegistry;
	private ServiceRegistry serviceRegistry;

	private final L2pLogger logger = L2pLogger.getInstance(Registry.class);

	/**
	 * Connect to Ethereum node, initialize contracts, and start updating state to mirror the
	 * blockchain.
	 */
	public Registry() throws BadEthereumCredentialsException {
		setFieldValues();

		this.web3j = Web3j.build(new HttpService(endpoint));
		//initCredentials();
		initContracts();

		keepTagsUpToDate();
		keepServiceIndexUpToDate();
		keepServiceReleasesUpToDate();
		keepServiceDeploymentsUpToDate();

		debug();
	}

	private void initCredentials() throws BadEthereumCredentialsException {
		if ((walletFile == null) == (privateKey == null)) {
			throw new BadEthereumCredentialsException("Credentials must be specified EITHER as walletFile+password or privateKey");
		} else if (walletFile != null) {
			try {
				this.credentials = WalletUtils.loadCredentials(password, walletFile);
			} catch (IOException | CipherException e) {
				throw new BadEthereumCredentialsException("Could not load or decrypt wallet file", e);
			}
		} else {
			this.credentials = Credentials.create(privateKey);
		}
	}

	private void initContracts() {
		if (this.removeHexPrefixFromAddresses) {
			this.communityTagIndexAddress = cleanHexPrefix(this.communityTagIndexAddress);
			this.userRegistryAddress = cleanHexPrefix(this.userRegistryAddress);
			this.serviceRegistryAddress = cleanHexPrefix(this.serviceRegistryAddress);
		}
		this.gasProvider = new StaticGasProvider(BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));

		ReadonlyTransactionManager transactionManager = new ReadonlyTransactionManager(web3j, account);

		this.communityTagIndex = CommunityTagIndex.load(communityTagIndexAddress, web3j, transactionManager, gasProvider);
		this.userRegistry = UserRegistry.load(userRegistryAddress, web3j, transactionManager, gasProvider);
		this.serviceRegistry = ServiceRegistry.load(serviceRegistryAddress, web3j, transactionManager, gasProvider);

		//this.communityTagIndex = CommunityTagIndex.load(communityTagIndexAddress, web3j, credentials, gasProvider);
		//this.userRegistry = UserRegistry.load(userRegistryAddress, web3j, credentials, gasProvider);
		//this.serviceRegistry = ServiceRegistry.load(serviceRegistryAddress, web3j, credentials, gasProvider);
	}

	/**
	 * Return version string of connected Ethereum client.
	 */
	public String getEthClientVersion() throws EthereumException {
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
			return web3ClientVersion.getWeb3ClientVersion();
		} catch (IOException e) {
			throw new EthereumException("failed to get client version", e);
		}
	}

	/**
	 * Create new tag on blockchain.
	 * @param tagName String with <= 32 UTF-8 characters
	 * @param tagDescription String of arbitrary (!?) length
	 * @throws EthereumException if transaction failed for some reason (gas? networking?)
	 */
	public void createTag(String tagName, String tagDescription) throws EthereumException {
		try {
			communityTagIndex.create(Util.padAndConvertString(tagName, 32), tagDescription).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Read description string of tag from blockchain.
	 */
	private String getTagDescription(String tagName) throws EthereumException {
		try {
			return communityTagIndex.viewDescription(Util.padAndConvertString(tagName, 32)).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Create a blockchain observer that reacts to all (past and future)
	 * tag creation events by putting them in the map.
	 */
	private void keepTagsUpToDate() {
		this.tags = new HashMap<>();

		this.communityTagIndex.communityTagCreatedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			CommunityTagIndex.CommunityTagCreatedEventResponse response = r;
			String tagName = Util.recoverString(response.name);
			try {
				String tagDescription = getTagDescription(tagName);
				this.tags.put(tagName, tagDescription);
			} catch (EthereumException e) {
				// actually handling this is apparently tricky in Java:
				// https://stackoverflow.com/questions/31270759/a-better-approach-to-handling-exceptions-in-a-functional-way/31270760#31270760
				logger.severe("Failure while updating tags: Could not get tag description.");
			}
		}, e -> logger.severe("Error observing tag event: " + e.toString()));
	}

	private void keepServiceIndexUpToDate() {
		this.serviceNameToAuthor = new HashMap<>();

		this.serviceRegistry.serviceCreatedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			ServiceRegistry.ServiceCreatedEventResponse response = r;
			try {
				String serviceName = this.serviceRegistry.hashToName(response.nameHash).send();
				this.serviceNameToAuthor.put(serviceName, Util.recoverString(response.author));
			} catch (Exception e) {
				logger.severe("Error while looking up new service's name.");
			}
		}, e -> logger.severe("Error observing service registration event: " + e.toString()));
	}

	private void keepServiceReleasesUpToDate() {
		this.serviceReleases = new HashMap<>();
		this.serviceReleasesByVersion = new HashMap<>();

		this.serviceRegistry.serviceReleasedEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			ServiceRegistry.ServiceReleasedEventResponse response = r;
			try {
				String serviceName = this.serviceRegistry.hashToName(response.nameHash).send();
				ServiceReleaseData release = new ServiceReleaseData(serviceName, response.versionMajor, response.versionMinor, response.versionPatch, new byte[]{});
				this.serviceReleases.computeIfAbsent(release.getServiceName(), k -> new ArrayList<>());
				this.serviceReleases.get(release.getServiceName()).add(release);

				storeReleaseByVersion(release);
			} catch (Exception e) {
				logger.severe("Error while looking up service's name.");
			}
		});
	}

	private void keepServiceDeploymentsUpToDate() {
		this.serviceDeployments = new HashMap<>();

		this.serviceRegistry.serviceDeploymentEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			ServiceRegistry.ServiceDeploymentEventResponse response = r;
			try {
				String serviceName = this.serviceRegistry.hashToName(response.nameHash).send();
				ServiceDeploymentData deployment = new ServiceDeploymentData(serviceName, response.versionMajor, response.versionMinor, response.versionPatch, response.timestamp, response.nodeId);
				this.serviceDeployments.computeIfAbsent(deployment.getServiceName(), k -> new ArrayList<>());
				this.serviceDeployments.get(deployment.getServiceName()).add(deployment);
			} catch (Exception e) {
				logger.severe("Error while looking up service's name.");
			}
		});
	}

	public void registerUser(String name, String agentId) throws EthereumException {
		// TODO: more parameters
		try {
			this.userRegistry.register(Util.padAndConvertString(name, 32), Util.padAndConvertString(agentId, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register user", e);
		}
	}

	public UserData getUser(String name) throws EthereumException {
		try {
			Tuple4<byte[], byte[], String, byte[]> t = this.userRegistry.users(Util.padAndConvertString(name, 32)).send();

			byte[] returnedName = t.getValue1();

			if (Arrays.equals(returnedName, new byte[returnedName.length])) {
				// name is 0s, meaning entry does not exist
				return null;
			}

			return new UserData(t.getValue1(), t.getValue2(), t.getValue3(), t.getValue4());
		} catch (Exception e) {
			throw new EthereumException("Could not get user", e);
		}
	}

	public void registerService(String serviceName, String authorName) throws EthereumException {
		try {
			this.serviceRegistry.register(serviceName, Util.padAndConvertString(authorName, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to register service", e);
		}
	}

	public String getServiceAuthor(String serviceName) throws EthereumException {
		try {
			byte[] serviceNameHash = Util.soliditySha3(serviceName);
			Tuple2<String, byte[]> t = this.serviceRegistry.services(serviceNameHash).send();
			return Util.recoverString(t.getValue2());
		} catch (Exception e) {
			throw new EthereumException("Failed look up service author", e);
		}
	}

	public void releaseService(String serviceName, String authorName, String versionString) throws EthereumException {
		int[] version = Util.parseVersion(versionString);
		releaseService(serviceName, authorName, version[0], version[1], version[2]);
	}

	public void releaseService(String serviceName, String authorName, int versionMajor, int versionMinor, int versionPatch) throws EthereumException {
		releaseService(serviceName, authorName, versionMajor, versionMinor, versionPatch, "");
	}

	private void releaseService(String serviceName, String authorName, int versionMajor, int versionMinor, int versionPatch, String dhtSupplement) throws EthereumException {
		if (getReleaseByVersion(serviceName, versionMajor, versionMinor, versionPatch) != null) {
			logger.severe("Tried to submit duplicate release (name / version already exist), ignoring! (Maybe look into why this happened?)");
			return;
		}

		try {
			this.serviceRegistry.release(serviceName, Util.padAndConvertString(authorName, 32),
					BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
					dhtSupplement).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service release", e);
		}
	}

	//public ServiceReleaseData getServiceRelease(String serviceName, int index) throws EthereumException {
	//	// TODO: index should not be a thing!
	//	try {
	//		Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]> t = this.serviceRegistry.serviceNameToReleases(
	//				Util.padAndConvertString(serviceName, 32),
	//				BigInteger.valueOf(index)).send();
	//		return new ServiceReleaseData(t.getValue1(), t.getValue2(), t.getValue3(), t.getValue4(), t.getValue5());
	//	} catch (Exception e) {
	//		logger.severe("Error: " + e);
	//		throw new EthereumException("Failed to look up service release", e);
	//	}
	//}

	// FIXME: for some reason, the node shell does not find functions with this signature:
	// java.lang.NoSuchMethodException: No signature of okay on Registry matches the given parameters
	// (same for different function with same params)
	public void announceDeployment(String serviceName, int versionMajor, int versionMinor, int versionPatch, String nodeId) throws EthereumException {
		long timeNow = Instant.now().getEpochSecond();
		try {
			this.serviceRegistry.announceDeployment(serviceName,
					BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
					BigInteger.valueOf(timeNow), nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment announcement", e);
		}
	}

	private void storeReleaseByVersion(ServiceReleaseData release) {
		// store release under "name -> x -> y -> z -> release" (i.e. essentially a map of the name and the version triple)
		this.serviceReleasesByVersion.computeIfAbsent(release.getServiceName(), k -> new HashMap<>());
		this.serviceReleasesByVersion.get(release.getServiceName()).computeIfAbsent(release.getVersionMajor(), k -> new HashMap<>());
		this.serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).computeIfAbsent(release.getVersionMinor(), k -> new HashMap<>());
		if (this.serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).containsKey(release.getVersionPatch())) {
			logger.warning("Tried to store duplicate release. Ignoring. FIXME: someone is misbehaving.");
		} else {
			this.serviceReleasesByVersion.get(release.getServiceName()).get(release.getVersionMajor()).get(release.getVersionMinor()).put(release.getVersionPatch(), release);
		}
	}

	private ServiceReleaseData getReleaseByVersion(String serviceName, int versionMajor, int versionMinor, int versionPatch) {
		try {
			return this.serviceReleasesByVersion.get(serviceName).get(versionMajor).get(versionMinor).get(versionPatch);
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * Map of currently known tags and their descriptions.
	 *
	 * Do not change; it is continuously updated to reflect the current known state of the
	 * blockchain. (With the expected delay of blocks being mined and broadcast.)
	 */
	public Map<String, String> getTags() {
		return this.tags;
	}

	public Set<String> getServiceNames() {
		return this.serviceNameToAuthor.keySet();
	}

	public Map<String, List<ServiceReleaseData>> getServiceReleases() {
		return this.serviceReleases;
	}

	public Map<String, List<ServiceDeploymentData>> getServiceDeployments() {
		return this.serviceDeployments;
	}

	/**
	 * Output some (changing) debug info.
	 */
	public String debug() {
		try {
			String passphrase = "hunter2";
			File walletDir = new File("tmp");
			Instant start = Instant.now();
			Bip39Wallet w = WalletUtils.generateBip39Wallet(passphrase, walletDir);
			Instant end = Instant.now();
			logger.info("Wallet creation took " + Duration.between(start, end).toString());
			logger.info("Wallet mnemonic is '" + w.getMnemonic() + "'");
			return w.toString();
		} catch (Exception e) {
			return "error: " + e;
		}
	}
}
