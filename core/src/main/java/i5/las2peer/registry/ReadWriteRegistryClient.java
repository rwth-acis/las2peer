package i5.las2peer.registry;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.exceptions.EthereumException;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Facade providing simple read/write access to the registry smart
 * contracts.
 *
 * Requires Ethereum credentials with sufficient Ether funds to pay
 * for transaction gas.
 *
 * @see ReadOnlyRegistryClient
 */
public class ReadWriteRegistryClient extends ReadOnlyRegistryClient {
	private final L2pLogger logger = L2pLogger.getInstance(ReadWriteRegistryClient.class);

	/**
	 * Create client providing access to both read and write registry
	 * functions.
	 * @param registryConfiguration addresses of registry contracts and
	 *                              Ethereum client HTTP JSON RPC API
	 *                              endpoint
	 */
	public ReadWriteRegistryClient(RegistryConfiguration registryConfiguration, Credentials credentials) {
		super(registryConfiguration, credentials);
	}

	/**
	 * Create new tag on blockchain.
	 * @param tagName tag name consisting of 1 to 32 UTF-8 characters
	 * @param tagDescription tag description of arbitrary length
	 * @throws EthereumException if transaction failed for some reason (gas? networking?)
	 */
	public void createTag(String tagName, String tagDescription) throws EthereumException {
		try {
			contracts.communityTagIndex.create(Util.padAndConvertString(tagName, 32), tagDescription).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Register a user name and attach it to an agent ID.
	 *
	 * This entry will be owned by the Ethereum address associated with
	 * the credentials used in the constructor.
	 *
	 * @param name user name consisting of 1 to 32 Unicode characters
	 * @param agentId 128 character las2peer agent ID
	 */
	public void registerUser(String name, String agentId) throws EthereumException {
		// TODO: change agentId field
		try {
			contracts.userRegistry.register(Util.padAndConvertString(name, 32),
				Util.padAndConvertString(agentId, 128)).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register user", e);
		}
	}

	/**
	 * Register a service name to the given author.
	 * @param serviceName service (package) name of arbitrary length
	 * @param authorName user name consisting of 1 to 32 Unicode characters
	 */
	public void registerService(String serviceName, String authorName) throws EthereumException {
		try {
			contracts.serviceRegistry.register(serviceName, Util.padAndConvertString(authorName, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to register service", e);
		}
	}

	/**
	 * Announce the release of a specific version of a service.
	 * @param serviceName name of existing service
	 * @param authorName name of author of this version
	 * @param versionString version consisting of digits and up to two periods
	 */
	// TODO: why ask for author here? isn't it implicitly the author/owner?
	// I guess not if there's ownership/rights delegation, but for now it's pointless
	public void releaseService(String serviceName, String authorName, String versionString) throws EthereumException {
		int[] version = Util.parseVersion(versionString);
		releaseService(serviceName, authorName, version[0], version[1], version[2]);
	}

	/**
	 * Announce the release of a specific version of a service.
	 * @param serviceName name of existing service
	 * @param authorName name of author of this version
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 */
	public void releaseService(String serviceName, String authorName,
							   int versionMajor, int versionMinor, int versionPatch) throws EthereumException {
		releaseService(serviceName, authorName, versionMajor, versionMinor, versionPatch, "");
	}

	private void releaseService(String serviceName, String authorName,
								int versionMajor, int versionMinor, int versionPatch,
								String dhtSupplement) throws EthereumException {
		logger.fine("DEBUG: release called: " + serviceName); // DEBUG
		if (observer.getReleaseByVersion(serviceName, versionMajor, versionMinor, versionPatch) != null) {
			logger.warning("Tried to submit duplicate release (name / version already exist), ignoring!");
			// FIXME: handle in contracts, cause this is a race condition
			return;
		}

		try {
			contracts.serviceRegistry.release(serviceName, Util.padAndConvertString(authorName, 32),
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				dhtSupplement).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service release", e);
		}
	}

	/**
	 * Announce the deployment of a specific version of a service at
	 * the given node.
	 * @param servicePackageName name of service (package). E.g.
	 *                           <code>com.example.some.package</code>.
	 * @param serviceClassName name of the service class that is run.
	 *                         E.g., <code>ExampleService</code>.
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 * @param nodeId identifier of the node running the deployment
	 */
	public void announceDeployment(String servicePackageName, String serviceClassName,
								   int versionMajor, int versionMinor, int versionPatch,
								   String nodeId) throws EthereumException {
		long timeNow = Instant.now().getEpochSecond();
		try {
			contracts.serviceRegistry.announceDeployment(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				BigInteger.valueOf(timeNow), nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment announcement ("
					+ "DEBUG: " + serviceClassName + ", " + e.getMessage() + ")", e);
		}
	}

	/**
	 * Announce that a service deployment is stopping or no longer
	 * accessible.
	 * @param servicePackageName name of service (package). E.g.
	 *                           <code>com.example.some.package</code>.
	 * @param serviceClassName name of the service class that is run.
	 *                         E.g., <code>ExampleService</code>.
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 * @param nodeId identifier of the node running the deployment
	 */
	// TODO: is there a more elegant way? referencing the tx is possible of course
	// but not really preferable
	public void announceDeploymentEnd(String servicePackageName, String serviceClassName,
									  int versionMajor, int versionMinor, int versionPatch,
									  String nodeId) throws EthereumException {
		try {
			contracts.serviceRegistry.announceDeploymentEnd(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment *end* announcement ("
					+ "DEBUG: " + serviceClassName + ", " + e.getMessage() + ")", e);
		}
	}

	/**
	 * Send ether to the recipient address.
	 *
	 * This sends ether from the "owner" of this registry client (i.e., the
	 * account specified by the credentials which were used to construct this
	 * client), to the given recipient address.
	 */
	// this is (so far) only for debugging / testing / etc.
	@Deprecated
	public void sendEther(String recipientAddress, BigDecimal valueInWei) throws EthereumException {
		try {
			TransactionReceipt receipt = Transfer.sendFunds(web3j, credentials, recipientAddress, valueInWei, Convert.Unit.WEI).send();
			if (!receipt.isStatusOK()) {
				throw new EthereumException("TX status field is not OK. TX failed.");
			}
		} catch (Exception e) {
			throw new EthereumException("Could not send ether to address '" + recipientAddress + "'", e);
		}
	}
}
