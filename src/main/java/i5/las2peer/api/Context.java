package i5.las2peer.api;

import java.io.Serializable;
import java.util.List;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.RemoteServiceException;
import i5.las2peer.api.exceptions.ServiceNotAvailableException;
import i5.las2peer.api.exceptions.ServiceNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

/**
 * Provides access to the context of the current call.
 *
 */
public interface Context {

	// old Context methods, to be replaced:

	/**
	 * @return the executing service agent.
	 */
	public ServiceAgent getServiceAgent();

	/**
	 * @return the current service.
	 */
	public Service getService();

	/**
	 * @return the calling agent.
	 */
	public Agent getMainAgent();

	/**
	 * Gets all group agents, which have been unlocked in this context.
	 * 
	 * @return all (unlocked) group agents of this context
	 */
	@Deprecated
	public GroupAgent[] getGroupAgents();

	/**
	 * Tries to open the given id for this context.
	 * 
	 * @param groupId
	 * @return the unlocked GroupAgent of the given id
	 * @throws AgentNotKnownException
	 * @throws L2pSecurityException
	 */
	public GroupAgent requestGroupAgent(String groupId) throws AgentNotKnownException, L2pSecurityException;

	/**
	 * returns an unlocked instance of the requested Agent
	 * 
	 * @param agentId the requested agent
	 * @return an unlocked agent instance
	 * @throws AgentNotKnownException agent not found
	 * @throws L2pSecurityException agent cannot be unlocked
	 */
	public Agent requestAgent(String agentId) throws AgentNotKnownException, L2pSecurityException;

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
	public Envelope getStoredObject(long id) throws ArtifactNotFoundException, StorageException;

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
	public Envelope getStoredObject(Class<?> cls, String identifier) throws ArtifactNotFoundException, StorageException;

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
			throws ArtifactNotFoundException, StorageException;

	/**
	 * Gives access to the local node.
	 * 
	 * @return the local P2P node
	 */
	public Node getLocalNode();

	/**
	 * Returns agents that are unlocked in this context first. E.g. necessary for opening a received
	 * {@link i5.las2peer.communication.Message}.
	 * 
	 * @param id
	 * @return get the agent of the given id
	 * @throws AgentNotKnownException
	 */
	public Agent getAgent(String id) throws AgentNotKnownException;

	@Deprecated
	public boolean hasAgent(String id);

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static Context getCurrent() {
		return L2pThread.getCurrent();
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
	public void openEnvelope(Envelope envelope) throws DecodingFailedException, L2pSecurityException;

	/**
	 * returns true if the main agent is unlocked and can unlock the given agent
	 * 
	 * @param agentId an agent id
	 * @return true if the main agent has access to the given agent, otherwise false
	 * @throws AgentNotKnownException agent not found
	 * @throws AgentLockedException main agent is locked
	 */
	public boolean hasAccess(String agentId) throws AgentNotKnownException, AgentLockedException;

	// Envelopes

	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException;

	public Envelope fetchEnvelope(String identifier) throws StorageException;

	public Envelope createEnvelope(String identifier, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException;

	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler);

	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws StorageException;

	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler);

	public Envelope createEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope) throws StorageException;

	public void storeEnvelope(Envelope envelope, long timeoutMs) throws StorageException;

	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException;

	// RMI

	/**
	 * Invokes the method of any other service on behalf of the main agent, thus sending the main agent as calling
	 * agent.
	 * 
	 * @param service The service class. A version may be specified (for example package.serviceClass@1.0.0-1 or
	 *            package.serviceClass@1.0). The core tries to find an appropriate version (version 1.0.5 matches 1.0).
	 *            If no version is specified, the newest version is picked.
	 * @param method The service method.
	 * @param parameters The parameters list.
	 * @return The invocation result.
	 * @throws ServiceNotFoundException If the service is not known to the network.
	 * @throws ServiceNotAvailableException If the service is temporarily not available.
	 * @throws RemoteServiceException If the remote service throws an exception.
	 */
	public Serializable invoke(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, RemoteServiceException;

	/**
	 * Invokes a service method using the agent of this service as calling agent.
	 * 
	 * @param service The service class. A version may be specified (for example package.serviceClass@1.0.0-1 or
	 *            package.serviceClass@1.0). The core tries to find an appropriate version (version 1.0.5 matches 1.0).
	 *            If no version is specified, the newest version is picked.
	 * @param method The service method.
	 * @param parameters The parameters list.
	 * @return The invocation result.
	 * @throws ServiceNotFoundException If the service is not known to the network.
	 * @throws ServiceNotAvailableException If the service is temporarily not available.
	 * @throws RemoteServiceException If the remote service throws an exception.
	 */
	public Serializable invokeInternally(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, RemoteServiceException;

}
