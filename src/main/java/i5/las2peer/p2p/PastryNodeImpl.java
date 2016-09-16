package i5.las2peer.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.pastry.MessageEnvelope;
import i5.las2peer.p2p.pastry.NodeApplication;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.SharedStorage;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.Context;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import rice.environment.Environment;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.socket.internet.InternetPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * A <a href="http://freepastry.org">FreePastry</a> implementation of a las2peer {@link Node}.
 * 
 * This class is the actual heart of the p2p based network of interacting nodes an agents in the las2peer setting.
 * 
 * The package {@link i5.las2peer.p2p.pastry} provides all necessary helper classes for the integration (and
 * encapsulation) of the FreePastry library.
 */
public class PastryNodeImpl extends Node {

	private static final int AGENT_GET_TIMEOUT = 10000;
	private static final int AGENT_STORE_TIMEOUT = 10000;
	private static final int ARTIFACT_GET_TIMEOUT = 10000;
	private static final int ARTIFACT_STORE_TIMEOUT = 10000;

	private InetAddress pastryBindAddress = null; // null = detect internet address

	public static final int STANDARD_PORT = 9901;
	private int pastryPort = STANDARD_PORT;

	public static final String STANDARD_BOOTSTRAP = "localhost:9900,localhost:9999";
	private String bootStrap = STANDARD_BOOTSTRAP;

	private ExecutorService threadpool; // gather all threads in node object to minimize idle threads
	private Environment pastryEnvironment;
	private PastryNode pastryNode;
	private NodeApplication application;
	private SharedStorage pastStorage;
	private STORAGE_MODE mode = STORAGE_MODE.FILESYSTEM;
	private String storageDir; // null = default choosen by SharedStorage
	private Long nodeIdSeed;
	private BasicAgentStorage locallyKnownAgents;

	/**
	 * This is the regular constructor used by the {@link i5.las2peer.tools.L2pNodeLauncher}. Its parameters can be set
	 * to start a new network or join an existing Pastry ring.
	 * 
	 * @param classLoader A class loader that is used by the node.
	 * @param useMonitoringObserver If true, the node sends monitoring information to the monitoring service.
	 * @param port A port number the PastryNode should listen to for network communication.
	 * @param bootstrap A bootstrap address that should be used, like hostname:port or <code>null</code> to start a new
	 *            network.
	 * @param storageMode A storage mode to be used by this node, see
	 *            {@link i5.las2peer.persistency.SharedStorage.STORAGE_MODE}.
	 * @param nodeIdSeed A node id (random) seed to enforce a specific node id. If <code>null</code>, the node id will
	 *            be random.
	 */
	public PastryNodeImpl(L2pClassManager classLoader, boolean useMonitoringObserver, int port, String bootstrap,
			STORAGE_MODE storageMode, Long nodeIdSeed) {
		super(classLoader, true, useMonitoringObserver);
		pastryPort = port;
		this.bootStrap = bootstrap;
		this.mode = storageMode;
		this.storageDir = null; // null = SharedStorage choses directory
		this.nodeIdSeed = nodeIdSeed;
		locallyKnownAgents = new BasicAgentStorage(this);
		setupPastryEnvironment();
		this.setStatus(NodeStatus.CONFIGURED);
	}

	/**
	 * This constructor is mainly used by the {@link i5.las2peer.testing.TestSuite} and sets all parameters for
	 * debugging and testing operation mode.
	 * 
	 * @param bootstrap A bootstrap address that should be used, like hostname:port or <code>null</code> to start a new
	 *            network.
	 * @param storageMode A storage mode to be used by this node, see
	 *            {@link i5.las2peer.persistency.SharedStorage.STORAGE_MODE}.
	 * @param storageDir A directory to persist data to. Only considered in persistent storage mode, but overwrites
	 *            {@link SharedStorage} configurations.
	 * @param nodeIdSeed A node id (random) seed to enforce a specific node id. If <code>null</code>, the node id will
	 *            be random.
	 */
	public PastryNodeImpl(String bootstrap, STORAGE_MODE storageMode, String storageDir, Long nodeIdSeed) {
		super(null, true, false);
		pastryBindAddress = InetAddress.getLoopbackAddress();
		// use system defined port
		try {
			ServerSocket tmpSocket = new ServerSocket(0);
			tmpSocket.close();
			pastryPort = tmpSocket.getLocalPort();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.bootStrap = bootstrap;
		this.mode = storageMode;
		this.storageDir = storageDir;
		this.nodeIdSeed = nodeIdSeed;
		locallyKnownAgents = new BasicAgentStorage(this);
		setupPastryEnvironment();
		this.setStatus(NodeStatus.CONFIGURED);
	}

	/**
	 * setup pastry environment settings
	 */
	private void setupPastryEnvironment() {
		pastryEnvironment = new Environment();
		String[] configFiles = new String[] { "etc/pastry.properties", "config/pastry.properties",
				"properties/pastry.properties" };
		String found = null;
		for (String filename : configFiles) {
			try {
				if (new File(filename).exists()) {
					found = filename;
					break;
				}
			} catch (Exception e) {
				// XXX logging
			}
		}
		Hashtable<String, String> properties = new Hashtable<>();
		if (found != null) {
			System.out.println("Using pastry property file " + found);
			try {
				Properties props = new Properties();
				props.load(new FileInputStream(found));

				for (Object propname : props.keySet()) {
					properties.put((String) propname, (String) props.get(propname));
				}
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open property file " + found);
			} catch (IOException e) {
				System.err.println("Error opening property file " + found + ": " + e.getMessage());
			}
		} else {
			System.out.println("No pastry property file found - using default values");
		}
		if (!properties.containsKey("nat_search_policy")) {
			properties.put("nat_search_policy", "never");
		}
		if (!properties.containsKey("firewall_test_policy")) {
			properties.put("firewall_test_policy", "never");
		}
		if (!properties.containsKey("nat_network_prefixes")) {
			properties.put("nat_network_prefixes", "127.0.0.1;10.;192.168.;");
		}
		if (pastryBindAddress != null && pastryBindAddress.isLoopbackAddress()) {
			properties.put("allow_loopback_address", "1");
		}
		if (!properties.containsKey("pastry_socket_known_network_address")) {
			if (!properties.containsKey("pastry_socket_known_network_address_port")) {
				properties.put("pastry_socket_known_network_address_port", "80");
			}
		}
		if (!properties.containsKey("nat_search_policy")) {
			properties.put("nat_search_policy", "never");
		}
		for (String prop : properties.keySet()) {
			pastryEnvironment.getParameters().setString(prop, properties.get(prop));
			// XXX logging
			System.out.println("setting: " + prop + ": '" + properties.get(prop) + "'");
		}
	}

	/**
	 * access to the underlying pastry node
	 * 
	 * @return the pastry node representing this las2peer node
	 */
	public PastryNode getPastryNode() {
		return pastryNode;
	}

	/**
	 * generate a collection of InetSocketAddresses from the given bootstrap string
	 * 
	 * @return collection of InetSocketAddresses from the given bootstrap string
	 */
	private Collection<InetSocketAddress> getBootstrapAddresses() {
		Vector<InetSocketAddress> result = new Vector<>();

		if (bootStrap == null || bootStrap.isEmpty()) {
			return result;
		}

		String[] addresses = bootStrap.split(",");
		for (String address : addresses) {
			String[] hostAndPort = address.split(":");
			int port = STANDARD_PORT;

			if (hostAndPort.length == 2) {
				port = Integer.parseInt(hostAndPort[1]);
			}

			try {
				result.add(new InetSocketAddress(InetAddress.getByName(hostAndPort[0]), port));
			} catch (UnknownHostException e) {
				// TODO this should be handled earlier
				if (address.equals("-")) {
					System.out.println("Starting new network..");
				} else {
					System.err.println("Cannot resolve address for: " + address + "\n Starting new network..");
				}
			}
		}

		return result;
	}

	/**
	 * Setup all pastry applications to run on this node.
	 * 
	 * This will be
	 * <ul>
	 * <li>the application for message passing {@link NodeApplication}</li>
	 * <li>a Past DHT storage from Freepastry</li>
	 * </ul>
	 * 
	 * For the past DHT either a memory mode or a disk persistence mode are selected based on {@link STORAGE_MODE}
	 * 
	 * @throws StorageException
	 */
	private void setupPastryApplications() throws StorageException {
		threadpool = Executors.newCachedThreadPool();
		application = new NodeApplication(this);
		pastStorage = new SharedStorage(pastryNode, mode, threadpool, storageDir);
	}

	/**
	 * start this node
	 */
	@Override
	protected void launchSub() throws NodeException {

		try {
			setStatus(NodeStatus.STARTING);

			NodeIdFactory nidFactory = null;
			if (nodeIdSeed == null) {
				nidFactory = new RandomNodeIdFactory(pastryEnvironment);
			} else {
				nidFactory = new NodeIdFactory() {
					@Override
					public rice.pastry.Id generateNodeId() {
						// same method as in rice.pastry.standard.RandomNodeIdFactory but except using nodeIdSeed
						byte raw[] = new byte[8];
						long tmp = ++nodeIdSeed;
						for (int i = 0; i < 8; i++) {
							raw[i] = (byte) (tmp & 0xff);
							tmp >>= 8;
						}
						MessageDigest md = null;
						try {
							md = MessageDigest.getInstance("SHA");
						} catch (NoSuchAlgorithmException e) {
							throw new RuntimeException("No SHA support!", e);
						}
						md.update(raw);
						byte[] digest = md.digest();
						rice.pastry.Id nodeId = rice.pastry.Id.build(digest);
						return nodeId;
					}
				};
			}
			InternetPastryNodeFactory factory = new InternetPastryNodeFactory(nidFactory, pastryBindAddress, pastryPort,
					pastryEnvironment, null, null, null);
			pastryNode = factory.newNode();

			setupPastryApplications();

			pastryNode.boot(getBootstrapAddresses());

			synchronized (pastryNode) {
				while (!pastryNode.isReady() && !pastryNode.joinFailed()) {
					// delay so we don't busy-wait
					pastryNode.wait(500);

					// abort if can't join
					if (pastryNode.joinFailed()) {
						throw new NodeException(
								"Could not join the FreePastry ring.  Reason:" + pastryNode.joinFailedReason());
					}
				}
			}

			setStatus(NodeStatus.RUNNING);

			registerShutdownMethod();

		} catch (IOException e) {
			throw new NodeException("IOException while joining pastry ring", e);
		} catch (StorageException e) {
			throw new NodeException("Shared storage exception while joining pastry ring", e);
		} catch (InterruptedException e) {
			throw new NodeException("Interrupted while joining pastry ring!", e);
		} catch (IllegalStateException e) {
			throw new NodeException("Unable to open Netwock socket - is the port already in use?", e);
		}
	}

	/**
	 * register node shutdown as JVM shutdown method
	 */
	private void registerShutdownMethod() {
		final PastryNodeImpl self = this;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				self.shutDown();
			}
		});
	}

	@Override
	public synchronized void shutDown() {
		this.setStatus(NodeStatus.CLOSING);
		super.shutDown();
		if (threadpool != null) {
			// destroy pending jobs first, because they miss the node the most
			threadpool.shutdownNow();
		}
		if (pastryNode != null) {
			pastryNode.destroy();
			pastryNode = null;
		}
		if (pastryEnvironment != null) {
			pastryEnvironment.destroy();
			pastryEnvironment = null;
		}
		this.setStatus(NodeStatus.CLOSED);
	}

	@Override
	public void registerReceiver(MessageReceiver receiver)
			throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException {

		synchronized (this) {
			super.registerReceiver(receiver);
			application.registerAgentTopic(receiver);

			// Observer is called in superclass!
		}

	}

	@Override
	public void unregisterReceiver(MessageReceiver receiver) throws AgentNotKnownException, NodeException {
		synchronized (this) {
			application.unregisterAgentTopic(receiver.getResponsibleForAgentId());
			super.unregisterReceiver(receiver);
		}
	}

	@Override
	public void registerReceiverToTopic(MessageReceiver receiver, long topic) throws AgentNotKnownException {
		synchronized (this) {
			super.registerReceiverToTopic(receiver, topic);
			application.registerTopic(topic);
		}
	}

	@Override
	public void unregisterReceiverFromTopic(MessageReceiver receiver, long topic) throws NodeException {
		synchronized (this) {
			super.unregisterReceiverFromTopic(receiver, topic);

			if (!super.hasTopic(topic)) {
				application.unregisterTopic(topic);
			}
		}
	}

	/**
	 * @deprecated Use {@link PastryNodeImpl#unregisterReceiver(MessageReceiver)} instead!
	 */
	@Deprecated
	@Override
	public void unregisterAgent(long id) throws AgentNotKnownException {
		synchronized (this) {
			application.unregisterAgentTopic(id);
			super.unregisterAgent(id);
		}
	}

	@Override
	public void sendMessage(Message message, MessageResultListener listener, SendMode mode) {
		// TODO: use mode?!?!
		observerNotice(Event.MESSAGE_SENDING, pastryNode, message.getSenderId(), null, message.getRecipientId(),
				"broadcasting");

		registerAnswerListener(message.getId(), listener);

		application.sendMessage(message);
	}

	@Override
	public void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws AgentNotKnownException, NodeNotFoundException, L2pSecurityException {

		if (!(atNodeId instanceof NodeHandle)) {
			String addition = "(null)";
			if (atNodeId != null) {
				addition = atNodeId.getClass().toString();
			}
			IllegalArgumentException e = new IllegalArgumentException(
					"node id in pastry nets has to be a NodeHandle instance but is " + addition);
			e.printStackTrace();
			throw e;
		}

		observerNotice(Event.MESSAGE_SENDING, pastryNode, message.getSenderId(), atNodeId, message.getRecipientId(),
				"");

		registerAnswerListener(message.getId(), listener);

		try {
			application.sendMessage(new MessageEnvelope(pastryNode.getLocalHandle(), message), (NodeHandle) atNodeId);
		} catch (MalformedXMLException e) {
			observerNotice(Event.MESSAGE_FAILED, pastryNode, message.getSenderId(), atNodeId, message.getRecipientId(),
					"XML exception!");
		} catch (MessageException e) {
			observerNotice(Event.MESSAGE_FAILED, pastryNode, message.getSenderId(), atNodeId, message.getRecipientId(),
					"Message exception!");
		}
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public Envelope fetchArtifact(long id) throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(Long.toString(id));
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public Envelope fetchArtifact(String identifier) throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(identifier);
	}

	/**
	 * @deprecated Use {@link #storeEnvelope(Envelope, Agent)} instead
	 */
	@Deprecated
	@Override
	public void storeArtifact(Envelope envelope) throws StorageException {
		storeEnvelope(envelope, Context.getCurrent().getMainAgent());
	}

	/**
	 * @deprecated Use {@link #removeEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public void removeArtifact(long id, byte[] signature) throws ArtifactNotFoundException, StorageException {
		removeEnvelope(Long.toString(id));
	}

	@Override
	public Object[] findRegisteredAgent(long agentId, int hintOfExpectedCount) throws AgentNotKnownException {
		observerNotice(Event.AGENT_SEARCH_STARTED, pastryNode, agentId, null, (Long) null, "");
		return application.searchAgent(agentId, hintOfExpectedCount).toArray();
	}

	@Override
	public boolean knowsAgentLocally(long agentId) {
		return locallyKnownAgents.hasAgent(agentId);
	}

	/**
	 * provides access to the underlying pastry application mostly for testing purposes
	 * 
	 * @return the pastry node application of this pastry node
	 */
	public NodeApplication getApplication() {
		return application;
	}

	@Override
	public Agent getAgent(long id) throws AgentNotKnownException {
		if (!locallyKnownAgents.hasAgent(id)) {
			observerNotice(Event.AGENT_GET_STARTED, pastryNode, id, null, (Long) null, "");
			try {
				Envelope agentEnvelope = pastStorage.fetchEnvelope(Envelope.getAgentIdentifier(id), AGENT_GET_TIMEOUT);
				Agent agentFromNet = Agent.createFromXml((String) agentEnvelope.getContent());
				observerNotice(Event.AGENT_GET_SUCCESS, pastryNode, id, null, (Long) null, "");
				locallyKnownAgents.registerAgent(agentFromNet);
			} catch (Exception e) {
				observerNotice(Event.AGENT_GET_FAILED, pastryNode, id, null, (Long) null, "");
				throw new AgentNotKnownException("Unable to retrieve Agent " + id + " from past storage", e);
			}
		}
		return locallyKnownAgents.getAgent(id);
	}

	@Override
	public void storeAgent(Agent agent) throws L2pSecurityException, AgentException {
		if (agent.isLocked()) {
			throw new L2pSecurityException("You have to unlock the agent before storage!");
			// because the agent has to sign itself
		}
		if (locallyKnownAgents.hasAgent(agent.getId())) {
			throw new AgentAlreadyRegisteredException("This agent is already known locally!");
		}
		observerNotice(Event.AGENT_UPLOAD_STARTED, pastryNode, agent, "");
		try {
			Agent stored = getAgent(agent.getId());
			observerNotice(Event.AGENT_UPLOAD_FAILED, pastryNode, agent, "Agent already known!");
			throw new AgentAlreadyRegisteredException("I already know stored version: " + stored);
		} catch (AgentNotKnownException e) {
		}
		locallyKnownAgents.registerAgent(agent);
		try {
			Envelope agentEnvelope = null;
			try {
				agentEnvelope = pastStorage.fetchEnvelope(Envelope.getAgentIdentifier(agent.getId()),
						AGENT_GET_TIMEOUT);
				agentEnvelope = pastStorage.createUnencryptedEnvelope(agentEnvelope, agentEnvelope.toXmlString());
			} catch (ArtifactNotFoundException e) {
				agentEnvelope = pastStorage.createUnencryptedEnvelope(Envelope.getAgentIdentifier(agent.getId()),
						agent.toXmlString());
			}
			pastStorage.storeEnvelope(agentEnvelope, agent, AGENT_STORE_TIMEOUT);
			if (agent instanceof UserAgent) {
				getUserManager().registerUserAgent((UserAgent) agent);
			}
			observerNotice(Event.AGENT_UPLOAD_SUCCESS, pastryNode, agent, "");
		} catch (CryptoException | SerializationException | StorageException e) {
			locallyKnownAgents.unregisterAgent(agent);
			observerNotice(Event.AGENT_UPLOAD_FAILED, pastryNode, agent, "Got interrupted!");
			throw new AgentException("Storage has been interrupted", e);
		}
	}

	/**
	 * @deprecated Use {@link #storeAgent(Agent)} instead
	 */
	@Deprecated
	@Override
	public void updateAgent(Agent agent) throws AgentException, L2pSecurityException, StorageException {
		storeAgent(agent);
	}

	/**
	 * get the identifier of this node (string representation of the pastry node)
	 * 
	 * @return complete identifier of this pastry node as String
	 */
	@Override
	public Serializable getNodeId() {
		if (pastryNode == null) {
			return null;
		} else {
			return pastryNode.getLocalNodeHandle();
		}
	}

	public int getPort() {
		return pastryPort;
	}

	@Override
	public Object[] getOtherKnownNodes() {
		return pastryNode.getLeafSet().getUniqueSet().toArray();
	}

	@Override
	public NodeInformation getNodeInformation(Object nodeId) throws NodeNotFoundException {
		if (!(nodeId instanceof NodeHandle)) {
			throw new NodeNotFoundException("Given node id is not a pastry Node Handle!");
		}
		return application.getNodeInformation((NodeHandle) nodeId);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException {
		storeEnvelope(envelope, author, ARTIFACT_STORE_TIMEOUT);
	}

	@Override
	public Envelope fetchEnvelope(String identifier) throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(identifier, ARTIFACT_GET_TIMEOUT);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createEnvelope(identifier, content, reader);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createEnvelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createEnvelope(previousVersion, content);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createEnvelope(previousVersion, content, reader);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createEnvelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createUnencryptedEnvelope(identifier, content);
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return pastStorage.createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException {
		try {
			pastStorage.storeEnvelope(envelope, author, timeoutMs);
		} catch (StorageException e) {
			observerNotice(Event.ARTIFACT_UPLOAD_FAILED, pastryNode,
					"Storage error for Artifact " + envelope.getIdentifier());
			throw e; // transparent exception forwarding
		}
		observerNotice(Event.ARTIFACT_ADDED, pastryNode, envelope.getIdentifier());
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		pastStorage.storeEnvelopeAsync(envelope, author, new StorageStoreResultHandler() {
			@Override
			public void onResult(Serializable serializable, int successfulOperations) {
				observerNotice(Event.ARTIFACT_ADDED, pastryNode, envelope.getIdentifier());
				if (resultHandler != null) {
					resultHandler.onResult(serializable, successfulOperations);
				}
			}
		}, collisionHandler, new StorageExceptionHandler() {
			@Override
			public void onException(Exception e) {
				observerNotice(Event.ARTIFACT_UPLOAD_FAILED, pastryNode,
						"Storage error for Artifact " + envelope.getIdentifier());
				if (exceptionHandler != null) {
					exceptionHandler.onException(e);
				}
			}
		});
	}

	@Override
	public Envelope fetchEnvelope(String identifier, long timeoutMs)
			throws ArtifactNotFoundException, StorageException {
		if (pastStorage == null) {
			throw new IllegalStateException(
					"Past storage not initialized! You can fetch artifacts only from running nodes!");
		}
		observerNotice(Event.ARTIFACT_FETCH_STARTED, pastryNode, identifier);
		try {
			Envelope contentFromNet = pastStorage.fetchEnvelope(identifier, timeoutMs);
			observerNotice(Event.ARTIFACT_RECEIVED, pastryNode, identifier);
			return contentFromNet;
		} catch (ArtifactNotFoundException e) {
			observerNotice(Event.ARTIFACT_FETCH_FAILED, pastryNode, identifier);
			throw e; // transparent exception forwarding
		} catch (Exception e) {
			observerNotice(Event.ARTIFACT_FETCH_FAILED, pastryNode, identifier);
			throw new StorageException("Unable to retrieve Artifact from past storage", e);
		}
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		if (getStatus() != NodeStatus.RUNNING) {
			throw new IllegalStateException("You can fetch artifacts only from running nodes!");
		}
		observerNotice(Event.ARTIFACT_FETCH_STARTED, pastryNode, identifier);
		pastStorage.fetchEnvelopeAsync(identifier, new StorageEnvelopeHandler() {
			@Override
			public void onEnvelopeReceived(Envelope result) {
				observerNotice(Event.ARTIFACT_RECEIVED, pastryNode, identifier);
				if (envelopeHandler != null) {
					envelopeHandler.onEnvelopeReceived(result);
				}
			}
		}, new StorageExceptionHandler() {
			@Override
			public void onException(Exception e) {
				observerNotice(Event.ARTIFACT_FETCH_FAILED, pastryNode, identifier);
				if (exceptionHandler != null) {
					exceptionHandler.onException(e);
				}
			}
		});
	}

	@Override
	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException {
		pastStorage.removeEnvelope(identifier);
	}

}
