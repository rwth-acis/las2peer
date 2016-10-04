package i5.las2peer.security;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.api.Service;
import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.RMIExceptionContent;
import i5.las2peer.communication.RMIResultContent;
import i5.las2peer.communication.RMIUnlockContent;
import i5.las2peer.communication.ServiceDiscoveryContent;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.NotFinishedException;
import i5.las2peer.execution.RMITask;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.DuplicateServiceAliasException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.SimpleTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

/**
 * A service agent represents a service and its access rights in the las2peer setting.
 * 
 */
public class ServiceAgent extends PassphraseAgent {

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
	 * @param id
	 * @param service
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 */
	protected ServiceAgent(long id, ServiceNameVersion service, KeyPair pair, String passphrase, byte[] salt)
			throws L2pSecurityException, CryptoException {
		super(id, pair, passphrase, salt);
		this.sService = service;
	}

	/**
	 * create a new service agent
	 * 
	 * @param id
	 * @param service
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected ServiceAgent(long id, ServiceNameVersion service, PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(id, pubKey, encodedPrivate, salt);
		this.sService = service;
	}

	/**
	 * get the name of the service class, this service agent belongs to
	 * 
	 * @return class name of the corresponding service
	 */
	public ServiceNameVersion getServiceNameVersion() {
		return this.sService;
	}

	@Override
	public String toXmlString() {
		try {
			return "<las2peer:agent type=\"service\" serviceclass=\"" + getServiceNameVersion().toString() + "\">\n"
					+ "\t<id>" + getId() + "</id>\n" + "\t<publickey encoding=\"base64\">"
					+ SerializeTools.serializeToBase64(getPublicKey()) + "</publickey>\n" + "\t<privatekey encrypted=\""
					+ CryptoTools.getSymmetricAlgorithm() + "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod()
					+ "\">\n" + "\t\t<salt encoding=\"base64\">" + Base64.encodeBase64String(getSalt()) + "</salt>\n"
					+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n" + "\t</privatekey>\n"
					+ "</las2peer:agent>\n";
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
				getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION, m.getSendingNodeId(), m.getSender(),
						getRunningAtNode().getNodeId(), this,
						this.getServiceNameVersion() + "/" + ((RMITask) content).getMethodName());

				L2pThread thread = new L2pThread(this, (RMITask) content, c);
				Message response;
				try {
					thread.start();
					thread.join();

					if (thread.hasException()) {
						// System.out.println ( "Exception: " + thread.getException());
						// thread.getException().printStackTrace();
						if (thread.getException() instanceof InvocationTargetException
								&& thread.getException().getCause() instanceof AgentLockedException) {
							getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION_FAILED, m.getSendingNodeId(),
									m.getSender(), getRunningAtNode().getNodeId(), this,
									"Need to unlock agent key for envelope access");
							response = new Message(m, new RMIUnlockContent(getRunningAtNode().getPublicNodeKey()));
						} else {
							response = new Message(m, new RMIExceptionContent(thread.getException()));
							getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION_FAILED, m.getSendingNodeId(),
									m.getSender(), getRunningAtNode().getNodeId(), this,
									"Exception: " + thread.getException());
						}
					} else {
						response = new Message(m, new RMIResultContent(thread.getResult()));
						getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION_FINISHED, m.getSendingNodeId(),
								m.getSender(), getRunningAtNode().getNodeId(), this,
								this.getServiceNameVersion() + "/" + ((RMITask) content).getMethodName());
					}
					response.setSendingNodeId(getRunningAtNode().getNodeId());
				} catch (InterruptedException e) {
					response = new Message(m, new RMIExceptionContent(e));
					getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION_FAILED, m.getSendingNodeId(),
							m.getSender(), getRunningAtNode().getNodeId(), this, "Exception: " + e);
				} catch (NotFinishedException e) {
					// should not occur, since join has been called!
					response = new Message(m, new RMIExceptionContent(e));
					getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION_FAILED, m.getSendingNodeId(),
							m.getSender(), getRunningAtNode().getNodeId(), this, "Exception: " + e);
				}

				if (m.getSendingNodeId() == null) {
					System.out.println("Node sender is null - where to send the answer to!?!?");
				}

				getRunningAtNode().sendResponse(response, m.getSendingNodeId());
			} else if (content instanceof ListMethodsContent) {
				if (!((ListMethodsContent) content).isRequest()) {
					throw new L2pServiceException("I don't know what to do with a response for a ListMethods request!");
				}
				if (m.getSendingNodeId() == null) {
					throw new L2pServiceException("If no sendind node is given - where should I send the answer to?!");
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
					throw new L2pServiceException("Got a ServiceDiscovery response - can't handle it!");
				}
				if (m.getSendingNodeId() == null) {
					throw new L2pServiceException("If no sendind node is given - where should I send the answer to?!");
				}

				// only answer if requirements are met
				if (((ServiceDiscoveryContent) content).accepts(this.getServiceNameVersion())) {
					ServiceDiscoveryContent result = new ServiceDiscoveryContent(this.getId(),
							this.getServiceNameVersion());

					Message response = new Message(m, result);
					response.setSendingNodeId(getRunningAtNode().getNodeId());
					getRunningAtNode().sendResponse(response, m.getSendingNodeId());
				}

			} else {
				throw new L2pServiceException("I don't know what to do with a message content of type "
						+ content.getClass().getCanonicalName());
			}
		} catch (L2pServiceException e) {
			throw new MessageException("service problems", e);
		} catch (L2pSecurityException e) {
			System.out.println("\n\n\nproblematic message:\n" + m.toXmlString() + "\n" + "Exception: " + e + "\n\n\n");

			e.printStackTrace();

			throw new MessageException(
					"security problems - " + m.getRecipient().getId() + " at node " + getRunningAtNode().getNodeId(),
					e);

		} catch (EncodingFailedException e) {
			throw new MessageException("message problems", e);
		} catch (SerializationException e) {
			throw new MessageException("message problems", e);
		} catch (AgentNotKnownException e) {
			throw new MessageException("answer receiver not found", e);
		} catch (NodeNotFoundException e) {
			throw new MessageException("answer destination not found", e);
		}

	}

	@Override
	public void notifyUnregister() {
		getRunningAtNode().getNodeServiceCache().unregisterLocalService(this);

		if (serviceInstance != null) {
			serviceInstance.close();
			serviceInstance = null;
		}
		Node runningAt = getRunningAtNode();
		if (runningAt != null) {
			runningAt.observerNotice(Event.SERVICE_SHUTDOWN, runningAt.getNodeId(), this,
					getServiceNameVersion().toString());
		}
		super.notifyUnregister();
	}

	/**
	 * @deprecated Please use {@link ServiceAgent#createServiceAgent(ServiceNameVersion, String)} instead
	 * @param forService
	 * @param passPhrase
	 * @return
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	@Deprecated
	public static ServiceAgent generateNewAgent(String forService, String passPhrase)
			throws CryptoException, L2pSecurityException {
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
	 * @throws L2pSecurityException
	 */
	public static ServiceAgent createServiceAgent(ServiceNameVersion service, String passphrase)
			throws CryptoException, L2pSecurityException {

		if (service.getVersion().toString().equals("*")) {
			throw new IllegalArgumentException("You must specify a version!");
		}

		Random r = new Random();

		return new ServiceAgent(r.nextLong(), service, CryptoTools.generateKeyPair(), passphrase,
				CryptoTools.generateSalt());
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
	 * @throws L2pSecurityException
	 */
	@Deprecated
	public static ServiceAgent createServiceAgent(String serviceName, String passphrase)
			throws CryptoException, L2pSecurityException {
		return createServiceAgent(new ServiceNameVersion(serviceName, "1.0"), passphrase);
	}

	/**
	 * factory: create a new service agent from the given XML representation
	 * 
	 * @param xml String containing XML information
	 * @return a service agent
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent createFromXml(String xml) throws MalformedXMLException {
		try {
			Element root = Parser.parse(xml, false);

			if (!"service".equals(root.getAttribute("type"))) {
				throw new MalformedXMLException("service agent expeced");
			}
			if (!"agent".equals(root.getName())) {
				throw new MalformedXMLException("agent expected");
			}

			return createFromXml(root);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}

	/**
	 * factory: create a service agent from the given xml representation
	 * 
	 * @param root
	 * @return a service agent
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent createFromXml(Element root) throws MalformedXMLException {
		try {
			Element elId = root.getFirstChild();
			if (!root.hasAttribute("serviceclass")) {
				throw new MalformedXMLException("serviceclass attribute expected!");
			}
			ServiceNameVersion service = ServiceNameVersion.fromString(root.getAttribute("serviceclass"));

			long id = Long.parseLong(elId.getFirstChild().getText());

			Element pubKey = root.getChild(1);
			if (!pubKey.getName().equals("publickey")) {
				throw new MalformedXMLException("public key expected");
			}
			if (!pubKey.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}

			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getFirstChild().getText());

			Element privKey = root.getChild(2);
			if (!privKey.getName().equals("privatekey")) {
				throw new MalformedXMLException("private key expected");
			}
			if (!privKey.getAttribute("encrypted").equals(CryptoTools.getSymmetricAlgorithm())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			}
			if (!privKey.getAttribute("keygen").equals(CryptoTools.getSymmetricKeygenMethod())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod() + " expected");
			}

			Element elSalt = privKey.getFirstChild();
			if (!elSalt.getName().equals("salt")) {
				throw new MalformedXMLException("salt expected");
			}
			if (!elSalt.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}

			byte[] salt = Base64.decodeBase64(elSalt.getFirstChild().getText());

			Element data = privKey.getChild(1);
			if (!data.getName().equals("data")) {
				throw new MalformedXMLException("data expected");
			}
			if (!data.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			byte[] encPrivate = Base64.decodeBase64(data.getFirstChild().getText());

			return new ServiceAgent(id, service, publicKey, encPrivate, salt);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		}
	}

	/**
	 * notify this service agent, that it has been registered (for usage) at the given node
	 * 
	 * @param node
	 * @throws L2pServiceException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void notifyRegistrationTo(Node node) throws L2pServiceException {
		try {
			Class<? extends Service> clServ = (Class<? extends Service>) node.getBaseClassLoader()
					.getServiceClass(sService.getName(), sService.getVersion().toString());

			Constructor<? extends Service> cons = clServ.getConstructor(new Class<?>[0]);
			serviceInstance = cons.newInstance();

			// notify the service, that it has been launched
			serviceInstance.launchedAt(node, this);

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
				} catch (AgentLockedException e) {
					throw new L2pServiceException("Service alias could not be registered!", e);
				} catch (DuplicateServiceAliasException e) {
					throw new L2pServiceException("Service alias is already used by anther service!", e);
				}
			}

		} catch (ClassLoaderException e1) {
			throw new L2pServiceException("Problems with the classloader", e1);
		} catch (InstantiationException e1) {
			throw new L2pServiceException("Consturctor failure while instantiating service", e1);
		} catch (IllegalAccessException e1) {
			throw new L2pServiceException("Constructor security exception", e1);
		} catch (ClassCastException e) {
			throw new L2pServiceException("given class " + sService + " is not a L2p-Service!");
		} catch (L2pServiceException e) {
			throw new L2pServiceException("Service instance problems!", e);
		} catch (AgentException e) {
			throw new L2pServiceException("Agent problems after service instantiation", e);
		} catch (NoSuchMethodException e) {
			throw new L2pServiceException("The given Service class has no standard constructor!", e);
		} catch (InvocationTargetException e) {
			throw new L2pServiceException("Exception in service constructor", e.getCause());
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
	 * @throws L2pSecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	public Object invoke(String method, Object[] parameters) throws IllegalArgumentException,
			NoSuchServiceMethodException, IllegalAccessException, InvocationTargetException, L2pSecurityException {
		if (!isRunning()) {
			throw new IllegalStateException("This agent instance does not handle a started service!");
		}

		return serviceInstance.execute(method, parameters);
	}

	/**
	 * execute a RMITask
	 * 
	 * @param task
	 * @return result of the method invocation
	 * @throws L2pServiceException
	 * @throws ServiceInvocationException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws L2pSecurityException
	 */
	public Serializable handle(RMITask task)
			throws L2pServiceException, ServiceInvocationException, SecurityException, IllegalArgumentException,
			NoSuchServiceMethodException, IllegalAccessException, InvocationTargetException, L2pSecurityException {
		if (!getServiceNameVersion().equals(task.getServiceNameVersion())) {
			throw new L2pServiceException("Service is not matching requestes class!" + getServiceNameVersion() + "/"
					+ task.getServiceNameVersion());
		}

		Object result = invoke(task.getMethodName(), task.getParameters());

		if (result == null) {
			return null;
		}

		if (!(result instanceof Serializable)) {
			throw new ServiceInvocationException("Result is not serializable! " + result.getClass() + " / " + result);
		}

		return (Serializable) result;
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
