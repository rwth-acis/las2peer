package i5.las2peer.security;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.w3c.dom.Element;

import i5.las2peer.api.Service;
import i5.las2peer.api.ServiceException;
import i5.las2peer.api.execution.InternalServiceException;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceInvocationFailedException;
import i5.las2peer.api.execution.ServiceMethodNotFoundException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.RMIExceptionContent;
import i5.las2peer.communication.RMIResultContent;
import i5.las2peer.communication.ServiceDiscoveryContent;
import i5.las2peer.execution.ExecutionContext;
import i5.las2peer.execution.RMITask;
import i5.las2peer.execution.ServiceHelper;
import i5.las2peer.p2p.AliasConflictException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SimpleTools;

/**
 * A service agent represents a service and its access rights in the las2peer setting.
 * 
 */
public class ServiceAgentImpl extends PassphraseAgentImpl implements ServiceAgent {

	/**
	 * the name of the service class, this agent represents in the network
	 */
	private ServiceNameVersion sService;

	/**
	 * instance of the service (if started at a node)
	 */
	private Service serviceInstance = null;

	/**
	 * create a new service agent
	 * 
	 * @param service
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws AgentOperationFailedException
	 * @throws CryptoException
	 */
	protected ServiceAgentImpl(ServiceNameVersion service, KeyPair pair, String passphrase, byte[] salt)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
		this.sService = service;
	}

	/**
	 * create a new service agent
	 * 
	 * @param service
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected ServiceAgentImpl(ServiceNameVersion service, PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(pubKey, encodedPrivate, salt);
		this.sService = service;
	}

	/**
	 * get the name of the service class, this service agent belongs to
	 * 
	 * @return class name of the corresponding service
	 */
	@Override
	public ServiceNameVersion getServiceNameVersion() {
		return this.sService;
	}

	@Override
	public String toXmlString() {
		try {
			return "<las2peer:agent type=\"service\" serviceclass=\"" + getServiceNameVersion().toString() + "\">\n"
					+ "\t<id>" + getIdentifier() + "</id>\n" + "\t<publickey encoding=\"base64\">"
					+ SerializeTools.serializeToBase64(getPublicKey()) + "</publickey>\n" + "\t<privatekey encrypted=\""
					+ CryptoTools.getSymmetricAlgorithm() + "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod()
					+ "\">\n" + "\t\t<salt encoding=\"base64\">" + Base64.getEncoder().encodeToString(getSalt())
					+ "</salt>\n" + "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n"
					+ "\t</privatekey>\n" + "</las2peer:agent>\n";
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	@Override
	public void receiveMessage(Message m, AgentContext c) throws MessageException {
		try {
			m.open(this, getRunningAtNode());
			Object content = m.getContent();

			// unlock context agent
			if (content instanceof UnlockAgentCall) {
				c.unlockMainAgent(((UnlockAgentCall) content).getPassphrase());
				content = ((UnlockAgentCall) content).getContent();
			}

			if (content instanceof RMITask) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_INVOCATION, m.getSendingNodeId(),
						m.getSender(), getRunningAtNode().getNodeId(), this,
						this.getServiceNameVersion() + "/" + ((RMITask) content).getMethodName());

				Message response;

				try {
					response = new Message(m, new RMIResultContent(handle((RMITask) content, c)));
					getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_INVOCATION_FINISHED, m.getSendingNodeId(),
							m.getSender(), getRunningAtNode().getNodeId(), this,
							this.getServiceNameVersion() + "/" + ((RMITask) content).getMethodName());
				} catch (ServiceInvocationException e) {
					response = new Message(m, new RMIExceptionContent(e));
					getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_INVOCATION_FAILED, m.getSendingNodeId(),
							m.getSender(), getRunningAtNode().getNodeId(), this, "Exception: " + e);
				}

				response.setSendingNodeId(getRunningAtNode().getNodeId());
				getRunningAtNode().sendResponse(response, m.getSendingNodeId());
			} else if (content instanceof ListMethodsContent) {
				if (!((ListMethodsContent) content).isRequest()) {
					throw new MessageException("I don't know what to do with a response for a ListMethods request!");
				}
				if (m.getSendingNodeId() == null) {
					throw new MessageException("If no sendind node is given - where should I send the answer to?!");
				}

				ListMethodsContent responseContent = new ListMethodsContent(false);
				for (Method method : getServiceInstance().getClass().getMethods()) {
					responseContent.addMethod(method);
				}
				responseContent.finalize();

				Message response = new Message(m, responseContent);
				getRunningAtNode().sendResponse(response, m.getSendingNodeId());
			} else if (content instanceof ServiceDiscoveryContent) {
				if (!((ServiceDiscoveryContent) content).isRequest()) {
					throw new MessageException("Got a ServiceDiscovery response - can't handle it!");
				}
				if (m.getSendingNodeId() == null) {
					throw new MessageException("If no sendind node is given - where should I send the answer to?!");
				}

				// only answer if requirements are met
				if (((ServiceDiscoveryContent) content).accepts(this.getServiceNameVersion())) {
					ServiceDiscoveryContent result = new ServiceDiscoveryContent(this.getIdentifier(),
							this.getServiceNameVersion());

					Message response = new Message(m, result);
					response.setSendingNodeId(getRunningAtNode().getNodeId());
					getRunningAtNode().sendResponse(response, m.getSendingNodeId());
				}

			} else {
				throw new MessageException("I don't know what to do with a message content of type "
						+ content.getClass().getCanonicalName());
			}
		} catch (InternalSecurityException | AgentAccessDeniedException e) {
			System.out.println("\n\n\nproblematic message:\n" + m.toXmlString() + "\n" + "Exception: " + e + "\n\n\n");

			e.printStackTrace();

			throw new MessageException("security problems - " + m.getRecipient().getIdentifier() + " at node "
					+ getRunningAtNode().getNodeId(), e);

		} catch (EncodingFailedException e) {
			throw new MessageException("message problems", e);
		} catch (SerializationException e) {
			throw new MessageException("message problems", e);
		} catch (AgentNotFoundException e) {
			throw new MessageException("answer receiver not found", e);
		} catch (AgentException e) {
			throw new MessageException("answer receiver not readable", e);
		} catch (NodeNotFoundException e) {
			throw new MessageException("answer destination not found", e);
		}

	}

	@Override
	public void notifyUnregister() {
		getRunningAtNode().getNodeServiceCache().unregisterLocalService(this);

		if (serviceInstance != null) {
			serviceInstance.onStop();
			System.out.println("Service " + this.getServiceNameVersion() + " has been stopped!");
			serviceInstance = null;
		}
		Node runningAt = getRunningAtNode();
		if (runningAt != null) {
			runningAt.observerNotice(MonitoringEvent.SERVICE_SHUTDOWN, runningAt.getNodeId(), this,
					getServiceNameVersion().toString());
		}
		super.notifyUnregister();
	}

	/**
	 * @deprecated Please use {@link ServiceAgentImpl#createServiceAgent(ServiceNameVersion, String)} instead
	 * @param forService
	 * @param passPhrase
	 * @return
	 * @throws CryptoException
	 * @throws AgentOperationFailedException
	 */
	@Deprecated
	public static ServiceAgentImpl generateNewAgent(String forService, String passPhrase)
			throws CryptoException, AgentOperationFailedException {
		return createServiceAgent(new ServiceNameVersion(forService, "1.0"), passPhrase);
	}

	/**
	 * create a completely new ServiceAgent for a given service class
	 * 
	 * the id will be generated randomly
	 * 
	 * @param service class name of the new service
	 * @param passphrase a pass phrase for the private key of the agent
	 * @return a new ServiceAgent
	 * @throws CryptoException
	 * @throws AgentOperationFailedException
	 */
	public static ServiceAgentImpl createServiceAgent(ServiceNameVersion service, String passphrase)
			throws CryptoException, AgentOperationFailedException {
		if (service.getVersion().toString().equals("*")) {
			throw new IllegalArgumentException("You must specify a version!");
		}

		return new ServiceAgentImpl(service, CryptoTools.generateKeyPair(), passphrase, CryptoTools.generateSalt());
	}

	/**
	 * create a ServiceAgent for version 1.0
	 * 
	 * Can be used to generate ServiceAgent for jUnit tests, since in this case las2peer loads classes using the default
	 * class loader and thus does not require version information.
	 * 
	 * @param serviceName
	 * @param passphrase
	 * @return
	 * @throws CryptoException
	 * @throws AgentOperationFailedException
	 */
	@Deprecated
	public static ServiceAgentImpl createServiceAgent(String serviceName, String passphrase)
			throws CryptoException, AgentOperationFailedException {
		return createServiceAgent(new ServiceNameVersion(serviceName, "1.0"), passphrase);
	}

	/**
	 * factory: create a new service agent from the given XML representation
	 * 
	 * @param xml String containing XML information
	 * @return a service agent
	 * @throws MalformedXMLException
	 */
	public static ServiceAgentImpl createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	/**
	 * factory: create a service agent from the given xml representation
	 * 
	 * @param rootElement
	 * @return a service agent
	 * @throws MalformedXMLException
	 */
	public static ServiceAgentImpl createFromXml(Element rootElement) throws MalformedXMLException {
		try {
			// read service class from XML
			if (!rootElement.hasAttribute("serviceclass")) {
				throw new MalformedXMLException("serviceclass attribute expected!");
			}
			ServiceNameVersion service = ServiceNameVersion.fromString(rootElement.getAttribute("serviceclass"));
			// read id from XML
			Element elId = XmlTools.getSingularElement(rootElement, "id");
			String id = elId.getTextContent();
			// read public key from XML
			Element pubKey = XmlTools.getSingularElement(rootElement, "publickey");
			if (!pubKey.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getTextContent());
			if (!id.equalsIgnoreCase(CryptoTools.publicKeyToSHA512(publicKey))) {
				throw new MalformedXMLException("id does not match with public key");
			}
			// read private key from XML
			Element privKey = XmlTools.getSingularElement(rootElement, "privatekey");
			if (!privKey.getAttribute("encrypted").equals(CryptoTools.getSymmetricAlgorithm())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			}
			if (!privKey.getAttribute("keygen").equals(CryptoTools.getSymmetricKeygenMethod())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod() + " expected");
			}
			// read salt from XML
			Element elSalt = XmlTools.getSingularElement(rootElement, "salt");
			if (!elSalt.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}

			byte[] salt = Base64.getDecoder().decode(elSalt.getTextContent());
			// read data from XML
			Element data = XmlTools.getSingularElement(rootElement, "data");
			if (!data.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			byte[] encPrivate = Base64.getDecoder().decode(data.getTextContent());

			return new ServiceAgentImpl(service, publicKey, encPrivate, salt);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		}
	}

	/**
	 * notify this service agent, that it has been registered (for usage) at the given node
	 * 
	 * @param node
	 * @throws AgentException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyRegistrationTo(Node node) throws AgentException {
		try {
			Class<? extends Service> clServ = (Class<? extends Service>) node.getBaseClassLoader()
					.getServiceClass(sService.getName(), sService.getVersion().toString());

			Constructor<? extends Service> cons = clServ.getConstructor(new Class<?>[0]);
			serviceInstance = cons.newInstance();

			// set up monitoring
			if (serviceInstance.isMonitor()) {
				node.setServiceMonitoring(this);
			}

			// notify the service, that it has been launched
			serviceInstance.onStart(this);

			// and the agent
			super.notifyRegistrationTo(node);

			// notify Node
			node.getNodeServiceCache().registerLocalService(this);

			// subscribe to service topic
			node.registerReceiverToTopic(this, serviceNameToTopicId(this.getServiceNameVersion().getName()));

			// register the service alias
			String alias = serviceInstance.getAlias();
			if (alias != null) {
				try {
					node.getServiceAliasManager().registerServiceAlias(this, alias);
				} catch (IllegalArgumentException e) {
					throw new ServiceException("Service alias could not be registered!", e);
				} catch (AliasConflictException e) {
					throw new ServiceException("Service alias is already used by anther service!", e);
				}
			}

			System.out.println("Service " + this.getServiceNameVersion() + " has been started!");

		} catch (ClassLoaderException e1) {
			throw new AgentException("Problems with the classloader", e1);
		} catch (InstantiationException e1) {
			throw new AgentException("Consturctor failure while instantiating service", e1);
		} catch (IllegalAccessException e1) {
			throw new AgentException("Constructor security exception", e1);
		} catch (ClassCastException e) {
			throw new AgentException("given class " + sService + " is not a L2p-Service!");
		} catch (ServiceException e) {
			throw new AgentException("Service instance problems!", e);
		} catch (AgentException e) {
			throw new AgentException("Agent problems after service instantiation", e);
		} catch (NoSuchMethodException e) {
			throw new AgentException("The given Service class has no standard constructor!", e);
		} catch (InvocationTargetException e) {
			throw new AgentException("Exception in service constructor", e.getCause());
		}
	}

	/**
	 * returns the service topic id for the given service name
	 * 
	 * @param service
	 * @return
	 */
	public static long serviceNameToTopicId(String service) {
		return SimpleTools.longHash(service);
	}

	/**
	 * is this (service) agent registered for running a service?
	 * 
	 * @return true, if the agent is running at a Las2peer node
	 */
	public boolean isRunning() {
		return !isLocked() && serviceInstance != null;
	}

	/**
	 * invoke a service method
	 * 
	 * @param method
	 * @param parameters
	 * @return result of the method invocation
	 * @throws InternalSecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws ServiceMethodNotFoundException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	public Object invoke(String method, Object[] parameters)
			throws IllegalArgumentException, ServiceMethodNotFoundException, IllegalAccessException,
			InvocationTargetException, InternalSecurityException {
		if (!isRunning()) {
			throw new IllegalStateException("This agent instance does not handle a started service!");
		}

		return ServiceHelper.execute(serviceInstance, method, parameters);
	}

	/**
	 * execute a RMITask
	 * 
	 * @param task
	 * @param agentContext
	 * @return result of the method invocation
	 * @throws ServiceInvocationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 */
	public Serializable handle(RMITask task, AgentContext agentContext) throws ServiceInvocationException {
		if (!getServiceNameVersion().equals(task.getServiceNameVersion())) {
			throw new ServiceInvocationFailedException("Service is not matching requested class!"
					+ getServiceNameVersion() + "/" + task.getServiceNameVersion());
		}

		// init context and executor
		ExecutionContext context = new ExecutionContext(this, agentContext, agentContext.getLocalNode());
		ExecutorService executor = context.getExecutor();

		// execute
		try {
			return executor.submit(() -> {
				Object res = invoke(task.getMethodName(), task.getParameters());

				if (res == null) {
					return null;
				}

				if (!(res instanceof Serializable)) {
					throw new ServiceInvocationFailedException(
							"Result is not serializable: " + res.getClass() + " / " + res);
				}

				return (Serializable) res;
			}).get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof InvocationTargetException) {
				if (e.getCause().getCause() instanceof ServiceInvocationException) {
					throw (ServiceInvocationException) e.getCause().getCause();
				} else {
					throw new InternalServiceException("Internal exception in service", e.getCause());
				}
			} else if (e.getCause() instanceof ServiceInvocationException) {
				throw (ServiceInvocationException) e.getCause();
			} else {
				throw new ServiceInvocationFailedException("Service invocation failed", e);
			}
		} catch (Exception e) {
			throw new ServiceInvocationFailedException("Service invocation failed", e);
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * get the actual service instance bound to this agent
	 * 
	 * @return the instance of the curresponding service currently running at the las2peer node
	 */
	public Service getServiceInstance() {
		return serviceInstance;
	}

}
