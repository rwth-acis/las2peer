package i5.las2peer.registry;

import i5.las2peer.logging.L2pLogger;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.time.Instant;

public class ReadWriteRegistryClient extends ReadOnlyRegistryClient {
	private final L2pLogger logger = L2pLogger.getInstance(ReadWriteRegistryClient.class);

	public ReadWriteRegistryClient(RegistryConfiguration registryConfiguration, Credentials credentials) {
		super(registryConfiguration, credentials);
	}

	/**
	 * Create new tag on blockchain.
	 * @param tagName String with <= 32 UTF-8 characters
	 * @param tagDescription String of arbitrary (!?) length
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

	public void registerUser(String name, char[] agentId) throws EthereumException {
		registerUser(name, new String(agentId));
	}

	public void registerUser(String name, String agentId) throws EthereumException {
		// TODO: change agentId field
		try {
			contracts.userRegistry.register(Util.padAndConvertString(name, 32),
				Util.padAndConvertString(agentId, 128)).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register user", e);
		}
	}

	public void registerService(String serviceName, String authorName) throws EthereumException {
		try {
			contracts.serviceRegistry.register(serviceName, Util.padAndConvertString(authorName, 32)).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to register service", e);
		}
	}

	public void releaseService(String serviceName, String authorName, String versionString) throws EthereumException {
		int[] version = Util.parseVersion(versionString);
		releaseService(serviceName, authorName, version[0], version[1], version[2]);
	}

	public void releaseService(String serviceName, String authorName,
							   int versionMajor, int versionMinor, int versionPatch) throws EthereumException {
		releaseService(serviceName, authorName, versionMajor, versionMinor, versionPatch, "");
	}

	private void releaseService(String serviceName, String authorName,
								int versionMajor, int versionMinor, int versionPatch,
								String dhtSupplement) throws EthereumException {
		if (observer.getReleaseByVersion(serviceName, versionMajor, versionMinor, versionPatch) != null) {
			logger.warning("Tried to submit duplicate release (name / version already exist), ignoring!");
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

	public void announceDeployment(String servicePackageName, String serviceClassName,
								   int versionMajor, int versionMinor, int versionPatch,
								   String nodeId) throws EthereumException {
		long timeNow = Instant.now().getEpochSecond();
		try {
			contracts.serviceRegistry.announceDeployment(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				BigInteger.valueOf(timeNow), nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment announcement", e);
		}
	}

	public void announceDeploymentEnd(String servicePackageName, String serviceClassName,
									  int versionMajor, int versionMinor, int versionPatch,
									  String nodeId) throws EthereumException {
		try {
			contracts.serviceRegistry.announceDeploymentEnd(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment announcement", e);
		}
	}
}
