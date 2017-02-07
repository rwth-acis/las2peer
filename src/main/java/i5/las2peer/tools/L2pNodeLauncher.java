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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import i5.las2peer.tools.helper.L2pNodeLauncherConfiguration;
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
	 * @throws AgentException
	 */
	public Object[] findService(String serviceNameVersion) throws AgentException {
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
				printErrorWithStacktrace("Error reading contents of " + filename, e);
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
				printErrorWithStacktrace("unable to deserialize contents of " + xmlFile.toString() + "!", e);
			} catch (L2pSecurityException e) {
				printErrorWithStacktrace("error storing agent from " + xmlFile.toString(), e);
			} catch (AgentAlreadyRegisteredException e) {
				printErrorWithStacktrace("agent from " + xmlFile.toString() + " already known at this node!", e);
			} catch (AgentException e) {
				printErrorWithStacktrace("unable to generate agent " + xmlFile.toString() + "!", e);
			}
		}
		// wait till all user agents are added from startup directory to unlock group agents
		for (GroupAgent currentGroupAgent : groupAgents) {
			for (String memberId : currentGroupAgent.getMemberList()) {
				Agent memberAgent = null;
				try {
					memberAgent = node.getAgent(memberId);
				} catch (AgentException e) {
					printErrorWithStacktrace("Can't get agent for group member " + memberId, e);
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
					printErrorWithStacktrace("Can't unlock group agent " + currentGroupAgent.getSafeId()
							+ " with member " + memberPassAgent.getSafeId(), e);
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
		} catch (Exception e) {
			printErrorWithStacktrace(" --> Problems starting the connector", e);
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
	 * @throws AgentException
	 * @throws InterruptedException
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 * @throws TimeoutException
	 */
	public ListMethodsContent getServiceMethods(String serviceNameVersion) throws L2pSecurityException, AgentException,
			InterruptedException, EncodingFailedException, SerializationException, TimeoutException {
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
		System.out.println("Entering interactive mode\n" + "-----------------------------------------------\n"
				+ "Enter 'help' for further information of the console.\n"
				+ "Use all public methods of the L2pNodeLauncher class for interaction with the P2P network.\n\n");

		commandPrompt.startPrompt();
		System.out.println("Exiting interactive mode");
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
	private L2pNodeLauncher(Integer port, String bootstrap, STORAGE_MODE storageMode, boolean monitoringObserver,
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

	/**
	 * actually start the node
	 * 
	 * @throws NodeException
	 */
	private void start() throws NodeException {
		node.launch();
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
	 * Prints a (Red) error message to the console including a stack trace.
	 * 
	 * @param message
	 */
	private static void printErrorWithStacktrace(String message, Throwable throwable) {
		message = ColoredOutput.colorize(message, ColoredOutput.ForegroundColor.Red);
		logger.log(Level.SEVERE, message, throwable);
	}

	/**
	 * @deprecated Use {@link #launchConfiguration(L2pNodeLauncherConfiguration)} instead.
	 * 
	 *             Launches a single node.
	 * 
	 * @param args
	 * @return the L2pNodeLauncher instance
	 * @throws CryptoException If the system encryption self test fails. See log/output for details.
	 * @throws NodeException If an issue with the launched node occurs.
	 * @throws IllegalArgumentException If an issue occurs with a configuration argument.
	 */
	@Deprecated
	public static L2pNodeLauncher launchSingle(Iterable<String> args) throws CryptoException, NodeException {
		return launchConfiguration(L2pNodeLauncherConfiguration.createFromIterableArgs(args));
	}

	/**
	 * @deprecated Use {@link #launchConfiguration(L2pNodeLauncherConfiguration)} instead.
	 * 
	 *             Launches a single node.
	 * 
	 * @param port
	 * @param bootstrap
	 * @param storageMode
	 * @param observer
	 * @param sLogDir
	 * @param serviceDirectories
	 * @param nodeIdSeed
	 * @param commands
	 * @return
	 * @throws CryptoException If the system encryption self test fails. See log/output for details.
	 * @throws NodeException If an issue with the launched node occurs.
	 * @throws IllegalArgumentException If an issue occurs with a configuration argument.
	 */
	@Deprecated
	public static L2pNodeLauncher launchSingle(int port, String bootstrap, STORAGE_MODE storageMode, boolean observer,
			String sLogDir, Iterable<String> serviceDirectories, Long nodeIdSeed, Iterable<String> commands)
			throws CryptoException, NodeException {
		L2pNodeLauncherConfiguration configuration = new L2pNodeLauncherConfiguration();
		configuration.setPort(port);
		configuration.setBootstrap(bootstrap);
		configuration.setStorageMode(storageMode);
		configuration.setUseMonitoringObserver(observer);
		configuration.setLogDir(sLogDir);
		serviceDirectories.forEach(configuration.getServiceDirectories()::add);
		configuration.setNodeIdSeed(nodeIdSeed);
		commands.forEach(configuration.getCommands()::add);
		return launchConfiguration(configuration);
	}

	/**
	 * @param launcherConfiguration
	 * @return Returns the launcher instance.
	 * @throws CryptoException If the system encryption self test fails. See log/output for details.
	 * @throws NodeException If an issue with the launched node occurs.
	 * @throws IllegalArgumentException If an issue occurs with a configuration argument.
	 */
	public static L2pNodeLauncher launchConfiguration(L2pNodeLauncherConfiguration launcherConfiguration)
			throws CryptoException, NodeException, IllegalArgumentException {
		System.setSecurityManager(new L2pSecurityManager()); // ENABLES SANDBOXING!!!
		// check configuration
		String logDir = launcherConfiguration.getLogDir();
		if (logDir != null) {
			try {
				L2pLogger.setGlobalLogDirectory(logDir);
			} catch (Exception ex) {
				throw new IllegalArgumentException("Couldn't use '" + logDir + "' as log directory.", ex);
			}
		}
		// in debug replace null node id seed with random seed
		if (launcherConfiguration.isDebugMode() && launcherConfiguration.getNodeIdSeed() == null) {
			launcherConfiguration.setNodeIdSeed(new Random().nextLong());
		}
		Set<String> serviceDirectories = launcherConfiguration.getServiceDirectories();
		if (serviceDirectories == null) {
			HashSet<String> directories = new HashSet<>();
			directories.add(DEFAULT_SERVICE_DIRECTORY);
			serviceDirectories = directories;
		}
		// instantiate launcher
		L2pClassManager cl = new L2pClassManager(new FileSystemRepository(serviceDirectories, true),
				L2pNodeLauncher.class.getClassLoader());
		L2pNodeLauncher launcher = new L2pNodeLauncher(launcherConfiguration.getPort(),
				launcherConfiguration.getBootstrap(), launcherConfiguration.getStorageMode(),
				launcherConfiguration.useMonitoringObserver(), cl, launcherConfiguration.getNodeIdSeed());
		// check special commands
		if (launcherConfiguration.isPrintHelp()) {
			launcher.bFinished = true;
			printHelp();
			return launcher;
		} else if (launcherConfiguration.isPrintVersion()) {
			launcher.bFinished = true;
			printVersion();
			return launcher;
		}
		// handle commands
		if (launcherConfiguration.isDebugMode()) {
			System.err.println("WARNING! Launching node in DEBUG mode! THIS NODE IS NON PERSISTENT!");
		}
		try {
			launcher.start();
			for (String command : launcherConfiguration.getCommands()) {
				System.out.println("Handling: '" + command + "'");
				launcher.commandPrompt.handleLine(command);
			}
			if (launcher.isFinished()) {
				printMessage("All commands have been handled and shutdown has been called -> end!");
			} else {
				printMessage("All commands have been handled, but not finished yet -> keeping node open!");
			}
			return launcher;
		} catch (NodeException e) {
			launcher.bFinished = true;
			logger.printStackTrace(e);
			throw e;
		}
	}

	/**
	 * Prints a help message for command line usage.
	 * 
	 */
	public static void printHelp() {
		System.out.println("las2peer Node Launcher");
		System.out.println("----------------------");
		System.out.println("Usage:");
		System.out.println("  java -cp lib/* " + L2pNodeLauncher.class.getCanonicalName() + " ["
				+ L2pNodeLauncherConfiguration.ARG_HELP + "|" + L2pNodeLauncherConfiguration.ARG_SHORT_HELP + "] ["
				+ L2pNodeLauncherConfiguration.ARG_VERSION + "|" + L2pNodeLauncherConfiguration.ARG_SHORT_VERSION
				+ "] [" + L2pNodeLauncherConfiguration.ARG_COLORED_SHELL + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_COLORED_SHELL
				+ "] [Node Argument ...] [Launcher Method ...]\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_HELP + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_HELP + "\t\t\t\tprints the help message and exits");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_VERSION + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_VERSION + "\t\t\t\tprints the version information and exits");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_COLORED_SHELL + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_COLORED_SHELL
				+ "\t\t\tenables colored output (better readable command line)");
		System.out.println("\nNode Arguments:");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_DEBUG + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_DEBUG
				+ "\t\t\tstarts the node in debug mode. This means the node will listen and accept connections only\n"
				+ "\t\t\t\t\tfrom localhost, has a operating system defined port and uses a non persistent storage mode.\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_PORT + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_PORT + " port\t\t\tspecifies the port number of the node\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_BOOTSTRAP + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_BOOTSTRAP
				+ " address|ip:port,...\trequires a comma seperated list of [address|ip:port] pairs of bootstrap nodes to connect to.");
		System.out.println("  no bootstrap argument states, that a complete new las2peer network is to start\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_LOG_DIRECTORY + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_LOG_DIRECTORY
				+ " directory\t\tlets you choose the directory for log files (default: "
				+ L2pLogger.DEFAULT_LOG_DIRECTORY + ")\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_SERVICE_DIRECTORY + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_SERVICE_DIRECTORY
				+ " directory\tadds the directory to the service class loader. This argument can occur multiple times.\n");
		System.out.println(
				"  " + L2pNodeLauncherConfiguration.ARG_OBSERVER + "|" + L2pNodeLauncherConfiguration.ARG_SHORT_OBSERVER
						+ "\t\t\t\tstarts a monitoring observer at this node\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_NODE_ID_SEED + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_NODE_ID_SEED
				+ " long\t\tgenerates the (random) node id by using this seed\n");
		System.out.println("  " + L2pNodeLauncherConfiguration.ARG_STORAGE_MODE + "|"
				+ L2pNodeLauncherConfiguration.ARG_SHORT_STORAGE_MODE + " mode\t\tsets Pastry's storage mode\n"
				+ "\t\t\t\t\tSupported Modes: "
				+ String.join(", ", Stream.of(STORAGE_MODE.values()).map(Enum::name).collect(Collectors.toList()))
				+ "\n");

		System.out.println("Launcher Methods:");
		System.out.println("The following methods can be used in arbitrary order and number:");

		for (Method m : L2pNodeLauncher.class.getMethods()) {
			if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
				System.out.print("  " + m.getName());
				for (int i = 0; i < m.getParameterTypes().length; i++) {
					System.out.print(" " + m.getParameterTypes()[i].getName() + " ");
				}
				System.out.print("\n");
			}
		}
	}

	/**
	 * Gets the las2peer version as String.
	 * 
	 * @return Returns the las2peer version as "major.minor.build" or "DEBUG" if not set.
	 */
	public static String getVersion() {
		Package p = L2pNodeLauncher.class.getPackage();
		String version = p.getImplementationVersion();
		if (version != null) {
			return version;
		} else {
			return "DEBUG";
		}
	}

	/**
	 * Prints the las2peer version.
	 */
	public static void printVersion() {
		System.out.println("las2peer version \"" + getVersion() + "\"");
	}

	/**
	 * Main method for command line processing.
	 * 
	 * The method will start a node and try to invoke all command line parameters as parameterless methods of this
	 * class.
	 * 
	 * @param argv
	 */
	public static void main(String[] argv) {
		try {
			// self test system encryption
			try {
				CryptoTools.encryptSymmetric("las2peer rulez!".getBytes(), CryptoTools.generateSymmetricKey());
			} catch (CryptoException e) {
				throw new CryptoException(
						"Fatal Error! Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files are not installed!",
						e);
			}
			// Launches the node
			L2pNodeLauncher launcher = launchConfiguration(L2pNodeLauncherConfiguration.createFromMainArgs(argv));
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
		} catch (CryptoException e) {
			e.printStackTrace();
		} catch (NodeException e) {
			// exception already logged
			System.exit(2);
		}
	}
}
