package i5.las2peer.p2p;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.security.*;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.classLoaders.libraries.BlockchainRepository;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.registry.*;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.data.UserData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.serialization.SerializationException;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node implementation that extends the FreePastry-based node with
 * access to an Ethereum blockchain-based service and user registry.
 *
 * Access to the registry is encapsulated in the package
 * {@link i5.las2peer.registry}. (The actual Ethereum client is run
 * separately, but see there for details.)
 *
 * The operator of an EthereumNode must have an Ethereum BIP39
 * mnemonic-derived key pair (e.g., as created for EthereumAgents).
 * The Ether funds of that account are used to announce service
 * deployments, i.e., services running at this node.
 * The same account should be used for mining in the Ethereum client,
 * so that new Ether is added.
 *
 * Operations triggered by agents, such as users registering and
 * releasing services, are paid for by them.
 *
 * @see EthereumAgent
 */
// TODO: it would make sense to just require an EthereumAgent XML file instead
// that would be cleaner both in terms of implementation and concept
public class EthereumNode extends PastryNodeImpl {
	private ReadWriteRegistryClient registryClient;
	private String ethereumMnemonic;
	private String ethereumPassword;

	// cache for DHT supplement hash lookups
	private Map<byte[], byte[]> hashToData = new HashMap<>();

	private static L2pLogger logger = L2pLogger.getInstance(EthereumNode.class);

	/**
	 * @param ethereumMnemonic BIP39 Ethereum mnemonic for Node operator's key
	 *                         pair
	 * @param ethereumPassword password to be used with mnemonic (may be null
	 *                               or empty, but obviously that's not
	 *                               recommended)
	 * @see PastryNodeImpl#PastryNodeImpl(ClassManager, boolean, InetAddress, Integer, List, SharedStorage.STORAGE_MODE, String, Long)
	 */
	public EthereumNode(ClassManager classManager, boolean useMonitoringObserver, InetAddress pastryBindAddress,
						Integer pastryPort, List<String> bootstrap, SharedStorage.STORAGE_MODE storageMode,
						String storageDir, Long nodeIdSeed, String ethereumMnemonic, String ethereumPassword) {
		super(classManager, useMonitoringObserver, pastryBindAddress, pastryPort, bootstrap, storageMode, storageDir,
			nodeIdSeed);
		this.ethereumMnemonic = ethereumMnemonic;
		this.ethereumPassword = ethereumPassword;
	}

	/** In addition to super(), set up registry client with CLI credentials */
	@Override
	protected void launchSub() throws NodeException {
		setStatus(NodeStatus.STARTING);
		RegistryConfiguration conf = new RegistryConfiguration();
		registryClient = new ReadWriteRegistryClient(conf,
				CredentialUtils.fromMnemonic(ethereumMnemonic, ethereumPassword));
		super.launchSub();
	}

	/** Add blockchain-enabled Pastry repo, which verifies service authors */
	@Override
	protected void setupRepository() {
		getBaseClassLoader().addRepository(new BlockchainRepository(this));
	}

	/**
	 * Announce deployment of the service associated with this service
	 * agent using the service registry.
	 * @param serviceAgent agent of service being started
	 */
	public void announceServiceDeployment(ServiceAgent serviceAgent) {
		announceServiceDeployment(serviceAgent.getServiceNameVersion());
	}

	/**
	 * Announce deployment of the service instance.
	 * @param nameVersion service being started
	 */
	public void announceServiceDeployment(ServiceNameVersion nameVersion) {
		// this could be refactored
		String serviceName = nameVersion.getPackageName();
		String className = nameVersion.getSimpleClassName();
		int versionMajor = nameVersion.getVersion().getMajor();
		int versionMinor = nameVersion.getVersion().getMinor();
		int versionPatch = nameVersion.getVersion().getSub();
		String nodeId = getPastryNode().getId().toStringFull();
		try {
			registryClient.announceDeployment(serviceName, className, versionMajor, versionMinor, versionPatch, nodeId);
		} catch (EthereumException e) {
			logger.severe("Error while announcing deployment: " + e);
		}
	}

	/**
	 * Announce end of deployment (i.e., shutdown) of the service
	 * associated with this service agent using the service registry.
	 * @param serviceAgent agent of service being shut down
	 */
	public void announceServiceDeploymentEnd(ServiceAgent serviceAgent) {
		announceServiceDeploymentEnd(serviceAgent.getServiceNameVersion());
	}

	/**
	 * Announce end of deployment (i.e., shutdown) of the service
	 * instance.
	 * @param nameVersion service being shut down
	 */
	private void announceServiceDeploymentEnd(ServiceNameVersion nameVersion) {
		String serviceName = nameVersion.getPackageName();
		String className = nameVersion.getSimpleClassName();
		int versionMajor = nameVersion.getVersion().getMajor();
		int versionMinor = nameVersion.getVersion().getMinor();
		int versionPatch = nameVersion.getVersion().getSub();
		String nodeId = getPastryNode().getId().toStringFull();
		try {
			registryClient.announceDeploymentEnd(serviceName, className, versionMajor, versionMinor, versionPatch, nodeId);
		} catch (EthereumException e) {
			logger.severe("Error while announcing end of deployment: " + e);
		}
	}

	@Override
	public AgentImpl getAgent(String id) throws AgentException {
		AgentImpl agent = super.getAgent(id);
		if (agent instanceof EthereumAgent) {
			try {
				if (!agentMatchesUserRegistryData((EthereumAgent) agent)) {
					throw new AgentException("User data in blockchain does not match agent stored in shared storage!");
				}
			} catch (EthereumException e) {
				throw new AgentException("Error while comparing stored agent to user registry. Aborting out of caution.");
			}
		}
		return agent;
	}

	@Override
	public void storeAgent(AgentImpl agent) throws AgentException {
		if (agent instanceof EthereumAgent) {
			try {
				registerAgentInBlockchain((EthereumAgent) agent);
			} catch (AgentException|EthereumException|SerializationException e) {
				logger.warning("Failed to register EthereumAgent; error: " + e);
				throw new AgentException("Problem storing Ethereum agent", e);
			}
		}
		super.storeAgent(agent);
	}

	// Note: Unfortunately the term "register" is also used for storing
	// the agent data in the shared storage in some parts of the code
	// base. So "registerAgent" is definitely ambiguous.
	private void registerAgentInBlockchain(EthereumAgent ethereumAgent)
			throws AgentException, EthereumException, SerializationException {
		String name = ethereumAgent.getLoginName();

		if (registryClient.usernameIsAvailable(name)) {
			registryClient.registerUser(ethereumAgent);
		} else if (!registryClient.usernameIsValid(name)) {
			// this should probably be checked during creation too
			throw new AgentException("Agent login name is not valid for registry smart contracts.");
		} else if (agentMatchesUserRegistryData(ethereumAgent)) {
			logger.fine("Agent was already registered (same data), so that's fine.");
		} else {
			throw new AgentAlreadyExistsException("Agent username is already taken in blockchain user registry and details do NOT match.");
		}
	}

	/** compares agent login name and public key */
	private boolean agentMatchesUserRegistryData(EthereumAgent agent) throws EthereumException {
		try {
			UserData userInBlockchain = registryClient.getUser(agent.getLoginName());

			// damn, we might not be able to compare the ethereum address, because it may be null if the agent is locked
			// does it matter? I guess not. if name and pubkey match, do we care who the owner address is?
			// let's go with no. TODO: consider implications
			return true //userInBlockchain.getOwnerAddress().equals(agent.getEthereumAddress())
					&& userInBlockchain.getAgentId().equals(agent.getIdentifier())
					&& userInBlockchain.getPublicKey().equals(agent.getPublicKey());
		} catch (NotFoundException e) {
			return false;
		} catch (SerializationException e) {
			throw new EthereumException("Public key in user registry can't be deserialized.", e);
		}
	}

	/**
	 * Registers a service release in the blockchain-based registry.
	 *
	 * Also registers the service name to the author, and registers the
	 * author, if those have not already happened.
	 */
	public void registerServiceInBlockchain(String serviceName, String serviceVersion, EthereumAgent author, byte[] supplementHash)
			throws AgentException, SerializationException {
		if (author.isLocked()) {
			throw new AgentLockedException("Cannot register service because Ethereum-enabled agent is locked.");
		}

		try {
			boolean serviceAlreadyRegistered = getRegistryClient().getServiceNames().contains(serviceName);
			if (serviceAlreadyRegistered) {
				if (!isServiceOwner(author.getLoginName(), serviceName)) {
					throw new AgentException("Service is already registered to someone else, cannot register!");
				}
			} else {
				registerServiceName(serviceName, author);
			}

			logger.info("Registering service release '" + serviceName + "', v" + serviceVersion + " ...");
			getRegistryClient().releaseService(serviceName, serviceVersion, author, supplementHash);
		} catch (EthereumException e) {
			logger.severe("FIXME Error while registering release: " + e);
		}
	}

	private boolean isServiceOwner(String authorName, String serviceName) throws EthereumException {
		try {
			String serviceOwnerName = getRegistryClient().getServiceAuthor(serviceName);
			return authorName.equals(serviceOwnerName);
		} catch (NotFoundException|EthereumException e) {
			throw new EthereumException("Ownership check errored or was inconsistent, investigate.");
		}
	}

	/** Register service name and if needed the author too */
	private void registerServiceName(String name, EthereumAgent author)
			throws EthereumException, AgentLockedException, SerializationException {
		String authorName = author.getLoginName();

		// register author if needed
		try {
			getRegistryClient().getUser(authorName);
		} catch (NotFoundException e) {
			if (!getRegistryClient().usernameIsAvailable(authorName)) {
				throw new EthereumException("User name not available but also not found, lord help us!");
			} else {
				logger.info("User '" + authorName + "' not yet registered, registering now ...");
				getRegistryClient().registerUser(author);
			}
		}

		logger.fine("Service '" + name + "' not already known, registering ...");
		getRegistryClient().registerService(name, author);
	}

	/** uses a cache in contrast to {@link i5.las2peer.p2p.PastryNodeImpl#fetchHashedContent(byte[])} */
	@Override
	public byte[] fetchHashedContent(byte[] hash) throws EnvelopeException {
		if (!hashToData.containsKey(hash)) {
			byte[] data = super.fetchHashedContent(hash);
			hashToData.put(hash, data);
		}
		return hashToData.get(hash);
	}

	/** @return registry client using this agent's credentials */
	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
	}
}
