package i5.las2peer.p2p;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.registryGateway.*;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.CryptoException;

import java.net.InetAddress;
import java.util.List;

public class EthereumNode extends PastryNodeImpl {
	private ReadWriteRegistryClient registryClient;
	private String ethereumWalletPath;
	private String ethereumWalletPassword;

	private static L2pLogger logger = L2pLogger.getInstance(EthereumNode.class);

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
	public synchronized void shutDown() {
		// TODO: stop all services
		super.shutDown();
	}

	@Override
	public ServiceAgentImpl startService(ServiceNameVersion nameVersion, String passphrase)
			throws CryptoException, AgentException {
		announceServiceDeployment(nameVersion);
		return super.startService(nameVersion, passphrase);
	}

	public void announceServiceDeployment(ServiceAgent serviceAgent) {
		announceServiceDeployment(serviceAgent.getServiceNameVersion());
	}

	public void announceServiceDeployment(ServiceNameVersion nameVersion) {
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
	public void stopService(ServiceNameVersion nameVersion) throws AgentNotRegisteredException, ServiceNotFoundException, NodeException {
		// should there be Stop Announcements? maybe.
		super.stopService(nameVersion);
	}

	@Override
	public AgentImpl getAgent(String id) throws AgentNotFoundException, AgentException {
		// maybe check agent is in blockchain?
		// no, actually probably not -- should just apply to service authors
		return super.getAgent(id);
	}

	@Override
	public void storeAgent(AgentImpl agent) throws AgentException {
		// TODO: figure out which should be stored
		super.storeAgent(agent);
	}

	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
	}
}
