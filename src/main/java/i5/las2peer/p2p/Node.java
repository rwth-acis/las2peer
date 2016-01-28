package i5.las2peer.p2p;

import java.io.File;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import com.sun.management.OperatingSystemMXBean;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.RMIExceptionContent;
import i5.las2peer.communication.RMIResultContent;
import i5.las2peer.communication.RMIUnlockContent;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NotFinishedException;
import i5.las2peer.execution.RMITask;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.execution.UnlockNeededException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.logging.monitoring.MonitoringObserver;
import i5.las2peer.p2p.pastry.PastryStorageException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.AgentStorage;
import i5.las2peer.security.Context;
import i5.las2peer.security.DuplicateEmailException;
import i5.las2peer.security.DuplicateLoginNameException;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.MonitoringAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.ServiceInfoAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.security.UserAgentList;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.socket.SocketNodeHandle;

/**
 * Base class for nodes in the LAS2peer environment.
 * 
 * A Node represents one enclosed unit in the network hosting an arbitrary number of agents willing to participate in
 * the P2P networking.
 * 
 */
public abstract class Node implements AgentStorage {

	private static final String MAINLIST_ID = "mainlist";

	/**
	 * The Sending mode for outgoing messages.
	 */
	public enum SendMode {
		ANYCAST,
		BROADCAST
	}

	/**
	 * Enum with the possible states of a node.
	 */
	public enum NodeStatus {
		UNCONFIGURED,
		CONFIGURED,
		STARTING,
		RUNNING,
		CLOSING,
		CLOSED
	}

	/**
	 * For performance measurement (load balance)
	 */
	private OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();
	public static final double DEFAULT_CPU_LOAD_TRESHOLD = 0.5;
	private double cpuLoadThreshold = DEFAULT_CPU_LOAD_TRESHOLD; // TODO: make it configurable
	private NodeServiceCache nodeServiceCache;
	// TODO make time as setting
	private int nodeServiceCacheLifetime = 10; // time before cached node info becomes invalidated

	/**
	 * observers to be notified of all occurring events
	 */
	private HashSet<NodeObserver> observers = new HashSet<NodeObserver>();

	/**
	 * contexts for local method invocation
	 */
	private Hashtable<Long, Context> htLocalExecutionContexts = new Hashtable<Long, Context>();

	/**
	 * status of this node
	 */
	private NodeStatus status = NodeStatus.UNCONFIGURED;

	/**
	 * hashtable with all {@link i5.las2peer.security.MessageReceiver}s registered at this node
	 */
	private Hashtable<Long, MessageReceiver> htRegisteredReceivers = new Hashtable<Long, MessageReceiver>();

	private ClassLoader baseClassLoader = null;

	private Hashtable<Long, MessageResultListener> htAnswerListeners = new Hashtable<Long, MessageResultListener>();

	private static final String DEFAULT_INFORMATION_FILE = "etc/nodeInfo.xml";
	private String sInformationFileName = DEFAULT_INFORMATION_FILE;

	private KeyPair nodeKeyPair;

	/**
	 * the list of users containing email an login name tables
	 */
	private Envelope activeUserList = null;

	/**
	 * a set of updates on user Agents to perform, if an update storage of {@link activeUserList} fails
	 */
	private HashSet<UserAgent> hsUserUpdates = new HashSet<UserAgent>();

	/**
	 * Creates a new node, if the standardObserver flag is true, an observer logging all events to a simple plain text
	 * log file will be generated. If not, no observer will be used at startup.
	 * 
	 * @param standardObserver
	 */
	public Node(boolean standardObserver) {
		this(null, standardObserver);
	}

	/**
	 * Creates a new node with a standard plain text log file observer.
	 */
	public Node() {
		this(true);
	}

	/**
	 * @param baseClassLoader
	 */
	public Node(ClassLoader baseClassLoader) {
		this(baseClassLoader, true);
	}

	/**
	 * @param baseClassLoader
	 * @param standardObserver
	 */
	public Node(ClassLoader baseClassLoader, boolean standardObserver) {
		this(baseClassLoader, standardObserver, false);
	}

	/**
	 * Generates a new Node with the given baseClassLoader. The Observer-flags determine, which observers will be
	 * registered at startup.
	 * 
	 * @param baseClassLoader
	 * @param standardObserver
	 * @param monitoringObserver
	 */
	public Node(ClassLoader baseClassLoader, boolean standardObserver, boolean monitoringObserver) {
		if (standardObserver) {
			initStandardLogfile();
		}
		if (monitoringObserver) {
			addObserver(new MonitoringObserver(50, this));
		}

		this.baseClassLoader = baseClassLoader;

		if (baseClassLoader == null)
			this.baseClassLoader = this.getClass().getClassLoader();

		nodeKeyPair = CryptoTools.generateKeyPair();
		nodeServiceCache = new NodeServiceCache(this, nodeServiceCacheLifetime);
	}

	/**
	 * Gets the public key of this node.
	 * 
	 * @return a public key
	 */
	public PublicKey getPublicNodeKey() {
		return nodeKeyPair.getPublic();
	}

	/**
	 * Creates an observer for a standard log-file. The name of the log-file will contain the id of the node to prevent
	 * conflicts if running multiple nodes on the same machine.
	 */
	private void initStandardLogfile() {
		addObserver(L2pLogger.getInstance(Node.class.getName()));
	}

	/**
	 * Handles a request from the (p2p) net to unlock the private key of a remote Agent.
	 * 
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public void unlockRemoteAgent(long agentId, byte[] enctryptedPass)
			throws L2pSecurityException, AgentNotKnownException, SerializationException, CryptoException {

		String passphrase = (String) CryptoTools.decryptAsymmetric(enctryptedPass, nodeKeyPair.getPrivate());

		Context context = getAgentContext(agentId);

		if (!context.getMainAgent().isLocked())
			return;

		context.unlockMainAgent(passphrase);

		observerNotice(Event.AGENT_UNLOCKED, this.getNodeId(), agentId, null, (Long) null, "");
	}

	/**
	 * Sends a request to unlock the agent's private key to the target node.
	 * 
	 * @param agentId
	 * @param passphrase
	 * @param targetNode
	 * @param nodeEncryptionKey
	 * @throws L2pSecurityException
	 */
	public abstract void sendUnlockRequest(long agentId, String passphrase, Object targetNode,
			PublicKey nodeEncryptionKey) throws L2pSecurityException;

	/**
	 * Adds an observer to this node.
	 * 
	 * @param observer
	 */
	public void addObserver(NodeObserver observer) {
		observers.add(observer);
	}

	/**
	 * Removes an observer from this node.
	 * 
	 * @param observer
	 */
	public void removeObserver(NodeObserver observer) {
		observers.remove(observer);
	}

	/**
	 * Enables the service monitoring for the requested Service.
	 * 
	 * @param service
	 */
	public void setServiceMonitoring(ServiceAgent service) {
		observerNotice(Event.SERVICE_ADD_TO_MONITORING, this.getNodeId(), service.getId(), null, null,
				service.getServiceClassName());
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param remarks
	 */
	public void observerNotice(Event event, String remarks) {
		Long sourceAgentId = null; // otherwise calls are ambigious
		observerNotice(event, null, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param remarks
	 */
	public void observerNotice(Event event, Object sourceNode, String remarks) {
		Long sourceAgentId = null; // otherwise calls are ambigious
		observerNotice(event, sourceNode, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param remarks
	 */
	public void observerNotice(Event event, Object sourceNode, long sourceAgentId, String remarks) {
		observerNotice(event, sourceNode, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgent
	 * @param remarks
	 */
	public void observerNotice(Event event, Object sourceNode, MessageReceiver sourceAgent, String remarks) {
		Long sourceAgentId = null;
		if (sourceAgent != null)
			sourceAgentId = sourceAgent.getResponsibleForAgentId();
		observerNotice(event, sourceNode, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgent
	 * @param destinationNode
	 * @param destinationAgent
	 * @param remarks
	 */
	public void observerNotice(Event event, Object sourceNode, Agent sourceAgent, Object destinationNode,
			Agent destinationAgent, String remarks) {
		Long sourceAgentId = null;
		if (sourceAgent != null)
			sourceAgentId = sourceAgent.getId();
		Long destinationAgentId = null;
		if (destinationAgent != null)
			destinationAgentId = destinationAgent.getId();
		observerNotice(event, sourceNode, sourceAgentId, destinationNode, destinationAgentId, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param destinationNode
	 * @param destinationAgentId
	 * @param remarks
	 */
	public void observerNotice(Event event, Object sourceNode, Long sourceAgentId, Object destinationNode,
			Long destinationAgentId, String remarks) {
		long timestamp = new Date().getTime();
		String sourceNodeRepresentation = getNodeRepresentation(sourceNode);
		String destinationNodeRepresentation = getNodeRepresentation(destinationNode);
		for (NodeObserver ob : observers)
			ob.log(timestamp, event, sourceNodeRepresentation, sourceAgentId, destinationNodeRepresentation,
					destinationAgentId, remarks);
	}

	/**
	 * Derive a String representation for a node from the given identifier object. The type of the object depends on the
	 * setting of the current node.
	 * 
	 * Tries to specify an ip address and a port for an actual p2p node ({@link i5.las2peer.p2p.PastryNodeImpl} or
	 * {@link rice.pastry.NodeHandle}).
	 * 
	 * @param node
	 * @return string representation for the given node object
	 */
	protected String getNodeRepresentation(Object node) {
		if (node == null)
			return null;
		else if (node instanceof SocketNodeHandle) {
			SocketNodeHandle nh = (SocketNodeHandle) node;
			return nh.getId() + "/" + nh.getIdentifier();
		} else if (node instanceof PastryNode) {
			PastryNode pNode = (PastryNode) node;
			return getNodeRepresentation(pNode.getLocalNodeHandle());
		} else
			return "" + node + " (" + node.getClass().getName() + ")";
	}

	/**
	 * Gets the status of this node.
	 * 
	 * @return status of this node
	 */
	public NodeStatus getStatus() {
		return status;
	}

	/**
	 * Gets some kind of node identifier.
	 * 
	 * @return id of this node
	 */
	public abstract Serializable getNodeId();

	/**
	 * Gets the class loader, this node is bound to. In a <i>real</i> LAS2peer environment, this should refer to a
	 * {@link i5.las2peer.classLoaders.L2pClassLoader}
	 * 
	 * Otherwise, the class loader of this Node class is used.
	 * 
	 * @return a class loader
	 */
	public ClassLoader getBaseClassLoader() {
		return baseClassLoader;
	}

	/**
	 * Sets the status of this node.
	 * 
	 * @param newstatus
	 */
	protected void setStatus(NodeStatus newstatus) {
		if (newstatus == NodeStatus.RUNNING && this instanceof PastryNodeImpl) {
			observerNotice(Event.NODE_STATUS_CHANGE, this.getNodeId(), "" + newstatus);
		} else if (newstatus == NodeStatus.CLOSING) {
			observerNotice(Event.NODE_STATUS_CHANGE, this.getNodeId(), "" + newstatus);
		} else {
			observerNotice(Event.NODE_STATUS_CHANGE, "" + newstatus);
		}
		status = newstatus;
	}

	/**
	 * Gets the filename of the current information file for this node. The file should be an XML file representation of
	 * a {@link NodeInformation}.
	 * 
	 * @return filename
	 */
	public String getInformationFilename() {
		return sInformationFileName;
	}

	/**
	 * Sets the nodes information filename.
	 * 
	 * @param filename
	 */
	public void setInformationFilename(String filename) {
		if (new File(filename).exists())
			sInformationFileName = filename;
	}

	/**
	 * Gets information about this node including all registered service classes.
	 * 
	 * @return node information
	 * @throws CryptoException
	 */
	public NodeInformation getNodeInformation() throws CryptoException {
		NodeInformation result = new NodeInformation(getRegisteredServices());

		try {
			if (sInformationFileName != null && new File(sInformationFileName).exists())
				result = NodeInformation.createFromXmlFile(sInformationFileName, getRegisteredServices());
		} catch (Exception e) {
			e.printStackTrace();
		}

		result.setNodeHandle(getNodeId());
		result.setNodeKey(nodeKeyPair.getPublic());

		result.setSignature(CryptoTools.signContent(result.getSignatureContent(), nodeKeyPair.getPrivate()));

		return result;
	}

	/**
	 * Gets information about a distant node.
	 * 
	 * @param nodeId
	 * 
	 * @return information about the node
	 * @throws NodeNotFoundException
	 */
	public abstract NodeInformation getNodeInformation(Object nodeId) throws NodeNotFoundException;

	/**
	 * Gets an array with identifiers of other (locally known) nodes in this network.
	 * 
	 * @return array with handles of other (known) p2p network nodes
	 */
	public abstract Object[] getOtherKnownNodes();

	/**
	 * Starts this node.
	 */
	public abstract void launch() throws NodeException;

	/**
	 * Stops the node.
	 */
	public synchronized void shutDown() {
		for (Long id : htRegisteredReceivers.keySet())
			htRegisteredReceivers.get(id).notifyUnregister();
		observerNotice(Event.NODE_SHUTDOWN, this.getNodeId(), null);
		for (NodeObserver observer : observers) {
			if (observer instanceof MonitoringObserver) {
				try {
					System.out.println("Wait a little to give the observer time to send its last message...");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		htRegisteredReceivers = new Hashtable<Long, MessageReceiver>();
	}

	/**
	 * Registers a (local) Agent for usage through this node. The Agent has to be unlocked before registration.
	 * 
	 * @param receiver
	 *
	 * @throws AgentAlreadyRegisteredException the given agent is already registered to this node
	 * @throws L2pSecurityException the agent is not unlocked
	 * @throws AgentException any problem with the agent itself (probably on calling
	 *             {@link i5.las2peer.security.Agent#notifyRegistrationTo}
	 */
	public void registerReceiver(MessageReceiver receiver)
			throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException {
		if (getStatus() != NodeStatus.RUNNING)
			throw new IllegalStateException("You can register agents only to running nodes!");

		if (htRegisteredReceivers.get(receiver.getResponsibleForAgentId()) != null) {
			// throw new AgentAlreadyRegisteredException ("This agent is already running here!");
			// why throw an exception here?
			return;
		}

		if ((receiver instanceof Agent)) {
			// we have an agent
			Agent agent = (Agent) receiver;
			if (agent.isLocked())
				throw new L2pSecurityException("An agent has to be unlocked for registering at a node");

			if (!knowsAgentLocally(agent.getId()))
				try {
					storeAgent(agent);
				} catch (AgentAlreadyRegisteredException e) {
					System.out.println(
							"Just for notice - not an error: tried to store an already known agent before registering");
					// nothing to do
				}

			try {
				// ensure (unlocked) context
				getAgentContext((Agent) receiver);
			} catch (Exception e) {
			}
			if (agent instanceof UserAgent) {
				observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), agent, "UserAgent");
			} else if (agent instanceof ServiceAgent) {
				observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), agent, "ServiceAgent");
			} else if (agent instanceof GroupAgent) {
				observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), agent, "GroupAgent");
			} else if (agent instanceof MonitoringAgent) {
				observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), agent, "MonitoringAgent");
			} else if (agent instanceof ServiceInfoAgent) {
				observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), agent, "ServiceInfoAgent");
			}
		} else {
			// ok, we have a mediator
			observerNotice(Event.AGENT_REGISTERED, this.getNodeId(), receiver, "Mediator");
		}

		htRegisteredReceivers.put(receiver.getResponsibleForAgentId(), receiver);

		try {
			receiver.notifyRegistrationTo(this);
		} catch (AgentException e) {
			observerNotice(Event.AGENT_LOAD_FAILED, this, receiver, e.toString());

			htRegisteredReceivers.remove(receiver.getResponsibleForAgentId());
			throw e;
		} catch (Exception e) {
			observerNotice(Event.AGENT_LOAD_FAILED, this, receiver, e.toString());

			htRegisteredReceivers.remove(receiver.getResponsibleForAgentId());
			throw new AgentException("problems notifying agent of registration", e);
		}

	}

	/**
	 * Unregisters an agent from this node.
	 * 
	 * @param agent
	 * @throws AgentNotKnownException the agent is not registered to this node
	 */
	public void unregisterAgent(Agent agent) throws AgentNotKnownException {
		unregisterAgent(agent.getId());
	}

	/**
	 * Is an instance of the given agent running at this node?
	 * 
	 * @param agentId
	 */
	public void unregisterAgent(long agentId) throws AgentNotKnownException {

		if (htRegisteredReceivers.get(agentId) == null)
			throw new AgentNotKnownException(agentId);

		observerNotice(Event.AGENT_REMOVED, this.getNodeId(), getAgent(agentId), "");

		htRegisteredReceivers.get(agentId).notifyUnregister();

		htRegisteredReceivers.remove(agentId);
	}

	/**
	 * Is an instance of the given agent running at this node?
	 * 
	 * @param agent
	 * @return true, if the given agent is running at this node
	 */
	public boolean hasLocalAgent(Agent agent) {
		return hasLocalAgent(agent.getId());
	}

	/**
	 * Is an instance of the given agent running at this node?
	 * 
	 * @param agentId
	 * @return true, if the given agent is registered here
	 */
	public boolean hasLocalAgent(long agentId) {
		return htRegisteredReceivers.get(agentId) != null;
	}

	/**
	 * Sends a message, recipient and sender are stated in the message. The node tries to find a node hosting the
	 * recipient and sends the message there.
	 * 
	 * @param message the message to send
	 * @param listener a listener for getting the result separately
	 */
	public void sendMessage(Message message, MessageResultListener listener) {
		sendMessage(message, listener, SendMode.ANYCAST);
	}

	/**
	 * Sends a message, recipient and sender are stated in the message. Depending on the mode, either all nodes running
	 * the given agent will be notified of this message, or only a random one.
	 * 
	 * NOTE: Pastry nodes will always use broadcast at the moment!
	 * 
	 * @param message the message to send
	 * @param listener a listener for getting the result separately
	 * @param mode is it a broadcast or an any-cast message?
	 */
	public abstract void sendMessage(Message message, MessageResultListener listener, SendMode mode);

	/**
	 * Sends a message to the agent residing at the given node.
	 * 
	 * @param message
	 * @param atNodeId
	 * @param listener a listener for getting the result separately
	 * 
	 * @throws AgentNotKnownException
	 * @throws NodeNotFoundException
	 * @throws L2pSecurityException
	 */
	public abstract void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws AgentNotKnownException, NodeNotFoundException, L2pSecurityException;

	/**
	 * Sends the given response message to the given node.
	 * 
	 * @param message
	 * @param atNodeId
	 * 
	 * @throws AgentNotKnownException
	 * @throws NodeNotFoundException
	 * @throws L2pSecurityException
	 */
	public void sendResponse(Message message, Object atNodeId)
			throws AgentNotKnownException, NodeNotFoundException, L2pSecurityException {
		sendMessage(message, atNodeId, null);
	}

	/**
	 * For <i>external</i> access to this node. Will be called by the (P2P) network library, when a new message has been
	 * received via the network and could not be handled otherwise.
	 * 
	 * Make sure, that the {@link #baseClassLoader} method is used for answer messages.
	 * 
	 * @param message
	 * 
	 * @throws AgentNotKnownException the designated recipient is not known at this node
	 * @throws MessageException
	 * @throws L2pSecurityException
	 */
	public void receiveMessage(Message message) throws AgentNotKnownException, MessageException, L2pSecurityException {
		if (message.isResponse())
			if (handoverAnswer(message))
				return;

		// Since this field is not always available
		if (message.getSendingNodeId() != null) {
			observerNotice(Event.MESSAGE_RECEIVED, message.getSendingNodeId(), message.getSenderId(), this.getNodeId(),
					message.getRecipientId(), message.getId() + "");
		} else {
			observerNotice(Event.MESSAGE_RECEIVED, null, message.getSenderId(), this.getNodeId(),
					message.getRecipientId(), message.getId() + "");
		}
		MessageReceiver receiver = htRegisteredReceivers.get(message.getRecipientId());

		if (receiver == null)
			throw new AgentNotKnownException(message.getRecipientId());

		receiver.receiveMessage(message, getAgentContext(message.getSenderId()));
	}

	/**
	 * Gets an artifact from the p2p storage.
	 * 
	 * @param id
	 * 
	 * @return the envelope containing the requested artifact
	 * 
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public abstract Envelope fetchArtifact(long id) throws ArtifactNotFoundException, StorageException;

	/**
	 * Stores an artifact to the p2p storage.
	 * 
	 * @param envelope
	 * @throws StorageException
	 * @throws L2pSecurityException
	 * 
	 */
	public abstract void storeArtifact(Envelope envelope) throws StorageException, L2pSecurityException;

	/**
	 * Removes an artifact from the p2p storage. <i>NOTE: This is not possible with a FreePastry backend!</i>
	 * 
	 * @param id
	 * @param signature
	 * 
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public abstract void removeArtifact(long id, byte[] signature) throws ArtifactNotFoundException, StorageException;

	/**
	 * Searches the nodes for registered Versions of the given Agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agentId id of the agent to look for
	 * @param hintOfExpectedCount a hint for the expected number of results (e.g. to wait for)
	 * @return array with the IDs of nodes, where the given agent is registered
	 * 
	 * @throws AgentNotKnownException
	 */
	public abstract Object[] findRegisteredAgent(long agentId, int hintOfExpectedCount) throws AgentNotKnownException;

	/**
	 * Search the nodes for registered versions of the given agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agent
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotKnownException
	 */
	public Object[] findRegisteredAgent(Agent agent) throws AgentNotKnownException {
		return findRegisteredAgent(agent.getId());
	}

	/**
	 * Searches the nodes for registered versions of the given agentId. Returns an array of objects identifying the
	 * nodes the given agent is registered to.
	 * 
	 * @param agentId id of the agent to look for
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotKnownException
	 */
	public Object[] findRegisteredAgent(long agentId) throws AgentNotKnownException {
		return findRegisteredAgent(agentId, 1);
	}

	/**
	 * searches the nodes for registered versions of the given agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agent
	 * @param hintOfExpectedCount a hint for the expected number of results (e.g. to wait for)
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotKnownException
	 */
	public Object[] findRegisteredAgent(Agent agent, int hintOfExpectedCount) throws AgentNotKnownException {
		return findRegisteredAgent(agent.getId(), hintOfExpectedCount);
	}

	/**
	 * Gets an agent description from the net.
	 * 
	 * make sure, always to return fresh versions of the requested agent, so that no thread can unlock the private key
	 * for another one!
	 * 
	 * @param id
	 * 
	 * @return the requested agent
	 * 
	 * @throws AgentNotKnownException
	 */
	public abstract Agent getAgent(long id) throws AgentNotKnownException;

	/**
	 * Does this node know an agent with the given id?
	 * 
	 * from {@link i5.las2peer.security.AgentStorage}
	 * 
	 * @param id
	 * @return true, if this node knows the given agent
	 */
	@Override
	public boolean hasAgent(long id) {
		// Since an request for this agent is probable after this check, it makes sense
		// to try to load it into this node and decide afterwards

		try {
			getAgent(id);
			return true;
		} catch (AgentNotKnownException e) {
			return false;
		}
	}

	/**
	 * Checks, if an agent of the given id is known locally.
	 * 
	 * @param agentId
	 * @return true, if this agent is (already) known here at this node
	 */
	public abstract boolean knowsAgentLocally(long agentId);

	/**
	 * Gets a local registered agent by its id.
	 * 
	 * @param id
	 * 
	 * @return the agent registered to this node
	 * @throws AgentNotKnownException
	 */
	public Agent getLocalAgent(long id) throws AgentNotKnownException {
		MessageReceiver result = htRegisteredReceivers.get(id);

		if (result == null)
			throw new AgentNotKnownException("The given agent agent is not registered to this node");

		if (result instanceof Agent)
			return (Agent) result;
		else
			throw new AgentNotKnownException("The requested Agent is only known as a Mediator here!");
	}

	/**
	 * Gets an array with all {@link i5.las2peer.security.UserAgent}s registered at this node.
	 * 
	 * @return all local registered UserAgents
	 */
	public UserAgent[] getRegisteredAgents() {
		Vector<UserAgent> result = new Vector<UserAgent>();

		for (MessageReceiver rec : htRegisteredReceivers.values()) {
			if (rec instanceof UserAgent)
				result.add((UserAgent) rec);
		}

		return result.toArray(new UserAgent[0]);
	}

	/**
	 * Gets an array with all {@link i5.las2peer.security.ServiceAgent}s registered at this node.
	 * 
	 * @return all local registered ServiceAgents
	 */
	public ServiceAgent[] getRegisteredServices() {
		Vector<ServiceAgent> result = new Vector<ServiceAgent>();

		for (MessageReceiver rec : htRegisteredReceivers.values()) {
			if (rec instanceof ServiceAgent)
				result.add((ServiceAgent) rec);
		}

		return result.toArray(new ServiceAgent[0]);

	}

	/**
	 * Gets a local registered mediator for the given agent id. If no mediator exists, registers a new one to this node.
	 * 
	 * @param agent
	 * 
	 * @return the mediator for the given agent
	 * 
	 * @throws AgentNotKnownException
	 * @throws L2pSecurityException
	 * @throws AgentException
	 * @throws AgentAlreadyRegisteredException
	 */
	public Mediator getOrRegisterLocalMediator(Agent agent)
			throws AgentNotKnownException, L2pSecurityException, AgentAlreadyRegisteredException, AgentException {
		if (agent.isLocked())
			throw new L2pSecurityException("you need to unlock the agent for mediation!");
		MessageReceiver result = htRegisteredReceivers.get(agent.getId());

		if (result != null && !(result instanceof Mediator))
			throw new AgentNotKnownException("The requested Agent is registered directly at this node!");

		if (result == null) {
			getAgentContext(agent);
			result = new Mediator(agent);
			registerReceiver(result);
		}

		return (Mediator) result;
	}

	/**
	 * Stores a new Agent to the network.
	 * 
	 * @param agent
	 * 
	 * @throws AgentAlreadyRegisteredException
	 * @throws L2pSecurityException
	 * @throws AgentException
	 */
	public abstract void storeAgent(Agent agent)
			throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException;

	/**
	 * Updates an existing agent of the network.
	 * 
	 * @param agent
	 * 
	 * @throws AgentException
	 * @throws L2pSecurityException
	 * @throws PastryStorageException
	 */
	public abstract void updateAgent(Agent agent) throws AgentException, L2pSecurityException, PastryStorageException;

	private Agent anonymousAgent = null;

	/**
	 * get an agent to use, if no <i>real</i> agent is available
	 * 
	 * @return a generic anonymous agent
	 */
	public Agent getAnonymous() {
		if (anonymousAgent == null) {
			try {
				anonymousAgent = getAgent(MockAgentFactory.getAnonymous().getId());
			} catch (Exception e) {
				try {
					anonymousAgent = MockAgentFactory.getAnonymous();
					((UserAgent) anonymousAgent).unlockPrivateKey("anonymous");
					storeAgent(anonymousAgent);
				} catch (Exception e1) {
					throw new RuntimeException("No anonymous agent could be initialized!?!", e1);
				}
			}
		}

		Agent result;
		try {
			result = anonymousAgent.cloneLocked();
			((UserAgent) result).unlockPrivateKey("anonymous");
		} catch (Exception e) {
			throw new RuntimeException("Strange - should not happen...");
		}

		return result;
	}

	/**
	 * Loads the central user list from the backend.
	 */
	private void loadUserList() {
		Agent owner = getAnonymous();
		boolean bLoadedOrCreated = false;
		try {
			try {
				activeUserList = fetchArtifact(Envelope.getClassEnvelopeId(UserAgentList.class, MAINLIST_ID));
				activeUserList.open(owner);
				bLoadedOrCreated = true;
			} catch (Exception e) {
				if (activeUserList == null) {
					activeUserList = Envelope.createClassIdEnvelope(new UserAgentList(), MAINLIST_ID, owner);
					activeUserList.open(owner);
					activeUserList.setOverWriteBlindly(false);
					activeUserList.lockContent();
					bLoadedOrCreated = true;
				}
			}

			if (bLoadedOrCreated && hsUserUpdates.size() > 0) {
				for (UserAgent agent : hsUserUpdates)
					activeUserList.getContent(UserAgentList.class).updateUser(agent);
				doStoreUserList();
			}
		} catch (Exception e) {
			observerNotice(Event.NODE_ERROR, "Error updating the user registration list: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Stores the current list. If the storage fails e.g. due to a failed up-to-date-check of the current user list, the
	 * storage is attempted a second time.
	 */
	private void storeUserList() {
		synchronized (hsUserUpdates) {
			try {
				doStoreUserList();
				loadUserList();
			} catch (Exception e) {
				e.printStackTrace();

				// one retry because of actuality problems:
				loadUserList();
				try {
					doStoreUserList();
					loadUserList();
				} catch (Exception e1) {
					observerNotice(Event.NODE_ERROR, "Error storing new User List: " + e.toString());
				}
			}
		}
	}

	/**
	 * The actual backend storage procedure.
	 * 
	 * @throws EncodingFailedException
	 * @throws StorageException
	 * @throws L2pSecurityException
	 * @throws DecodingFailedException
	 */
	private void doStoreUserList()
			throws EncodingFailedException, StorageException, L2pSecurityException, DecodingFailedException {
		Agent anon = getAnonymous();
		activeUserList.open(anon);

		activeUserList.addSignature(anon);
		activeUserList.close();
		storeArtifact(activeUserList);
		activeUserList.open(anon);

		// ok, all changes are stored
		hsUserUpdates.clear();
	}

	/**
	 * Forces the node to publish all changes on the userlist.
	 */
	public void forceUserListUpdate() {
		if (hsUserUpdates.size() > 0)
			storeUserList();
	}

	/**
	 * Update the registry of login and mail addresses of known / stored user agents on an {@link #updateAgent(Agent)}
	 * or {@link #storeAgent(Agent)} action.
	 * 
	 * @param agent
	 * @throws DuplicateEmailException
	 * @throws DuplicateLoginNameException
	 */
	protected void updateUserAgentList(UserAgent agent) throws DuplicateEmailException, DuplicateLoginNameException {

		synchronized (hsUserUpdates) {
			if (activeUserList == null)
				loadUserList();

			try {
				activeUserList.getContent(UserAgentList.class).updateUser(agent);
			} catch (EnvelopeException e) {
				observerNotice(Event.NODE_ERROR, "Envelope error while updating user list: " + e);
			}

			synchronized (hsUserUpdates) {
				hsUserUpdates.add(agent);

				if (hsUserUpdates.size() > 10) {
					storeUserList();
				}
			}
		}
	}

	/**
	 * Gets an id for the user for the given login name.
	 * 
	 * @param login
	 * @return agent id
	 * @throws AgentNotKnownException
	 */
	public long getAgentIdForLogin(String login) throws AgentNotKnownException {
		if (activeUserList == null) {
			loadUserList();
		}

		if (activeUserList == null)
			throw new AgentNotKnownException("No agents known!");

		try {
			return activeUserList.getContent(UserAgentList.class).getLoginId(login);
		} catch (AgentNotKnownException e) {
			// retry once
			loadUserList();

			try {
				return activeUserList.getContent(UserAgentList.class).getLoginId(login);
			} catch (EnvelopeException e1) {
				throw e;
			}
		} catch (EnvelopeException e) {
			throw new AgentNotKnownException("Envelope Problems with user list!", e);
		}
	}

	/**
	 * Gets an id for the user for the given email address.
	 * 
	 * @param email
	 * @return agent id
	 * @throws AgentNotKnownException
	 */
	public long getAgentIdForEmail(String email) throws AgentNotKnownException {
		if (activeUserList == null)
			loadUserList();

		if (activeUserList == null)
			throw new AgentNotKnownException("No agents known!");

		try {
			return activeUserList.getContent(UserAgentList.class).getLoginId(email);
		} catch (AgentNotKnownException e) {
			throw e;
		} catch (EnvelopeException e) {
			throw new AgentNotKnownException("Evelope Problems with user list!", e);
		}
	}

	/**
	 * Gets the agent representing the given service class.
	 * 
	 * prefer using a locally registered agent
	 * 
	 * @param serviceClass
	 * @return the ServiceAgent responsible for the given service class
	 * @throws AgentNotKnownException
	 */
	public ServiceAgent getServiceAgent(String serviceClass) throws AgentNotKnownException {
		long agentId = ServiceAgent.serviceClass2Id(serviceClass);

		Agent result;
		try {
			result = getLocalAgent(agentId);
		} catch (AgentNotKnownException e) {
			result = getAgent(agentId);
		}

		if (result == null || !(result instanceof ServiceAgent)) {
			throw new AgentNotKnownException("The corresponding agent is not a ServiceAgent!?");
		}

		return (ServiceAgent) result;
	}

	/**
	 * Invokes a service method of a local running service agent.
	 * 
	 * @param executingAgentId
	 * @param serviceClass
	 * @param method
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws AgentNotKnownException cannot find the executing agent
	 * @throws L2pSecurityException
	 * @throws InterruptedException
	 * @throws L2pServiceException
	 */
	public Serializable invokeLocally(long executingAgentId, String serviceClass, String method,
			Serializable[] parameters)
					throws L2pSecurityException, AgentNotKnownException, InterruptedException, L2pServiceException {
		if (getStatus() != NodeStatus.RUNNING)
			throw new IllegalStateException("You can invoke methods only on a running node!");

		long serviceAgentId = ServiceAgent.serviceClass2Id(serviceClass);
		if (!hasLocalAgent(serviceAgentId))
			throw new NoSuchServiceException("Service not known locally!");

		ServiceAgent serviceAgent;
		try {
			serviceAgent = getServiceAgent(serviceClass);
		} catch (AgentNotKnownException e1) {
			throw new NoSuchServiceException(serviceClass, e1);
		}

		RMITask task = new RMITask(serviceClass, method, parameters);
		Context context = getAgentContext(executingAgentId);

		L2pThread thread = new L2pThread(serviceAgent, task, context);

		thread.start();

		thread.join();

		if (thread.hasException()) {
			Exception e = thread.getException();

			if (e instanceof ServiceInvocationException) {
				throw (ServiceInvocationException) e;
			} else if ((e instanceof InvocationTargetException) && (e.getCause() instanceof L2pSecurityException)) {
				// internal L2pSecurityException (like internal method access or unauthorizes object access)
				throw new L2pSecurityException("internal securityException!", e.getCause());
			} else {
				throw new ServiceInvocationException("Internal exception in service", e);
			}
		}

		try {
			return thread.getResult();
		} catch (NotFinishedException e) {
			// should not occur, since we joined before
			throw new L2pServiceException("Interrupted service execution?!", e);
		}
	}

	/**
	 * Tries to get an instance of the given class as a registered service of this node.
	 * 
	 * @param classname
	 * @return the instance of the given service class running at this node
	 * @throws NoSuchServiceException
	 */
	public Service getLocalServiceInstance(String classname) throws NoSuchServiceException {
		try {
			ServiceAgent agent = (ServiceAgent) getLocalAgent(ServiceAgent.serviceClass2Id(classname));
			return agent.getServiceInstance();
		} catch (Exception e) {
			throw new NoSuchServiceException(classname);
		}
	}

	/**
	 * Tries to get an instance of the given class as a registered service of this node.
	 * 
	 * @param cls
	 * @return the (typed) instance of the given service class running at this node
	 * @throws NoSuchServiceException
	 */
	@SuppressWarnings("unchecked")
	public <ServiceType extends Service> ServiceType getLocalServiceInstance(Class<ServiceType> cls)
			throws NoSuchServiceException {
		try {
			return (ServiceType) getLocalServiceInstance(cls.getName());
		} catch (ClassCastException e) {
			throw new NoSuchServiceException(cls.getName());
		}
	}

	private int invocationDistributerIndex = 0;

	/**
	 * Invokes a service method of the network.
	 * 
	 * @param executing
	 * @param serviceClass
	 * @param serviceMethod
	 * @param parameters
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pSecurityException
	 * @throws ServiceInvocationException several reasons -- see subclasses
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws UnlockNeededException
	 */
	public Serializable invokeGlobally(Agent executing, String serviceClass, String serviceMethod,
			Serializable[] parameters) throws L2pSecurityException, ServiceInvocationException, InterruptedException,
					TimeoutException, UnlockNeededException {
		if (getStatus() != NodeStatus.RUNNING)
			throw new IllegalStateException("you can invoke methods only on a running node!");
		// Do not log service class name (privacy..)
		this.observerNotice(Event.RMI_SENT, this.getNodeId(), executing, null);

		// TODO disabled? reenable for more security?
		/*if (executing.isLocked()){
			System.out.println(	"The executing agent has to be unlocked to call a RMI");
			throw new L2pSecurityException("The executing agent has to be unlocked to call a RMI");
		}*/

		try {
			// TODO versions of services
			Agent target = nodeServiceCache.getServiceAgent(serviceClass, "1.0");
			if (target == null) {
				target = getServiceAgent(serviceClass);
			}

			Message rmiMessage = new Message(executing, target, new RMITask(serviceClass, serviceMethod, parameters));

			if (this instanceof LocalNode) {
				rmiMessage.setSendingNodeId((Long) getNodeId());
			} else {
				rmiMessage.setSendingNodeId((NodeHandle) getNodeId());
			}
			Message resultMessage;
			NodeHandle targetNode = null;// =nodeServiceCache.getRandomServiceNode(serviceClass,"1.0");

			// TODO versions of services
			ArrayList<NodeHandle> targetNodes = nodeServiceCache.getServiceNodes(serviceClass, "1.0");
			if (targetNodes != null && targetNodes.size() > 0) {
				// TODO seems like round robin, should be replaced with a more generic node choose strategy
				invocationDistributerIndex %= targetNodes.size();
				targetNode = targetNodes.get(invocationDistributerIndex);
				invocationDistributerIndex++;
				if (invocationDistributerIndex >= targetNodes.size())
					invocationDistributerIndex = 0;

			}

			if (targetNode != null) {
				try {
					resultMessage = sendMessageAndWaitForAnswer(rmiMessage, targetNode);
				} catch (NodeNotFoundException nex) {
					// remove so unavailable nodes will not be tried again
					// TODO versions of services
					nodeServiceCache.removeEntryNode(serviceClass, "1.0", targetNode);
					resultMessage = sendMessageAndWaitForAnswer(rmiMessage); // TODO why anycast and not try another node?!?
				}
			} else {
				resultMessage = sendMessageAndWaitForAnswer(rmiMessage);
			}

			resultMessage.open(executing, this);
			Object resultContent = resultMessage.getContent();

			if (resultContent instanceof RMIUnlockContent) {
				// service method needed to unlock some envelope(s)
				this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing,
						"mediator needed at the target node"); // Do not log service class name (privacy..)
				throw new UnlockNeededException("mediator needed at the target node", resultMessage.getSendingNodeId(),
						((RMIUnlockContent) resultContent).getNodeKey());
			} else if (resultContent instanceof RMIExceptionContent) {
				Exception thrown = ((RMIExceptionContent) resultContent).getException();
				// Do not log service class name (privacy..)
				this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing, thrown.toString());
				if (thrown instanceof ServiceInvocationException)
					throw (ServiceInvocationException) thrown;
				else if ((thrown instanceof InvocationTargetException)
						&& (thrown.getCause() instanceof L2pSecurityException)) {
					// internal L2pSecurityException (like internal method access or unauthorizes object access)
					throw new L2pSecurityException("internal securityException!", thrown.getCause());
				} else
					throw new ServiceInvocationException("remote exception at target node", thrown);

			} else if (resultContent instanceof RMIResultContent) {
				// Do not log service class name (privacy..)
				this.observerNotice(Event.RMI_SUCCESSFUL, this.getNodeId(), executing, null);
				return ((RMIResultContent) resultContent).getContent();
			} else {
				// Do not log service class name (privacy..)
				this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing,
						"Unknown RMI response type: " + resultContent.getClass().getCanonicalName());
				throw new ServiceInvocationException(
						"Unknown RMI response type: " + resultContent.getClass().getCanonicalName());
			}
		} catch (AgentNotKnownException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new NoSuchServiceException(serviceClass, e);
		} catch (EncodingFailedException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new ServiceInvocationException("message problems!", e);
		} catch (SerializationException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(Event.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new ServiceInvocationException("message problems!", e);
		}
	}

	/**
	 * Registers a MessageResultListener for collecting answers.
	 * 
	 * @param messageId
	 * @param listener
	 */
	public void registerAnswerListener(long messageId, MessageResultListener listener) {
		if (listener == null)
			return;
		htAnswerListeners.put(messageId, listener);
	}

	/**
	 * Hands over an answer message to the corresponding listener.
	 * 
	 * @param answer
	 * 
	 * @return true, if a listener for this answer was notified
	 */
	public boolean handoverAnswer(Message answer) {
		if (!answer.isResponse())
			return false;
		observerNotice(Event.MESSAGE_RECEIVED_ANSWER, answer.getSendingNodeId(), answer.getSenderId(), this.getNodeId(),
				answer.getRecipientId(), "" + answer.getResponseToId());

		MessageResultListener listener = htAnswerListeners.get(answer.getResponseToId());
		if (listener == null) {
			System.out.println("Did not find corresponding observer!");
			return false;
		}

		listener.collectAnswer(answer);

		return true;
	}

	/**
	 * Sends a message and wait for one answer message.
	 * 
	 * @param m
	 * 
	 * @return a (possible) response message
	 * 
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public Message sendMessageAndWaitForAnswer(Message m) throws InterruptedException, TimeoutException {
		long timeout = m.getTimeoutTs() - new Date().getTime();
		MessageResultListener listener = new MessageResultListener(timeout);

		sendMessage(m, listener);

		listener.waitForOneAnswer();

		if (listener.isSuccess())
			return listener.getResults()[0];
		else
			throw new TimeoutException();
	}

	/**
	 * Sends a message to the given id and wait for one answer message.
	 * 
	 * @param m
	 * @param atNodeId
	 * 
	 * @return a response message
	 * 
	 * @throws AgentNotKnownException
	 * @throws NodeNotFoundException
	 * @throws L2pSecurityException
	 * @throws InterruptedException
	 */
	public Message sendMessageAndWaitForAnswer(Message m, Object atNodeId)
			throws AgentNotKnownException, NodeNotFoundException, L2pSecurityException, InterruptedException {
		long timeout = m.getTimeoutTs() - new Date().getTime();
		MessageResultListener listener = new MessageResultListener(timeout);

		sendMessage(m, atNodeId, listener);

		listener.waitForOneAnswer();

		// TODO what happens if the answers timeouts? Throw TimeoutException?
		return listener.getResults()[0];
	}

	/**
	 * Gets the local execution context of an agent. If there is currently none, a new one will be created and stored
	 * for later use.
	 * 
	 * @param agentId
	 * 
	 * @return the context for the given agent
	 * 
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 */
	protected Context getAgentContext(long agentId) throws L2pSecurityException, AgentNotKnownException {
		Context result = htLocalExecutionContexts.get(agentId);

		if (result == null) {
			Agent agent = getAgent(agentId);
			result = new Context(this, agent);
			htLocalExecutionContexts.put(agentId, result);
		}

		return result;
	}

	/**
	 * Gets a (possibly fresh) context for the given agent.
	 * 
	 * @param agent
	 * 
	 * @return a context
	 */
	protected Context getAgentContext(Agent agent) {
		Context result = htLocalExecutionContexts.get(agent.getId());

		if (result == null || (result.getMainAgent().isLocked() && !agent.isLocked())) {
			try {
				result = new Context(this, agent);
			} catch (L2pSecurityException e) {
			}
			htLocalExecutionContexts.put(agent.getId(), result);
		}

		return result;
	}

	/**
	 * Checks, if the given service class is running at this node.
	 * 
	 * @param serviceClass
	 * @return true, if this node as an instance of the given service running
	 */
	public boolean hasService(String serviceClass) {
		return hasAgent(ServiceAgent.serviceClass2Id(serviceClass));
	}

	/**
	 * Gets the approximate CPU load of the JVM the Node is running on. Correct value only available a few seconds after
	 * the start of the Node.
	 * 
	 * @return value between 0 and 1: CPU load of the JVM process running this node
	 */
	public double getNodeCpuLoad() {
		double load = osBean.getProcessCpuLoad();
		if (load < 0) { // no CPU load information are available
			load = 0;
		} else if (load > 1) {
			load = 1;
		}
		return load;
	}

	public boolean isBusy() {
		return (getNodeCpuLoad() > cpuLoadThreshold);
	}

}
