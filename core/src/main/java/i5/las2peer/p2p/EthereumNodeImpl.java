package i5.las2peer.p2p;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.registryGateway.BadEthereumCredentialsException;
import i5.las2peer.registryGateway.EthereumException;
import i5.las2peer.registryGateway.Registry;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.CryptoException;

import java.net.InetAddress;
import java.util.List;

public class EthereumNodeImpl extends PastryNodeImpl {
	private Registry registry;

	private static L2pLogger logger = L2pLogger.getInstance(EthereumNodeImpl.class);

	public EthereumNodeImpl(ClassManager classManager, boolean useMonitoringObserver, InetAddress pastryBindAddress, Integer pastryPort, List<String> bootstrap, SharedStorage.STORAGE_MODE storageMode, String storageDir, Long nodeIdSeed) {
		super(classManager, useMonitoringObserver, pastryBindAddress, pastryPort, bootstrap, storageMode, storageDir, nodeIdSeed);
	}

	@Override
	public ServiceAgentImpl startService(ServiceNameVersion nameVersion, String passphrase) throws CryptoException, AgentException {
		String serviceName = nameVersion.getName();
		int versionMajor = nameVersion.getVersion().getMajor();
		int versionMinor = nameVersion.getVersion().getMinor();
		int versionPatch = nameVersion.getVersion().getSub();
		String nodeId = getPastryNode().getId().toStringFull();
		try {
			registry.announceDeployment(serviceName, versionMajor, versionMinor, versionPatch, nodeId);
		} catch (EthereumException e) {
			logger.severe("FIXME Error while announcing deployment: " + e);
		}
		return super.startService(nameVersion, passphrase);
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

	@Override
	protected void launchSub() throws NodeException {
		try {
			setStatus(NodeStatus.STARTING);
			this.registry = new Registry();
		} catch (BadEthereumCredentialsException e) {
			// it should be possible to log in later / use current agent credentials
			// TODO: check how Web3j can handle that case
			throw new NodeException("Bad Ethereum credentials. Cannot start.", e);
		}
		super.launchSub();
	}

	public Registry getRegistry() {
		return registry;
	}
}
