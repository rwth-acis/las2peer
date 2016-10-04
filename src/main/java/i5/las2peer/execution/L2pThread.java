package i5.las2peer.execution;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.RemoteServiceException;
import i5.las2peer.api.exceptions.ServiceNotAvailableException;
import i5.las2peer.api.exceptions.ServiceNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.io.Serializable;
import java.util.List;

/**
 * a L2pThread is responsible for running a single {@link RMITask} inside a {@link i5.las2peer.p2p.Node}
 * 
 * 
 *
 */
public class L2pThread extends Thread implements Context {

	final private AgentContext callerContext;
	final private RMITask task;
	final private ServiceAgent serviceAgent;

	private Serializable result = null;
	private Exception exception = null;

	private boolean bFinished = false;

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static L2pThread getCurrent() {
		Thread t = Thread.currentThread();

		if (!(t instanceof L2pThread)) {
			throw new IllegalStateException("Not executed in a L2pThread environment!");
		}

		return (L2pThread) t;
	}

	/**
	 * Returns the class loader used by the service executed in this thread.
	 * 
	 * @return Returns the class loader instance.
	 */
	public static ClassLoader getServiceClassLoader() {
		return getCurrent().serviceAgent.getServiceInstance().getClass().getClassLoader();
	}

	/**
	 * create a new L2pThread
	 * 
	 * @param agent
	 * @param task
	 * @param context
	 */
	public L2pThread(ServiceAgent agent, RMITask task, AgentContext context) {
		this.serviceAgent = agent;
		this.task = task;
		this.callerContext = context;
	}

	/**
	 * get the task to be executed
	 * 
	 * @return the corresponding invocation task
	 */
	public RMITask getTask() {
		return task;
	}

	/**
	 * get the context in which this task has to be executed
	 * 
	 * @return the security context this task is running in
	 */
	public AgentContext getCallerContext() {
		return callerContext;
	}

	/**
	 * the actual work
	 */
	@Override
	public void run() {
		try {
			result = serviceAgent.handle(task);
			bFinished = true;
		} catch (Exception e) {
			exception = e;
		}
	}

	/**
	 * is the execution of this task finished (either successfully or with an exception)
	 * 
	 * @return true, if this task (thread) is finished
	 */
	public boolean isFinished() {
		return bFinished || hasException();
	}

	/**
	 * is the execution of this task finished successfully?
	 * 
	 * @return true, if this task is finished successfully (i.e. without any exception)
	 */
	public boolean isSuccess() {
		return bFinished && !hasException();
	}

	/**
	 * did the execution result in an exception?
	 * 
	 * @return true, if any exception occurred while execution
	 */
	public boolean hasException() {
		return exception != null;
	}

	/**
	 * get the result of the execution
	 * 
	 * @return the result of the method invocation
	 * @throws NotFinishedException
	 */
	public Serializable getResult() throws NotFinishedException {
		if (!isFinished()) {
			throw new NotFinishedException("Job not Finished yet");
		}
		return result;
	}

	/**
	 * get a possibly thrown exception
	 * 
	 * @return a (possibly) occurred exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * access to the agent registered at the L2pNode
	 * 
	 * @return the ServiceAgent responsible for the service requested via the invocation task
	 */
	@Override
	public ServiceAgent getServiceAgent() {
		return serviceAgent;
	}

	// Old Context methods. Will be refactored soon.

	@Override
	public Agent getMainAgent() {
		return callerContext.getMainAgent();
	}

	@Override
	public GroupAgent[] getGroupAgents() {
		return callerContext.getGroupAgents();
	}

	@Override
	public GroupAgent requestGroupAgent(long groupId) throws AgentNotKnownException, L2pSecurityException {
		return callerContext.requestGroupAgent(groupId);
	}

	@Override
	public Agent requestAgent(long agentId) throws AgentNotKnownException, L2pSecurityException {
		return callerContext.requestAgent(agentId);
	}

	@Override
	public Envelope getStoredObject(long id) throws ArtifactNotFoundException, StorageException {
		return null;
	}

	@Override
	public Envelope getStoredObject(Class<?> cls, String identifier) throws ArtifactNotFoundException, StorageException {
		return null;
	}

	@Override
	public Envelope getStoredObject(String className, String identifier) throws ArtifactNotFoundException,
			StorageException {
		return null;
	}

	@Override
	public Node getLocalNode() {
		return this.getLocalNode();
	}

	@Override
	public Agent getAgent(long id) throws AgentNotKnownException {
		return callerContext.getAgent(id);
	}

	@Override
	public boolean hasAgent(long id) {
		return callerContext.hasAgent(id);
	}

	@Override
	public void openEnvelope(Envelope envelope) throws DecodingFailedException, L2pSecurityException {
		return;
	}

	@Override
	public boolean hasAccess(long agentId) throws AgentNotKnownException, AgentLockedException {
		return callerContext.hasAccess(agentId);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException {
		callerContext.storeEnvelope(envelope, author);
	}

	@Override
	public Envelope fetchEnvelope(String identifier) throws StorageException {
		return callerContext.fetchEnvelope(identifier);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return callerContext.createEnvelope(identifier, content, reader);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return callerContext.createEnvelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... reader)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return callerContext.createEnvelope(previousVersion, content, reader);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return callerContext.createEnvelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content) throws IllegalArgumentException,
			SerializationException, CryptoException {
		return callerContext.createUnencryptedEnvelope(identifier, content);
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return callerContext.createUnencryptedEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException {
		callerContext.storeEnvelope(envelope, author, timeoutMs);
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		callerContext.storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler, exceptionHandler);
	}

	@Override
	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws StorageException {
		return callerContext.fetchEnvelope(identifier, timeoutMs);
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		callerContext.fetchEnvelopeAsync(identifier, envelopeHandler, exceptionHandler);
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content) throws IllegalArgumentException,
			SerializationException, CryptoException {
		return callerContext.createEnvelope(identifier, content);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content) throws IllegalArgumentException,
			SerializationException, CryptoException {
		return callerContext.createEnvelope(previousVersion, content);
	}

	@Override
	public void storeEnvelope(Envelope envelope) throws StorageException {
		callerContext.storeEnvelope(envelope);
	}

	@Override
	public void storeEnvelope(Envelope envelope, long timeoutMs) throws StorageException {
		callerContext.storeEnvelope(envelope, timeoutMs);
	}

	@Override
	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException {
		callerContext.removeEnvelope(identifier);
	}

	@Override
	public Service getService() {
		return this.serviceAgent.getServiceInstance();
	}

	@Override
	public Serializable invoke(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, RemoteServiceException {
		return invokeWithAgent(callerContext.getMainAgent(), service, method, parameters);
	}

	@Override
	public Serializable invokeInterally(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, RemoteServiceException {
		return invokeWithAgent(serviceAgent, service, method, parameters);
	}

	private Serializable invokeWithAgent(Agent agent, String service, String method, Serializable[] parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, RemoteServiceException {
		try {
			return callerContext.getLocalNode().invoke(agent, service, method, parameters);
		} catch (AgentNotKnownException e) {
			throw new ServiceNotFoundException("The service is not known.", e);
		} catch (ServiceInvocationException e) {
			throw new RemoteServiceException("The service has thrown an exception.", e);
		} catch (L2pServiceException | InterruptedException e) {
			throw new RemoteServiceException("The service seems not to be available.", e);
		} catch (L2pSecurityException e) {
			throw new IllegalStateException("Agent should be unlocked, but it isn't.");
		}
	}

}
