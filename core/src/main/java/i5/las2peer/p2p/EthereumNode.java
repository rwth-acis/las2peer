package i5.las2peer.p2p;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.*;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.registry.*;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.data.UserData;
import i5.las2peer.registry.exceptions.BadEthereumCredentialsException;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.CryptoException;

import java.net.InetAddress;
import java.util.List;

// TODO: send stop announcements on service stop / node shutdown
// actually, don't do that here, instead extend NodeServiceCache
// otherwise there would be a lot of redundancy
/**
 * Node implementation that extends the FreePastry-based node with
 * access to an Ethereum blockchain-based service and user registry.
 *
 * Access to the registry is encapsulated in the package
 * {@link i5.las2peer.registry}. (The actual Ethereum client is run
 * separately, but see there for details.)
 *
 * The operator of an EthereumNode must have an Ethereum wallet (which
 * is a JSON file containing a possibly encrypted key pair, much like
 * las2peer's agent XML files, as well as an Ethereum address).
 * The Ether funds of that wallet are used to announce service
 * deployments, i.e., services running at this node.
 * The same account should be used for mining in the Ethereum client,
 * so that new Ether is added.
 *
 * Operations triggered by agents, such as users registering and
 * releasing services, are paid for by them.
 *
 * @see EthereumAgent
 */
public class EthereumNode extends PastryNodeImpl {
	private ReadWriteRegistryClient registryClient;
	private String ethereumWalletPath;
	private String ethereumWalletPassword;

	private static L2pLogger logger = L2pLogger.getInstance(EthereumNode.class);

	/**
	 * @param ethereumWalletPath path to standard Ethereum wallet file
	 *                           belonging to the Node operator
	 * @param ethereumWalletPassword password for wallet (may be null
	 *                               or empty, but obviously that's not
	 *                               recommended)
	 * @see PastryNodeImpl#PastryNodeImpl(ClassManager, boolean, InetAddress, Integer, List, SharedStorage.STORAGE_MODE, String, Long)
	 */
	public EthereumNode(ClassManager classManager, boolean useMonitoringObserver, InetAddress pastryBindAddress,
						Integer pastryPort, List<String> bootstrap, SharedStorage.STORAGE_MODE storageMode,
						String storageDir, Long nodeIdSeed, String ethereumWalletPath, String ethereumWalletPassword) {
		super(classManager, useMonitoringObserver, pastryBindAddress, pastryPort, bootstrap, storageMode, storageDir,
			nodeIdSeed);
		this.ethereumWalletPath = ethereumWalletPath;
		this.ethereumWalletPassword = ethereumWalletPassword;
	}

	@Override
	protected void launchSub() throws NodeException {
		setStatus(NodeStatus.STARTING);
		RegistryConfiguration conf = new RegistryConfiguration();
		try {
			registryClient = new ReadWriteRegistryClient(conf,
				CredentialUtils.fromWallet(ethereumWalletPath, ethereumWalletPassword));
		} catch (BadEthereumCredentialsException e) {
			throw new NodeException("Bad Ethereum credentials. Cannot start.", e);
		}
		super.launchSub();
	}

	@Override
	public ServiceAgentImpl startService(ServiceNameVersion nameVersion, String passphrase)
			throws CryptoException, AgentException {
		announceServiceDeployment(nameVersion);
		return super.startService(nameVersion, passphrase);
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
	private void announceServiceDeployment(ServiceNameVersion nameVersion) {
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

	@Override
	public AgentImpl getAgent(String id) throws AgentException {
		AgentImpl agent = super.getAgent(id);
		if (agent instanceof EthereumAgent) {
			try {
				if (agentMatchesUserRegistryData((EthereumAgent) agent)) {

				}
			} catch (EthereumException e) {
				throw new AgentException("Error while comparing stored agent to user registry. Aborting out of caution.");
			}
		}
		return agent;
	}

	@Override
	public void storeAgent(AgentImpl agent) throws AgentException {
		super.storeAgent(agent);
		if (agent instanceof EthereumAgent) {
			try {
				registerAgentInBlockchain((EthereumAgent) agent);
			} catch (AgentException|EthereumException e) {
				throw new AgentException("Problem storing Ethereum agent", e);
			}
		}
	}

	// Note: Unfortunately the term "register" is also used for storing
	// the agent data in the shared storage in some parts of the code
	// base. So "registerAgent" is definitely ambiguous.
	private void registerAgentInBlockchain(EthereumAgent ethereumAgent) throws AgentException, EthereumException {
		String name = ethereumAgent.getLoginName();

		if (registryClient.usernameIsAvailable(name)) {
			registryClient.registerUser(name, ethereumAgent.getIdentifier());
		} else if (!registryClient.usernameIsValid(name)) {
			// this should probably be checked during creation too
			throw new AgentException("Agent login name is not valid for registry smart contracts.");
		} else if (agentMatchesUserRegistryData(ethereumAgent)) {
			// already registered, but ID and address match
			// this is fine, I guess
		} else {
			throw new AgentAlreadyExistsException("Agent username is already taken in blockchain user registry and details do NOT match.");
		}
	}

	private boolean agentMatchesUserRegistryData(EthereumAgent agent) throws EthereumException {
		UserData userInBlockchain = registryClient.getUser(agent.getLoginName());
		if (userInBlockchain == null) {
			return false;
		} else {
			return userInBlockchain.getOwnerAddress().equals(agent.getEthereumAddress())
					&& userInBlockchain.getAgentId().equals(agent.getIdentifier());
		}
	}

	/** @return registry client using this agent's credentials */
	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
	}
}
