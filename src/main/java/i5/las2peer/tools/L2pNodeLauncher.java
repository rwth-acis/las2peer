package i5.las2peer.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import i5.las2peer.api.Connector;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.classLoaders.L2pClassLoader;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.communication.Message;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.PastryNodeImpl.STORAGE_MODE;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.security.UserAgentList;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;
import rice.p2p.commonapi.NodeHandle;

/**
 * This class implements the LAS2peer node launcher functionalities.
 * 
 * All methods to be executed can be stated via additional command line parameters to the {@link #main} method.
 */
public class L2pNodeLauncher {

	private static final String DEFAULT_SERVICE_DIRECTORY = "./service/";

	private static final String DEFAULT_LOG_DIRECTORY = "./log/";

	private CommandPrompt commandPrompt;

	private static List<Connector> connectors = new ArrayList<Connector>();

	private boolean bFinished = false;

	public boolean isFinished() {
		return bFinished;
	}

	private PastryNodeImpl node;

	public PastryNodeImpl getNode() {
		return node;
	}

	private UserAgent currentUser;

	/**
	 * Get the envelope with the given id. If the id is empty, the main user-list is returned.
	 * 
	 * @param id
	 * 
	 * @return the XML-representation of an envelope as a String
	 * @throws StorageException
	 * @throws ArtifactNotFoundException
	 * @throws NumberFormatException
	 */
	public String getEnvelope(String id) throws NumberFormatException, ArtifactNotFoundException, StorageException {
		if (id == null || id.equals(""))
			id = "" + Envelope.getClassEnvelopeId(UserAgentList.class, "mainlist");

		return node.fetchArtifact(Long.valueOf(id)).toXmlString();
	}

	/**
	 * Searches for the given agent in the LAS2peer network.
	 * 
	 * @param id
	 * @return node handles
	 * @throws AgentNotKnownException
	 */
	public Object[] findAgent(String id) throws AgentNotKnownException {
		long agentId = Long.parseLong(id);
		return node.findRegisteredAgent(agentId);
	}

	/**
	 * Looks for the given service in the LAS2peer network.
	 * 
	 * @param serviceClass
	 * @return node handles
	 * @throws AgentNotKnownException
	 */
	public Object[] findService(String serviceClass) throws AgentNotKnownException {
		Agent agent = node.getServiceAgent(serviceClass);
		return node.findRegisteredAgent(agent);
	}

	/**
	 * Closes the current node.
	 */
	public void shutdown() {
		node.shutDown();
		this.bFinished = true;
	}

	/**
	 * load passphrases from a simple text file where each line consists of the filename of the agent's xml file and a
	 * passphrase separated by a ;
	 * 
	 * @param filename
	 * @return hashtable containing agent file &gt;&gt; passphrase
	 */
	private Hashtable<String, String> loadPassphrases(String filename) {
		Hashtable<String, String> result = new Hashtable<String, String>();

		File file = new File(filename);
		if (file.isFile()) {
			String[] content;
			try {
				content = FileContentReader.read(file).split("\n");
				for (String line : content) {
					line = line.trim();
					String[] split = line.split(";", 2);
					if (split.length != 2) {
						printWarning("Ignoring invalid passphrase line (" + line + ") in '" + filename + "'");
						continue;
					}
					result.put(split[0], split[1]);
				}
			} catch (IOException e) {
				printWarning("Error reading contents of " + filename + ": " + e);
				e.printStackTrace();
				bFinished = true;
			}
		}

		return result;
	}

	/**
	 * Uploads the contents of the given directory to the global storage of the LAS2peer network.
	 * 
	 * Each contained .xml-file is used as an artifact or - in case the name of the file starts with <i>agent-</i> - as
	 * an agent to upload.
	 *
	 * If agents are to be uploaded, make sure, that the startup directory contains a <i>passphrases.txt</i> file giving
	 * the passphrases for the agents.
	 * 
	 * @param directory
	 */
	public void uploadStartupDirectory(String directory) {
		File dir = new File(directory);
		if (!dir.isDirectory())
			throw new IllegalArgumentException(directory + " is not a directory!");
		Hashtable<String, String> htPassphrases = loadPassphrases(directory + "/passphrases.txt");
		Map<Long, String> agentIdToXml = new HashMap<Long, String>();
		List<GroupAgent> groupAgents = new LinkedList<GroupAgent>();
		for (File xmlFile : dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		})) {
			try {
				Element xmlRoot = Parser.parse(xmlFile, false);
				if (xmlRoot.getName().equalsIgnoreCase(Agent.class.getSimpleName())) {
					Agent agent = Agent.createFromXml(xmlRoot);
					agentIdToXml.put(agent.getId(), xmlFile.getName());
					if (agent instanceof PassphraseAgent) {
						String passphrase = htPassphrases.get(xmlFile.getName());
						if (passphrase != null) {
							((PassphraseAgent) agent).unlockPrivateKey(passphrase);
						} else {
							printWarning("\t- got no passphrase for agent from " + xmlFile.getName());
						}
						node.storeAgent(agent);
						printMessage("\t- stored agent from " + xmlFile);
					} else if (agent instanceof GroupAgent) {
						GroupAgent ga = (GroupAgent) agent;
						groupAgents.add(ga);
					} else {
						throw new IllegalArgumentException("Unknown agent type: " + agent.getClass());
					}
				} else if (xmlRoot.getName().equalsIgnoreCase(Envelope.class.getSimpleName())) {
					Envelope e = Envelope.createFromXml(xmlRoot);
					node.storeArtifact(e);
					printMessage("\t- stored artifact from " + xmlFile);
				} else {
					printWarning(
							"Ingoring unknown XML object (" + xmlRoot.getName() + ") in '" + xmlFile.toString() + "'!");
				}
			} catch (XMLSyntaxException e1) {
				printWarning("Unable to parse XML contents of '" + xmlFile.toString() + "'!");
			} catch (MalformedXMLException e) {
				printWarning("unable to deserialize contents of " + xmlFile.toString() + "!");
			} catch (IOException e) {
				printWarning("problems reading the contents of " + xmlFile.toString() + ": " + e);
			} catch (L2pSecurityException e) {
				printWarning("error storing agent from " + xmlFile.toString() + ": " + e);
			} catch (AgentAlreadyRegisteredException e) {
				printWarning("agent from " + xmlFile.toString() + " already known at this node!");
			} catch (AgentException e) {
				printWarning("unable to generate agent " + xmlFile.toString() + "!");
			} catch (StorageException e) {
				printWarning("unable to store contents of " + xmlFile.toString() + "!");
			}
		}
		node.forceUserListUpdate();
		// wait till all user agents are added from startup directory to unlock group agents
		for (GroupAgent currentGroupAgent : groupAgents) {
			for (Long memberId : currentGroupAgent.getMemberList()) {
				Agent memberAgent = null;
				try {
					memberAgent = node.getAgent(memberId);
				} catch (AgentNotKnownException e) {
					printWarning("Can't get agent for group member " + memberId);
					continue;
				}
				if ((memberAgent instanceof PassphraseAgent) == false) {
					printWarning("Unknown agent type to unlock, type: " + memberAgent.getClass().getName());
					continue;
				}
				PassphraseAgent memberPassAgent = (PassphraseAgent) memberAgent;
				String xmlName = agentIdToXml.get(memberPassAgent.getId());
				if (xmlName == null) {
					printWarning("No known xml file for agent " + memberPassAgent.getId());
					continue;
				}
				String passphrase = htPassphrases.get(xmlName);
				if (passphrase == null) {
					printWarning("No known password for agent " + memberPassAgent.getId());
					continue;
				}
				try {
					memberPassAgent.unlockPrivateKey(passphrase);
					currentGroupAgent.unlockPrivateKey(memberPassAgent);
					node.storeAgent(currentGroupAgent);
					printMessage("\t- stored group agent from " + xmlName);
					break;
				} catch (Exception e) {
					printWarning("Can't unlock group agent " + currentGroupAgent.getId() + " with member "
							+ memberPassAgent.getId());
					continue;
				}
			}
			if (currentGroupAgent.isLocked()) {
				throw new IllegalArgumentException("group agent still locked!");
			}
		}
	}

	/**
	 * Upload the contents of the <i>startup</i> sub directory to the global storage of the LAS2peer network.
	 * 
	 * Each contained .xml-file is used as an artifact or - in case the name of the file starts with <i>agent-</i> - as
	 * an agent to upload.
	 * 
	 * If agents are to be uploaded, make sure, that the startup directory contains a <i>passphrases.txt</i> file giving
	 * the passphrases for the agents.
	 */
	public void uploadStartupDirectory() {
		uploadStartupDirectory("etc/startup");
	}

	/**
	 * Starts the HTTP connector.
	 */
	public void startHttpConnector() {
		startConnector("i5.las2peer.httpConnector.HttpConnector");
	}

	/**
	 * Start the Web-Connector.
	 */
	public void startWebConnector() {
		startConnector("i5.las2peer.webConnector.WebConnector");
	}

	/**
	 * Starts a connector given by its classname.
	 * 
	 * @param connectorClass
	 */
	public void startConnector(String connectorClass) {
		try {

			printMessage("Starting connector with class name: " + connectorClass + "!");
			Connector connector = loadConnector(connectorClass);
			connector.start(node);
			connectors.add(connector);

		} catch (ConnectorException e) {
			printWarning(" --> Problems starting the connector: " + e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops the http Connector.
	 */
	public void stopHttpConnector() {
		stopConnector("i5.las2peer.httpConnector.HttpConnector");
	}

	/**
	 * Stops the Web-Connector.
	 */
	public void stopWebConnector() {
		stopConnector("i5.las2peer.webConnector.WebConnector");
	}

	/**
	 * Stops a connector given by its classname.
	 */
	public void stopConnector(String connectorClass) {

		Iterator<Connector> iterator = connectors.iterator();

		while (iterator.hasNext()) {
			try {
				Connector connector = iterator.next();
				if (connector.getClass().getName().equals(connectorClass)) {
					connector.stop();
					iterator.remove();
					return;
				}
			} catch (ConnectorException e) {
				e.printStackTrace();
			}
		}
		printWarning("No connector with the given classname was started!");

	}

	/**
	 * Returns a connector for the given classname.
	 * 
	 * @param classname
	 * @return the loaded connector
	 * 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private Connector loadConnector(String classname) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		Class<?> connectorClass = L2pNodeLauncher.class.getClassLoader().loadClass(classname);
		Connector connector = (Connector) connectorClass.newInstance();
		return connector;
	}

	/**
	 * Try to register the user of the given id at the node and for later usage in this launcher, i.e. for service
	 * method calls via {@link #invoke}.
	 * 
	 * @param id id or login of the agent to load
	 * @param passphrase passphrase to unlock the private key of the agent
	 * 
	 * @return the registered agent
	 */
	public boolean registerUserAgent(String id, String passphrase) {
		try {
			if (id.matches("-?[0-9].*"))
				currentUser = (UserAgent) node.getAgent(Long.valueOf(id));
			else
				currentUser = (UserAgent) node.getAgent(node.getAgentIdForLogin(id));

			currentUser.unlockPrivateKey(passphrase);

			node.registerReceiver(currentUser);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			currentUser = null;
			return false;
		}
	}

	/**
	 * Register the given agent at the LAS2peer node and for later usage with {@link #invoke}.
	 * 
	 * Make sure, that the private key of the agent is unlocked before registering
	 * 
	 * @param agent
	 * 
	 * @throws L2pSecurityException
	 * @throws AgentAlreadyRegisteredException
	 * @throws AgentException
	 */
	public void registerUserAgent(UserAgent agent) throws L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException {
		registerUserAgent(agent, null);
	}

	/**
	 * Register the given agent at the LAS2peer node and for later usage with {@link #invoke}.
	 * 
	 * If the private key of the agent is not unlocked and a pass phrase has been given, an attempt to unlock the key is
	 * started before registering.
	 * 
	 * @param agent
	 * @param passphrase
	 * 
	 * @throws L2pSecurityException
	 * @throws AgentAlreadyRegisteredException
	 * @throws AgentException
	 */
	public void registerUserAgent(UserAgent agent, String passphrase) throws L2pSecurityException, AgentException {
		if (passphrase != null && agent.isLocked())
			agent.unlockPrivateKey(passphrase);
		if (agent.isLocked())
			throw new IllegalStateException("You have to unlock the agent first or give a correct passphrase!");
		try {
			node.registerReceiver(agent);

		} catch (AgentAlreadyRegisteredException e) {
		}
		currentUser = agent;
	}

	/**
	 * Unregister the current user from the LAS2peer node and from this launcher.
	 * 
	 * @see #registerUserAgent
	 */
	public void unregisterCurrentAgent() {
		if (currentUser == null)
			return;

		try {
			node.unregisterAgent(currentUser);
		} catch (AgentNotKnownException e) {
		}

		currentUser = null;
	}

	/**
	 * Invokes a service method as the current agent.
	 * 
	 * The arguments must be passed via ONE String separated by "-".
	 * 
	 * @see #registerUserAgent
	 * 
	 * @param serviceClass
	 * @param parameters pass an empty string if you want to call a method without parameters
	 * @throws L2pServiceException any exception during service method invocation
	 */
	public Serializable invoke(String serviceClass, String serviceMethod, String parameters)
			throws L2pServiceException {
		if (parameters.equals(""))
			return invoke(serviceClass, serviceMethod, new Serializable[0]);
		String[] split = parameters.trim().split("-");
		return invoke(serviceClass, serviceMethod, (Serializable[]) split);
	}

	/**
	 * Invokes a service method as the current agent.
	 * 
	 * @see #registerUserAgent
	 * 
	 * @param serviceClass
	 * @param parameters
	 * @throws L2pServiceException any exception during service method invocation
	 */
	private Serializable invoke(String serviceClass, String serviceMethod, Serializable... parameters)
			throws L2pServiceException {
		if (currentUser == null)
			throw new IllegalStateException("Please register a valid user with registerUserAgent before invoking!");

		try {
			try {
				return node.invokeLocally(currentUser.getId(), serviceClass, serviceMethod, parameters);
			} catch (NoSuchServiceException e) {
				return node.invokeGlobally(currentUser, serviceClass, serviceMethod, parameters);
			}
		} catch (Exception e) {
			throw new L2pServiceException("Exception during service method invocation!", e);
		}
	}

	/**
	 * Returns a list of available methods for the given service class name.
	 * 
	 * @param serviceName
	 * 
	 * @return list of methods encapsulated in a ListMethodsContent
	 * 
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws InterruptedException
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 * @throws TimeoutException
	 */
	public ListMethodsContent getServiceMethods(String serviceName) throws L2pSecurityException,
			AgentNotKnownException, InterruptedException, EncodingFailedException, SerializationException,
			TimeoutException {
		if (currentUser == null)
			throw new IllegalStateException("please log in a valid user with registerUserAgent before!");

		Agent receiver = node.getServiceAgent(serviceName);
		Message request = new Message(currentUser, receiver, (Serializable) new ListMethodsContent(), 30000);
		request.setSendingNodeId((NodeHandle) node.getNodeId());

		Message response = node.sendMessageAndWaitForAnswer(request);
		response.open(currentUser, node);

		return (ListMethodsContent) response.getContent();
	}

	/**
	 * Generate a new {@link i5.las2peer.security.ServiceAgent} instance for the given service class and start an
	 * instance of this service at the current LAS2peer node.
	 * 
	 * @param serviceClass
	 * 
	 * @return Returns the passphrase of the generated {@link i5.las2peer.security.ServiceAgent} or null if the agent is
	 *         already known.
	 * @throws L2pServiceException
	 */
	public String startService(String serviceClass) throws L2pServiceException {
		try {
			String passPhrase = SimpleTools.createRandomString(20);

			ServiceAgent myAgent = ServiceAgent.createServiceAgent(serviceClass, passPhrase);
			myAgent.unlockPrivateKey(passPhrase);

			startService(myAgent);
			return passPhrase;
		} catch (AgentAlreadyRegisteredException e) {
			printMessage("Agent already registered. Please use the existing instance.");
			return null;
		} catch (Exception e) {

			if (e instanceof L2pServiceException)
				throw (L2pServiceException) e;
			else
				throw new L2pServiceException("Error registering the service at the node!", e);
		}
	}

	/**
	 * start a service defined by an XML file of the corresponding agent
	 * 
	 * @param file
	 * @param passphrase
	 * @return the service agent
	 * @throws Exception
	 */
	public ServiceAgent startServiceXml(String file, String passphrase) throws Exception {
		try {
			ServiceAgent sa = ServiceAgent.createFromXml(FileContentReader.read(file));
			sa.unlockPrivateKey(passphrase);
			startService(sa);
			return sa;
		} catch (Exception e) {
			System.out.println("Starting service failed");
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * start a service with a known agent or generate a new agent for the service
	 * 
	 * @param serviceClass
	 * @param agentPass
	 * @throws L2pSecurityException
	 * @throws AgentException
	 * @throws AgentAlreadyRegisteredException
	 * @throws CryptoException
	 */
	public void startService(String serviceClass, String agentPass) throws AgentNotKnownException,
			L2pSecurityException, AgentAlreadyRegisteredException, AgentException, CryptoException {
		ServiceAgent sa = null;
		try {
			sa = node.getServiceAgent(serviceClass);
		} catch (Exception e) {
			ColoredOutput.println("Can't get service agent for " + serviceClass + ". Generating new instance...");
			sa = ServiceAgent.createServiceAgent(serviceClass, agentPass);
		}
		sa.unlockPrivateKey(agentPass);
		startService(sa);
	}

	/**
	 * start the service defined by the given (Service-)Agent
	 * 
	 * @param serviceAgent
	 * 
	 * @throws AgentAlreadyRegisteredException
	 * @throws L2pSecurityException
	 * @throws AgentException
	 */
	public void startService(Agent serviceAgent) throws AgentAlreadyRegisteredException, L2pSecurityException,
			AgentException {
		if (!(serviceAgent instanceof ServiceAgent))
			throw new IllegalArgumentException("given Agent is not a service agent!");
		if (serviceAgent.isLocked())
			throw new IllegalStateException("You have to unlock the agent before starting the corresponding service!");

		node.registerReceiver(serviceAgent);
	}

	/**
	 * load an agent from an XML file and return it for later usage
	 * 
	 * @param filename name of the file to load
	 * 
	 * @return the loaded agent
	 * @throws AgentException
	 */
	public Agent loadAgentFromXml(String filename) throws AgentException {
		try {
			String contents = FileContentReader.read(filename);
			Agent result = Agent.createFromXml(contents);
			return result;
		} catch (Exception e) {
			throw new AgentException("problems loading an agent from the given file", e);
		}
	}

	/**
	 * try to unlock the private key of the given agent with the given pass phrase
	 * 
	 * @param agent
	 * @param passphrase
	 * @throws L2pSecurityException
	 */
	public void unlockAgent(PassphraseAgent agent, String passphrase) throws L2pSecurityException {
		agent.unlockPrivateKey(passphrase);
	}

	/**
	 * start interactive console mode based on a {@link i5.las2peer.tools.CommandPrompt}
	 */
	public void interactive() {
		System.out
				.println(
						"Entering interactive mode for node "
								+ this
								+ "\n"
								+ "-----------------------------------------------\n"
								+ "Enter 'help' for further information of the console.\n"
								+ "Use all public methods of the L2pNodeLauncher class for interaction with the P2P network.\n\n");

		commandPrompt.startPrompt();

		System.out.println("Exiting interactive mode for node " + this);
	}

	/**
	 * get the information stored about the local Node
	 * 
	 * @return a node information
	 * @throws CryptoException
	 */
	public NodeInformation getLocalNodeInfo() throws CryptoException {
		return node.getNodeInformation();
	}

	/**
	 * get information about other nodes (probably neighbors in the ring etc)
	 * 
	 * @return string with node information
	 */
	public String getNetInfo() {
		return SimpleTools.join(node.getOtherKnownNodes(), "\n\t");
	}

	/**
	 * Creates a new node launcher instance.
	 * 
	 * @param port local port number to open
	 * @param bootstrap comma separated list of bootstrap nodes to connect to or "-" for a new network
	 * @param monitoringObserver determines, if the monitoring-observer will be started at this node
	 * @param cl the classloader to be used with this node
	 */
	private L2pNodeLauncher(int port, String bootstrap, boolean monitoringObserver, L2pClassLoader cl,
			Long nodeIdSeed) {
		if (System.getenv().containsKey("MEM_STORAGE")) {
			node = new PastryNodeImpl(port, bootstrap, STORAGE_MODE.memory, monitoringObserver, cl, nodeIdSeed);
		} else {
			node = new PastryNodeImpl(port, bootstrap, STORAGE_MODE.filesystem, monitoringObserver, cl, nodeIdSeed);
		}

		commandPrompt = new CommandPrompt(this);
	}

	/**
	 * actually start the node
	 * 
	 * @throws NodeException
	 */
	private void start() throws NodeException {
		node.launch();
		printMessage("node started!");
	}

	/**
	 * Prints a (yellow) message to the console.
	 * 
	 * @param message
	 */
	private void printMessage(String message) {
		ColoredOutput.printlnYellow(message);
	}

	/**
	 * Prints a (red) warning message to the console.
	 * 
	 * @param message
	 */
	private void printWarning(String message) {
		ColoredOutput.printlnRed(message);
	}

	/**
	 * Launches a single node.
	 * 
	 * @param args
	 * @throws NodeException
	 */
	public static L2pNodeLauncher launchSingle(Iterable<String> args) throws NodeException {
		Integer port = null;
		String bootstrap = null;
		boolean observer = false;
		String sLogDir = DEFAULT_LOG_DIRECTORY;
		ArrayList<String> serviceDirectories = new ArrayList<>();
		serviceDirectories.add(DEFAULT_SERVICE_DIRECTORY);
		Long nodeIdSeed = null;
		List<String> commands = new ArrayList<>();
		// parse args
		Iterator<String> itArg = args.iterator();
		while (itArg.hasNext() == true) {
			String arg = itArg.next();
			itArg.remove();
			String larg = arg.toLowerCase();
			if (larg.equals("-p") == true || larg.equals("--port") == true) {
				if (itArg.hasNext() == false) {
					ColoredOutput.printlnYellow("ignored '" + arg + "', because port number expected after it");
				} else {
					String sPort = itArg.next();
					try {
						int p = Integer.valueOf(sPort);
						// in case of an exception this structure doesn't override an already set port number
						itArg.remove();
						port = p;
					} catch (NumberFormatException ex) {
						ColoredOutput.printlnYellow("ignored '" + arg + "', because " + sPort + " is not an integer");
					}
				}
			} else if (larg.equals("-b") == true || larg.equals("--bootstrap") == true) {
				if (itArg.hasNext() == false) {
					ColoredOutput.printlnYellow("ignored '" + arg
							+ "', because comma separated bootstrap list expected after it");
				} else {
					String[] bsList = itArg.next().split(",");
					for (String bs : bsList) {
						if (bs.isEmpty() == false) {
							if (bootstrap == null || bootstrap.isEmpty() == true) {
								bootstrap = bs;
							} else {
								bootstrap += "," + bs;
							}
						}
					}
					itArg.remove();
				}
			} else if (larg.equals("-o") == true || larg.equals("--observer") == true) {
				observer = true;
			} else if (larg.equals("-l") == true || larg.equals("--log-directory") == true) {
				if (itArg.hasNext() == false) {
					ColoredOutput.printlnYellow("ignored '" + arg + "', because log directory expected after it");
				} else {
					sLogDir = itArg.next();
					itArg.remove();
				}
			} else if (larg.equals("-n") == true || larg.equals("--node-id-seed") == true) {
				if (itArg.hasNext() == false) {
					ColoredOutput.printlnYellow("ignored '" + arg + "', because node id seed expected after it");
				} else {
					String sNodeId = itArg.next();
					try {
						long idSeed = Long.valueOf(sNodeId);
						// in case of an exception this structure doesn't override an already set node id seed
						itArg.remove();
						nodeIdSeed = idSeed;
					} catch (NumberFormatException ex) {
						ColoredOutput.printlnYellow("ignored '" + arg + "', because " + sNodeId + " is not an integer");
					}
				}
			} else if (larg.equals("-s") == true || larg.equals("--service-directory") == true) {
				if (itArg.hasNext() == false) {
					ColoredOutput.printlnYellow("ignored '" + arg + "', because service directory expected after it");
				} else {
					serviceDirectories.add(itArg.next());
					itArg.remove();
				}
			} else {
				commands.add(arg);
			}
		}
		// check parameters
		if (port == null) {
			ColoredOutput.printlnRed("no port number specified");
			return null;
		} else if (port < 1) {
			ColoredOutput.printlnRed("invalid port number specified");
			return null;
		}
		File logDir = null;
		try {
			logDir = new File(sLogDir);
			logDir.mkdirs();
		} catch (Exception ex) {
			ColoredOutput.printlnYellow("couldn't use '" + sLogDir + "' as log directory." + ex);
		}
		if (logDir == null) {
			ColoredOutput.printlnYellow("no log directory specified");
		}
		L2pClassLoader cl = setupClassLoader(serviceDirectories.toArray(new String[0]));
		// instantiate launcher
		L2pNodeLauncher launcher = new L2pNodeLauncher(port, bootstrap, observer, cl, nodeIdSeed);
		launcher.node.setLogfilePrefix(sLogDir);
		try {
			launcher.start();

			for (String command : commands) {
				System.out.println("Handling: '" + command + "'");
				launcher.commandPrompt.handleLine(command);
			}

			if (launcher.isFinished()) {
				launcher.printMessage("All commands have been handled and shutdown has been called -> end!");
			} else {
				launcher.printMessage("All commands have been handled -- keeping node open!");
			}
		} catch (NodeException e) {
			launcher.bFinished = true;
			e.printStackTrace();
			throw e;
		}

		return launcher;
	}

	/**
	 * Sets up the classloader.
	 * 
	 * @return a class loader looking into the given directories
	 * 
	 */
	private static L2pClassLoader setupClassLoader(String[] serviceDirectory) {
		return new L2pClassLoader(
				new FileSystemRepository(serviceDirectory, true), L2pNodeLauncher.class.getClassLoader());
	}

	/**
	 * Prints a help message for command line usage.
	 * 
	 * @param message a custom message that will be shown before the help message content
	 */
	public static void printHelp(String message) {
		if (message != null && !message.equals(""))
			System.out.println(message + "\n\n");

		System.out.println("LAS2peer Node Launcher");
		System.out.println("----------------------\n");
		System.out.println("Usage:\n");

		System.out.println("Help Message:");
		System.out.println("\t['--help'|'-h']");

		System.out.println("\nStart Node:");
		System.out
				.println("\t{optional: --windows-shell|-w} -p [port] {optional1} {optional2} {method1} {method2} ...");

		System.out.println("\nOptional arguments");
		System.out.println(
				"\t--windows-shell|-w disables the colored output (better readable for windows command line clients)\n");
		System.out.println("\t--log-directory|-l [directory] lets you choose the directory for log files (default: "
				+ DEFAULT_LOG_DIRECTORY + ")\n");
		System.out
				.println("\t--service-directory|-s [directory] adds the directory you added your services to (default: "
						+ DEFAULT_SERVICE_DIRECTORY
						+ ") to the class loader. This argument can occur multiple times.\n");
		System.out.println("\t--port|-p [port] specifies the port number of the node\n");
		System.out.println("\tno bootstrap argument states, that a complete new LAS2peer network is to start");
		System.out.println("\tor");
		System.out
				.println(
						"\t--bootstrap|-b [host-list] requires a comma seperated list of [address:ip] pairs of bootstrap nodes to connect to. This argument can occur multiple times.\n");
		System.out.println("\t--observer|-o starts a monitoring observer at this node\n");
		System.out
				.println(
						"\t--node-id-seed|-n [long] generates the node id by using this seed to provide persistence\n");

		System.out.println("The following methods can be used in arbitrary order and number:");

		for (Method m : L2pNodeLauncher.class.getMethods()) {
			if (Modifier.isPublic(m.getModifiers())
					&& !Modifier.isStatic(m.getModifiers())) {
				System.out.print("\t- " + m.getName());
				for (int i = 0; i < m.getParameterTypes().length; i++)
					System.out.print(" " + m.getParameterTypes()[i].getName() + " ");
				System.out.print("\n");
			}
		}
	}

	/**
	 * Prints a help message for command line usage.
	 */
	public static void printHelp() {
		printHelp(null);
	}

	/**
	 * Main method for command line processing.
	 * 
	 * The method will start a node and try to invoke all command line parameters as parameterless methods of this
	 * class.
	 * 
	 * Hint: use "windows_shell" as the first command to turn off all colored output (since this creates cryptic symbols
	 * in a Windows environment).
	 * 
	 * Hint: with "log-directory=.." you can set the logfile directory you want to use.
	 * 
	 * Hint: with "service-directory=.." you can set the directory your service jars are located at.
	 * 
	 * @param argv
	 * 
	 * @throws InterruptedException
	 * @throws MalformedXMLException
	 * @throws IOException
	 * @throws L2pSecurityException
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 * @throws NodeException
	 */
	public static void main(String[] argv) throws InterruptedException, MalformedXMLException, IOException,
			L2pSecurityException, EncodingFailedException, SerializationException, NodeException {
		// parse command line parameter into list
		List<String> instArgs = new ArrayList<>();
		for (String arg : argv) {
			String larg = arg.toLowerCase();
			if (larg.equals("-h") == true || larg.equals("--help") == true) { // Help Message
				printHelp();
				System.exit(1);
			} else if (larg.equals("-w") == true || larg.equals("--windows-shell") == true) { // turn off colored output
				ColoredOutput.allOff();
			} else { // node instance parameter
				instArgs.add(arg);
			}
		}

		// Launches the node
		L2pNodeLauncher launcher = launchSingle(instArgs);
		if (launcher == null) {
			System.exit(2);
		}

		if (launcher.isFinished()) {
			System.out.println("node has handled all commands and shut down!");
			try {
				Iterator<Connector> iterator = connectors.iterator();
				while (iterator.hasNext())
					iterator.next().stop();
			} catch (ConnectorException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("node has handled all commands -- keeping node open\n");
			System.out.println("press Strg-C to exit\n");
			try {
				while (true) {
					Thread.sleep(5000);
				}
			} catch (InterruptedException e) {
				try {
					Iterator<Connector> iterator = connectors.iterator();
					while (iterator.hasNext())
						iterator.next().stop();
				} catch (ConnectorException ce) {
					ce.printStackTrace();
				}
			}
		}
	}
}
