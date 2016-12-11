package i5.las2peer.security;

import java.io.Serializable;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.ContextStorageInterface;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

/**
 * Each {@link i5.las2peer.execution.L2pThread} is bound to a context, which is determined by the executing agent.
 */
public class AgentContext implements AgentStorage, ContextStorageInterface {

	private Agent agent;

	private Hashtable<String, GroupAgent> groupAgents = new Hashtable<>();

	private Node localNode;

	private long lastUsageTime = -1;

	/**
	 * Creates a new (local) context.
	 * 
	 * @param mainAgent
	 * @param localNode
	 * @throws L2pSecurityException
	 */
	public AgentContext(Node localNode, Agent mainAgent) throws L2pSecurityException {
		this.agent = mainAgent;

		this.localNode = localNode;

		touch();
	}

	/**
	 * Gets the main agent of this context.
	 * 
	 * @return the agent of this context
	 */
	public Agent getMainAgent() {
		return agent;
	}

	/**
	 * Gets all group agents, which have been unlocked in this context.
	 * 
	 * @return all (unlocked) group agents of this context
	 */
	public GroupAgent[] getGroupAgents() {
		return groupAgents.entrySet().toArray(new GroupAgent[0]);
	}

	/**
	 * Tries to open the given id for this context.
	 * 
	 * @param groupId
	 * @return the unlocked GroupAgent of the given id
	 * @throws AgentNotKnownException If the agent is not found
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 * @throws L2pSecurityException
	 */
	public GroupAgent requestGroupAgent(String groupId)
			throws AgentNotKnownException, AgentException, L2pSecurityException {
		if (groupAgents.containsKey(groupId)) {
			return groupAgents.get(groupId);
		}

		Agent agent = localNode.getAgent(groupId);

		if (!(agent instanceof GroupAgent)) {
			throw new AgentNotKnownException("Agent " + groupId + " is not a group agent!");
		}

		GroupAgent group = (GroupAgent) agent;

		if (group.isMember(this.getMainAgent())) {
			try {
				group.unlockPrivateKey(this.getMainAgent());
			} catch (SerializationException | CryptoException e) {
				throw new L2pSecurityException("Unable to open group!", e);
			}
		} else {
			for (String memberId : group.getMemberList()) {
				try {
					GroupAgent member = requestGroupAgent(memberId);
					group.unlockPrivateKey(member);
					break;
				} catch (AgentNotKnownException e) {
					L2pLogger.logEvent(Event.SERVICE_ERROR, e.getMessage());
				} catch (Exception e) {
					// do nothing
				}
			}
		}

		if (group.isLocked()) {
			throw new L2pSecurityException("Unable to open group!");
		}

		groupAgents.put(groupId, group);

		return group;
	}

	/**
	 * returns an unlocked instance of the requested Agent
	 * 
	 * @param agentId the requested agent
	 * @return an unlocked agent instance
	 * @throws AgentNotKnownException If the agent is not found
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 * @throws L2pSecurityException agent cannot be unlocked
	 */
	public Agent requestAgent(String agentId) throws AgentNotKnownException, AgentException, L2pSecurityException {
		if (agentId.equalsIgnoreCase(getMainAgent().getSafeId())) {
			return getMainAgent();
		} else {
			return requestGroupAgent(agentId);
		}
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead.
	 * 
	 *             Gets a stored envelope from the p2p network.
	 * 
	 * @param id
	 * @return envelope containing the requested data
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	@Deprecated
	public Envelope getStoredObject(long id) throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(Long.toString(id));
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 * 
	 *             Gets a stored envelope from the p2p network. The envelope will be identified by the stored class and
	 *             an arbitrary identifier selected by the using service(s).
	 * 
	 * @param cls
	 * @param identifier
	 * @return envelope containing the requested data
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	@Deprecated
	public Envelope getStoredObject(Class<?> cls, String identifier)
			throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(cls.getCanonicalName() + "-" + identifier);
	}

	/**
	 * @deprecated Use {@link #fetchEnvelope(String)} instead
	 * 
	 *             Gets a stored envelope from the p2p network. The envelope will be identified by the stored class and
	 *             an arbitrary identifier selected by the using service(s).
	 * 
	 * @param className
	 * @param identifier
	 * @return envelope containing the requested data
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	@Deprecated
	public Envelope getStoredObject(String className, String identifier)
			throws ArtifactNotFoundException, StorageException {
		return fetchEnvelope(className + "-" + identifier);
	}

	/**
	 * Gives access to the local node.
	 * 
	 * @return the local P2P node
	 */
	public Node getLocalNode() {
		return localNode;
	}

	/**
	 * Mark the current time as the last usage.
	 */
	public void touch() {
		lastUsageTime = new Date().getTime();
	}

	/**
	 * Returns the time of the last usage of this context.
	 * 
	 * @return the timestamp of the last usage
	 */
	public long getLastUsageTimestamp() {
		return lastUsageTime;
	}

	/**
	 * Uses this context as {@link AgentStorage}. Returns agents that are unlocked in this context first. E.g. necessary
	 * for opening a received {@link i5.las2peer.communication.Message}.
	 * 
	 * @param id
	 * @return get the agent of the given id
	 * @throws AgentNotKnownException If the agent is not found
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 */
	@Override
	public Agent getAgent(String id) throws AgentNotKnownException, AgentException {
		if (id.equalsIgnoreCase(agent.getSafeId())) {
			return agent;
		}

		Agent result;
		if ((result = groupAgents.get(id)) != null) {
			return result;
		}

		return localNode.getAgent(id);
	}

	@Override
	public boolean hasAgent(String id) {
		return id.equalsIgnoreCase(agent.getSafeId()) || groupAgents.containsKey(id);
	}

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static AgentContext getCurrent() {
		return L2pThread.getCurrent().getCallerContext();
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Object, Event, String)} with {@link Event#SERVICE_MESSAGE} instead!
	 *             <p>
	 *             Logs a message to the l2p system using the observers.
	 * 
	 *             Since this method will/should only be used in an L2pThread, the message will come from a service or a
	 *             helper, so a SERVICE_MESSAGE is assumed. Then this message will not be monitored by the monitoring
	 *             observer.
	 * 
	 * @param from the calling class
	 * @param message
	 */
	@Deprecated
	public static void logMessage(Object from, String message) {
		getCurrent().getLocalNode().observerNotice(Event.SERVICE_MESSAGE, getCurrent().getLocalNode().getNodeId(),
				from.getClass().getSimpleName() + ": " + message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Object, Event, String, Agent, Agent)} instead!
	 *             <p>
	 *             Writes a log message. The given index (1-99) can be used to differentiate between different log
	 *             messages. The serviceAgent and actingUser can be set to null if not known. Then this message will not
	 *             be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param index an index between 1 and 99
	 * @param message
	 * @param serviceAgent
	 * @param actingUser
	 */
	@Deprecated
	public static void logMessage(Object from, int index, String message, Agent serviceAgent, Agent actingUser) {
		Event event = Event.SERVICE_MESSAGE; // Default
		if (index >= 1 && index <= 99) {
			event = Event.values()[Event.SERVICE_CUSTOM_MESSAGE_1.ordinal() + (index - 1)];
		}
		getCurrent().getLocalNode().observerNotice(event, getCurrent().getLocalNode().getNodeId(), serviceAgent, null,
				actingUser, from.getClass().getSimpleName() + ": " + message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Object, Event, String)} with {@link Event#SERVICE_ERROR} instead!
	 *             <p>
	 *             Logs an error message to the l2p system using the observers.
	 * 
	 *             Since this method will/should only be used in an L2pThread, the message will come from a service or a
	 *             helper, so a SERVICE_MESSAGE is assumed. Then this message will not be monitored by the monitoring
	 *             observer.
	 * 
	 * @param from the calling class
	 * @param message
	 */
	@Deprecated
	public static void logError(Object from, String message) {
		getCurrent().getLocalNode().observerNotice(Event.SERVICE_ERROR, getCurrent().getLocalNode().getNodeId(),
				from.getClass().getSimpleName() + ": " + message);
	}

	/**
	 * @deprecated Use {@link L2pLogger#logEvent(Object, Event, String, Agent, Agent)} instead!
	 *             <p>
	 *             Writes an error message. The given index (1-99) can be used to differentiate between different log
	 *             messages. The serviceAgent and userAgent can be set to null if not known. Then this message will not
	 *             be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param index an index between 1 and 99
	 * @param message
	 * @param serviceAgent
	 * @param actingUser
	 */
	@Deprecated
	public static void logError(Object from, int index, String message, Agent serviceAgent, Agent actingUser) {
		Event event = Event.SERVICE_ERROR; // Default
		if (index >= 1 && index <= 99) {
			event = Event.values()[Event.SERVICE_CUSTOM_ERROR_1.ordinal() + (index - 1)];
		}
		getCurrent().getLocalNode().observerNotice(event, getCurrent().getLocalNode().getNodeId(), serviceAgent, null,
				actingUser, from.getClass().getSimpleName() + ": " + message);
	}

	/**
	 * @deprecated Use {@link i5.las2peer.persistency.Envelope#getContent()}
	 * 
	 *             This method is stub and will be removed soon.
	 * 
	 * @param envelope the Envelope to unlock
	 * @throws DecodingFailedException
	 * @throws L2pSecurityException the MainAgent is not able to open the Envelope
	 */
	@Deprecated
	public void openEnvelope(Envelope envelope) throws DecodingFailedException, L2pSecurityException {
		// method stub
	}

	/**
	 * Tries to unlock the private key of the main agent.
	 * 
	 * @param passphrase
	 * @throws L2pSecurityException
	 */
	public void unlockMainAgent(String passphrase) throws L2pSecurityException {
		if (agent instanceof PassphraseAgent) {
			((PassphraseAgent) agent).unlockPrivateKey(passphrase);
		} else {
			throw new L2pSecurityException("this is not passphrase protected agent!");
		}
	}

	/**
	 * returns true if the main agent is unlocked and can unlock the given agent
	 * 
	 * @param agentId an agent id
	 * @return true if the main agent has access to the given agent, otherwise false
	 * @throws AgentNotKnownException If the agent is not found
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 * @throws AgentLockedException main agent is locked
	 */
	public boolean hasAccess(String agentId) throws AgentNotKnownException, AgentException, AgentLockedException {
		if (getMainAgent().isLocked()) {
			throw new AgentLockedException();
		}

		if (agentId.equalsIgnoreCase(getMainAgent().getSafeId())) {
			return true;
		}

		Agent a = getAgent(agentId);

		if (a instanceof GroupAgent) {
			return ((GroupAgent) a).isMemberRecursive(getMainAgent());
		}

		return false;
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException {
		localNode.storeEnvelope(envelope, author);
	}

	@Override
	public Envelope fetchEnvelope(String identifier) throws StorageException {
		return localNode.fetchEnvelope(identifier);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(identifier, content, reader);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(previousVersion, content, reader);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createUnencryptedEnvelope(identifier, content);
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException {
		localNode.storeEnvelope(envelope, author, timeoutMs);
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		localNode.storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler, exceptionHandler);
	}

	@Override
	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws StorageException {
		return localNode.fetchEnvelope(identifier, timeoutMs);
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		localNode.fetchEnvelopeAsync(identifier, envelopeHandler, exceptionHandler);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(identifier, content, getMainAgent());
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return localNode.createEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope) throws StorageException {
		localNode.storeEnvelope(envelope, getMainAgent());
	}

	@Override
	public void storeEnvelope(Envelope envelope, long timeoutMs) throws StorageException {
		localNode.storeEnvelope(envelope, getMainAgent(), timeoutMs);
	}

	@Override
	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException {
		localNode.removeEnvelope(identifier);
	}

}