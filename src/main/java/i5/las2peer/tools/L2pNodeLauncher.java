package i5.las2peer.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import i5.las2peer.api.Connector;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.communication.ListMethodsContent;
import i5.las2peer.communication.Message;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.L2pSecurityManager;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import rice.p2p.commonapi.NodeHandle;

/**
 * las2peer node launcher
 * 
 * It is the main tool for service developers / node maintainers to start their services, upload agents to the network
 * and to test their service methods directly in the p2p network. The launcher both supports the start of a new network
 * as well as starting a node that connects to an already existing network via a bootstrap.
 * 
 * All methods to be executed can be stated via additional command line parameters to the {@link #main} method.
 */
public class L2pNodeLauncher {

	// this is the main class and therefore the logger needs to be static
	private static final L2pLogger logger = L2pLogger.getInstance(L2pNodeLauncher.class.getName());

	private static final String DEFAULT_SERVICE_DIRECTORY = "./service/";
	private static final String DEFAULT_STARTUP_DIRECTORY = "etc/startup/";
	private static final String DEFAULT_SERVICE_AGENT_DIRECTORY = "etc/startup/";

	private CommandPrompt commandPrompt;

	private static List<Connector> connectors = new ArrayList<>();

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
	 * Get the envelope with the given id
	 * 
	 * @param id
	 * @return the XML-representation of an envelope as a String
	 * @throws StorageException
	 * @throws ArtifactNotFoundException
	 * @throws NumberFormatException
	 * @throws SerializationException
	 */
	public String getEnvelope(String id)
			throws NumberFormatException, ArtifactNotFoundException, StorageException, SerializationException {
		return node.fetchEnvelope(id).toXmlString();
	}

	/**
	 * Searches for the given agent in the las2peer network.
	 * 
	 * @param agentId
	 * @return node handles
	 * @throws AgentNotKnownException
	 */
	public Object[] findAgent(String agentId) throws AgentNotKnownException {
		return node.findRegisteredAgent(agentId);
	}

	/**
	 * Looks for the given service in the las2peer network.
	 * 
	 * Needs an active user
	 * 
	 * @see #registerUserAgent
	 * 
	 * @param serviceNameVersion Exact name and version, same syantax as in {@link #startService(String)}
	 * @return node handles
	 * @throws AgentNotKnownException
	 */
	public Object[] findService(String serviceNameVersion) throws AgentNotKnownException {
		if (currentUser == null) {
			throw new IllegalStateException("Please register a valid user with registerUserAgent before invoking!");
		}

		Agent agent = node.getServiceAgent(ServiceNameVersion.fromString(serviceNameVersion), currentUser);
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
		Hashtable<String, String> result = new Hashtable<>();

		File file = new File(filename);
		if (file.isFile()) {
			String[] content;
			try {
				content = FileContentReader.read(file).split("\n");
				for (String line : content) {
					line = line.trim();
					if (line.isEmpty()) {
						continue;
					}
					String[] split = line.split(";", 2);
					if (split.length != 2) {
						printWarning("Ignoring invalid passphrase line (" + line + ") in '" + filename + "'");
						continue;
					}
					result.put(split[0], split[1]);
				}
			} catch (IOException e) {
				printError("Error reading contents of " + filename + ": " + e);
				logger.printStackTrace(e);
				bFinished = true;
			}
		}

		return result;
	}

	/**
	 * Uploads the contents of the given directory to the global storage of the las2peer network.
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
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(directory + " is not a directory!");
		}
		Hashtable<String, String> htPassphrases = loadPassphrases(directory + "/passphrases.txt");
		Map<String, String> agentIdToXml = new HashMap<>();
		List<GroupAgent> groupAgents = new LinkedList<>();
		for (File xmlFile : dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		})) {
			try {
				// maybe an agent?
				Agent agent = Agent.createFromXml(xmlFile);
				agentIdToXml.put(agent.getSafeId(), xmlFile.getName());
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
			} catch (MalformedXMLException e) {
				printError("unable to deserialize contents of " + xmlFile.toString() + "!");
			} catch (L2pSecurityException e) {
				printError("error storing agent from " + xmlFile.toString() + ": " + e);
			} catch (AgentAlreadyRegisteredException e) {
				printError("agent from " + xmlFile.toString() + " already known at this node!");
			} catch (AgentException e) {
				printError("unable to generate agent " + xmlFile.toString() + "!");
			}
		}
		// wait till all user agents are added from startup directory to unlock group agents
		for (GroupAgent currentGroupAgent : groupAgents) {
			for (String memberId : currentGroupAgent.getMemberList()) {
				Agent memberAgent = null;
				try {
					memberAgent = node.getAgent(memberId);
				} catch (AgentNotKnownException e) {
					printError("Can't get agent for group member " + memberId);
					continue;
				}
				if ((memberAgent instanceof PassphraseAgent) == false) {
					printError("Unknown agent type to unlock, type: " + memberAgent.getClass().getName());
					continue;
				}
				PassphraseAgent memberPassAgent = (PassphraseAgent) memberAgent;
				String xmlName = agentIdToXml.get(memberPassAgent.getSafeId());
				if (xmlName == null) {
					printError("No known xml file for agent " + memberPassAgent.getSafeId());
					continue;
				}
				String passphrase = htPassphrases.get(xmlName);
				if (passphrase == null) {
					printError("No known password for agent " + memberPassAgent.getSafeId());
					continue;
				}
				try {
					memberPassAgent.unlockPrivateKey(passphrase);
					currentGroupAgent.unlockPrivateKey(memberPassAgent);
					node.storeAgent(currentGroupAgent);
					printMessage("\t- stored group agent from " + xmlName);
					break;
				} catch (Exception e) {
					printError("Can't unlock group agent " + currentGroupAgent.getSafeId() + " with member "
							+ memberPassAgent.getSafeId());
					continue;
				}
			}
			if (currentGroupAgent.isLocked()) {
				throw new IllegalArgumentException("group agent still locked!");
			}
		}
	}

	/**
	 * Upload the contents of the <i>startup</i> sub directory to the global storage of the las2peer network.
	 * 
	 * Each contained .xml-file is used as an artifact or - in case the name of the file starts with <i>agent-</i> - as
	 * an agent to upload.
	 * 
	 * If agents are to be uploaded, make sure, that the startup directory contains a <i>passphrases.txt</i> file giving
	 * the passphrases for the agents.
	 */
	public void uploadStartupDirectory() {
		uploadStartupDirectory(DEFAULT_STARTUP_DIRECTORY);
	}

	/**
	 * Uploads the service jar file and its dependencies into the shared storage to be used for network class loading.
	 * 
	 * @param serviceJarFile The service jar file that should be uploaded.
	 * @param developerAgentXMLFile The XML file of the developers agent.
	 * @param developerPassword The password for the developer agent.
	 */
	public void uploadServicePackage(String serviceJarFile, String developerAgentXMLFile, String developerPassword)
			throws ServicePackageException {
		PackageUploader.uploadServicePackage(node, serviceJarFile, developerAgentXMLFile, developerPassword);
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
			printError(" --> Problems starting the connector: " + e);
			logger.printStackTrace(e);
		} catch (ClassNotFoundException e) {
			logger.printStackTrace(e);
		} catch (InstantiationException e) {
			logger.printStackTrace(e);
		} catch (IllegalAccessException e) {
			logger.printStackTrace(e);
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
	 * 
	 * @param connectorClass
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
				logger.printStackTrace(e);
			}
		}
		printWarning("No connector with the given classname was started!");
	}

	/**
	 * Returns a connector for the given classname.
	 * 
	 * @param classname
	 * @return the loaded connector
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private Connector loadConnector(String classname)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
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
	 * @return the registered agent
	 */
	public boolean registerUserAgent(String id, String passphrase) {
		try {
			if (id.matches("-?[0-9].*")) {
				currentUser = (UserAgent) node.getAgent(id);
			} else {
				currentUser = (UserAgent) node.getAgent(node.getUserManager().getAgentIdByLogin(id));
			}

			currentUser.unlockPrivateKey(passphrase);

			node.registerReceiver(currentUser);

			return true;
		} catch (Exception e) {
			logger.printStackTrace(e);
			currentUser = null;
			return false;
		}
	}

	/**
	 * Register the given agent at the las2peer node and for later usage with {@link #invoke}.
	 * 
	 * Make sure, that the private key of the agent is unlocked before registering
	 * 
	 * @param agent
	 * @throws L2pSecurityException
	 * @throws AgentAlreadyRegisteredException
	 * @throws AgentException
	 */
	public void registerUserAgent(UserAgent agent)
			throws L2pSecurityException, AgentAlreadyRegisteredException, AgentException {
		registerUserAgent(agent, null);
	}

	/**
	 * Register the given agent at the las2peer node and for later usage with {@link #invoke}.
	 * 
	 * If the private key of the agent is not unlocked and a pass phrase has been given, an attempt to unlock the key is
	 * started before registering.
	 * 
	 * @param agent
	 * @param passphrase
	 * @throws L2pSecurityException
	 * @throws AgentAlreadyRegisteredException
	 * @throws AgentException
	 */
	public void registerUserAgent(UserAgent agent, String passphrase) throws L2pSecurityException, AgentException {
		if (passphrase != null && agent.isLocked()) {
			agent.unlockPrivateKey(passphrase);
		}
		if (agent.isLocked()) {
			throw new IllegalStateException("You have to unlock the agent first or give a correct passphrase!");
		}
		try {
			node.registerReceiver(agent);

		} catch (AgentAlreadyRegisteredException e) {
		}
		currentUser = agent;
	}

	/**
	 * Unregister the current user from the las2peer node and from this launcher.
	 * 
	 * @see #registerUserAgent
	 */
	public void unregisterCurrentAgent() {
		if (currentUser == null) {
			return;
		}

		try {
			node.unregisterReceiver(currentUser);
		} catch (AgentNotKnownException | NodeException e) {
		}

		currentUser = null;
	}

	/**
	 * Invokes a service method as the current agent, choosing an approptiate service version.
	 * 
	 * The arguments must be passed via ONE String separated by "-".
	 * 
	 * @see #registerUserAgent
	 * @param serviceIdentifier
	 * @param serviceMethod
	 * @param parameters pass an empty string if you want to call a method without parameters
	 * @return
	 * @throws L2pServiceException any exception during service method invocation
	 */
	public Serializable invoke(String serviceIdentifier, String serviceMethod, String parameters)
			throws L2pServiceException {
		if (parameters.isEmpty()) {
			return invoke(serviceIdentifier, serviceMethod, new Serializable[0]);
		}
		String[] split = parameters.trim().split("-");
		return invoke(serviceIdentifier, serviceMethod, (Serializable[]) split);
	}

	/**
	 * Invokes a service method as the current agent, choosing an approptiate service version.
	 * 
	 * @see #registerUserAgent
	 * @param serviceIdentifier
	 * @param serviceMethod
	 * @param parameters
	 * @return
	 * @throws L2pServiceException any exception during service method invocation
	 */
	private Serializable invoke(String serviceIdentifier, String serviceMethod, Serializable... parameters)
			throws L2pServiceException {
		if (currentUser == null) {
			throw new IllegalStateException("Please register a valid user with registerUserAgent before invoking!");
		}

		try {
			return node.invoke(currentUser, serviceIdentifier, serviceMethod, parameters);
		} catch (Exception e) {
			throw new L2pServiceException("Exception during service method invocation!", e);
		}
	}

	/**
	 * Returns a list of available methods for the given service class name.
	 * 
	 * @param serviceNameVersion Exact service name and version, same syntax as in {@link #startService(String)}
	 * @return list of methods encapsulated in a ListMethodsContent
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 * @throws InterruptedException
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 * @throws TimeoutException
	 */
	public ListMethodsContent getServiceMethods(String serviceNameVersion)
			throws L2pSecurityException, AgentNotKnownException, InterruptedException, EncodingFailedException,
			SerializationException, TimeoutException {
		if (currentUser == null) {
			throw new IllegalStateException("please log in a valid user with registerUserAgent before!");
		}

		Agent receiver = node.getServiceAgent(ServiceNameVersion.fromString(serviceNameVersion), currentUser);
		Message request = new Message(currentUser, receiver, new ListMethodsContent());
		request.setSendingNodeId((NodeHandle) node.getNodeId());

		Message response = node.sendMessageAndWaitForAnswer(request);
		response.open(currentUser, node);

		return (ListMethodsContent) response.getContent();
	}

	/**
	 * Starts a service with a known agent or generate a new agent for the service (using a random passphrase)
	 * 
	 * Will create an xml file for the generated agent and store the passphrase lcoally
	 * 
	 * @param serviceNameVersion Specify the service name and version to run: package.serviceClass@Version. Exact match
	 *            required.
	 * @throws Exception on error
	 */
	public void startService(String serviceNameVersion) throws Exception {
		String passPhrase = SimpleTools.createRandomString(20);
		startService(serviceNameVersion, passPhrase);
	}

	/**
	 * start a service with a known agent or generate a new agent for the service
	 * 
	 * will create an xml file for the generated agent and store the passphrase lcoally
	 * 
	 * @param serviceNameVersion the exact name and version of the service to be started
	 * @param defaultPass this pass will be used to generate the agent if no agent exists
	 * @throws Exception on error
	 */
	public void startService(String serviceNameVersion, String defaultPass) throws Exception {
		ServiceNameVersion service = ServiceNameVersion.fromString(serviceNameVersion);

		if (service.getVersion() == null && service.getVersion().toString().equals("*")) {
			printError("You must specify an exact version of the service you want to start.");
			return;
		}

		File file = new File(DEFAULT_SERVICE_AGENT_DIRECTORY + serviceNameVersion + ".xml");
		if (!file.exists()) {
			// create agent
			ServiceAgent a = ServiceAgent.createServiceAgent(service, defaultPass);
			file.getParentFile().mkdirs();
			file.createNewFile();

			// save agent
			Files.write(file.toPath(), a.toXmlString().getBytes());

			// save passphrase
			Path passphrasesPath = Paths.get(DEFAULT_SERVICE_AGENT_DIRECTORY + "passphrases.txt");
			String passphraseLine = serviceNameVersion + ".xml;" + defaultPass;
			try {
				Files.write(passphrasesPath, ("\n" + passphraseLine).getBytes(), StandardOpenOption.APPEND);
			} catch (NoSuchFileException e) {
				Files.write(passphrasesPath, passphraseLine.getBytes(), StandardOpenOption.CREATE);
			}
		}

		// get passphrase from file
		Hashtable<String, String> htPassphrases = loadPassphrases(DEFAULT_SERVICE_AGENT_DIRECTORY + "passphrases.txt");
		if (htPassphrases.containsKey(serviceNameVersion.toString() + ".xml")) {
			defaultPass = htPassphrases.get(serviceNameVersion.toString() + ".xml");
		}

		// start
		startServiceXml(file.toPath().toString(), defaultPass);
	}

	/**
	 * start a service defined by an XML file of the corresponding agent
	 * 
	 * @param file path to the file containing the service
	 * @param passphrase passphrase to unlock the service agent
	 * @return the service agent
	 * @throws Exception on error
	 */
	public ServiceAgent startServiceXml(String file, String passphrase) throws Exception {
		try {
			ServiceAgent xmlAgent = ServiceAgent.createFromXml(FileContentReader.read(file));
			ServiceAgent serviceAgent;
			try {
				// check if the agent is already known to the network
				serviceAgent = (ServiceAgent) node.getAgent(xmlAgent.getSafeId());
				serviceAgent.unlockPrivateKey(passphrase);
			} catch (AgentNotKnownException e) {
				xmlAgent.unlockPrivateKey(passphrase);
				node.storeAgent(xmlAgent);
				logger.info("ServiceAgent was not known in network. Published it");
				serviceAgent = xmlAgent;
			}
			startService(serviceAgent);
			return serviceAgent;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Starting service failed", e);
			throw e;
		}
	}

	/**
	 * start the service defined by the given (Service-)Agent
	 * 
	 * @param serviceAgent
	 * @throws AgentAlreadyRegisteredException
	 * @throws L2pSecurityException
	 * @throws AgentException
	 */
	public void startService(Agent serviceAgent)
			throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException {
		if (!(serviceAgent instanceof ServiceAgent)) {
			throw new IllegalArgumentException("given Agent is not a service agent!");

		}
		if (serviceAgent.isLocked()) {
			throw new IllegalStateException("You have to unlock the agent before starting the corresponding service!");
		}

		node.registerReceiver(serviceAgent);
	}

	/**
	 * stop the given service
	 * 
	 * needs name and version
	 * 
	 * @param serviceNameVersion
	 * @throws AgentNotKnownException
	 * @throws NoSuchServiceException
	 * @throws NodeException
	 */
	public void stopService(String serviceNameVersion)
			throws AgentNotKnownException, NoSuchServiceException, NodeException {
		ServiceAgent agent = node.getLocalServiceAgent(ServiceNameVersion.fromString(serviceNameVersion));
		node.unregisterReceiver(agent);
	}

	/**
	 * load an agent from an XML file and return it for later usage
	 * 
	 * @param filename name of the file to load
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
		System.out.println("Entering interactive mode for node " + this.getNode().getPastryNode().getId().toStringFull()
				+ "\n" + "-----------------------------------------------\n"
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
	 * @param storageMode A {@link STORAGE_MODE} used by the local node instance for persistence.
	 * @param monitoringObserver determines, if the monitoring-observer will be started at this node
	 * @param cl the class loader to be used with this node
	 * @param nodeIdSeed the seed to generate node IDs from
	 */
	private L2pNodeLauncher(int port, String bootstrap, STORAGE_MODE storageMode, boolean monitoringObserver,
			L2pClassManager cl, Long nodeIdSeed) {
		if (storageMode == null) {
			if (System.getenv().containsKey("MEM_STORAGE")) {
				storageMode = STORAGE_MODE.MEMORY;
			} else {
				storageMode = STORAGE_MODE.FILESYSTEM;
			}
		}
		node = new PastryNodeImpl(cl, monitoringObserver, port, bootstrap, storageMode, nodeIdSeed);

		commandPrompt = new CommandPrompt(this);
	}

	private L2pNodeLauncher(L2pClassManager cl, Integer port, String bootstrap) {
		node = new PastryNodeImpl(cl, port, bootstrap, STORAGE_MODE.MEMORY, null, null);
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
	 * Prints a message to the console.
	 * 
	 * @param message
	 */
	private static void printMessage(String message) {
		logger.info(message);
	}

	/**
	 * Prints a (Yellow) warning message to the console.
	 * 
	 * @param message
	 */
	private static void printWarning(String message) {
		message = ColoredOutput.colorize(message, ColoredOutput.ForegroundColor.Yellow);
		logger.warning(message);
	}

	/**
	 * Prints a (Red) error message to the console.
	 * 
	 * @param message
	 */
	private static void printError(String message) {
		message = ColoredOutput.colorize(message, ColoredOutput.ForegroundColor.Red);
		logger.severe(message);
	}

	/**
	 * Launches a single node.
	 * 
	 * @param args
	 * @return the L2pNodeLauncher instance
	 * @throws NodeException
	 */
	public static L2pNodeLauncher launchSingle(Iterable<String> args) throws NodeException {
		boolean debugMode = false;
		Integer port = null;
		String bootstrap = null;
		STORAGE_MODE storageMode = null;
		boolean observer = false;
		String sLogDir = null;
		ArrayList<String> serviceDirectories = null;
		Long nodeIdSeed = null;
		List<String> commands = new ArrayList<>();
		// parse args
		Iterator<String> itArg = args.iterator();
		while (itArg.hasNext() == true) {
			String arg = itArg.next();
			itArg.remove();
			String larg = arg.toLowerCase();
			if (larg.equals("-debug") || larg.equals("--debug")) {
				debugMode = true;
			} else if (larg.equals("-p") == true || larg.equals("--port") == true) {
				if (itArg.hasNext() == false) {
					printWarning("ignored '" + arg + "', because port number expected after it");
				} else {
					String sPort = itArg.next();
					try {
						int p = Integer.valueOf(sPort);
						// in case of an exception this structure doesn't override an already set port number
						itArg.remove();
						port = p;
					} catch (NumberFormatException ex) {
						printWarning("ignored '" + arg + "', because " + sPort + " is not an integer");
					}
				}
			} else if (larg.equals("-b") == true || larg.equals("--bootstrap") == true) {
				if (itArg.hasNext() == false) {
					printWarning("ignored '" + arg + "', because comma separated bootstrap list expected after it");
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
					printWarning("ignored '" + arg + "', because log directory expected after it");
				} else {
					sLogDir = itArg.next();
					itArg.remove();
				}
			} else if (larg.equals("-n") == true || larg.equals("--node-id-seed") == true) {
				if (itArg.hasNext() == false) {
					printWarning("ignored '" + arg + "', because node id seed expected after it");
				} else {
					String sNodeId = itArg.next();
					try {
						long idSeed = Long.valueOf(sNodeId);
						// in case of an exception this structure doesn't override an already set node id seed
						itArg.remove();
						nodeIdSeed = idSeed;
					} catch (NumberFormatException ex) {
						printWarning("ignored '" + arg + "', because " + sNodeId + " is not an integer");
					}
				}
			} else if (larg.equals("-s") == true || larg.equals("--service-directory") == true) {
				if (itArg.hasNext() == false) {
					printWarning("ignored '" + arg + "', because service directory expected after it");
				} else {
					if (serviceDirectories == null) {
						serviceDirectories = new ArrayList<>();
					}
					serviceDirectories.add(itArg.next());
					itArg.remove();
				}
			} else if (larg.equals("-m") == true || larg.equals("--storage-mode") == true) {
				if (itArg.hasNext() == false) {
					printWarning("ignored '" + arg + "', because storage mode expected after it");
				} else {
					String val = itArg.next();
					if (val.equals("memory")) {
						storageMode = STORAGE_MODE.MEMORY;
					} else if (val.equals("filesystem")) {
						storageMode = STORAGE_MODE.FILESYSTEM;
					} else {
						printWarning("ignored '" + arg + "', because storage mode expected after it");
					}
					itArg.remove();
				}
			} else {
				commands.add(arg);
			}
		}
		// check parameters and launch node
		if (debugMode) {
			System.err.println("WARNING! Launching node in DEBUG mode! THIS NODE IS NON PERSISTENT!");
			return launchDebug(port, bootstrap, sLogDir, serviceDirectories, commands);
		} else {
			if (port == null) {
				printError("no port number specified");
				return null;
			} else if (port < 1) {
				printError("invalid port number specified");
				return null;
			}
			return launchSingle(port, bootstrap, storageMode, observer, sLogDir, serviceDirectories, nodeIdSeed,
					commands);
		}
	}

	public static L2pNodeLauncher launchSingle(int port, String bootstrap, STORAGE_MODE storageMode, boolean observer,
			String sLogDir, Iterable<String> serviceDirectories, Long nodeIdSeed, Iterable<String> commands)
			throws NodeException {
		// check parameters
		if (sLogDir != null) {
			try {
				L2pLogger.setGlobalLogDirectory(sLogDir);
			} catch (Exception ex) {
				printWarning("couldn't use '" + sLogDir + "' as log directory." + ex);
			}
		}
		if (serviceDirectories == null) {
			ArrayList<String> directories = new ArrayList<>();
			directories.add(DEFAULT_SERVICE_DIRECTORY);
			serviceDirectories = directories;
		}
		if (commands == null) {
			commands = new ArrayList<>();
		}
		// instantiate launcher
		L2pClassManager cl = new L2pClassManager(new FileSystemRepository(serviceDirectories, true),
				L2pNodeLauncher.class.getClassLoader());
		L2pNodeLauncher launcher = new L2pNodeLauncher(port, bootstrap, storageMode, observer, cl, nodeIdSeed);
		// handle commands
		try {
			launcher.start();

			for (String command : commands) {
				System.out.println("Handling: '" + command + "'");
				launcher.commandPrompt.handleLine(command);
			}

			if (launcher.isFinished()) {
				printMessage("All commands have been handled and shutdown has been called -> end!");
			} else {
				printMessage("All commands have been handled -- keeping node open!");
			}
		} catch (NodeException e) {
			launcher.bFinished = true;
			logger.printStackTrace(e);
			throw e;
		}

		return launcher;
	}

	private static L2pNodeLauncher launchDebug(Integer port, String boostrap, String sLogDir,
			Iterable<String> serviceDirectories, Iterable<String> commands) throws NodeException {
		// check parameters
		if (sLogDir != null) {
			try {
				L2pLogger.setGlobalLogDirectory(sLogDir);
			} catch (Exception ex) {
				printWarning("couldn't use '" + sLogDir + "' as log directory." + ex);
			}
		}
		if (serviceDirectories == null) {
			ArrayList<String> directories = new ArrayList<>();
			directories.add(DEFAULT_SERVICE_DIRECTORY);
			serviceDirectories = directories;
		}
		if (commands == null) {
			commands = new ArrayList<>();
		}
		// instantiate launcher
		L2pClassManager cl = new L2pClassManager(new FileSystemRepository(serviceDirectories, true),
				L2pNodeLauncher.class.getClassLoader());
		L2pNodeLauncher launcher = new L2pNodeLauncher(cl, port, boostrap);
		// handle commands
		try {
			launcher.start();

			for (String command : commands) {
				System.out.println("Handling: '" + command + "'");
				launcher.commandPrompt.handleLine(command);
			}

			if (launcher.isFinished()) {
				printMessage("All commands have been handled and shutdown has been called -> end!");
			} else {
				printMessage("All commands have been handled -- keeping node open!");
			}
		} catch (NodeException e) {
			launcher.bFinished = true;
			logger.printStackTrace(e);
			throw e;
		}

		return launcher;
	}

	/**
	 * Prints a help message for command line usage.
	 * 
	 * @param message a custom message that will be shown before the help message content
	 */
	public static void printHelp(String message) {
		if (message != null && !message.isEmpty()) {
			System.out.println(message + "\n\n");
		}

		System.out.println("las2peer Node Launcher");
		System.out.println("----------------------\n");
		System.out.println("Usage:\n");

		System.out.println("Help Message:");
		System.out.println("\t['--help'|'-h']");

		System.out.println("las2peer version:");
		System.out.println("\t['--version'|'-v']");

		System.out.println("\nStart Node:");
		System.out
				.println("\t{optional: --colored-shell|-c} -p [port] {optional1} {optional2} {method1} {method2} ...");

		System.out.println("\nOptional arguments");
		System.out.println("\t--colored-shell|-c enables colored output (better readable command line)\n");
		System.out.println("\t--log-directory|-l [directory] lets you choose the directory for log files (default: "
				+ L2pLogger.DEFAULT_LOG_DIRECTORY + ")\n");
		System.out
				.println("\t--service-directory|-s [directory] adds the directory you added your services to (default: "
						+ DEFAULT_SERVICE_DIRECTORY
						+ ") to the class loader. This argument can occur multiple times.\n");
		System.out.println("\t--port|-p [port] specifies the port number of the node\n");
		System.out.println("\tno bootstrap argument states, that a complete new las2peer network is to start");
		System.out.println("\tor");
		System.out.println(
				"\t--bootstrap|-b [host-list] requires a comma seperated list of [address:ip] pairs of bootstrap nodes to connect to. This argument can occur multiple times.\n");
		System.out.println("\t--observer|-o starts a monitoring observer at this node\n");
		System.out.println(
				"\t--node-id-seed|-n [long] generates the node id by using this seed to provide persistence\n");
		System.out
				.println("\t--storage-mode|-m filesystem|memory sets Pastry's storage mode, defaults to filesystem\n");

		System.out.println("The following methods can be used in arbitrary order and number:");

		for (Method m : L2pNodeLauncher.class.getMethods()) {
			if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
				System.out.print("\t- " + m.getName());
				for (int i = 0; i < m.getParameterTypes().length; i++) {
					System.out.print(" " + m.getParameterTypes()[i].getName() + " ");
				}
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
	 * Prints the las2peer version.
	 */
	public static void printVersion() {
		Package p = L2pNodeLauncher.class.getPackage();
		String version = p.getImplementationVersion();
		System.out.println("las2peer version \"" + version + "\"");
	}

	/**
	 * Main method for command line processing.
	 * 
	 * The method will start a node and try to invoke all command line parameters as parameterless methods of this
	 * class.
	 * 
	 * Hint: with "log-directory=.." you can set the logfile directory you want to use.
	 * 
	 * Hint: with "service-directory=.." you can set the directory your service jars are located at.
	 * 
	 * @param argv
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
		System.setSecurityManager(new L2pSecurityManager());
		// self test encryption environment
		try {
			CryptoTools.encryptSymmetric("las2peer rulez!".getBytes(), CryptoTools.generateSymmetricKey());
		} catch (CryptoException e) {
			throw new L2pSecurityException(
					"Fatal Error! Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files are not installed!",
					e);
		}
		// parse command line parameter into list
		List<String> instArgs = new ArrayList<>();
		// a command can have brackets with spaces inside, which is split by arg parsing falsely
		List<String> argvJoined = new ArrayList<>();
		String joined = "";
		for (String arg : argv) {
			int opening = arg.length() - arg.replace("(", "").length(); // nice way to count opening brackets
			int closing = arg.length() - arg.replace(")", "").length();
			if (opening == closing && joined.isEmpty()) {
				// just an argument
				argvJoined.add(arg);
			} else {
				// previous arg was unbalanced, attach this arg
				joined += arg;
				int openingJoined = joined.length() - joined.replace("(", "").length();
				int closingJoined = joined.length() - joined.replace(")", "").length();
				if (openingJoined == closingJoined) {
					// now its balanced
					argvJoined.add(joined);
					joined = "";
				} else if (openingJoined < closingJoined) {
					throw new IllegalArgumentException("command \"" + joined + "\" has too many closing brackets!");
				} // needs more args to balance
			}
		}
		if (!joined.isEmpty()) {
			throw new IllegalArgumentException("command \"" + joined + "\" has too many opening brackets!");
		}
		for (String arg : argvJoined) {
			String larg = arg.toLowerCase();
			if (larg.equals("-h") == true || larg.equals("--help") == true) { // Help Message
				printHelp();
				System.exit(1);
			} else if (larg.equals("-v") || larg.equals("--version")) {
				printVersion();
				System.exit(1);
			} else if (larg.equals("-w") || larg.equals("--windows-shell")) {
				printWarning(
						"Ignoring obsolete argument '" + arg + "', because colored output is disabled by default.");
			} else if (larg.equals("-c") == true || larg.equals("--colored-shell") == true) { // turn on colored output
				ColoredOutput.allOn();
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
				while (iterator.hasNext()) {
					iterator.next().stop();
				}
			} catch (ConnectorException e) {
				logger.printStackTrace(e);
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
					while (iterator.hasNext()) {
						iterator.next().stop();
					}
				} catch (ConnectorException ce) {
					logger.printStackTrace(ce);
				}
			}
		}
	}
}
