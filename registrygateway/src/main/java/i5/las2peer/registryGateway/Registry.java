package i5.las2peer.registryGateway;

import i5.las2peer.api.Configurable;
import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.ServiceRegistry;
import i5.las2peer.registryGateway.contracts.UserRegistry;

import i5.las2peer.logging.L2pLogger;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.web3j.utils.Numeric.cleanHexPrefix;

public class Registry extends Configurable {
	/**
	 * Map of currently known tags and their descriptions.
	 *
	 * Do not change; it is continuously updated to reflect the current known state of the
	 * blockchain. (With the expected delay of blocks being mined and broadcast.)
	 */
	public Map<String, String> tags;

	public Map<String, String> serviceNameToAuthor;
	public Map<String, List<ServiceReleaseData>> serviceReleases;

	// injected from config file
	private String endpoint;
	private long gasPrice;
	private long gasLimit;
	private boolean filterAddressHasHexPrefix;

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

	private CommunityTagIndex communityTagIndex;
	private UserRegistry userRegistry;
	private ServiceRegistry serviceRegistry;

	private final L2pLogger logger = L2pLogger.getInstance(Registry.class);

	/**
	 * Connect to Ethereum node, initialize contracts, and start updating state to mirror the
	 * blockchain.
	 */
	public Registry() throws BadEthereumCredentialsException {
		this.setFieldValues();

		this.web3j = Web3j.build(new HttpService(endpoint));
		this.initCredentials();
		this.initContracts();

		try {
			this.keepTagsUpToDate();
			this.keepServiceIndexUpToDate();
			this.keepServiceReleasesUpToDate();
		} catch (EthereumException e) {
			logger.severe("bad stuff happened FIXME");
		}
	}

	private void initCredentials() throws BadEthereumCredentialsException {
		if ((walletFile == null) == (privateKey == null)) {
			logger.severe("credentials must be specified: EITHER as walletFile/password or privateKey");
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
		BigInteger gasPrice = BigInteger.valueOf(this.gasPrice);
		BigInteger gasLimit = BigInteger.valueOf(this.gasLimit);
		this.communityTagIndex = CommunityTagIndex.load(communityTagIndexAddress, web3j, credentials, gasPrice, gasLimit);
		this.userRegistry = UserRegistry.load(userRegistryAddress, web3j, credentials, gasPrice, gasLimit);
		this.serviceRegistry = ServiceRegistry.load(serviceRegistryAddress, web3j, credentials, gasPrice, gasLimit);
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
	private void keepTagsUpToDate() throws EthereumException {
		this.tags = new HashMap<>();

		this.communityTagIndex.communityTagCreatedEventObservable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			CommunityTagIndex.CommunityTagCreatedEventResponse response = r;
			String tagName = Util.recoverString(response.name);
			try {
				String tagDescription = getTagDescription(tagName);
				this.tags.put(tagName, tagDescription);
			} catch (EthereumException e) {
				// actually handling this is apparently tricky in Java:
				// https://stackoverflow.com/questions/31270759/a-better-approach-to-handling-exceptions-in-a-functional-way/31270760#31270760
				logger.severe("FIXME exception in lambda, oh no, good luck");
			}
		});
	}

	private void keepServiceIndexUpToDate() throws EthereumException {
		this.serviceNameToAuthor = new HashMap<>();

		this.serviceRegistry.serviceCreatedEventObservable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			ServiceRegistry.ServiceCreatedEventResponse response = r;
			String serviceName = Util.recoverString(response.name);
			String authorName = Util.recoverString(response.author);
			this.serviceNameToAuthor.put(serviceName, authorName);
		});
	}

	private void keepServiceReleasesUpToDate() throws EthereumException {
		this.serviceReleases = new HashMap<>();

		this.serviceRegistry.serviceReleasedEventObservable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).subscribe(r -> {
			ServiceRegistry.ServiceReleasedEventResponse response = r;
			ServiceReleaseData release = new ServiceReleaseData(response.name, response.versionMajor, response.versionMinor, response.versionPatch, new byte[]{});
			this.serviceReleases.computeIfAbsent(release.getServiceName(), k -> new ArrayList<>());
			this.serviceReleases.get(release.getServiceName()).add(release);
		});
	}

	private void registerUser(String name, String agentId) throws EthereumException {
		// TODO: more parameters
		try {
			this.userRegistry.register(Util.padAndConvertString(name, 32), Util.padAndConvertString(agentId, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register user", e);
		}
	}

	private UserData getUser(String name) throws EthereumException {
		try {
			Tuple4<byte[], byte[], String, byte[]> t = this.userRegistry.users(Util.padAndConvertString(name, 32)).send();
			return new UserData(t.getValue1(), t.getValue2(), t.getValue3(), t.getValue4());
		} catch (Exception e) {
			throw new EthereumException("Could not get user", e);
		}
	}

	private void registerService(String serviceName, String authorName) throws EthereumException {
		try {
			this.serviceRegistry.register(Util.padAndConvertString(serviceName, 32), Util.padAndConvertString(authorName, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to register service", e);
		}
	}

	private String getServiceAuthor(String serviceName) throws EthereumException {
		try {
			byte[] author = this.serviceRegistry.serviceNameToAuthor(Util.padAndConvertString(serviceName, 32)).send();
			return Util.recoverString(author);
		} catch (Exception e) {
			throw new EthereumException("Failed look up service author", e);
		}
	}

	private void releaseService(String serviceName, String authorName, int versionMajor, int versionMinor, int versionPatch) throws EthereumException {
		try {
			this.serviceRegistry.release(
					Util.padAndConvertString(serviceName, 32),
					Util.padAndConvertString(authorName, 32),
					BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch)).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service release", e);
		}
	}

	private ServiceReleaseData getServiceRelease(String serviceName, int index) throws EthereumException {
		// TODO: index should not be a thing!
		try {
			Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]> t = this.serviceRegistry.serviceNameToReleases(
					Util.padAndConvertString(serviceName, 32),
					BigInteger.valueOf(index)).send();
			return new ServiceReleaseData(t.getValue1(), t.getValue2(), t.getValue3(), t.getValue4(), t.getValue5());
		} catch (Exception e) {
			throw new EthereumException("Failed to look up service release", e);
		}
	}

	/**
	 * Output some (changing) debug info.
	 */
	public String debug() {
		try {
			this.releaseService("someService","alice", 0, 0, 2);
			return getServiceRelease("someService", 1).toString();
		} catch (Exception e) {
			return "error: " + e;
		}
	}
}
