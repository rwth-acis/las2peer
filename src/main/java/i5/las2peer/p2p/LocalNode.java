package i5.las2peer.p2p;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.LocalStorage;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.TimerThread;

/**
 * Implementation of the abstract {@link Node} class mostly for testing purposes. All data and agents will be stored in
 * the same JVM, which may be used in JUnit test cases or to launch a <i>local only</i> server for example.
 * 
 * TODO: uses loggers / observers
 */
public class LocalNode extends Node {

	/**
	 * an id for this node
	 */
	private long nodeId;

	/**
	 * create a LocalNode
	 */
	private LocalNode() {
		super();

		Random r = new Random();
		nodeId = r.nextLong();

		setStatus(NodeStatus.CONFIGURED);
	}

	/**
	 * create a LocalNode
	 * 
	 * @param classManager
	 */
	private LocalNode(L2pClassManager classManager) {
		super(classManager);

		Random r = new Random();
		nodeId = r.nextLong();

		setStatus(NodeStatus.CONFIGURED);
	}

	/**
	 * get the id of this node
	 * 
	 * @return id of this node
	 */
	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	protected void launchSub() {
		setStatus(NodeStatus.RUNNING);

		registerNode(this);
	}

	@Override
	public void shutDown() {
		super.shutDown();
		unregisterNode(this);
	}

	@Override
	public void registerReceiver(MessageReceiver receiver)
			throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException {
		super.registerReceiver(receiver);

		if (receiver instanceof Agent) {
			Agent agent = (Agent) receiver;
			try {
				htKnownAgents.put(((Agent) receiver).getId(), agent.toXmlString());
			} catch (SerializationException e) {
				throw new AgentException("Could not register agent reciever", e);
			}
		}

		deliverPendingMessages(receiver.getResponsibleForAgentId(), getNodeId());
	}

	// TODO this code should be here or not?
//	@Override
//	public void unregisterReceiver(MessageReceiver receiver) throws AgentNotKnownException, NodeException {
//		super.unregisterReceiver(receiver);
//		if (receiver instanceof Agent) {
//			Agent agent = (Agent) receiver;
//			htKnownAgents.remove(agent.getId());
//		}
//	}

	@Override
	public void sendMessage(Message message, MessageResultListener listener, SendMode mode) {
		message.setSendingNodeId(this.getNodeId());
		registerAnswerListener(message.getId(), listener);

		try {
			switch (mode) {
			case ANYCAST:
				if (!message.isTopic()) {
					localSendMessage(findFirstNodeWithAgent(message.getRecipientId()), message);
				} else {
					Long[] ids = findAllNodesWithTopic(message.getTopicId());

					if (ids.length == 0) {
						listener.collectException(
								new MessageException("No agent listening to topic " + message.getTopicId()));
					} else {
						localSendMessage(ids[0], message);
					}
				}

				break;
			case BROADCAST:
				if (!message.isTopic()) {
					Long[] ids = findAllNodesWithAgent(message.getRecipientId());

					if (ids.length == 0) {
						listener.collectException(new AgentNotKnownException(message.getRecipientId()));
					} else {
						listener.addRecipients(ids.length - 1);
						for (long id : ids) {
							localSendMessage(id, message);
						}
					}
				} else {
					Long[] ids = findAllNodesWithTopic(message.getTopicId());

					if (ids.length == 0) {
						listener.collectException(
								new MessageException("No agent listening to topic " + message.getTopicId()));
					} else {
						listener.addRecipients(ids.length - 1);
						for (long id : ids) {
							localSendMessage(id, message);
						}
					}

				}

			}
		} catch (AgentNotKnownException e) {
			storeMessage(message, listener);
		}
	}

	@Override
	public void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws AgentNotKnownException, NodeNotFoundException {
		message.setSendingNodeId(this.getNodeId());

		if (!(atNodeId instanceof Long)) {
			throw new IllegalArgumentException("a node id for a LocalNode has to be a Long!");
		}

		if (!hasNode((Long) atNodeId)) {
			if (listener != null) {
				listener.collectException(new NodeNotFoundException((Long) atNodeId));
			}
		} else {
			if (listener != null) {
				registerAnswerListener(message.getId(), listener);
			}
			localSendMessage((Long) atNodeId, message);
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
	 * @deprecated Use {@link #storeEnvelope(Envelope, Agent)} instead
	 */
	@Deprecated
	@Override
	public void storeArtifact(Envelope envelope) throws EnvelopeAlreadyExistsException, StorageException {
		storage.storeEnvelope(envelope, AgentContext.getCurrent().getMainAgent(), 0);
	}

	/**
	 * @deprecated Use {@link #removeEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public void removeArtifact(long id, byte[] signature) throws ArtifactNotFoundException, StorageException {
		storage.removeEnvelope(Long.toString(id));
	}

	@Override
	public Object[] findRegisteredAgent(long agentId, int hintOfExpectedCount) throws AgentNotKnownException {
		return findAllNodesWithAgent(agentId);
	}

	@Override
	public Agent getAgent(long id) throws AgentNotKnownException {
		Agent anonymous = getAnonymous();
		if (id == anonymous.getId()) { // TODO use isAnonymous, special ID or Classing for identification
			return anonymous;
		} else {
			synchronized (htKnownAgents) {
				String xml = htKnownAgents.get(id);
				if (xml == null) {
					throw new AgentNotKnownException(id);
				}

				try {
					return Agent.createFromXml(xml);
				} catch (MalformedXMLException e) {
					throw new AgentNotKnownException("XML problems with storage!", e);
				}
			}
		}
	}

	@Override
	public void storeAgent(Agent agent) throws L2pSecurityException, AgentException {
		synchronized (htKnownAgents) {
			// only accept unlocked agents at startup
			if (agent.isLocked() && getStatus() == NodeStatus.RUNNING) {
				throw new L2pSecurityException("Only unlocked agents may be updated during runtime!");
			}

			if (htKnownAgents.get(agent.getId()) != null) {
				throw new AgentAlreadyRegisteredException("Agent " + agent.getId() + " already in storage");
			}

			String agentXml = null;
			try {
				agentXml = agent.toXmlString();
			} catch (SerializationException e) {
				throw new AgentException("Serialization failed!", e);
			}

			htKnownAgents.put(agent.getId(), agentXml);

			if (agent instanceof UserAgent) {
				getUserManager().registerUserAgent((UserAgent) agent);
			}
		}
	}

	@Override
	public void updateAgent(Agent agent) throws AgentException, L2pSecurityException {
		if (agent.isLocked()) {
			throw new L2pSecurityException("Only unlocked agents may be updated!");
		}

		synchronized (htKnownAgents) {
			if (htKnownAgents.get(agent.getId()) == null) {
				throw new AgentNotKnownException(agent.getId());
			}

			// TODO: verify, that it is the same agent!!! (e.g. the same private key)
			// idea: encrypt to stored agent
			// decrypt with new agent (which is unlocked)
			// then update is ok

			// other idea:
			// get rid of agent id
			// use hash value of private key instead

			// this is verifyable on each local node

			String agentXml = null;
			try {
				agentXml = agent.toXmlString();
			} catch (SerializationException e) {
				throw new AgentException("Serialization failed!", e);
			}

			htKnownAgents.put(agent.getId(), agentXml);

			if (agent instanceof UserAgent) {
				getUserManager().updateUserAgent((UserAgent) agent);
			}
		}
	}

	@Override
	public Object[] getOtherKnownNodes() {
		return htLocalNodes.values().toArray();
	}

	@Override
	public NodeInformation getNodeInformation(Object nodeId) throws NodeNotFoundException {
		try {
			LocalNode node = getNode((Long) nodeId);
			return node.getNodeInformation();
		} catch (Exception e) {
			throw new NodeNotFoundException("Node with id " + nodeId + " not found");
		}
	}

	/************************** factories ***************************************/

	/**
	 * 
	 * @return a new configured but not running node
	 */
	public static LocalNode newNode() {
		return new LocalNode();
	}

	/**
	 * create a LocalNode using a FileSystemRepository at the given location
	 * 
	 * @param fileSystemRepository a path to the service directory
	 * @return
	 */
	public static LocalNode newNode(String fileSystemRepository) {
		return new LocalNode(new L2pClassManager(new FileSystemRepository(fileSystemRepository),
				ClassLoader.getSystemClassLoader()));
	}

	/**
	 * factory: launch a node
	 * 
	 * @return a freshly started node
	 */
	public static LocalNode launchNode() {
		LocalNode result = newNode();
		try {
			result.launch();
		} catch (NodeException e) {
		}
		return result;
	}

	/**
	 * factory: launch a node an register the given agent
	 * 
	 * @param a
	 * @return a freshly started node hosting the given agent
	 * @throws L2pSecurityException
	 * @throws AgentException
	 */
	public static LocalNode launchAgent(Agent a) throws L2pSecurityException, AgentException {
		LocalNode result = launchNode();
		try {
			result.registerReceiver(a);
		} catch (AgentAlreadyRegisteredException e) {
			// should not occur with a freshly generated node
		}
		return result;
	}

	/****************************** static *****************************************/

	private static Hashtable<Long, LocalNode> htLocalNodes = new Hashtable<Long, LocalNode>();

	private static LocalStorage storage = new LocalStorage();

	private static Hashtable<Long, Hashtable<Message, MessageResultListener>> htPendingMessages = new Hashtable<Long, Hashtable<Message, MessageResultListener>>();

	/**
	 * Hashtable with string representations of all known agents
	 */
	private static Hashtable<Long, String> htKnownAgents = new Hashtable<Long, String>();

	/**
	 * register a node for later use
	 * 
	 * @param node
	 */
	private static void registerNode(LocalNode node) {
		synchronized (htLocalNodes) {
			htLocalNodes.put(node.getNodeId(), node);
		}
	}

	/**
	 * remove a node from the central storage
	 * 
	 * @param node
	 */
	private static void unregisterNode(LocalNode node) {
		synchronized (htLocalNodes) {
			htLocalNodes.remove(node.getNodeId());
		}
	}

	/**
	 * get a node from the central storage
	 * 
	 * @param id
	 * @return the node with the given id
	 */
	public static LocalNode getNode(long id) {
		synchronized (htLocalNodes) {
			return htLocalNodes.get(id);
		}
	}

	/**
	 * does the given node exist in the central storage?
	 * 
	 * @param id
	 * @return true, if a node of the given it is known to the registry
	 */
	public static boolean hasNode(long id) {
		synchronized (htLocalNodes) {
			return getNode(id) != null;
		}
	}

	/**
	 * do a complete restart of all nodes, artifacts and messages
	 */
	public static void reset() {
		htPendingMessages = new Hashtable<Long, Hashtable<Message, MessageResultListener>>();
		storage = new LocalStorage();
		htKnownAgents = new Hashtable<Long, String>();
		htLocalNodes = new Hashtable<Long, LocalNode>();

		iMessageMinWait = DEFAULT_MESSAGE_MIN_WAIT;
		iMessageMaxWait = DEFAULT_MESSAGE_MAX_WAIT;
		lPendingTimeout = DEFAULT_PENDING_TIMEOUT;

		stopCleaner();

		startPendingTimer();
	}

	/**
	 * stop the timeout cleaner thread
	 */
	public static void stopCleaner() {
		if (pendingTimer != null) {
			pendingTimer.stopTimer();
			pendingTimer.interrupt();
			try {
				pendingTimer.join();
			} catch (InterruptedException e) {
			}
		}

	}

	/**
	 * find the first node, where the given agent is registered to
	 * 
	 * @param agentId
	 * @return id of a node hosting the given agent
	 * @throws AgentNotKnownException
	 */
	public static long findFirstNodeWithAgent(long agentId) throws AgentNotKnownException {
		synchronized (htLocalNodes) {

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasLocalAgent(agentId)) {
					return nodeId;
				}
			}

			throw new AgentNotKnownException(agentId);
		}
	}

	/**
	 * get the ids of all nodes where the given agent is running
	 * 
	 * @param agentId
	 * @return array with all ids of nodes hosting the given agent
	 */
	public static Long[] findAllNodesWithAgent(long agentId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<Long>();

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasLocalAgent(agentId)) {
					hsResult.add(nodeId);
				}
			}

			return hsResult.toArray(new Long[0]);
		}
	}

	/**
	 * get the ids of all nodes where agents listening to the given topic are running
	 * 
	 * @param topicId
	 * @return
	 */
	public static Long[] findAllNodesWithTopic(long topicId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<Long>();

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasTopic(topicId)) {
					hsResult.add(nodeId);
				}
			}

			return hsResult.toArray(new Long[0]);
		}
	}

	/**
	 * store messages for agents not known to this "network" of nodes
	 * 
	 * @param message
	 * @param listener
	 */
	protected static void storeMessage(Message message, MessageResultListener listener) {
		synchronized (htPendingMessages) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get(message.getRecipientId());
			if (pending == null) {
				pending = new Hashtable<Message, MessageResultListener>();
				htPendingMessages.put(message.getRecipientId(), pending);
			}

			pending.put(message, listener);
		}
	}

	/**
	 * fetch all pending messages for the given agent
	 * 
	 * @param recipientId
	 * @param nodeId
	 */
	protected static void deliverPendingMessages(long recipientId, long nodeId) {

		synchronized (htPendingMessages) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get(recipientId);

			if (pending != null) {
				for (Message m : pending.keySet()) {
					System.out.println("send pending message..." + m.getId());
					localSendMessage(nodeId, m);
				}

				htPendingMessages.remove(recipientId);
			}
		}

	}

	/**
	 * get all expired messages and notify their senders
	 */
	protected static void notifExpiredMessages() {
		synchronized (htPendingMessages) {

			System.out.println("checking for expired messages");
			System.out.println("waiting for " + htPendingMessages.size() + " agents ");

			for (long agentId : htPendingMessages.keySet()) {
				Hashtable<Message, MessageResultListener> agentMessages = htPendingMessages.get(agentId);

				for (Message m : agentMessages.keySet()) {
					MessageResultListener mrl = agentMessages.get(m);
					if (mrl.checkTimeOut()) {
						System.out.println("message " + m.getId() + " is timed out!");
						agentMessages.remove(m);
					}
				}

				// remove agent entry, if empty
				if (agentMessages.size() == 0) {
					htPendingMessages.remove(agentId);
				}
			}
		}
	}

	private static final long DEFAULT_PENDING_TIMEOUT = 20000; // 20 seconds
	private static final int DEFAULT_MESSAGE_MIN_WAIT = 500;
	private static final int DEFAULT_MESSAGE_MAX_WAIT = 550;

	private static int iMessageMinWait = DEFAULT_MESSAGE_MIN_WAIT;
	private static int iMessageMaxWait = DEFAULT_MESSAGE_MAX_WAIT;
	private static long lPendingTimeout = DEFAULT_PENDING_TIMEOUT;

	private static TimerThread pendingTimer = null;

	public static void setPendingTimeOut(int newtimeout) {
		lPendingTimeout = newtimeout;
	}

	public static int getMinMessageWait() {
		return iMessageMinWait;
	}

	public static int getMaxMessageWait() {
		return iMessageMaxWait;
	}

	public static void setMinMessageWait(int time) {
		iMessageMinWait = time;
	}

	public static void setMaxMessageWait(int time) {
		iMessageMaxWait = time;
	}

	static {
		startPendingTimer();
	}

	/**
	 * start a thread clearing up expired messages from time to time
	 */
	private static void startPendingTimer() {
		pendingTimer = new TimerThread(lPendingTimeout) {

			@Override
			public void tick() {
				notifExpiredMessages();
			}

		};

		pendingTimer.start();
	}

	/**
	 * does the actual <i>sending</i> of a message in a separate thread with a configurable delay
	 * 
	 * @param nodeId
	 * @param message
	 */
	private static void localSendMessage(final long nodeId, final Message message) {

		// it is important to close the message her,
		// since the recipient knows other versions of the involved agents
		message.close();

		new Thread(new Runnable() {
			@Override
			public void run() {
				Random r = new Random();

				int wait = iMessageMinWait + r.nextInt(iMessageMaxWait - iMessageMinWait + 1);
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e1) {
				}

				try {
					getNode(nodeId).receiveMessage(message.clone());
				} catch (Exception e) {
					System.out.println("problems at node " + nodeId);
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(identifier, content, reader);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content, reader);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException {
		// XXX make configurable
		storage.storeEnvelope(envelope, author, 10000);
	}

	@Override
	public Envelope fetchEnvelope(String identifier) throws StorageException {
		// XXX make configurable
		return storage.fetchEnvelope(identifier, 10000);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createUnencryptedEnvelope(identifier, content);
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs)
			throws EnvelopeAlreadyExistsException, StorageException {
		storage.storeEnvelope(envelope, author, timeoutMs);
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		storage.storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler, exceptionHandler);
	}

	@Override
	public Envelope fetchEnvelope(String identifier, long timeoutMs)
			throws ArtifactNotFoundException, StorageException {
		return storage.fetchEnvelope(identifier, timeoutMs);
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		storage.fetchEnvelopeAsync(identifier, envelopeHandler, exceptionHandler);
	}

	@Override
	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException {
		storage.removeEnvelope(identifier);
	}

	@Override
	public Envelope fetchArtifact(String identifier) throws ArtifactNotFoundException, StorageException {
		// XXX make configurable
		return storage.fetchEnvelope(identifier, 10000);
	}

}
