package i5.las2peer.p2p;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Random;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.classLoaders.ClassManager;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.StorageCollisionHandler;
import i5.las2peer.persistency.StorageEnvelopeHandler;
import i5.las2peer.persistency.StorageExceptionHandler;
import i5.las2peer.persistency.StorageStoreResultHandler;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.BotAgent;
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

	private final LocalNodeManager localNodeManager;

	/**
	 * an id for this node
	 */
	private long nodeId;

	/**
	 * create a LocalNode
	 * 
	 * @param localNodeManager A manager to handle a group (network) of local nodes
	 */
	public LocalNode(LocalNodeManager localNodeManager) {
		this(localNodeManager, null);
	}

	/**
	 * create a LocalNode
	 * 
	 * @param localNodeManager A manager to handle a group (network) of local nodes
	 * @param classManager A class manager to use
	 */
	public LocalNode(LocalNodeManager localNodeManager, ClassManager classManager) {
		super(classManager);
		this.localNodeManager = localNodeManager;

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

		localNodeManager.registerNode(this);
	}

	@Override
	public void shutDown() {
		super.shutDown();
		localNodeManager.unregisterNode(this);
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException, AgentException {
		super.registerReceiver(receiver);

		if (receiver instanceof AgentImpl) {
			AgentImpl agent = (AgentImpl) receiver;
			try {
				localNodeManager.getKnownAgents().put(((AgentImpl) receiver).getIdentifier(), agent.toXmlString());
			} catch (SerializationException e) {
				throw new AgentException("Could not register agent reciever", e);
			}
		}

		localNodeManager.deliverPendingMessages(receiver.getResponsibleForAgentSafeId(), getNodeId());
	}

	@Override
	public void sendMessage(Message message, MessageResultListener listener, SendMode mode) {
		message.setSendingNodeId(this.getNodeId());
		registerAnswerListener(message.getId(), listener);

		try {
			switch (mode) {
			case ANYCAST:
				if (!message.isTopic()) {
					localNodeManager.localSendMessage(localNodeManager.findFirstNodeWithAgent(message.getRecipientId()),
							message);
				} else {
					Long[] ids = localNodeManager.findAllNodesWithTopic(message.getTopicId());

					if (ids.length == 0) {
						listener.collectException(
								new MessageException("No agent listening to topic " + message.getTopicId()));
					} else {
						localNodeManager.localSendMessage(ids[0], message);
					}
				}

				break;
			case BROADCAST:
				if (!message.isTopic()) {
					Long[] ids = localNodeManager.findAllNodesWithAgent(message.getRecipientId());

					if (ids.length == 0) {
						listener.collectException(new AgentNotRegisteredException(message.getRecipientId()));
					} else {
						listener.addRecipients(ids.length - 1);
						for (long id : ids) {
							localNodeManager.localSendMessage(id, message);
						}
					}
				} else {
					Long[] ids = localNodeManager.findAllNodesWithTopic(message.getTopicId());

					if (ids.length == 0) {
						listener.collectException(
								new MessageException("No agent listening to topic " + message.getTopicId()));
					} else {
						listener.addRecipients(ids.length - 1);
						for (long id : ids) {
							localNodeManager.localSendMessage(id, message);
						}
					}

				}

			}
		} catch (AgentNotRegisteredException e) {
			localNodeManager.storeMessage(message, listener);
		}
	}

	@Override
	public void sendMessage(Message message, Object atNodeId, MessageResultListener listener)
			throws NodeNotFoundException {
		message.setSendingNodeId(this.getNodeId());

		if (!(atNodeId instanceof Long)) {
			throw new IllegalArgumentException("a node id for a LocalNode has to be a Long!");
		}

		if (!localNodeManager.hasNode((Long) atNodeId)) {
			if (listener != null) {
				listener.collectException(new NodeNotFoundException((Long) atNodeId));
			}
		} else {
			if (listener != null) {
				registerAnswerListener(message.getId(), listener);
			}
			localNodeManager.localSendMessage((Long) atNodeId, message);
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
		localNodeManager.getStorage().storeEnvelope(envelope, AgentContext.getCurrent().getMainAgent(), 0);
	}

	/**
	 * @deprecated Use {@link #removeEnvelope(String)} instead
	 */
	@Deprecated
	@Override
	public void removeArtifact(long id, byte[] signature) throws EnvelopeNotFoundException, EnvelopeException {
		localNodeManager.getStorage().removeEnvelope(Long.toString(id));
	}

	@Override
	public Object[] findRegisteredAgent(String agentId, int hintOfExpectedCount) throws AgentNotRegisteredException {
		return localNodeManager.findAllNodesWithAgent(agentId);
	}

	@Override
	public AgentImpl getAgent(String id) throws AgentNotFoundException {
		if (id.equalsIgnoreCase(AnonymousAgent.IDENTIFIER)) {
			return AnonymousAgentImpl.getInstance();
		} else {
			synchronized (localNodeManager.getKnownAgents()) {
				String xml = localNodeManager.getKnownAgents().get(id);
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

	public void storeAgent(Agent agent) throws AgentException {
		storeAgent((AgentImpl) agent);
	}
	@Override
	public void secretFunctionStore(AgentImpl agent) throws AgentException {
		this.storeAgent(agent);
	}
	@Override
	public AgentImpl getAgentSecret(String id) throws AgentException {
		AgentImpl agent = this.getAgent(id);
		return agent;
	}
	@Override
	public void storeAgent(AgentImpl agent) throws AgentException {
		synchronized (localNodeManager.getKnownAgents()) {
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

			localNodeManager.getKnownAgents().put(agent.getIdentifier(), agentXml);

			if (agent instanceof UserAgentImpl) {
				getUserManager().registerUserAgent((UserAgentImpl) agent);
			} else if (agent instanceof BotAgent) {
				getUserManager().registerUserAgent((BotAgent) agent);
			}
		}
	}

	@Deprecated
	@Override
	public void updateAgent(AgentImpl agent) throws AgentException {
		storeAgent(agent);
	}

	@Override
	public Object[] getOtherKnownNodes() {
		return localNodeManager.getAllNodes();
	}

	@Override
	public NodeInformation getNodeInformation(Object nodeId) throws NodeNotFoundException {
		try {
			LocalNode node = localNodeManager.getNode((Long) nodeId);
			return node.getNodeInformation();
		} catch (Exception e) {
			throw new NodeNotFoundException("Node with id " + nodeId + " not found");
		}
	}

	@Override
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			AgentImpl... reader) throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createEnvelope(identifier, authorPubKey, content, reader);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createEnvelope(previousVersion, content);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, AgentImpl... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createEnvelope(previousVersion, content, reader);
	}

	@Override
	public void storeEnvelope(EnvelopeVersion envelope, AgentImpl author) throws EnvelopeException {
		// XXX make configurable
		localNodeManager.getStorage().storeEnvelope(envelope, author, 10000);
	}

	@Override
	public EnvelopeVersion fetchEnvelope(String identifier) throws EnvelopeException {
		// XXX make configurable
		return localNodeManager.getStorage().fetchEnvelope(identifier, 10000);
	}

	@Override
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			Collection<?> readers) throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createEnvelope(identifier, authorPubKey, content, readers);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createEnvelope(previousVersion, content, readers);
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(String identifier, PublicKey authorPubKey, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createUnencryptedEnvelope(identifier, authorPubKey, content);
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNodeManager.getStorage().createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(EnvelopeVersion envelope, AgentImpl author, long timeoutMs)
			throws EnvelopeAlreadyExistsException, EnvelopeException {
		localNodeManager.getStorage().storeEnvelope(envelope, author, timeoutMs);
	}

	@Override
	public void storeEnvelopeAsync(EnvelopeVersion envelope, AgentImpl author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		localNodeManager.getStorage().storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler,
				exceptionHandler);
	}

	@Override
	public EnvelopeVersion fetchEnvelope(String identifier, long timeoutMs)
			throws EnvelopeNotFoundException, EnvelopeException {
		return localNodeManager.getStorage().fetchEnvelope(identifier, timeoutMs);
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		localNodeManager.getStorage().fetchEnvelopeAsync(identifier, envelopeHandler, exceptionHandler);
	}

	@Override
	public void removeEnvelope(String identifier) throws EnvelopeNotFoundException, EnvelopeException {
		localNodeManager.getStorage().removeEnvelope(identifier);
	}

	@Override
	public EnvelopeVersion fetchArtifact(String identifier) throws EnvelopeNotFoundException, EnvelopeException {
		// XXX make configurable
		return localNodeManager.getStorage().fetchEnvelope(identifier, 10000);
	}

}
