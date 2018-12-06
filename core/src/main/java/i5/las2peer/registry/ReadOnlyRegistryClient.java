package i5.las2peer.registry;

import i5.las2peer.logging.L2pLogger;

import i5.las2peer.security.EthereumAgent;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;

import java.io.IOException;
import java.util.*;

public class ReadOnlyRegistryClient {
	Web3j web3j;
	Contracts.ContractsConfig contractsConfig;
	Contracts contracts;
	BlockchainObserver observer;

	private final L2pLogger logger = L2pLogger.getInstance(ReadOnlyRegistryClient.class);

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
	 */
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

	public boolean usernameIsAvailable(String name) throws EthereumException {
		try {
			return contracts.userRegistry.nameIsAvailable(Util.padAndConvertString(name, 32)).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	public boolean usernameIsValid(String name) throws EthereumException {
		try {
			return contracts.userRegistry.nameIsValid(Util.padAndConvertString(name, 32)).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

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

	public String getServiceAuthor(String serviceName) throws EthereumException {
		try {
			byte[] serviceNameHash = Util.soliditySha3(serviceName);
			Tuple2<String, byte[]> t = contracts.serviceRegistry.services(serviceNameHash).send();
			return Util.recoverString(t.getValue2());
		} catch (Exception e) {
			throw new EthereumException("Failed look up service author", e);
		}
	}

	/**
	 * Output some (changing) debug info.
	 */
	public String debug(/*Node node*/) {
		try {
			/*
			String identifier = "foooo-im-an-id";
			UserAgentImpl agent = UserAgentImpl.createUserAgent("hunter2");
			agent.unlock("hunter2");
			String agentId = agent.getIdentifier();
			String content = "hello there";

			EnvelopeVersion envName = node.createUnencryptedEnvelope(identifier, agent.getPublicKey(), content);
			node.storeEnvelope(envName, agent);

			EnvelopeVersion retrieved = node.fetchEnvelope(identifier);
			Serializable serializable = retrieved.getContent();
			String recovered = (String) serializable;

			return "{\"id\": \"" + identifier + "\", \"content\": \"" + recovered + "\"}";
			*/

			EthereumAgent agent = EthereumAgent.createEthereumAgent("hunter2");
			agent.unlock("hunter2");
			String xmlString = agent.toXmlString();
			EthereumAgent sameAgent = EthereumAgent.createFromXml(xmlString);

			if (!xmlString.equals(sameAgent.toXmlString())) {
				return "hell no this aint working";
			}

			return sameAgent.toXmlString();
		} catch (Exception e) {
			return "error: " + e;
		}
	}

	public Map<String, String> getTags() {
		return observer.tags;
	}

	public Set<String> getServiceNames() {
		return observer.serviceNameToAuthor.keySet();
	}

	public Map<String, List<ServiceReleaseData>> getServiceReleases() {
		return observer.serviceReleases;
	}

	public Map<String, List<ServiceDeploymentData>> getServiceDeployments() {
		return observer.serviceDeployments;
	}
}
