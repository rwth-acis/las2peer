package i5.las2peer.p2p;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.LocalStorage;
import i5.las2peer.persistency.StorageCollisionHandler;
import i5.las2peer.persistency.StorageEnvelopeHandler;
import i5.las2peer.persistency.StorageExceptionHandler;
import i5.las2peer.persistency.StorageStoreResultHandler;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

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
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException, AgentException {
		super.registerReceiver(receiver);

		if (receiver instanceof AgentImpl) {
			AgentImpl agent = (AgentImpl) receiver;
			try {
				htKnownAgents.put(((AgentImpl) receiver).getIdentifier(), agent.toXmlString());
			} catch (SerializationException e) {
				throw new AgentException("Could not register agent reciever", e);
			}
		}

		deliverPendingMessages(receiver.getResponsibleForAgentSafeId(), getNodeId());
	}

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
						listener.collectException(new AgentNotRegisteredException(message.getRecipientId()));
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
		} catch (AgentNotRegisteredException e) {
			storeMessage(message, listener);
		}
	}

	@Override
	public void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws NodeNotFoundException {
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
	public EnvelopeVersion fetchArtifact(long id) throws EnvelopeNotFoundException, EnvelopeException {
		return fetchEnvelope(Long.toString(id));
	}

	/**
	 * @deprecated Use {@link #storeEnvelope(EnvelopeVersion, AgentImpl)} instead
	 */
	@Deprecated
	@Override
	public void storeArtifact(EnvelopeVersion envelope) throws EnvelopeAlreadyExistsException, EnvelopeException {
		storage.storeEnvelope(envelope, AgentContext.getCurrent().getMainAgent(), 0);
	}

	/**
	 * @deprecated Use {@link #removeEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public void removeArtifact(long id, byte[] signature) throws EnvelopeNotFoundException, EnvelopeException {
		storage.removeEnvelope(Long.toString(id));
	}

	@Override
	public Object[] findRegisteredAgent(String agentId, int hintOfExpectedCount) throws AgentNotRegisteredException {
		return findAllNodesWithAgent(agentId);
	}

	@Override
	public AgentImpl getAgent(String id) throws AgentNotFoundException {
		if (id.equalsIgnoreCase(AnonymousAgent.IDENTIFIER)) {
			return AnonymousAgentImpl.getInstance();
		} else {
			synchronized (htKnownAgents) {
				String xml = htKnownAgents.get(id);
				if (xml == null) {
					throw new AgentNotFoundException(id);
				}

				try {
					return AgentImpl.createFromXml(xml);
				} catch (MalformedXMLException e) {
					throw new AgentNotFoundException("XML problems with storage!", e);
				}
			}
		}
	}

	@Override
	public void storeAgent(AgentImpl agent) throws AgentException {
		synchronized (htKnownAgents) {
			// only accept unlocked agents at startup
			if (agent.isLocked()) {
				throw new AgentLockedException();
			}
			if (agent instanceof AnonymousAgent) {
				throw new AgentException("Must not store anonymous agent");
			}

			String agentXml = null;
			try {
				agentXml = agent.toXmlString();
			} catch (SerializationException e) {
				throw new AgentException("Serialization failed!", e);
			}

			htKnownAgents.put(agent.getIdentifier(), agentXml);

			if (agent instanceof UserAgentImpl) {
				getUserManager().registerUserAgent((UserAgentImpl) agent);
			}
		}
	}

	@Deprecated
	@Override
	public void updateAgent(AgentImpl agent) throws AgentException, InternalSecurityException {
		storeAgent(agent);
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
	 * @return Returns a LocalNode instance
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
	 * @throws InternalSecurityException
	 * @throws AgentException
	 */
	public static LocalNode launchAgent(AgentImpl a) throws InternalSecurityException, AgentException {
		LocalNode result = launchNode();
		try {
			result.registerReceiver(a);
		} catch (AgentAlreadyRegisteredException e) {
			// should not occur with a freshly generated node
		}
		return result;
	}

	/****************************** static *****************************************/

	private static Hashtable<Long, LocalNode> htLocalNodes = new Hashtable<>();

	private static LocalStorage storage = new LocalStorage();

	private static Hashtable<String, Hashtable<Message, MessageResultListener>> htPendingMessages = new Hashtable<>();

	/**
	 * Hashtable with string representations of all known agents
	 */
	private static Hashtable<String, String> htKnownAgents = new Hashtable<>();

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
		htPendingMessages = new Hashtable<>();
		storage = new LocalStorage();
		htKnownAgents = new Hashtable<>();
		htLocalNodes = new Hashtable<>();

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
		synchronized (pendingTimer) {
			if (pendingTimerTask != null) {
				pendingTimerTask.cancel();
				pendingTimerTask = null;
			}
		}
	}

	/**
	 * find the first node, where the given agent is registered to
	 * 
	 * @param agentId
	 * @return id of a node hosting the given agent
	 * @throws AgentNotRegisteredException
	 */
	public static long findFirstNodeWithAgent(String agentId) throws AgentNotRegisteredException {
		synchronized (htLocalNodes) {

			for (long nodeId : htLocalNodes.keySet()) {
				if (htLocalNodes.get(nodeId).hasLocalAgent(agentId)) {
					return nodeId;
				}
			}

			throw new AgentNotRegisteredException(agentId);
		}
	}

	/**
	 * get the ids of all nodes where the given agent is running
	 * 
	 * @param agentId
	 * @return array with all ids of nodes hosting the given agent
	 */
	public static Long[] findAllNodesWithAgent(String agentId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<>();

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
	 * @return Returns a list with all node ids for the given topic
	 */
	public static Long[] findAllNodesWithTopic(long topicId) {
		synchronized (htLocalNodes) {
			HashSet<Long> hsResult = new HashSet<>();

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
				pending = new Hashtable<>();
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
	protected static void deliverPendingMessages(String recipientId, long nodeId) {

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

			for (String agentId : htPendingMessages.keySet()) {
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

	private final static Timer pendingTimer = new Timer();
	private static TimerTask pendingTimerTask;

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
		synchronized (pendingTimer) {
			if (pendingTimerTask == null) {
				pendingTimerTask = new TimerTask() {
					@Override
					public void run() {
						notifExpiredMessages();
					}
				};
				pendingTimer.schedule(pendingTimerTask, 0, lPendingTimeout);
			}
		}
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
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}).start();
	}

	@Override
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			AgentImpl... reader) throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(identifier, authorPubKey, content, reader);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, AgentImpl... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content, reader);
	}

	@Override
	public void storeEnvelope(EnvelopeVersion envelope, AgentImpl author) throws EnvelopeException {
		// XXX make configurable
		storage.storeEnvelope(envelope, author, 10000);
	}

	@Override
	public EnvelopeVersion fetchEnvelope(String identifier) throws EnvelopeException {
		// XXX make configurable
		return storage.fetchEnvelope(identifier, 10000);
	}

	@Override
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			Collection<?> readers) throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(identifier, authorPubKey, content, readers);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createEnvelope(previousVersion, content, readers);
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(String identifier, PublicKey authorPubKey, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createUnencryptedEnvelope(identifier, authorPubKey, content);
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return storage.createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(EnvelopeVersion envelope, AgentImpl author, long timeoutMs)
			throws EnvelopeAlreadyExistsException, EnvelopeException {
		storage.storeEnvelope(envelope, author, timeoutMs);
	}

	@Override
	public void storeEnvelopeAsync(EnvelopeVersion envelope, AgentImpl author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		storage.storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler, exceptionHandler);
	}

	@Override
	public EnvelopeVersion fetchEnvelope(String identifier, long timeoutMs)
			throws EnvelopeNotFoundException, EnvelopeException {
		return storage.fetchEnvelope(identifier, timeoutMs);
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		storage.fetchEnvelopeAsync(identifier, envelopeHandler, exceptionHandler);
	}

	@Override
	public void removeEnvelope(String identifier) throws EnvelopeNotFoundException, EnvelopeException {
		storage.removeEnvelope(identifier);
	}

	@Override
	public EnvelopeVersion fetchArtifact(String identifier) throws EnvelopeNotFoundException, EnvelopeException {
		// XXX make configurable
		return storage.fetchEnvelope(identifier, 10000);
	}

}
