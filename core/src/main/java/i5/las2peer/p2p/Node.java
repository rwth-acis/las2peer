package i5.las2peer.p2p;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import com.sun.management.OperatingSystemMXBean;

import i5.las2peer.api.Configurable;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.classLoaders.libraries.Repository;
import i5.las2peer.classLoaders.policies.DefaultPolicy;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.RMIExceptionContent;
import i5.las2peer.communication.RMIResultContent;
import i5.las2peer.execution.RMITask;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.logging.monitoring.MonitoringObserver;
import i5.las2peer.p2p.NodeServiceCache.ServiceInstance;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.NodeStorageInterface;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AgentStorage;
import i5.las2peer.security.BotAgent;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.MonitoringAgent;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UnlockAgentCall;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.security.UserAgentManager;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.socket.SocketNodeHandle;

/**
 * Base class for nodes in the las2peer environment.
 * 
 * A Node represents one enclosed unit in the network hosting an arbitrary number of agents willing to participate in
 * the P2P networking.
 * 
 */
public abstract class Node extends Configurable implements AgentStorage, NodeStorageInterface {

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

	private final L2pLogger logger = L2pLogger.getInstance(Node.class);

	/**
	 * For performance measurement (load balance)
	 */
	private OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();

	private final NodeServiceCache nodeServiceCache;

	public static final double DEFAULT_CPU_LOAD_TRESHOLD = 0.9;
	/**
	 * cpu load threshold to determine whether the node is considered busy
	 */
	private double cpuLoadThreshold = DEFAULT_CPU_LOAD_TRESHOLD;

	public static final int DEFAULT_NODE_SERVICE_CACHE_LIFETIME = 60;
	/**
	 * time before cached service information becomes invalidated
	 */
	private int nodeServiceCacheLifetime = DEFAULT_NODE_SERVICE_CACHE_LIFETIME;

	public static final int DEFAULT_NODE_SERVICE_CACHE_RESULT_COUNT = 3;
	/**
	 * number of service answers collected during service discovery
	 */
	private int nodeServiceCacheResultCount = DEFAULT_NODE_SERVICE_CACHE_RESULT_COUNT;

	public static final int DEFAULT_TIDY_UP_TIMER_INTERVAL = 60;
	/**
	 * frequency of the tidy up timer
	 */
	private int tidyUpTimerInterval = DEFAULT_TIDY_UP_TIMER_INTERVAL;

	public static final int DEFAULT_AGENT_CONTEXT_LIFETIME = 60;
	/**
	 * period of time after which an agent context will be available for deletion
	 */
	private int agentContextLifetime = DEFAULT_AGENT_CONTEXT_LIFETIME;

	public static final int DEFAULT_INVOCATION_RETRY_COUNT = 3;
	/**
	 * number of retries if an RMI fails
	 */
	private int invocationRetryCount = DEFAULT_INVOCATION_RETRY_COUNT;

	/**
	 * observers to be notified of all occurring events
	 */
	private HashSet<NodeObserver> observers = new HashSet<>();

	/**
	 * contexts for local method invocation
	 */
	private Hashtable<String, AgentContext> htLocalExecutionContexts = new Hashtable<>();

	/**
	 * Timer to tidy up hashtables etc (Contexts)
	 */
	private Timer tidyUpTimer;

	/**
	 * Runtime to get performance information (RAM)
	 */
	private Runtime runtime;

	/**
	 * status of this node
	 */
	private NodeStatus status = NodeStatus.UNCONFIGURED;

	/**
	 * hashtable with all {@link i5.las2peer.security.MessageReceiver}s registered at this node
	 */
	private Hashtable<String, MessageReceiver> htRegisteredReceivers = new Hashtable<>();

	/**
	 * map with all topics and their listeners
	 */
	private HashMap<Long, TreeMap<String, MessageReceiver>> mapTopicListeners = new HashMap<>();
	/**
	 * other direction of {@link #mapTopicListeners}
	 */
	private HashMap<String, TreeSet<Long>> mapListenerTopics = new HashMap<>();

	private ClassManager classManager = null;

	private Hashtable<Long, MessageResultListener> htAnswerListeners = new Hashtable<>();

	private static final String DEFAULT_INFORMATION_FILE = "etc/nodeInfo.xml";
	private String sInformationFileName = DEFAULT_INFORMATION_FILE;

	/**
	 * logger for writing custom events to a separate logfile
	 */
	private L2pLogger serviceLogger;
	private static final String SERVICE_LOGFILE = "service.log";

	/**
	 * maps names and emails to UserAgents
	 */
	private UserAgentManager userManager;

	/**
	 * maps service alias to service names
	 */
	private ServiceAliasManager aliasManager;

	private Date startTime;

	/**
	 * Creates a new node, if the standardObserver flag is true, an observer logging all events to a simple plain text
	 * log file will be generated. If not, no observer will be used at startup.
	 * 
	 * @param standardObserver If true, the node uses the default logger.
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
	 * @param classManager A default class loader used by this node.
	 */
	public Node(ClassManager classManager) {
		this(classManager, true);
	}

	/**
	 * @param classManager A default class loader used by this node.
	 * @param standardObserver If true, the node uses the default logger.
	 */
	public Node(ClassManager classManager, boolean standardObserver) {
		this(classManager, standardObserver, false);
	}

	/**
	 * Generates a new Node with the given baseClassLoader. The Observer-flags determine, which observers will be
	 * registered at startup.
	 * 
	 * @param classManager A default class loader used by this node.
	 * @param standardObserver If true, the node uses the default logger.
	 * @param monitoringObserver If true, the monitoring is enabled for this node.
	 */
	public Node(ClassManager classManager, boolean standardObserver, boolean monitoringObserver) {
		setFieldValues();

		if (standardObserver) {
			initStandardLogfile();
			initServiceLogfile();
		}

		if (monitoringObserver) {
			addObserver(new MonitoringObserver(50, this));
		}

		this.classManager = classManager;

		if (classManager == null) {
			this.classManager = new ClassManager(new Repository[0], this.getClass().getClassLoader(),
					new DefaultPolicy());
		}

		nodeServiceCache = new NodeServiceCache(this, nodeServiceCacheLifetime, nodeServiceCacheResultCount);

		userManager = new UserAgentManager(this);
		aliasManager = new ServiceAliasManager(this);

		this.runtime = Runtime.getRuntime();
	}

	/**
	 * Creates an observer for a standard log-file + console
	 */
	private void initStandardLogfile() {
		L2pLogger logger = L2pLogger.getInstance(this.getClass());
		addObserver(logger);
	}

	/**
	 * Creates an additional observer for the log-file for custom mesages
	 */
	private void initServiceLogfile() {
		serviceLogger = L2pLogger.getInstance("service");
		try {
			serviceLogger.setLogfilePrefix(SERVICE_LOGFILE);
			addObserver(serviceLogger);
		} catch (IOException e) {
			System.err.println(
					"Fatal Error! Can't use logging prefix '" + SERVICE_LOGFILE + "'! File logging is disabled!");
		}
	}

	/**
	 * Adds an observer to this node.
	 * 
	 * @param observer The observer that should be notified.
	 */
	public void addObserver(NodeObserver observer) {
		observers.add(observer);
	}

	/**
	 * Removes an observer from this node.
	 * 
	 * @param observer The observer that should be removed.
	 */
	public void removeObserver(NodeObserver observer) {
		observers.remove(observer);
	}

	/**
	 * Enables the service monitoring for the requested Service.
	 * 
	 * @param service The service that should be monitored.
	 */
	public void setServiceMonitoring(ServiceAgentImpl service) {
		observerNotice(MonitoringEvent.SERVICE_ADD_TO_MONITORING, this.getNodeId(), service.getIdentifier(), null, null,
				"{\"serviceName\":\"" + service.getServiceNameVersion().toString() + "\",\"serviceAlias\":\""
						+ service.getServiceInstance().getAlias() + "\"}");
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, String remarks) {
		observerNotice(event, null, (String) null, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param sourceNode A source node for this event
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, Object sourceNode, String remarks) {
		observerNotice(event, sourceNode, (String) null, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param sourceNode A source node for this event
	 * @param sourceAgentId A source agent id for this event
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, Object sourceNode, String sourceAgentId, String remarks) {
		observerNotice(event, sourceNode, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param sourceNode A source node for this event
	 * @param sourceAgent A source agent for this event
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, Object sourceNode, MessageReceiver sourceAgent, String remarks) {
		String sourceAgentId = null;
		if (sourceAgent != null) {
			sourceAgentId = sourceAgent.getResponsibleForAgentSafeId();
		}
		observerNotice(event, sourceNode, sourceAgentId, null, null, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param sourceNode A source node for this event
	 * @param sourceAgent A source agent for this event
	 * @param destinationNode A destination node for this event
	 * @param destinationAgent A destination agent for this event
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, Object sourceNode, Agent sourceAgent, Object destinationNode,
			Agent destinationAgent, String remarks) {
		String sourceAgentId = null;
		if (sourceAgent != null) {
			sourceAgentId = sourceAgent.getIdentifier();
		}
		String destinationAgentId = null;
		if (destinationAgent != null) {
			destinationAgentId = destinationAgent.getIdentifier();
		}
		observerNotice(event, sourceNode, sourceAgentId, destinationNode, destinationAgentId, remarks);
	}

	/**
	 * Logs an event to all observers.
	 * 
	 * @param event The event for this notification.
	 * @param sourceNode A source node for this event
	 * @param sourceAgentId A source agent id for this event
	 * @param destinationNode A destination node for this event
	 * @param destinationAgentId A destination agent id for this event
	 * @param remarks Some free text note or description about this event.
	 */
	public void observerNotice(MonitoringEvent event, Object sourceNode, String sourceAgentId, Object destinationNode,
			String destinationAgentId, String remarks) {
		long timestamp = new Date().getTime();
		String sourceNodeRepresentation = getNodeRepresentation(sourceNode);
		String destinationNodeRepresentation = getNodeRepresentation(destinationNode);
		for (NodeObserver ob : observers) {
			if (ob == serviceLogger && (Math.abs(event.getCode()) < 7500 || Math.abs(event.getCode()) >= 7600)) {
				// custom logger shall only log service messages
				continue;
			}
			ob.log(timestamp, event, sourceNodeRepresentation, sourceAgentId, destinationNodeRepresentation,
					destinationAgentId, remarks);
		}
	}

	/**
	 * Derive a String representation for a node from the given identifier object. The type of the object depends on the
	 * setting of the current node.
	 * 
	 * Tries to specify an ip address and a port for an actual p2p node ({@link i5.las2peer.p2p.PastryNodeImpl} or
	 * {@link rice.pastry.NodeHandle}).
	 * 
	 * @param node The node that should be represented.
	 * @return string representation for the given node object
	 */
	protected String getNodeRepresentation(Object node) {
		if (node == null) {
			return null;
		} else if (node instanceof SocketNodeHandle) {
			SocketNodeHandle nh = (SocketNodeHandle) node;
			return nh.getId() + "/" + nh.getIdentifier();
		} else if (node instanceof PastryNode) {
			PastryNode pNode = (PastryNode) node;
			return getNodeRepresentation(pNode.getLocalNodeHandle());
		} else {
			return "" + node + " (" + node.getClass().getName() + ")";
		}
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

	public abstract AgentImpl getAgentSecret(String id) throws AgentException;
	public abstract void secretFunctionStore(AgentImpl agent) throws AgentException;

	/**
	 * Gets the class loader, this node is bound to. In a <i>real</i> las2peer environment, this should refer to a
	 * {@link i5.las2peer.classLoaders.ClassManager}
	 * 
	 * Otherwise, the class loader of this Node class is used.
	 * 
	 * @return a class loader
	 */
	public ClassManager getBaseClassLoader() {
		return classManager;
	}

	/**
	 * Sets the status of this node.
	 * 
	 * @param newstatus The new status for this node.
	 */
	protected void setStatus(NodeStatus newstatus) {
		if (newstatus == NodeStatus.RUNNING && this instanceof PastryNodeImpl) {
			observerNotice(MonitoringEvent.NODE_STATUS_CHANGE, this.getNodeId(), "" + newstatus);
		} else if (newstatus == NodeStatus.CLOSING) {
			observerNotice(MonitoringEvent.NODE_STATUS_CHANGE, this.getNodeId(), "" + newstatus);
		} else {
			observerNotice(MonitoringEvent.NODE_STATUS_CHANGE, "" + newstatus);
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
	 * @param filename The filename for the information file.
	 */
	public void setInformationFilename(String filename) {
		if (new File(filename).exists()) {
			sInformationFileName = filename;
		}
	}

	/**
	 * Gets information about this node including all registered service classes.
	 * 
	 * @return node information
	 * @throws CryptoException If an issue occurs with the given key or selected algorithm.
	 */
	public NodeInformation getNodeInformation() throws CryptoException {
		NodeInformation result = new NodeInformation(getRegisteredServices());

		try {
			if (sInformationFileName != null && new File(sInformationFileName).exists()) {
				result = NodeInformation.createFromXmlFile(sInformationFileName, getRegisteredServices());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		result.setNodeHandle(getNodeId());
		// logger.info("retrieving node info: \n" + result.toXmlString());
		return result;
	}

	/**
	 * Gets information about a distant node.
	 * 
	 * @param nodeId A node id to query
	 * @return information about the node
	 * @throws NodeNotFoundException If the node was not found
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
	 * 
	 * @throws NodeException If launching the node fails
	 */
	protected abstract void launchSub() throws NodeException;

	/**
	 * Starts this node.
	 * 
	 * @throws NodeException If launching the node fails
	 */
	public final void launch() throws NodeException {
		launchSub();

		startTime = new Date();

		startTidyUpTimer();
	}

	/**
	 * Stops the node.
	 */
	public synchronized void shutDown() {
		stopTidyUpTimer();

		startTime = null;

		// avoid ConcurrentModificationEception
		String[] receivers = htRegisteredReceivers.keySet().toArray(new String[0]);
		for (String id : receivers) {
			htRegisteredReceivers.get(id).notifyUnregister();
		}
		observerNotice(MonitoringEvent.NODE_SHUTDOWN, this.getNodeId(), null);
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
		htRegisteredReceivers = new Hashtable<>();
	}

	/**
	 * Registers a (local) Agent for usage through this node. The Agent has to be unlocked before registration.
	 * 
	 * @param receiver A message receiver to register
	 * @throws AgentAlreadyRegisteredException the given agent is already registered to this node
	 * @throws AgentLockedException the agent is not unlocked
	 * @throws AgentException any problem with the agent itself (probably on calling
	 *             {@link i5.las2peer.security.AgentImpl#notifyRegistrationTo}
	 */
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException, AgentException {

		// TODO allow multiple mediators registered at the same time for one agent to avoid conflicts between connectors

		if (getStatus() != NodeStatus.RUNNING) {
			throw new IllegalStateException("You can register agents only to running nodes!");
		}

		if (htRegisteredReceivers.contains(receiver.getResponsibleForAgentSafeId())
				&& htRegisteredReceivers.get(receiver.getResponsibleForAgentSafeId()) != receiver) {
			throw new AgentAlreadyRegisteredException(
					"Another instance of this agent (or mediator) is already registered here!");
		}

		if ((receiver instanceof AgentImpl)) {
			// we have an agent
			AgentImpl agent = (AgentImpl) receiver;
			if (agent.isLocked()) {
				throw new AgentLockedException("An agent has to be unlocked for registering at a node.");
			}

			try {
				// ensure (unlocked) context
				getAgentContext(agent);
			} catch (Exception e) {
			}
			if (agent instanceof UserAgentImpl) {
				observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), agent, "UserAgent");
			} else if (agent instanceof ServiceAgentImpl) {
				observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), agent, "ServiceAgent");
			} else if (agent instanceof GroupAgentImpl) {
				observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), agent, "GroupAgent");
			} else if (agent instanceof MonitoringAgent) {
				observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), agent, "MonitoringAgent");
			} else if (agent instanceof BotAgent) {
				observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), agent, "BotAgent");
			}
		} else if (receiver instanceof Mediator) {
			// ok, we have a mediator
			observerNotice(MonitoringEvent.AGENT_REGISTERED, this.getNodeId(), receiver, "Mediator");
		} else {
			throw new IllegalArgumentException("Given receiver is not an agent or mediator. Got "
					+ receiver.getClass().getCanonicalName() + " instead");
		}

		htRegisteredReceivers.put(receiver.getResponsibleForAgentSafeId(), receiver);

		try {
			receiver.notifyRegistrationTo(this);
		} catch (AgentException e) {
			observerNotice(MonitoringEvent.AGENT_LOAD_FAILED, this, receiver, e.toString());

			htRegisteredReceivers.remove(receiver.getResponsibleForAgentSafeId());
			throw e;
		} catch (Exception e) {
			observerNotice(MonitoringEvent.AGENT_LOAD_FAILED, this, receiver, e.toString());

			htRegisteredReceivers.remove(receiver.getResponsibleForAgentSafeId());
			throw new AgentException("problems notifying agent of registration", e);
		}

	}

	/**
	 * Unregisters a MessageReceiver from this node.
	 * 
	 * @param receiver the receiver to unregister
	 * @throws AgentNotRegisteredException The given MessageReceiver is not registered to this node
	 * @throws NodeException error in underlying layer
	 */
	public void unregisterReceiver(MessageReceiver receiver) throws AgentNotRegisteredException, NodeException {
		String agentId = receiver.getResponsibleForAgentSafeId();
		unregisterReceiver(agentId);

		// unregister from topics
		if (mapListenerTopics.containsKey(agentId)) {
			Long[] topics = mapListenerTopics.get(agentId).toArray(new Long[0]);
			for (long topic : topics) {
				unregisterReceiverFromTopic(receiver, topic);
			}
		}
	}

	private void unregisterReceiver(String agentId) throws AgentNotRegisteredException {
		if (!htRegisteredReceivers.containsKey(agentId)) {
			throw new AgentNotRegisteredException(agentId);
		}
		observerNotice(MonitoringEvent.AGENT_REMOVED, getNodeId(), agentId, "");
		htRegisteredReceivers.remove(agentId).notifyUnregister();
	}

	/**
	 * register a receiver to a topic
	 * 
	 * @param receiver the MessageReceiver
	 * @param topic the topic id
	 * @throws AgentNotRegisteredException The given MessageReceiver is not registered to this node
	 */
	public void registerReceiverToTopic(MessageReceiver receiver, long topic) throws AgentNotRegisteredException {
		if (!htRegisteredReceivers.containsKey(receiver.getResponsibleForAgentSafeId())) {
			throw new AgentNotRegisteredException(receiver.getResponsibleForAgentSafeId());
		}

		synchronized (mapListenerTopics) {
			synchronized (mapTopicListeners) {
				if (mapListenerTopics.get(receiver.getResponsibleForAgentSafeId()) == null) {
					mapListenerTopics.put(receiver.getResponsibleForAgentSafeId(), new TreeSet<>());
				}

				if (mapListenerTopics.get(receiver.getResponsibleForAgentSafeId()).add(topic)) {
					if (mapTopicListeners.get(topic) == null) {
						mapTopicListeners.put(topic, new TreeMap<>());
					}
					mapTopicListeners.get(topic).put(receiver.getResponsibleForAgentSafeId(), receiver);
				}
			}
		}
	}

	/**
	 * unregister a receiver from a topic
	 * 
	 * @param receiver the receiver
	 * @param topic the topic id
	 * @throws NodeException If unregistering fails
	 */
	public void unregisterReceiverFromTopic(MessageReceiver receiver, long topic) throws NodeException {
		unregisterReceiverFromTopic(receiver.getResponsibleForAgentSafeId(), topic);
	}

	private void unregisterReceiverFromTopic(String receiverId, long topic) {
		synchronized (mapListenerTopics) {
			synchronized (mapTopicListeners) {
				if (!mapListenerTopics.containsKey(receiverId) || !mapListenerTopics.get(receiverId).contains(topic)) {
					return;
				}

				mapListenerTopics.get(receiverId).remove(topic);
				if (mapListenerTopics.get(receiverId).size() == 0) {
					mapListenerTopics.remove(receiverId);
				}

				mapTopicListeners.get(topic).remove(receiverId);
				if (mapTopicListeners.get(topic).size() == 0) {
					mapTopicListeners.remove(topic);
				}

			}
		}
	}

	/**
	 * checks if a receiver is registered to the topic
	 * 
	 * @param topic topic id
	 * @return true if someone is registered to the topic
	 */
	protected boolean hasTopic(long topic) {
		return mapTopicListeners.containsKey(topic);
	}

	/**
	 * Is an instance of the given agent running at this node?
	 * 
	 * @param agent An agent to check for
	 * @return true, if the given agent is running at this node
	 */
	public boolean hasLocalAgent(AgentImpl agent) {
		return hasLocalAgent(agent.getIdentifier());
	}

	/**
	 * Is an instance of the given agent running at this node?
	 * 
	 * @param agentId An agent id to check for
	 * @return true, if the given agent is registered here
	 */
	public boolean hasLocalAgent(String agentId) {
		return htRegisteredReceivers.get(agentId) != null;
	}

	/**
	 * Starts a new instance of the given service on this node. This creates, stores and registers a new service agent.
	 * 
	 * @param nameVersion A service name and version to identify the service
	 * @param passphrase A passphrase to secure this instance
	 * @return Returns the local service agent instance
	 * @throws CryptoException
	 * @throws AgentException
	 */
	public ServiceAgentImpl startService(ServiceNameVersion nameVersion, String passphrase)
			throws CryptoException, AgentException {
		ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(nameVersion, passphrase);
		serviceAgent.unlock(passphrase);
		storeAgent(serviceAgent);
		registerReceiver(serviceAgent);
		return serviceAgent;
	}

	/**
	 * Stops the local service instance.
	 * 
	 * @param nameVersion A service name and version to identify the service
	 * @throws AgentNotRegisteredException If the service is not registered locally
	 * @throws ServiceNotFoundException If the service is not known locally
	 * @throws NodeException
	 */
	public void stopService(ServiceNameVersion nameVersion)
			throws AgentNotRegisteredException, ServiceNotFoundException, NodeException {
		stopService(getLocalServiceAgent(nameVersion));
	}

	/**
	 * Stops the local service instance.
	 * 
	 * @param serviceAgent
	 * @throws AgentNotRegisteredException If the service is not registered locally
	 * @throws NodeException
	 */
	public void stopService(ServiceAgentImpl serviceAgent) throws AgentNotRegisteredException, NodeException {
		unregisterReceiver(serviceAgent);
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
	 * @param message A message to send
	 * @param atNodeId A node id to send from
	 * @param listener a listener for getting the result separately
	 * @throws NodeNotFoundException If the node was not found
	 */
	public abstract void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws NodeNotFoundException;

	/**
	 * Sends the given response message to the given node.
	 * 
	 * @param message A message to send
	 * @param atNodeId A node id to send from
	 * @throws NodeNotFoundException If the node was not found
	 */
	public void sendResponse(Message message, Object atNodeId) throws NodeNotFoundException {
		sendMessage(message, atNodeId, null);
	}

	/**
	 * For <i>external</i> access to this node. Will be called by the (P2P) network library, when a new message has been
	 * received via the network and could not be handled otherwise.
	 * 
	 * @param message A message that is received
	 * @throws AgentNotRegisteredException If the designated recipient is not known at this node
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 * @throws MessageException If handling the message fails
	 */
	public void receiveMessage(Message message) throws AgentNotRegisteredException, AgentException, MessageException {
		if (message.isResponse()) {
			if (handoverAnswer(message)) {
				return;
			}
		}

		// Since this field is not always available
		if (message.getSendingNodeId() != null) {
			observerNotice(MonitoringEvent.MESSAGE_RECEIVED, message.getSendingNodeId(), message.getSenderId(),
					this.getNodeId(), message.getRecipientId(), message.getId() + "");
		} else {
			observerNotice(MonitoringEvent.MESSAGE_RECEIVED, null, message.getSenderId(), this.getNodeId(),
					message.getRecipientId(), message.getId() + "");
		}

		if (!message.isTopic()) {
			MessageReceiver receiver = htRegisteredReceivers.get(message.getRecipientId());

			if (receiver == null) {
				throw new AgentNotRegisteredException(message.getRecipientId());
			}

			receiver.receiveMessage(message, getAgentContext(message.getSenderId()));
		} else {
			TreeMap<String, MessageReceiver> map = mapTopicListeners.get(message.getTopicId());

			if (map == null) {
				throw new MessageException("No receiver registered for this topic!");
			}

			synchronized (map) {
				for (MessageReceiver receiver : map.values()) {
					try {
						Message msg = message;
						if (map.size() > 1) {
							msg = msg.clone();
						}

						msg.setRecipientId(receiver.getResponsibleForAgentSafeId());

						receiver.receiveMessage(msg, getAgentContext(message.getSenderId()));
					} catch (CloneNotSupportedException e) {
						throw new MessageException("Cloning failed", e);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Message receiver failed", e);
					}
				}
			}
		}
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 * 
	 *             Gets an artifact from the p2p storage.
	 * 
	 * @param id An id to identify the envelope
	 * @return the envelope containing the requested artifact
	 * @throws EnvelopeNotFoundException If the envelope was not found
	 * @throws EnvelopeException If an issue with the envelope occurred
	 */
	@Deprecated
	public abstract EnvelopeVersion fetchArtifact(long id) throws EnvelopeNotFoundException, EnvelopeException;

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 * 
	 *             Gets an artifact from the p2p storage.
	 * 
	 * @param identifier An identifier for the envelope
	 * @return the envelope containing the requested artifact
	 * @throws EnvelopeNotFoundException If the envelope was not found
	 * @throws EnvelopeException If an issue with the envelope occurred
	 */
	@Deprecated
	public abstract EnvelopeVersion fetchArtifact(String identifier)
			throws EnvelopeNotFoundException, EnvelopeException;

	/**
	 * @deprecated Use {@link #storeEnvelope(EnvelopeVersion, AgentImpl)} instead
	 * 
	 *             Stores an artifact to the p2p storage.
	 * 
	 * @param envelope An envelope to store
	 * @throws EnvelopeAlreadyExistsException If the envelope already exists
	 * @throws EnvelopeException If an issue with the envelope occurred
	 */
	@Deprecated
	public abstract void storeArtifact(EnvelopeVersion envelope)
			throws EnvelopeAlreadyExistsException, EnvelopeException;

	/**
	 * @deprecated Use {@link #removeEnvelope(String)} instead
	 * 
	 *             Removes an artifact from the p2p storage. <i>NOTE: This is not possible with a FreePastry
	 *             backend!</i>
	 * 
	 * @param id An identifier for the artifact
	 * @param signature A signature to use
	 * @throws EnvelopeNotFoundException If the envelope was not found
	 * @throws EnvelopeException If an issue with the envelope occurred
	 */
	@Deprecated
	public abstract void removeArtifact(long id, byte[] signature) throws EnvelopeNotFoundException, EnvelopeException;

	/**
	 * Searches the nodes for registered Versions of the given Agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agentId id of the agent to look for
	 * @param hintOfExpectedCount a hint for the expected number of results (e.g. to wait for)
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotRegisteredException If the agent is not registered at this node
	 */
	public abstract Object[] findRegisteredAgent(String agentId, int hintOfExpectedCount)
			throws AgentNotRegisteredException;

	/**
	 * Search the nodes for registered versions of the given agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agent An agent to find
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotRegisteredException If the agent is not registered at this node
	 */
	public Object[] findRegisteredAgent(AgentImpl agent) throws AgentNotRegisteredException {
		return findRegisteredAgent(agent.getIdentifier());
	}

	/**
	 * Searches the nodes for registered versions of the given agentId. Returns an array of objects identifying the
	 * nodes the given agent is registered to.
	 * 
	 * @param agentId id of the agent to look for
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotRegisteredException If the agent is not registered at this node
	 */
	public Object[] findRegisteredAgent(String agentId) throws AgentNotRegisteredException {
		return findRegisteredAgent(agentId, 1);
	}

	/**
	 * searches the nodes for registered versions of the given agent. Returns an array of objects identifying the nodes
	 * the given agent is registered to.
	 * 
	 * @param agent An agent to look for
	 * @param hintOfExpectedCount a hint for the expected number of results (e.g. to wait for)
	 * @return array with the IDs of nodes, where the given agent is registered
	 * @throws AgentNotRegisteredException If the agent is not registered at this node
	 */

	public Object[] findRegisteredAgent(AgentImpl agent, int hintOfExpectedCount) throws AgentNotRegisteredException {
		return findRegisteredAgent(agent.getIdentifier(), hintOfExpectedCount);
	}

	/**
	 * Gets an agent description from the net.
	 * 
	 * make sure, always to return fresh versions of the requested agent, so that no thread can unlock the private key
	 * for another one!
	 * 
	 * @param id An agent id
	 * @return the requested agent
	 * @throws AgentNotFoundException If the agent is not found
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 */
	@Override
	public abstract AgentImpl getAgent(String id) throws AgentNotFoundException, AgentException;

	@Override
	public boolean hasAgent(String id) throws AgentException {
		// Since an request for this agent is probable after this check, it makes sense
		// to try to load it into this node and decide afterwards
		try {
			getAgent(id);
			return true;
		} catch (AgentNotFoundException e) {
			return false;
		}
	}

	/**
	 * Gets a local registered agent by its id.
	 * 
	 * @param id An agent id
	 * @return the agent registered to this node
	 * @throws AgentNotRegisteredException If the agent is not found at this node
	 */
	public AgentImpl getLocalAgent(String id) throws AgentNotRegisteredException {
		MessageReceiver result = htRegisteredReceivers.get(id);

		if (result == null) {
			throw new AgentNotRegisteredException("The given agent agent is not registered to this node");
		}

		if (result instanceof AgentImpl) {
			return (AgentImpl) result;
		} else {
			throw new AgentNotRegisteredException("The requested Agent is only known as a Mediator here!");
		}
	}

	/**
	 * Gets an array with all {@link i5.las2peer.security.UserAgentImpl}s registered at this node.
	 * 
	 * @return all local registered UserAgents
	 */
	public UserAgentImpl[] getRegisteredAgents() {
		Vector<UserAgentImpl> result = new Vector<>();

		for (MessageReceiver rec : htRegisteredReceivers.values()) {
			if (rec instanceof UserAgentImpl) {
				result.add((UserAgentImpl) rec);
			}
		}

		return result.toArray(new UserAgentImpl[0]);
	}

	/**
	 * Gets an array with all {@link i5.las2peer.security.ServiceAgentImpl}s registered at this node.
	 * 
	 * @return all local registered ServiceAgents
	 */
	public ServiceAgentImpl[] getRegisteredServices() {
		Vector<ServiceAgentImpl> result = new Vector<>();

		for (MessageReceiver rec : htRegisteredReceivers.values()) {
			if (rec instanceof ServiceAgentImpl) {
				result.add((ServiceAgentImpl) rec);
			}
		}

		return result.toArray(new ServiceAgentImpl[0]);

	}

	/**
	 * Gets a local registered mediator for the given agent id. If no mediator exists, registers a new one to this node.
	 * 
	 * @param agent An agent to mediate
	 * @return the mediator for the given agent
	 * @throws AgentLockedException If the agent is locked.
	 * @throws AgentAlreadyRegisteredException If the agent is already directly registered at this node
	 */
	public Mediator createMediatorForAgent(AgentImpl agent)
			throws AgentLockedException, AgentAlreadyRegisteredException {
		if (agent.isLocked()) {
			throw new AgentLockedException("You need to unlock the agent for mediation!");
		}
		MessageReceiver receiver = htRegisteredReceivers.get(agent.getIdentifier());

		if (receiver != null && !(receiver instanceof Mediator)) {
			throw new AgentAlreadyRegisteredException("The requested Agent is registered directly at this node!");
		}

		if (receiver == null) {
			getAgentContext(agent);
		}

		return new Mediator(this, agent);
	}

	/**
	 * Stores a new Agent to the network.
	 * 
	 * @param agent An agent to store
	 * @throws AgentException If any issue with the agent occurs
	 */
	public abstract void storeAgent(AgentImpl agent) throws AgentException;

	/**
	 * Updates an existing agent of the network.
	 * 
	 * @param agent An agent to update
	 * @throws AgentException If any issue with the agent occurs
	 */
	@Deprecated
	public abstract void updateAgent(AgentImpl agent) throws AgentException;

	/**
	 * returns the manager responsible for user management
	 * 
	 * @return this node's user manager
	 */
	public UserAgentManager getUserManager() {
		return userManager;
	}

	/**
	 * Gets an id for the user for the given login name.
	 * 
	 * @param login A login name to identify agent
	 * @return agent id
	 * @throws AgentNotFoundException If no agent for the given login is found
	 * @throws AgentOperationFailedException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public String getAgentIdForLogin(String login) throws AgentNotFoundException, AgentOperationFailedException {
		return userManager.getAgentIdByLogin(login);
	}

	/**
	 * Gets an id for the user for the given email address.
	 * 
	 * @param email An email address to identify agent
	 * @return agent id
	 * @throws AgentNotFoundException If no agent for the given email is found
	 * @throws AgentOperationFailedException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public String getAgentIdForEmail(String email) throws AgentNotFoundException, AgentOperationFailedException {
		return userManager.getAgentIdByEmail(email);
	}

	/**
	 * Gets an id for the group for the given group name.
	 * 
	 * @param groupName The name of the group
	 * @return agent id
	 * @throws AgentNotFoundException If no agent for the given name is found
	 * @throws AgentOperationFailedException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public String getAgentIdForGroupName(String groupName) throws AgentNotFoundException, AgentOperationFailedException {
		return userManager.getAgentIdByGroupName(groupName);
	}

	/**
	 * get the manager responsible for the mapping from service alias to service names
	 * 
	 * @return Returns the {@code ServiceAliasManager} instance of this node
	 */
	public ServiceAliasManager getServiceAliasManager() {
		return aliasManager;
	}

	/**
	 * Gets an currently running agent executing the given service.
	 * 
	 * Prefer using a locally registered agent.
	 * 
	 * @param service service to be invoked
	 * @param acting agent
	 * @return the ServiceAgent responsible for the given service class
	 * @throws AgentException If any issue with the agent occurs, e. g. not found, XML not readable
	 */
	public ServiceAgentImpl getServiceAgent(ServiceNameVersion service, AgentImpl acting) throws AgentException {
		ServiceInstance inst = nodeServiceCache.getServiceAgentInstance(service, true, false, acting);
		if (inst.local()) {
			return inst.getServiceAgent();
		} else {
			AgentImpl result = getAgent(inst.getServiceAgentId());
			if (result == null || !(result instanceof ServiceAgentImpl)) {
				throw new AgentNotFoundException("The corresponding agent is not a ServiceAgent.");
			}
			return (ServiceAgentImpl) result;
		}
	}

	/**
	 * invoke a service in the network
	 * 
	 * @param executing the executing agent
	 * @param service service to be invoked
	 * @param method service method
	 * @param parameters invocation parameters
	 * @return Returns the invocation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent was locked
	 */
	public Serializable invoke(AgentImpl executing, String service, String method, Serializable[] parameters)
			throws AgentLockedException, ServiceInvocationException {
		return invoke(executing, ServiceNameVersion.fromString(service), method, parameters, false, false);
	}

	/**
	 * invoke a service in the network (choosing an appropriate version)
	 * 
	 * @param executing the executing agent
	 * @param service service to be invoked
	 * @param method service method
	 * @param parameters invocation parameters
	 * @return Returns the invocation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent was locked
	 */
	public Serializable invoke(AgentImpl executing, ServiceNameVersion service, String method,
			Serializable[] parameters) throws AgentLockedException, ServiceInvocationException {
		return invoke(executing, service, method, parameters, false, false);
	}

	/**
	 * invoke a service in the network or locally
	 * 
	 * @param executing the executing agent
	 * @param service service to be invoked
	 * @param method service method
	 * @param parameters invocation parameters
	 * @param exactVersion if true, an exact version match is required, otherwise, an appropriate version will be chosen
	 * @return Returns the invocation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent was locked
	 */
	public Serializable invoke(AgentImpl executing, ServiceNameVersion service, String method,
			Serializable[] parameters, boolean exactVersion) throws AgentLockedException, ServiceInvocationException {
		return invoke(executing, service, method, parameters, exactVersion, false);
	}

	/**
	 * invoke a service method
	 * 
	 * @param executing the executing agent
	 * @param service service to be invoked
	 * @param method service method
	 * @param parameters invocation parameters
	 * @param exactVersion if true, an exact version match is required, otherwise, an appropriate version will be chosen
	 * @param localOnly if true, only locally running services are executed
	 * @return Returns the invocation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent was locked
	 */
	public Serializable invoke(AgentImpl executing, ServiceNameVersion service, String method,
			Serializable[] parameters, boolean exactVersion, boolean localOnly)
			throws ServiceInvocationException, AgentLockedException {

		if (getStatus() != NodeStatus.RUNNING) {
			throw new IllegalStateException("You can invoke methods only on a running node!");
		}

		if (executing.isLocked()) {
			throw new AgentLockedException("The executing agent has to be unlocked to call a RMI");
		}

		int retry = invocationRetryCount;
		while (retry > 0) {
			retry--;

			NodeServiceCache.ServiceInstance instance;

			try {
				instance = this.nodeServiceCache.getServiceAgentInstance(service, exactVersion, localOnly, executing);
			} catch (AgentNotRegisteredException e) {
				throw new ServiceNotFoundException(service.toString(), e);
			}

			if (instance.local()) {
				return invokeLocally(executing, instance.getServiceAgent(), method, parameters);
			} else {
				try {
					return invokeGlobally(executing, instance.getServiceAgentId(), instance.getNodeId(), method,
							parameters);
				} catch (ServiceNotAvailableException e) {
					nodeServiceCache.removeGlobalServiceInstance(instance);
					if (retry == 0) {
						throw new ServiceNotAvailableException("Cannot reach service.", e);
					}
				}
			}
		}

		throw new IllegalStateException();
	}

	/**
	 * invokes a locally running service agent
	 * 
	 * preferably, use {@link #invoke(AgentImpl, ServiceNameVersion, String, Serializable[], boolean, boolean)}
	 * 
	 * @param executing the executing agent
	 * @param serviceAgent the service agent that should be invoked (must run on this node)
	 * @param method service method
	 * @param parameters method parameters
	 * @return innovation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent was locked
	 */
	public Serializable invokeLocally(AgentImpl executing, ServiceAgentImpl serviceAgent, String method,
			Serializable[] parameters) throws ServiceInvocationException, AgentLockedException {

		if (getStatus() != NodeStatus.RUNNING) {
			throw new IllegalStateException("You can invoke methods only on a running node!");
		}

		if (executing.isLocked()) {
			throw new AgentLockedException("The executing agent has to be unlocked to call a RMI");
		}

		// check if local service agent
		if (!hasLocalAgent(serviceAgent)) {
			throw new ServiceNotFoundException("This ServiceAgent is not known locally!");
		}

		// execute
		RMITask task = new RMITask(serviceAgent.getServiceNameVersion(), method, parameters);
		AgentContext context = getAgentContext(executing);

		return serviceAgent.handle(task, context);
	}

	/**
	 * invokes a service instance in the network
	 * 
	 * preferably, use {@link #invoke(AgentImpl, ServiceNameVersion, String, Serializable[], boolean, boolean)}
	 * 
	 * @param executing the executing agent
	 * @param serviceAgentId the id of the service agent
	 * @param nodeId id of the node running the agent (may be null)
	 * @param method service method
	 * @param parameters method parameters
	 * @return invocation result
	 * @throws ServiceInvocationException If service invocation fails
	 * @throws AgentLockedException If the executing agent is locked
	 */
	public Serializable invokeGlobally(AgentImpl executing, String serviceAgentId, Object nodeId, String method,
			Serializable[] parameters) throws ServiceInvocationException, AgentLockedException {

		if (getStatus() != NodeStatus.RUNNING) {
			throw new IllegalStateException("You can invoke methods only on a running node!");
		}

		// Do not log service class name (privacy..)
		this.observerNotice(MonitoringEvent.RMI_SENT, this.getNodeId(), executing, null);

		if (executing.isLocked()) {
			throw new AgentLockedException("The executing agent has to be unlocked to call a RMI");
		}

		ServiceAgentImpl serviceAgent;
		try {
			serviceAgent = (ServiceAgentImpl) getAgent(serviceAgentId);
		} catch (AgentNotFoundException | ClassCastException e) {
			throw new ServiceNotFoundException("This is not a service agent!", e);
		} catch (AgentException e) {
			throw new ServiceNotAvailableException("This service agent is not available!", e);
		}

		try {
			Serializable msg;
			if (executing instanceof PassphraseAgentImpl) {
				msg = new UnlockAgentCall(new RMITask(serviceAgent.getServiceNameVersion(), method, parameters),
						((PassphraseAgentImpl) executing).getPassphrase());
			} else {
				msg = new RMITask(serviceAgent.getServiceNameVersion(), method, parameters);
			}
			Message rmiMessage = new Message(executing, serviceAgent, msg);

			if (this instanceof LocalNode) {
				rmiMessage.setSendingNodeId((Long) getNodeId());
			} else {
				rmiMessage.setSendingNodeId((NodeHandle) getNodeId());
			}
			Message resultMessage;

			if (nodeId != null) {
				try {
					resultMessage = sendMessageAndWaitForAnswer(rmiMessage, nodeId);
				} catch (NodeNotFoundException nex) {
					throw new ServiceNotAvailableException("Cannot reach node!", nex);
				}
			} else {
				resultMessage = sendMessageAndWaitForAnswer(rmiMessage);
			}

			ClassLoader msgClsLoader = null;
			try {
				ServiceAgentImpl localInst = getLocalServiceAgent(serviceAgent.getServiceNameVersion());
				if (localInst != null) {
					msgClsLoader = localInst.getServiceInstance().getClass().getClassLoader();
				}
			} catch (ServiceNotFoundException e) {
				// ok, no local instance found
			}

			try {
				resultMessage.open(executing, this, msgClsLoader);
			} catch (AgentException e) {
				throw new ServiceInvocationException("Could not open received answer!", e);
			}
			Object resultContent = resultMessage.getContent();

			if (resultContent instanceof RMIExceptionContent) {
				Throwable thrown = ((RMIExceptionContent) resultContent).getException();
				// Do not log service class name (privacy..)
				this.observerNotice(MonitoringEvent.RMI_FAILED, this.getNodeId(), executing, thrown.toString());
				if (thrown instanceof ServiceInvocationException) {
					throw (ServiceInvocationException) thrown;
				} else if ((thrown instanceof InvocationTargetException)
						&& (thrown.getCause() instanceof InternalSecurityException)) {
					// internal L2pSecurityException (like internal method access or unauthorizes object access)
					throw new ServiceAccessDeniedException("Internal security exception!", thrown.getCause());
				} else {
					throw new ServiceInvocationException("remote exception at target node", thrown);
				}

			} else if (resultContent instanceof RMIResultContent) {
				// Do not log service class name (privacy..)
				this.observerNotice(MonitoringEvent.RMI_SUCCESSFUL, this.getNodeId(), executing, null);
				return ((RMIResultContent) resultContent).getContent();
			} else {
				// Do not log service class name (privacy..)
				this.observerNotice(MonitoringEvent.RMI_FAILED, this.getNodeId(), executing,
						"Unknown RMI response type: " + resultContent.getClass().getCanonicalName());
				throw new ServiceInvocationException(
						"Unknown RMI response type: " + resultContent.getClass().getCanonicalName());
			}
		} catch (InternalSecurityException e) {
			throw new ServiceInvocationFailedException("Cannot encrypt or decrypt message!", e);
		} catch (TimeoutException | InterruptedException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(MonitoringEvent.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new ServiceNotAvailableException("Service does not respond", e);
		} catch (EncodingFailedException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(MonitoringEvent.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new ServiceInvocationException("message problems!", e);
		} catch (SerializationException e) {
			// Do not log service class name (privacy..)
			this.observerNotice(MonitoringEvent.RMI_FAILED, this.getNodeId(), executing, e.toString());
			throw new ServiceInvocationException("message problems!", e);
		}
	}

	/**
	 * Tries to get an instance of the given class as a registered service of this node.
	 * 
	 * @param service A service name and version to check for
	 * @return the instance of the given service class running at this node
	 * @throws ServiceNotFoundException If the service is not found
	 */
	public ServiceAgentImpl getLocalServiceAgent(ServiceNameVersion service) throws ServiceNotFoundException {
		try {
			return nodeServiceCache.getLocalService(service);
		} catch (Exception e) {
			throw new ServiceNotFoundException(service.toString());
		}
	}

	/**
	 * Registers a MessageResultListener for collecting answers.
	 * 
	 * @param messageId A message id to register for
	 * @param listener An answer listener
	 */
	public void registerAnswerListener(long messageId, MessageResultListener listener) {
		if (listener == null) {
			return;
		}
		htAnswerListeners.put(messageId, listener);
		// Thread to check whether a listener reaches timeout and needs to be removed from the table
		new Thread() {
		      public void run(){
		    	  	long listenerId =  messageId;
		    	  	long sleepTime = listener.getTimeoutTime();
			        while(!htAnswerListeners.get(listenerId).checkTimeOut()) {			        	
				        try {
							Thread.sleep(sleepTime);		
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							System.out.println("Thread interupted because of "+e );
							e.printStackTrace();
						}
				        if(htAnswerListeners.get(listenerId) == null) {
							this.interrupt();
							break;
						}
			        }
			        if(!this.isInterrupted()) {
			        	htAnswerListeners.remove(listenerId);
			        }
		        }
		   }.start();
	}

	/**
	 * Hands over an answer message to the corresponding listener.
	 * 
	 * @param answer A answer message to handle
	 * @return true, if a listener for this answer was notified
	 */
	public boolean handoverAnswer(Message answer) {
		if (!answer.isResponse()) {
			return false;
		}

		observerNotice(MonitoringEvent.MESSAGE_RECEIVED_ANSWER, answer.getSendingNodeId(), answer.getSenderId(),
				this.getNodeId(), answer.getRecipientId(), "" + answer.getResponseToId());

		MessageResultListener listener = htAnswerListeners.get(answer.getResponseToId());
		if (listener == null) {
			System.out.println("Did not find corresponding observer!");
			return false;
		}
		
		listener.collectAnswer(answer);
		// Remove listener from list if no more messages are expected
		if(listener.getNumberOfExpectedResults() == listener.getNumberOfResults()) {
			htAnswerListeners.remove(answer.getResponseToId());
		}
		return true;
	}

	/**
	 * Sends a message and wait for one answer message.
	 * 
	 * @param m A message to send
	 * @return a (possible) response message
	 * @throws InterruptedException If sending the message was interrupted
	 * @throws TimeoutException If sending the message timeouts
	 */
	public Message sendMessageAndWaitForAnswer(Message m) throws InterruptedException, TimeoutException {
		long timeout = m.getTimeoutTs() - new Date().getTime();
		MessageResultListener listener = new MessageResultListener(timeout);

		sendMessage(m, listener);

		listener.waitForOneAnswer();

		if (listener.isSuccess()) {
			return listener.getResults()[0];
		} else {
			throw new TimeoutException();
		}
	}

	/**
	 * Sends a message to the given id and wait for one answer message.
	 * 
	 * @param m A message to send
	 * @param atNodeId A node id to send from
	 * @return a response message
	 * @throws NodeNotFoundException If no node was not found with given id
	 * @throws InterruptedException If sending the message was interrupted
	 * @throws TimeoutException If sending the message timeouts
	 */
	public Message sendMessageAndWaitForAnswer(Message m, Object atNodeId)
			throws NodeNotFoundException, InterruptedException, TimeoutException {
		long timeout = m.getTimeoutTs() - new Date().getTime();
		MessageResultListener listener = new MessageResultListener(timeout);

		sendMessage(m, atNodeId, listener);

		listener.waitForOneAnswer();

		if (listener.getResults().length == 0) {
			throw new TimeoutException("No answer received!");
		}

		return listener.getResults()[0];
	}

	/**
	 * Sends a message and wait for answer messages
	 * 
	 * Uses a broadcast
	 * 
	 * @param m A message to send
	 * @param recipientCount expected number of answers
	 * @return Returns an array with all collected answers
	 * @throws InterruptedException If sending the message was interrupted
	 * @throws TimeoutException If sending the message timeouts
	 */
	public Message[] sendMessageAndCollectAnswers(Message m, int recipientCount)
			throws InterruptedException, TimeoutException {
		long timeout = m.getTimeoutTs() - new Date().getTime();
		MessageResultListener listener = new MessageResultListener(timeout, timeout / 4);
		listener.addRecipients(recipientCount);

		sendMessage(m, listener, SendMode.BROADCAST);

		listener.waitForAllAnswers(false);

		Message[] results = listener.getResults();

		if (results.length > 0) {
			return results;
		} else {
			throw new TimeoutException("No answer received!");
		}
	}

	/**
	 * Gets the local execution context of an agent. If there is currently none, a new one will be created and stored
	 * for later use.
	 * 
	 * @param agentId An agent id to get the context for
	 * @return the context for the given agent
	 * @throws AgentException If any issue with the agent occurs, e. g. XML not readable
	 */
	public AgentContext getAgentContext(String agentId) throws AgentException {
		AgentContext result = htLocalExecutionContexts.get(agentId);

		if (result == null) {
			AgentImpl agent = getAgent(agentId);
			result = new AgentContext(this, agent);
			htLocalExecutionContexts.put(agentId, result);
		}

		result.touch();

		return result;
	}

	/**
	 * Gets a (possibly fresh) context for the given agent.
	 * 
	 * @param agent An agent to get the context for
	 * @return Returns a context
	 */
	public AgentContext getAgentContext(AgentImpl agent) {
		AgentContext result = htLocalExecutionContexts.get(agent.getIdentifier());

		if (result == null || (result.getMainAgent().isLocked() && !agent.isLocked())) {
			result = new AgentContext(this, agent);
			htLocalExecutionContexts.put(agent.getIdentifier(), result);
		}

		result.touch();

		return result;
	}

	/**
	 * Checks, if the given service class is running at this node.
	 * 
	 * @param service A service name and version to check for
	 * @return true, if this node as an instance of the given service running
	 */
	public boolean hasService(ServiceNameVersion service) {
		// return hasAgent(ServiceAgent.serviceClass2Id(service));
		try {
			nodeServiceCache.getLocalService(service);
			return true;
		} catch (AgentNotRegisteredException e) {
			return false;
		}
	}

	/**
	 * get the NodeServiceCache of this node
	 * 
	 * @return Returns the {@code NodeServiceCache} instance for this node
	 */
	public NodeServiceCache getNodeServiceCache() {
		return this.nodeServiceCache;
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
		double cpuLoad = getNodeCpuLoad();
		if (cpuLoad > cpuLoadThreshold) {
			observerNotice(MonitoringEvent.NODE_BUSY, this.getNodeId(),
					"CPU Load: " + cpuLoad + " (Node threshold: " + cpuLoadThreshold + ")");
			return true;
		} else {
			return false;
		}
	}

	public void setCpuLoadThreshold(double cpuLoadThreshold) {
		this.cpuLoadThreshold = cpuLoadThreshold;
	}

	/**
	 * Gets the approximate RAM load of the JVM the Node is running on.
	 * 
	 * @return the total amount of memory currently available for current and future objects, measured in bytes.
	 */
	public long getNodeRAMLoad() {
		return runtime.totalMemory();
	}

	public long getNodeFreeRAMLoad() {
		return runtime.freeMemory();
	}

	// Tidy up Timer

	/**
	 * starts the tidy up timer
	 */
	private void startTidyUpTimer() {
		if (tidyUpTimer != null) {
			return;
		}
		tidyUpTimer = new Timer();
		tidyUpTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				runTidyUpTimer();
			}
		}, 0, tidyUpTimerInterval * 1000);
	}

	/**
	 * stops the tidy up timer
	 */
	private void stopTidyUpTimer() {
		if (tidyUpTimer != null) {
			tidyUpTimer.cancel();
			tidyUpTimer = null;
		}
	}

	/**
	 * executed by the tidy up timer, currently it does:
	 * 
	 * Deleting old {@link AgentContext} objects from {@link #htLocalExecutionContexts}
	 */
	protected void runTidyUpTimer() {
		synchronized (htLocalExecutionContexts) {
			Iterator<AgentContext> itContext = htLocalExecutionContexts.values().iterator();
			while (itContext.hasNext()) {
				AgentContext context = itContext.next();
				if (context.getLastUsageTimestamp() <= new Date().getTime() - agentContextLifetime * 1000) {
					itContext.remove();
				}
			}
		}
	}

	public Date getStartTime() {
		return startTime;
	}

}
