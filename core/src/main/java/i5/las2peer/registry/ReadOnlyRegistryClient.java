package i5.las2peer.registry;

import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.data.ServiceDeploymentData;
import i5.las2peer.registry.data.ServiceReleaseData;
import i5.las2peer.registry.data.UserData;
import i5.las2peer.registry.exceptions.EthereumException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Facade providing simple read-only access to the registry smart
 * contracts.
 *
 * @see ReadWriteRegistryClient
 */
public class ReadOnlyRegistryClient {
	Web3j web3j;
	Contracts.ContractsConfig contractsConfig;
	Contracts contracts;
	BlockchainObserver observer;

	/**
	 * Create client providing access to read-only registry functions.
	 * @param registryConfiguration addresses of registry contracts and
	 *                              Ethereum client HTTP JSON RPC API
	 *                              endpoint
	 */
	public ReadOnlyRegistryClient(RegistryConfiguration registryConfiguration) {
		this(registryConfiguration, null);
	}

	ReadOnlyRegistryClient(RegistryConfiguration registryConfiguration, Credentials credentials) {
		web3j = Web3j.build(new HttpService(registryConfiguration.getEndpoint()));

		contractsConfig = new Contracts.ContractsConfig(registryConfiguration.getCommunityTagIndexAddress(),
				registryConfiguration.getUserRegistryAddress(), registryConfiguration.getServiceRegistryAddress(), registryConfiguration.getEndpoint());

		observer = BlockchainObserver.getInstance(contractsConfig);

		contracts = new Contracts.ContractsBuilder(contractsConfig).
				setGasOptions(registryConfiguration.getGasPrice(), registryConfiguration.getGasLimit())
				.setCredentials(credentials) // may be null, that's okay here
				.build();
	}

	/**
	 * Return version string of connected Ethereum client.
	 * @deprecated there's no reason to reveal this implementation
	 * 	           detail, so this may be removed
	 */
	// this is the only place where `web3j` is (directly) accessed
	@Deprecated
	public String getEthClientVersion() throws EthereumException {
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
			return web3ClientVersion.getWeb3ClientVersion();
		} catch (IOException e) {
			throw new EthereumException("Failed to get client version", e);
		}
	}

	private String getTagDescription(String tagName) throws EthereumException {
		try {
			return contracts.communityTagIndex.viewDescription(Util.padAndConvertString(tagName, 32)).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Return true if user name is both valid and not already taken and
	 * thus can be registered.
	 * @param name user name consisting of 1 to 32 Unicode characters
	 */
	public boolean usernameIsAvailable(String name) throws EthereumException {
		try {
			return contracts.userRegistry.nameIsAvailable(Util.padAndConvertString(name, 32)).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Return true if user name is both valid, as encoded in the
	 * registry smart contract code.
	 *
	 * (Any non-empty String of up to 32 characters should work, but
	 * let's not press our luck.)
	 *
	 * @param name user name consisting of 1 to 32 Unicode characters
	 */
	public boolean usernameIsValid(String name) throws EthereumException {
		try {
			return contracts.userRegistry.nameIsValid(Util.padAndConvertString(name, 32)).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Retrieve user data stored in registry for given name.
	 * @param name user name consisting of 1 to 32 Unicode characters
	 * @return user data object containing ID and owner address, or
	 * 		   <code>null</code> if user name is not taken
	 */
	public UserData getUser(String name) throws EthereumException {
		try {
			Tuple4<byte[], byte[], String, byte[]> t = contracts.userRegistry.users(Util.padAndConvertString(name, 32)).send();

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

	/**
	 * Look up author/owner for a given service.
	 * @param serviceName service package name
	 * @return author owning the service name
	 */
	public String getServiceAuthor(String serviceName) throws EthereumException {
		try {
			byte[] serviceNameHash = Util.soliditySha3(serviceName);
			Tuple2<String, byte[]> t = contracts.serviceRegistry.services(serviceNameHash).send();
			return Util.recoverString(t.getValue2());
		} catch (Exception e) {
			throw new EthereumException("Failed look up service author", e);
		}
	}

	/** @return map of tags to descriptions */
	public ConcurrentMap<String, String> getTags() {
		return observer.tags;
	}

	/** @return set of registered service (package) names */
	public Set<String> getServiceNames() {
		return observer.serviceNameToAuthor.keySet();
	}

	/** @return map of service names to their authors */
	public ConcurrentMap<String, String> getServiceAuthors() {
		return observer.serviceNameToAuthor;
	}

	/** @return map of names to service release objects */
	public ConcurrentMap<String, List<ServiceReleaseData>> getServiceReleases() {
		return observer.releases;
	}

	/** @return map of names to service deployments announcements */
	public Map<String, List<ServiceDeploymentData>> getServiceDeployments() {
		// just getting rid of the redundant (for our purposes) nested map
		return observer.deployments.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
				value -> new ArrayList<>(value.getValue().values()))); // ooh, yeah
	}
}
