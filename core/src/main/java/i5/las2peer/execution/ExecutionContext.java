package i5.las2peer.execution;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.beans.finder.ClassFinder;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.execution.ServiceNotAuthorizedException;
import i5.las2peer.api.execution.ServiceNotAvailableException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeCollisionHandler;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.persistency.MergeFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentAlreadyExistsException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.EnvelopeImpl;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class ExecutionContext implements Context {

	final private AgentContext callerContext;
	final private ServiceAgentImpl serviceAgent;
	final private ServiceThreadFactory threadFactory;
	final private ExecutorService executor;
	final private Node node;

	public ExecutionContext(ServiceAgentImpl agent, AgentContext context, Node node) {
		this.serviceAgent = agent;
		this.callerContext = context;
		this.node = node;
		this.threadFactory = new ServiceThreadFactory(this);
		this.executor = Executors.newSingleThreadExecutor(this.threadFactory);
	}

	public static ExecutionContext getCurrent() {
		return ServiceThread.getCurrentContext();
	}

	public AgentContext getCallerContext() {
		return callerContext;
	}

	/*
	 * Context implementation
	 */

	@Override
	public ClassLoader getServiceClassLoader() {
		return serviceAgent.getServiceInstance().getClass().getClassLoader();
	}

	@Override
	public ExecutorService getExecutor() {
		return this.executor;
	}

	@Override
	public Service getService() {
		return this.serviceAgent.getServiceInstance();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Service> T getService(Class<T> serviceType) {
		return (T) getService();
	}

	@Override
	public ServiceAgent getServiceAgent() {
		return serviceAgent;
	}

	@Override
	public Agent getMainAgent() {
		return callerContext.getMainAgent();
	}

	@Override
	public Serializable invoke(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException,
			ServiceNotAuthorizedException {
		return invoke(ServiceNameVersion.fromString(service), method, parameters);
	}

	@Override
	public Serializable invoke(ServiceNameVersion service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException,
			ServiceNotAuthorizedException {
		return invokeWithAgent(callerContext.getMainAgent(), service, method, parameters);
	}

	@Override
	public Serializable invokeInternally(String service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException,
			ServiceNotAuthorizedException {
		return invokeInternally(ServiceNameVersion.fromString(service), method, parameters);
	}

	@Override
	public Serializable invokeInternally(ServiceNameVersion service, String method, Serializable... parameters)
			throws ServiceNotFoundException, ServiceNotAvailableException, InternalServiceException,
			ServiceMethodNotFoundException, ServiceInvocationFailedException, ServiceAccessDeniedException,
			ServiceNotAuthorizedException {
		return invokeWithAgent(serviceAgent, service, method, parameters);
	}

	private Serializable invokeWithAgent(AgentImpl agent, ServiceNameVersion service, String method,
			Serializable[] parameters) throws ServiceNotFoundException, ServiceNotAvailableException,
			InternalServiceException, ServiceMethodNotFoundException, ServiceInvocationFailedException,
			ServiceAccessDeniedException, ServiceNotAuthorizedException {
		try {
			Serializable rmiResult = callerContext.getLocalNode().invoke(agent, service, method, parameters);
			if (rmiResult == null) {
				return null;
			}
			ClassLoader localServiceLoader = serviceAgent.getServiceInstance().getClass().getClassLoader();
			if (rmiResult.getClass().getClassLoader() != localServiceLoader) {
				// mimic global invocation serialization/deserialization to avoid class cast/not-found exceptions
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					new ObjectOutputStream(baos).writeObject(rmiResult);
					baos.close();
					ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())) {
						@Override
						protected Class<?> resolveClass(ObjectStreamClass classDesc)
								throws IOException, ClassNotFoundException {
							return ClassFinder.resolveClass(classDesc.getName(), localServiceLoader);
						}
					};
					rmiResult = (Serializable) ois.readObject();
				} catch (IOException | ClassNotFoundException e) {
					throw new ServiceInvocationFailedException("Re-serialization failed", e);
				}
			}
			return rmiResult;
		} catch (ServiceNotFoundException | ServiceNotAvailableException | InternalServiceException
				| ServiceMethodNotFoundException | ServiceInvocationFailedException | ServiceAccessDeniedException
				| ServiceNotAuthorizedException e) {
			throw e;
		} catch (ServiceInvocationException e) {
			throw new ServiceInvocationFailedException("Service invocation failed.", e);
		} catch (AgentLockedException e) {
			throw new IllegalStateException("Agent should be unlocked, but it isn't.");
		}
	}

	@Override
	public void monitorEvent(String message) {
		monitorEvent(null, MonitoringEvent.SERVICE_MESSAGE, message);
	}
	
	@Override
	public void monitorEvent(MonitoringEvent event, String message) {
		monitorEvent(null, event, message);
	}

	@Override
	public void monitorEvent(Object from, MonitoringEvent event, String message) {
		monitorEvent(from, event, message, false);

	}

	@Override
	public void monitorEvent(Object from, MonitoringEvent event, String message, boolean includeActingUser) {
		Agent actingUser = null;
		if (includeActingUser) {
			actingUser = getMainAgent();
		}
		String msg = message;
		if (from != null) {
			msg = from.getClass().getName() + ": " + message;
		}
		node.observerNotice(event, node.getNodeId(), serviceAgent, null, actingUser, msg);

	}

	@Override
	public UserAgent createUserAgent(String passphrase) throws AgentOperationFailedException {
		try {
			UserAgent agent = UserAgentImpl.createUserAgent(passphrase);
			agent.unlock(passphrase);
			return agent;
		} catch (CryptoException | AgentAccessDeniedException e) {
			throw new AgentOperationFailedException(e);
		}
	}

	@Override
	public GroupAgent createGroupAgent(Agent[] members, String groupName) throws AgentOperationFailedException {
		try {
			GroupAgent agent = GroupAgentImpl.createGroupAgent(members,  groupName);
			for (Agent a : members) {
				try {
					agent.unlock(a);
					break;
				} catch (AgentAccessDeniedException | AgentLockedException | AgentOperationFailedException e) {
				}
			}
			if (agent.isLocked()) {
				throw new AgentOperationFailedException("Cannot unlock group agent.");
			}
			return agent;
		} catch (CryptoException | SerializationException e) {
			throw new AgentOperationFailedException(e);
		}
	}

	@Override
	public Agent fetchAgent(String agentId) throws AgentNotFoundException, AgentOperationFailedException {
		try {
			return node.getAgent(agentId);
		} catch (AgentNotFoundException e) {
			throw e;
		} catch (AgentException e) {
			throw new AgentOperationFailedException("Error!", e);
		}

	}

	@Override
	public Agent requestAgent(String agentId, Agent using)
			throws AgentAccessDeniedException, AgentNotFoundException, AgentOperationFailedException {
		return node.getAgentContext((AgentImpl) using).requestAgent(agentId);
	}

	@Override
	public Agent requestAgent(String agentId)
			throws AgentAccessDeniedException, AgentNotFoundException, AgentOperationFailedException {
		return callerContext.requestAgent(agentId);
	}

	@Override
	public void storeAgent(Agent agent) throws AgentAccessDeniedException, AgentAlreadyExistsException,
			AgentOperationFailedException, AgentLockedException {
		if (agent.isLocked()) {
			throw new AgentLockedException();
		} else if (agent instanceof AnonymousAgent) {
			throw new AgentAccessDeniedException("Anonymous agent must not be stored");
		} else if (agent instanceof GroupAgentImpl) {
			((GroupAgentImpl) agent).apply();
		}

		try {
			node.storeAgent((AgentImpl) agent);
		} catch (AgentAlreadyExistsException | AgentLockedException | AgentAccessDeniedException
				| AgentOperationFailedException e) {
			throw e;
		} catch (AgentException e) {
			throw new AgentOperationFailedException(e);
		}
	}

	@Override
	public boolean hasAccess(String agentId, Agent using) throws AgentNotFoundException {
		return node.getAgentContext((AgentImpl) using).hasAccess(agentId);
	}

	@Override
	public boolean hasAccess(String agentId) throws AgentNotFoundException {
		return callerContext.hasAccess(agentId);
	}

	@Override
	public String getUserAgentIdentifierByLoginName(String loginName)
			throws AgentNotFoundException, AgentOperationFailedException {
		return node.getAgentIdForLogin(loginName);
	}

	@Override
	public String getUserAgentIdentifierByEmail(String emailAddress)
			throws AgentNotFoundException, AgentOperationFailedException {
		return node.getAgentIdForEmail(emailAddress);
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException, AgentException {
		node.registerReceiver(receiver);
	}

	@Override
	public Logger getLogger(Class<?> cls) {
		return L2pLogger.getInstance(cls);
	}

	@Override
	public Envelope requestEnvelope(String identifier, Agent using)
			throws EnvelopeAccessDeniedException, EnvelopeNotFoundException, EnvelopeOperationFailedException {
		EnvelopeVersion version;
		try {
			version = node.fetchEnvelope(serviceAgent.getServiceNameVersion().getName() + "$" + identifier);
		} catch (EnvelopeNotFoundException e1) {
			throw e1;
		} catch (EnvelopeException e1) {
			throw new EnvelopeOperationFailedException("Problems with the storage!", e1);
		}

		Envelope envelope;
		try {
			envelope = new EnvelopeImpl(version, node.getAgentContext((AgentImpl) using));
		} catch (CryptoException e) {
			throw new EnvelopeAccessDeniedException("This agent does not have access to the envelope!", e);
		} catch (SerializationException e) {
			throw new EnvelopeOperationFailedException("Envelope cannot be deserialized!", e);
		}
		return envelope;
	}

	@Override
	public Envelope requestEnvelope(String identifier)
			throws EnvelopeAccessDeniedException, EnvelopeNotFoundException, EnvelopeOperationFailedException {
		return requestEnvelope(identifier, callerContext.getMainAgent());
	}

	@Override
	public void storeEnvelope(Envelope env, Agent using)
			throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException {
		storeEnvelope(env, (Envelope env1, Envelope env2) -> {
			throw new MergeFailedException("No collision handler implemented.");
		}, using);
	}

	@Override
	public void storeEnvelope(Envelope env) throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException {
		storeEnvelope(env, callerContext.getMainAgent());
	}

	@Override
	public void storeEnvelope(Envelope env, EnvelopeCollisionHandler handler, Agent using)
			throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException {
		if (using instanceof AnonymousAgent) {
			throw new EnvelopeOperationFailedException("Anonymous agent must not be used to persist data");
		}
		// TODO collision handler

		// create reader set
		EnvelopeImpl envelope = (EnvelopeImpl) env;
		HashSet<Object> keys = new HashSet<>();
		if (!envelope.getRevokeAllReaders() && envelope.getVersion() != null) {
			keys.addAll(envelope.getVersion().getReaderKeys().keySet());
			for (AgentImpl a : envelope.getReaderToRevoke()) {
				keys.remove(a.getPublicKey());
			}
			for (AgentImpl a : envelope.getReaderToAdd()) {
				if (!keys.contains(a.getPublicKey())) {
					keys.add(a);
				}
			}
		} else if (!envelope.getRevokeAllReaders()) {
			for (AgentImpl a : envelope.getReaderToAdd()) {
				keys.add(a);
			}
		}

		// create new envelope version
		try {
			EnvelopeVersion version;
			AgentImpl signing = (AgentImpl) requestAgent(envelope.getOwnerId(), using);
			if (envelope.getVersion() != null) {
				version = node.createEnvelope(envelope.getVersion(), envelope.getContent(), keys);
			} else {
				version = node.createEnvelope(
						serviceAgent.getServiceNameVersion().getName() + "$" + envelope.getIdentifier(),
						signing.getPublicKey(), envelope.getContent(), keys);
			}
			node.storeEnvelope(version, signing);
			envelope.setVersion(version);
		} catch (IllegalArgumentException | SerializationException e) {
			throw new EnvelopeOperationFailedException(e);
		} catch (AgentAccessDeniedException | AgentNotFoundException | EnvelopeAccessDeniedException
				| CryptoException e) {
			throw new EnvelopeAccessDeniedException(e);
		} catch (EnvelopeException | AgentException e) {
			throw new EnvelopeOperationFailedException(e);
		}
	}

	@Override
	public void storeEnvelope(Envelope env, EnvelopeCollisionHandler handler)
			throws EnvelopeAccessDeniedException, EnvelopeOperationFailedException {
		storeEnvelope(env, handler, callerContext.getMainAgent());
	}

	@Override
	public void reclaimEnvelope(String identifier, Agent using)
			throws EnvelopeAccessDeniedException, EnvelopeNotFoundException, EnvelopeOperationFailedException {
		try {
			node.removeEnvelope(identifier);
		} catch (EnvelopeAccessDeniedException e) {
			throw e;
		} catch (EnvelopeException e) {
			throw new EnvelopeOperationFailedException("The operation failed.", e);
		}
	}

	@Override
	public void reclaimEnvelope(String identifier)
			throws EnvelopeAccessDeniedException, EnvelopeNotFoundException, EnvelopeOperationFailedException {
		reclaimEnvelope(identifier, callerContext.getMainAgent());
	}

	@Override
	public Envelope createEnvelope(String identifier, Agent using)
			throws EnvelopeOperationFailedException, EnvelopeAccessDeniedException {
		EnvelopeImpl envelope = new EnvelopeImpl(identifier, (AgentImpl) using);
		envelope.addReader(using);
		return envelope;
	}

	@Override
	public Envelope createEnvelope(String identifier)
			throws EnvelopeOperationFailedException, EnvelopeAccessDeniedException {
		return createEnvelope(identifier, callerContext.getMainAgent());
	}

}
