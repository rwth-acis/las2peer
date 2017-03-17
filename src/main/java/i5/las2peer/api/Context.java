package i5.las2peer.api;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeCollisionHandler;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentAlreadyExistsException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.execution.ServiceThread;

/**
 * Provides access to the context of the current call.
 *
 */
public interface Context {

	// get Context

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static Context getCurrent() {
		return ServiceThread.getCurrentContext();
	}

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static Context get() {
		return getCurrent();
	}

	// get Service

	/**
	 * Get the current service.
	 * 
	 * @return the current service
	 */
	public Service getService();

	/**
	 * Get the current service, avoiding casting.
	 * 
	 * @param <T> type of the service
	 * @param serviceType service class
	 * @return the current service
	 */
	public <T extends Service> T getService(Class<T> serviceType);

	// Agents

	/**
	 * Get the main agent of this context. In most cases, this is the user.
	 * 
	 * @return the calling agent.
	 */
	public Agent getMainAgent();

	/**
	 * Get the current service agent responsible for executing the service.
	 * 
	 * @return the executing service agent.
	 */
	public ServiceAgent getServiceAgent();

	/**
	 * Creates a new UserAgent.
	 * 
	 * @param passphrase the passphrase to protect the newly generated agent
	 * @return A new unlocked UserAgent which is not stored to the network yet.
	 * @throws AgentOperationFailedException if an error occurred on the node.
	 */
	public UserAgent createUserAgent(String passphrase) throws AgentOperationFailedException;

	/**
	 * Creates a new GroupAgent.
	 * 
	 * @param members Initial member list
	 * @return A new unlocked GroupAgent which is not stored to the network yet.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	public GroupAgent createGroupAgent(Agent[] members) throws AgentOperationFailedException;

	/**
	 * Fetches an agent from the network and trys to unlock it using the specified agent.
	 * 
	 * @param agentId The id of the agent to fetch.
	 * @param using The agent used to unlock the fetched agent.
	 * @return An unlocked instance of the requested agent.
	 * @throws AgentAccessDeniedException If the given agent cannot access hte fetched agent.
	 * @throws AgentNotFoundException If the specified agent cannot be found.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	public Agent requestAgent(String agentId, Agent using) throws AgentAccessDeniedException, AgentNotFoundException,
			AgentOperationFailedException;

	/**
	 * Requests an agent from the network using the calling (main) agent.
	 * 
	 * @param agentId The id of the agent to fetch.
	 * @return An unlocked instance of the requested agent.
	 * @throws AgentAccessDeniedException If the main agent cannot access hte fetched agent.
	 * @throws AgentNotFoundException If the specified agent cannot be found.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	public Agent requestAgent(String agentId) throws AgentAccessDeniedException, AgentNotFoundException,
			AgentOperationFailedException;

	/**
	 * Fetches an agent from the network.
	 * 
	 * @param agentId The id of the agent to fetch.
	 * @return A probably locked instance of the specified agent.
	 * @throws AgentNotFoundException If the specified agent cannot be found.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	public Agent fetchAgent(String agentId) throws AgentNotFoundException, AgentOperationFailedException;

	/**
	 * Stores and/or updates an agent to the network.
	 * 
	 * The given agent must be unlocked.
	 * 
	 * @param agent The unlocked agent to store.
	 * @throws AgentAccessDeniedException If the agent cannot be overridden due to access restrictions.
	 * @throws AgentAlreadyExistsException If another agent already exists.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 * @throws AgentLockedException If the agent is locked.
	 */
	public void storeAgent(Agent agent) throws AgentAccessDeniedException, AgentAlreadyExistsException,
			AgentOperationFailedException, AgentLockedException;

	/**
	 * Checks if the agent specified by using is able to unlock the agent agentId. This also includes recursive
	 * unlocking.
	 * 
	 * @param agentId The agent to be checked.
	 * @param using The agent to unlock.
	 * @return true If using is able to unlock agentId.
	 * @throws AgentNotFoundException If the agent specified by agentId does not exist.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	boolean hasAccess(String agentId, Agent using) throws AgentNotFoundException, AgentOperationFailedException;

	/**
	 * Checks if the main agent is able to unlock the agent agentId. This also includes recursive unlocking.
	 * 
	 * @param agentId The agent to be checked.
	 * @return true If the main agent is able to unlock the given agent.
	 * @throws AgentNotFoundException If the agent specified by agentId does not exist.
	 * @throws AgentOperationFailedException If an error occurred on the node.
	 */
	boolean hasAccess(String agentId) throws AgentNotFoundException, AgentOperationFailedException;

	// Envelopes

	/**
	 * Requests an envelope from the network. This means fetching and decrypting it using the specified agent.
	 * 
	 * @param identifier Identifier of the envelope.
	 * @param using Agentu sing to open the envelope.
	 * @return An opened envelope.
	 * @throws EnvelopeAccessDeniedException If the given agent is not able to access the envelope.
	 * @throws EnvelopeNotFoundException If the envelope doesn not exist.
	 * @throws EnvelopeOperationFailedException If an error occurred in the node or network.
	 */
	public Envelope requestEnvelope(String identifier, Agent using) throws EnvelopeAccessDeniedException,
			EnvelopeNotFoundException, EnvelopeOperationFailedException;

	/**
	 * Requests an envelope from the network. This means fetching and decrypting it using the current main agent.
	 * 
	 * @param identifier Identifier of the envelope.
	 * @return An opened envelope.
	 * @throws EnvelopeAccessDeniedException If the given agent is not able to access the envelope.
	 * @throws EnvelopeNotFoundException If the envelope doesn not exist.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public Envelope requestEnvelope(String identifier) throws EnvelopeAccessDeniedException, EnvelopeNotFoundException,
			EnvelopeOperationFailedException;

	/**
	 * Stores the envelope to the network and signs it with the specified agent.
	 * 
	 * @param env The envelope to store.
	 * @param using The agent to be used to sign the envelope.
	 * @throws EnvelopeAccessDeniedException If the specified agent is not allowed to write to the envelope.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void storeEnvelope(Envelope env, Agent using) throws EnvelopeAccessDeniedException,
			EnvelopeOperationFailedException;

	/**
	 * Stores the envelope to the network and signs it with the current main agent.
	 * 
	 * @param env The envelope to store.
	 * @throws EnvelopeAccessDeniedException If the specified agent is not allowed to write to the envelope.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void storeEnvelope(Envelope env) throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException;

	/**
	 * Stores the envelope to the network and signs it with the specified agent.
	 * 
	 * @param env The envelope to store.
	 * @param handler An handler to resolve storage conflict (e.g. the envelope has been updated in the meantime).
	 * @param using The agent to be used to sign the envelope (and must have signed the envelope or must have access to
	 *            the signing agent if there are previous versions).
	 * @throws EnvelopeAccessDeniedException If the specified agent is not allowed to write to the envelope.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void storeEnvelope(Envelope env, EnvelopeCollisionHandler handler, Agent using)
			throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException;

	/**
	 * Stores the envelope to the network and signs it with the current main agent.
	 * 
	 * @param env The envelope to store.
	 * @param handler An handler to resolve storage conflict (e.g. the envelope has been updated in the meantime).
	 * @throws EnvelopeAccessDeniedException If the specified agent is not allowed to write to the envelope.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void storeEnvelope(Envelope env, EnvelopeCollisionHandler handler) throws EnvelopeAccessDeniedException,
			EnvelopeOperationFailedException;

	/**
	 * Reclaims the envelope using the specified agent.
	 * 
	 * A reclaim operation marks the envelope as deleted and indicates that the envelope is no longer needed anymore
	 * (e.g. can be deleted by other nodes). However, it is not guaranteed that the envelope will be deleted since the
	 * nature of a p2p network.
	 * 
	 * @param identifier The identifier of the envelope.
	 * @param using The agent that has signed the envelope or an agent that has access to the signing agent.
	 * @throws EnvelopeAccessDeniedException If the agent has not signed the envelope.
	 * @throws EnvelopeNotFoundException If the envelope does not exist.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void reclaimEnvelope(String identifier, Agent using) throws EnvelopeAccessDeniedException,
			EnvelopeNotFoundException, EnvelopeOperationFailedException;

	/**
	 * Reclaims the envelope using the current main agent agent.
	 * 
	 * A reclaim operation marks the envelope as deleted and indicates that the envelope is no longer needed anymore
	 * (e.g. can be deleted by other nodes). However, it is not guaranteed that the envelope will be deleted since the
	 * nature of a p2p network.
	 * 
	 * @param identifier The identifier of the envelope.
	 * @throws EnvelopeAccessDeniedException If the agent has not signed the envelope.
	 * @throws EnvelopeNotFoundException If the envelope does not exist.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public void reclaimEnvelope(String identifier) throws EnvelopeAccessDeniedException, EnvelopeNotFoundException,
			EnvelopeOperationFailedException;

	/**
	 * Creates a new envelope with the given agent as signing agent and first reader.
	 * 
	 * @param identifier Identifier of the envelope.
	 * @param using Signing agent (owner) of the envelope.
	 * @return An envelope that is not stored to the network yet.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public Envelope createEnvelope(String identifier, Agent using) throws EnvelopeOperationFailedException;

	/**
	 * Creates a new envelope with the current main agent as signing agent and first reader.
	 * 
	 * @param identifier Identifier of the envelope.
	 * @return An envelope that is not stored to the network yet.
	 * @throws EnvelopeOperationFailedException If an error occurred at the node or in the network.
	 */
	public Envelope createEnvelope(String identifier) throws EnvelopeOperationFailedException;

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
	 * @throws InternalServiceException If the remote service throws an exception.
	 * @throws ServiceMethodNotFoundException If the service method does not exist.
	 * @throws ServiceInvocationFailedException If the service invocation failed.
	 * @throws ServiceAccessDeniedException If the access to the service has been denied.
	 */
	public Serializable invoke(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException;

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
	 * @throws InternalServiceException If the remote service throws an exception.
	 * @throws ServiceMethodNotFoundException If the service method does not exist.
	 * @throws ServiceInvocationFailedException If the service invocation failed.
	 * @throws ServiceAccessDeniedException If the access to the service has been denied.
	 */
	public Serializable invoke(ServiceNameVersion service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException;

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
	 * @throws InternalServiceException If the remote service throws an exception.
	 * @throws ServiceMethodNotFoundException If the service method does not exist.
	 * @throws ServiceInvocationFailedException If the service invocation failed.
	 * @throws ServiceAccessDeniedException If the access to the service has been denied.
	 */
	public Serializable invokeInternally(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException;

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
	 * @throws InternalServiceException If the remote service throws an exception.
	 * @throws ServiceMethodNotFoundException If the service method does not exist.
	 * @throws ServiceInvocationFailedException If the service invocation failed.
	 * @throws ServiceAccessDeniedException If the access to the service has been denied.
	 */
	public Serializable invokeInternally(ServiceNameVersion service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException;

	// Execution

	/**
	 * Gets the executor for this service.
	 * 
	 * Async tasks should be handeld using this executor. They can access the current context.
	 * 
	 * @return The executor responsible for the current service call.
	 */
	public ExecutorService getExecutor();

	// Logging

	/**
	 * Gets the logger for the given class.
	 * 
	 * @param cls A class.
	 * 
	 * @return The logging instance for the current service.
	 */
	public Logger getLogger(Class<?> cls);

	// Monitoring

	/**
	 * Writes a log message to the l2p system using node observers. Also makes data available to MobSOS.
	 * 
	 * Does not include the current acting main agent.
	 *
	 * @param event Differentiates between different log messages. Use MonitoringEvent.SERVICE_CUSTOM_MESSAGE_XXX as
	 *            parameter.
	 * @param message A message.
	 */
	public void monitorEvent(MonitoringEvent event, String message);

	/**
	 * Writes a log message to the l2p system using node observers. Also makes data available to MobSOS.
	 * 
	 * Does not include the current acting main agent.
	 * 
	 * @param from Specifies from which class the message is sent from. Usually "this" is passed as parameter.
	 * @param event Differentiates between different log messages. Use MonitoringEvent.SERVICE_CUSTOM_MESSAGE_XXX as
	 *            parameter.
	 * @param message A message.
	 */
	public void monitorEvent(Object from, MonitoringEvent event, String message);

	/**
	 * Writes a log message to the l2p system using node observers. Also makes data available to MobSOS.
	 *
	 * @param from Specifies from which class the message is sent from. Usually "this" is passed as parameter.
	 * @param event Differentiates between different log messages. Use MonitoringEvent.SERVICE_CUSTOM_MESSAGE_XXX as
	 *            parameter.
	 * @param message A message.
	 * @param includeActingUser If set to true, the current main agent will be included.
	 */
	public void monitorEvent(Object from, MonitoringEvent event, String message, boolean includeActingUser);

	// Class loading

	/**
	 * Gets the class loader responsible for loading the current service.
	 * 
	 * @return The current service class loader.
	 */
	public ClassLoader getServiceClassLoader();

}
