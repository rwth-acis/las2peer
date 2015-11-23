package i5.las2peer.security;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.api.Service;
import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.classLoaders.L2pClassLoader;
import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.RMIExceptionContent;
import i5.las2peer.communication.RMIResultContent;
import i5.las2peer.communication.RMIUnlockContent;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.NotFinishedException;
import i5.las2peer.execution.RMITask;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeNotFoundException;
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
 * A service agent represents a service and its access rights in the LAS2peer setting.
 * 
 */
public class ServiceAgent extends PassphraseAgent {

	/**
	 * the name of the service class, this agent represents in the network
	 */
	private String sServiceClass;

	private boolean timerRunning = false;
	private Timer timer;
	private int timerIntervalSeconds = 10;
	private int timerRunTimes = 0;
	private int TIMER_RUN_TIMES_MAX = 3;
	/**
	 * instance of the service (if started at a node)
	 */
	private Service serviceInstance = null;

	/**
	 * create a new service agent
	 * 
	 * @param id
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 */
	protected ServiceAgent(long id, String serviceClass, KeyPair pair, String passphrase, byte[] salt)
			throws L2pSecurityException, CryptoException {
		super(id, pair, passphrase, salt);
		this.sServiceClass = serviceClass;
	}

	/**
	 * create a new service agent
	 * 
	 * @param id
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected ServiceAgent(long id, String serviceClass, PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(id, pubKey, encodedPrivate, salt);
		this.sServiceClass = serviceClass;
	}

	/**
	 * get the name of the service class, this service agent belongs to
	 * 
	 * @return class name of the corresponding service
	 */
	public String getServiceClassName() {
		return this.sServiceClass;
	}

	@Override
	public String toXmlString() {
		try {
			return "<las2peer:agent type=\"service\" serviceclass=\"" + getServiceClassName() + "\">\n" + "\t<id>"
					+ getId() + "</id>\n" + "\t<publickey encoding=\"base64\">"
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
	public void receiveMessage(Message m, Context c) throws MessageException {
		try {
			m.open(this, getRunningAtNode());
			Object content = m.getContent();

			if (content instanceof RMITask) {
				getRunningAtNode().observerNotice(Event.SERVICE_INVOCATION, m.getSendingNodeId(), m.getSender(),
						getRunningAtNode().getNodeId(), this,
						this.getServiceClassName() + "/" + ((RMITask) content).getMethodName());

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
								this.getServiceClassName() + "/" + ((RMITask) content).getMethodName());
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

				if (m.getSendingNodeId() == null)
					System.out.println("Node sender is null - where to send the answer to!?!?");

				getRunningAtNode().sendResponse(response, m.getSendingNodeId());
			} else if (content instanceof ListMethodsContent) {
				if (!((ListMethodsContent) content).isRequest())
					throw new L2pServiceException("I don't know what to do with a response for a ListMethods request!");
				if (m.getSendingNodeId() == null)
					throw new L2pServiceException("If no sendind node is given - where should I send the answer to?!");

				System.out.println("received request: " + m.toXmlString());
				System.out.println("received: " + content);
				System.out.println("sender: " + m.getSendingNodeId());

				ListMethodsContent responseContent = new ListMethodsContent(false);
				for (Method method : getServiceInstance().getClass().getMethods())
					responseContent.addMethod(method);
				responseContent.finalize();

				Message response = new Message(m, (Serializable) responseContent);
				getRunningAtNode().sendResponse(response, m.getSendingNodeId());
			} else
				throw new L2pServiceException("I don't know what to do with a message content of type "
						+ content.getClass().getCanonicalName());
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
		serviceInfoAgentNotifyUnregister();
		serviceInstance.close();
		serviceInstance = null;
		super.notifyUnregister();
	}

	/**
	 * @deprecated Please use {@link ServiceAgent#createServiceAgent(String, String)} instead
	 * 
	 * @param forService
	 * @param passPhrase
	 * @return
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	@Deprecated
	public static ServiceAgent generateNewAgent(String forService, String passPhrase)
			throws CryptoException, L2pSecurityException {
		return createServiceAgent(forService, passPhrase);
	}

	/**
	 * create a completely new ServiceAgent for a given service class
	 * 
	 * @param serviceClassName class name of the new service
	 * @param passphrase a pass phrase for the private key of the agent
	 * @return a new ServiceAgent
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	public static ServiceAgent createServiceAgent(String serviceClassName, String passphrase)
			throws CryptoException, L2pSecurityException {
		return new ServiceAgent(serviceClass2Id(serviceClassName), serviceClassName, CryptoTools.generateKeyPair(),
				passphrase, CryptoTools.generateSalt());
	}

	/**
	 * factory: create a new service agent from the given XML representation
	 * 
	 * @param xml String containing XML information
	 * 
	 * @return a service agent
	 * 
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent createFromXml(String xml) throws MalformedXMLException {
		try {
			Element root = Parser.parse(xml, false);

			if (!"service".equals(root.getAttribute("type")))
				throw new MalformedXMLException("service agent expeced");
			if (!"agent".equals(root.getName()))
				throw new MalformedXMLException("agent expected");

			return createFromXml(root);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}

	/**
	 * factory: create a service agent from the given xml representation
	 * 
	 * @param root
	 * 
	 * @return a service agent
	 * 
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent createFromXml(Element root) throws MalformedXMLException {
		try {
			Element elId = root.getFirstChild();
			if (!root.hasAttribute("serviceclass"))
				throw new MalformedXMLException("serviceclass attribute expected!");
			String serviceClass = root.getAttribute("serviceclass");

			long id = Long.parseLong(elId.getFirstChild().getText());

			Element pubKey = root.getChild(1);
			if (!pubKey.getName().equals("publickey"))
				throw new MalformedXMLException("public key expected");
			if (!pubKey.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");

			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getFirstChild().getText());

			Element privKey = root.getChild(2);
			if (!privKey.getName().equals("privatekey"))
				throw new MalformedXMLException("private key expected");
			if (!privKey.getAttribute("encrypted").equals(CryptoTools.getSymmetricAlgorithm()))
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			if (!privKey.getAttribute("keygen").equals(CryptoTools.getSymmetricKeygenMethod()))
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod() + " expected");

			Element elSalt = privKey.getFirstChild();
			if (!elSalt.getName().equals("salt"))
				throw new MalformedXMLException("salt expected");
			if (!elSalt.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");

			byte[] salt = Base64.decodeBase64(elSalt.getFirstChild().getText());

			Element data = privKey.getChild(1);
			if (!data.getName().equals("data"))
				throw new MalformedXMLException("data expected");
			if (!data.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");
			byte[] encPrivate = Base64.decodeBase64(data.getFirstChild().getText());

			return new ServiceAgent(id, serviceClass, publicKey, encPrivate, salt);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		}
	}

	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		timerRunning = false;
		timerRunTimes = 0;
	}

	/**
	 * Notifies the {@link i5.las2peer.security.ServiceInfoAgent} about itself
	 */
	private void serviceInfoAgentNotifyUnregister() {
		try {
			stopTimer();
			ServiceInfoAgent agent = getServiceInfoAgent();
			agent.serviceRemoved(this, getRunningAtNode());
		} catch (Exception e) {
			// ignore for now
		}
	}

	/**
	 * Registers and returns the {@link i5.las2peer.security.ServiceInfoAgent}
	 * 
	 * @return
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws AgentException
	 */
	private ServiceInfoAgent getServiceInfoAgent()
			throws CryptoException, L2pSecurityException, SerializationException, AgentException {
		ServiceInfoAgent agent = ServiceInfoAgent.getServiceInfoAgent();
		return agent;
	}

	/**
	 * Notifies the {@link i5.las2peer.security.ServiceInfoAgent} about itself *
	 * 
	 * @throws L2pServiceException
	 */
	public void serviceInfoAgentNotifyRegister() throws L2pServiceException {
		try {
			startTimer();
		} catch (Exception e) {
			throw new L2pServiceException("Error creating ServiceInfoAgent", e);
		}

	}

	private void startTimer() throws Exception {
		if (timerRunning)
			return;

		timer = new Timer();
		ServiceInfoAgent agent = getServiceInfoAgent();

		final ServiceInfoAgent finalAgent = agent;
		final Node finalNode = this.getRunningAtNode();
		timerRunning = true;
		timerRunTimes = 0;
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				executeTimer(finalNode, finalAgent);
			}
		}, 0, // run first occurrence immediately
				timerIntervalSeconds * 1000); // run every x seconds
	}

	private void executeTimer(Node finalNode, ServiceInfoAgent finalAgent) {
		try {
			finalAgent.serviceAdded(this, finalNode);
			timerRunTimes++;
			if (timerRunning && timerRunTimes > TIMER_RUN_TIMES_MAX) {
				stopTimer();
			}
		} catch (Exception e) {
			// do nothing for now
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
		Class<? extends Service> clServ;
		try {
			ClassLoader cl = node.getBaseClassLoader();
			if (cl instanceof L2pClassLoader) {
				// System.out.println( "loading via L2pLoader!");
				clServ = (Class<? extends Service>) ((L2pClassLoader) cl).getServiceClass(sServiceClass);
			} else if (cl != null) {
				clServ = (Class<? extends Service>) cl.loadClass(sServiceClass);
			} else
				clServ = (Class<? extends Service>) Class.forName(sServiceClass);

			Constructor<? extends Service> cons = clServ.getConstructor(new Class<?>[0]);
			serviceInstance = cons.newInstance();

			// notify the service, that it has been launched
			serviceInstance.launchedAt(node);

			// and the agent
			super.notifyRegistrationTo(node);

			// notify Service Info Agent
			serviceInfoAgentNotifyRegister();

		} catch (ClassLoaderException e1) {
			throw new L2pServiceException("Problems with the classloader", e1);
		} catch (ClassNotFoundException e1) {
			throw new L2pServiceException("Service class not found!", e1);
		} catch (InstantiationException e1) {
			throw new L2pServiceException("Consturctor failure while instantiating service", e1);
		} catch (IllegalAccessException e1) {
			throw new L2pServiceException("Constructor security exception", e1);
		} catch (ClassCastException e) {
			throw new L2pServiceException("given class " + sServiceClass + " is not a L2p-Service!");
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
	 * just use a long hash value of the service class name as id for the agent
	 * 
	 * @param serviceClassName
	 * 
	 * @return (hashed) ID for the given service class
	 */
	public static long serviceClass2Id(String serviceClassName) {
		return SimpleTools.longHash(serviceClassName);
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
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pSecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws NoSuchServiceMethodException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 */
	public Object invoke(String method, Object[] parameters) throws IllegalArgumentException,
			NoSuchServiceMethodException, IllegalAccessException, InvocationTargetException, L2pSecurityException {
		if (!isRunning())
			throw new IllegalStateException("This agent instance does not handle a started service!");

		return serviceInstance.execute(method, parameters);
	}

	/**
	 * execute a RMITask
	 * 
	 * @param task
	 * 
	 * @return result of the method invocation
	 * 
	 * @throws L2pServiceException
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
		if (!getServiceClassName().equals(task.getServiceName()))
			throw new L2pServiceException(
					"Service is not matching requestes class!" + getServiceClassName() + "/" + task.getServiceName());

		Object result = invoke(task.getMethodName(), task.getParameters());

		if (result == null)
			return null;

		if (!(result instanceof Serializable))
			throw new ServiceInvocationException("Result is not serializable! " + result.getClass() + " / " + result);

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
