package i5.las2peer.tools;

import i5.las2peer.api.ConnectorException;
import i5.las2peer.api.Service;
import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.classLoaders.L2pClassLoader;
import i5.las2peer.classLoaders.libraries.FileSystemRepository;
import i5.las2peer.execution.L2pServiceException;
import i5.las2peer.httpConnector.HttpConnector;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A command line tool for starting services in a p2p environment.
 * 
 * See {@link #main} for parameter documentation.
 * 
 * This class is the main class of the las2peer library jar.
 * 
 * 
 * @author Holger Janssen
 * @version $Revision: 1.5 $, $Date: 2013/02/27 21:48:11 $
 * 
 */
public class ServiceStarter {

	public static final String DEFAULT_LOG = "./log/las2peer.log";
	public static final String DEFAULT_BOOTSTRAP = "127.0.0.1:9000";
	// public static final String DEFAULT_BOOTSTRAP =
	// "tosini.informatik.rwth-aachen.de:9085";

	public static final int DEFAULT_PORT = 9010;
	public static final String DEFAULT_PASTRY_DIR = ".pastry_data";
	public static final String DEFAULT_LIB = "./lib/";

	/**
	 * walk through the given parameter values, check all parameters and return
	 * a corresponding hashtable
	 * 
	 * @param argv
	 * 
	 * @return hashtable with parsed parameters
	 */
	@SuppressWarnings("unchecked")
	private static Hashtable<String, Object> parseParameters(String[] argv) {
		Hashtable<String, Object> result = new Hashtable<String, Object>();
		result.put("libdirs", new Vector<String>());

		Vector<String> serviceClasses = new Vector<String>();
		Vector<String> serviceXmls = new Vector<String>();
		Hashtable<String, String> htAgentPasses = new Hashtable<String, String>();

		int lauf = 0;
		while (lauf < argv.length) {
			if (argv[lauf].equals("-c")) {
				lauf++;
				if (serviceClasses.contains(argv[lauf]))
					throw new IllegalArgumentException(argv[lauf]
							+ " is given more than once!");
				serviceClasses.add(argv[lauf]);
				lauf++;
			} else if (argv[lauf].equals("-x")) {
				lauf++;
				if (serviceXmls.contains(argv[lauf]))
					throw new IllegalArgumentException(argv[lauf]
							+ " is given more than once!");
				htAgentPasses.put(argv[lauf], argv[lauf + 1]);
				serviceXmls.add(argv[lauf]);
				lauf += 2;
			} else if (argv[lauf].equals("-l")) {
				if (result.containsKey("logprefix"))
					throw new IllegalArgumentException(
							"-l given more than once!");
				result.put("logfile", argv[lauf + 1]);
				lauf += 2;
			} else if (argv[lauf].equals("-d")) {
				((Vector<String>) result.get("libdirs")).add(argv[lauf + 1]);
				lauf += 2;
			} else if (argv[lauf].equals("-b")) {
				if (result.containsKey("bootstrap"))
					throw new IllegalArgumentException(
							"-b given more than once!");
				result.put("bootstrap", argv[lauf + 1]);
				lauf += 2;
			} else if (argv[lauf].equals("-n")) {
				if (result.containsKey("pastryDir"))
					throw new IllegalArgumentException(
							"-n given more than once!");
				result.put("pastryDir", argv[lauf + 1]);
				lauf += 2;
			} else if (argv[lauf].equals("-p")) {
				if (result.containsKey("port"))
					throw new IllegalArgumentException(
							"-p given more than once!");
				result.put("port", Integer.parseInt(argv[lauf + 1]));
				lauf += 2;
			} else if (argv[lauf].equals("-http")) {
				if (serviceClasses.contains("startHttp"))
					throw new IllegalArgumentException(argv[lauf]
							+ " is given more than once!");

				lauf++;
				int port = 8080;
				if (lauf < (argv.length) && !argv[lauf].startsWith("-")) {
					port = Integer.valueOf(argv[lauf]);
					lauf++;
				}
				result.put("startHttp", port);
			} else {
				printHelp("unkown parameter: " + argv[lauf]);
				System.exit(10);
			}
		}

		result.put("serviceClasses", serviceClasses);
		result.put("serviceXmls", serviceXmls);
		result.put("agentPasses", htAgentPasses);

		if (serviceClasses.size() + serviceXmls.size() == 0)
			throw new IllegalArgumentException(
					"You have to give at least one -x or -c parameter to start a service node!");

		return result;
	}

	/**
	 * set the default values for all missing parameters
	 * 
	 * @param parameters
	 */
	@SuppressWarnings("unchecked")
	private static void addDefaultValues(Hashtable<String, Object> parameters) {
		if (!parameters.containsKey("bootstrap"))
			parameters.put("bootstrap", DEFAULT_BOOTSTRAP);
		if (!parameters.containsKey("port"))
			parameters.put("port", DEFAULT_PORT);
		if (!parameters.containsKey("pastryDir"))
			parameters.put("pastryDir", DEFAULT_PASTRY_DIR);
		if (!parameters.containsKey("libdirs")
				|| ((Vector<String>) parameters.get("libdirs")).size() == 0) {
			Vector<String> libdirs = new Vector<String>();
			libdirs.add(DEFAULT_LIB);
			parameters.put("libdirs", libdirs);
		}
		if (!parameters.containsKey("logprefix"))
			parameters.put("logprefix", DEFAULT_LOG);
	}

	/**
	 * print an error message to standard error and the normal help message to
	 * standard out
	 * 
	 * @param message
	 */
	private static void printHelp(String message) {
		System.err.println(message + "\n");
		printHelp();
	}

	/**
	 * print a simple help message to standard out
	 */
	private static void printHelp() {
		System.out
				.println("Start a node hosting one or more services. "
						+ "At least one of the parameters -x or -c is needed. "
						+ "Optional parameters are -p, -d, -l, -n, -b and -http. See the Java Docs of the "
						+ "i5.las2peer.tools.ServiceStarter class for further information.");

	}

	/**
	 * set up the classloader
	 * 
	 * @return a class loader looking into the given directories
	 * 
	 */
	private static L2pClassLoader setupClassLoader(Vector<String> libDirs) {
		return new L2pClassLoader(new FileSystemRepository(
				libDirs.toArray(new String[0])),
				ServiceStarter.class.getClassLoader());
	}

	/**
	 * register all given agents to the running las2peer node
	 * 
	 * @param node
	 * @param agents
	 * 
	 * @throws L2pServiceException
	 */
	private static void launchAgents(PastryNodeImpl node,
			Vector<ServiceAgent> agents) throws L2pServiceException {

		for (ServiceAgent agent : agents) {
			try {
				node.registerReceiver(agent);

				System.out.println("registered Agent " + agent.getId());
				if (agent instanceof ServiceAgent)
					System.out.println(" -> Service: "
							+ ((ServiceAgent) agent).getServiceClassName());
			} catch (Exception e) {
				e.printStackTrace();
				throw new L2pServiceException(
						"Error registering the service agent to the node! "
								+ e.getMessage());
			}
		}
	}

	/**
	 * start a pastry node implementation of a las2peer network node
	 * 
	 * @param cl
	 * 
	 * @param parameters
	 * 
	 * @return a running node
	 * @throws NodeException
	 */
	private static PastryNodeImpl startNode(L2pClassLoader cl,
			Hashtable<String, Object> parameters) throws NodeException {
		String bootstrap = (String) parameters.get("bootstrap");
		int port = (Integer) parameters.get("port");

		PastryNodeImpl node = new PastryNodeImpl(cl, port, bootstrap);
		node.setLogfilePrefix((String) parameters.get("logprefix"));

		node.launch();

		return node;
	}

	/**
	 * create new service agents for the given service classes and store them as
	 * [class].agent.xml files in the current directory.
	 * 
	 * The corresponding passphrases will be collected in the file
	 * passphrases.txt
	 * 
	 * @param cl
	 * 
	 * @param vector
	 * @return
	 * @throws AgentException
	 */
	private static Vector<ServiceAgent> createAgents(L2pClassLoader cl,
			Vector<String> vector) throws AgentException {
		if (vector.size() == 0)
			return new Vector<ServiceAgent>();

		Vector<ServiceAgent> result = new Vector<ServiceAgent>();

		Hashtable<String, String> passphrases = new Hashtable<String, String>();

		StringBuffer passphraseOut = new StringBuffer();

		for (String clsName : vector) {
			try {
				cl.registerService(clsName);

				@SuppressWarnings("unchecked")
				Class<? extends Service> cls = (Class<? extends Service>) cl
						.getServiceClass(clsName);

				String passphrase = SimpleTools.createRandomString(20);
				ServiceAgent agent = ServiceAgent.generateNewAgent(
						cls.getName(), passphrase);

				passphrases.put(clsName, passphrase);

				int counter = 0;
				File agentFile = new File(clsName + ".agent.xml");
				while (agentFile.exists()) {
					agentFile = new File(clsName + ".agent." + counter + ".xml");
					counter++;
				}

				// write agent XML file
				PrintStream ps = new PrintStream(
						new FileOutputStream(agentFile));
				ps.print(agent.toXmlString());
				ps.close();

				System.out.println("Written " + agentFile);

				passphraseOut.append(agentFile + "\t:\t" + passphrase + "\n");

				agent.unlockPrivateKey(passphrase);
				result.add(agent);
			} catch (ClassCastException e) {
				throw new AgentException("clsName is not a Service class!");
			} catch (ClassLoaderException e) {
				e.printStackTrace();
				throw new AgentException("Service class " + clsName
						+ " cannot be found: " + e.getMessage());
			} catch (L2pSecurityException e) {
				throw new AgentException(
						"Strange key problems while generating agent for service "
								+ clsName);
			} catch (CryptoException e) {
				throw new AgentException(
						"Strange crypto problems while generating agent for service "
								+ clsName);
			} catch (Exception e) {
				e.printStackTrace();
				throw new AgentException("Unable to write agent file for "
						+ clsName + ": " + e.getMessage());
			}
		}

		// write passphrase file
		File passFile = new File("passphrases.txt");
		int counter = 0;
		while (passFile.exists()) {
			passFile = new File("passphrases." + counter + ".txt");
			counter++;
		}

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(passFile));
			ps.println(passphraseOut);
			ps.close();
		} catch (Exception e) {
			throw new AgentException("error writing the passphrase file "
					+ passFile + ": " + e.getMessage());
		}

		return result;
	}

	/**
	 * load and unlock the agents from the given XML files
	 * 
	 * @param xmlFiles
	 *            strings defining XML files containing service agent
	 *            information
	 * @param passphrases
	 *            passphrases for the service agents' keys
	 * 
	 * @return vector with all loaded and unlocked agents
	 * @throws AgentException
	 */
	private static Vector<ServiceAgent> loadAgents(Vector<String> xmlFiles,
			Hashtable<String, String> passphrases) throws AgentException {
		Vector<ServiceAgent> result = new Vector<ServiceAgent>();

		for (String file : xmlFiles) {
			try {
				ServiceAgent loaded = ServiceAgent
						.createFromXml(FileContentReader.read(file));
				loaded.unlockPrivateKey(passphrases.get(file));
				result.add(loaded);
				System.out.println("loaded agent from " + file);
			} catch (MalformedXMLException e) {
				throw new AgentException(
						"Xml format problems with agent XML file " + file);
			} catch (IOException e) {
				throw new AgentException("Error opening XML file " + file);
			} catch (L2pSecurityException e) {
				throw new AgentException(
						"Security exception unlocking agent file " + file
								+ " -- wrong passphrase?");
			}
		}

		return result;
	}

	/**
	 * Main method for command line usage.
	 * 
	 * Standard parameters are
	 * <table>
	 * <tr>
	 * <th>-c [classname]</th>
	 * <td>class name of a service to start, a new (temporary) agent will be
	 * generated and stored to class.agent.xml in the launch directory</td>
	 * <td>multiple</td>
	 * </tr>
	 * <tr>
	 * <td>-x [xmlfile] [passphrase]</th>
	 * <td>Name of an XML file containing a
	 * {@link i5.las2peer.security.ServiceAgent} immediately followed by the
	 * passphrase to unlock the private key of the service agent
	 * <td>
	 * <td>multiple</td>
	 * </tr>
	 * </table>
	 * 
	 * At least one value for -c or -x needs to be given.
	 * 
	 * Optional parameters are:
	 * 
	 * <table>
	 * <tr>
	 * <th>parameter</th>
	 * <th>default</th>
	 * <th>how often</th>
	 * <th>description</th>
	 * </tr>
	 * <tr>
	 * <th>-l [file prefix]</th>
	 * <td>./log/las2peer-node.</td>
	 * <td>once</td>
	 * <td>prefix for the log file generated by the las2peer node, the prefix
	 * will be followed by the ID of the pastry node in the P2P network</td>
	 * </tr>
	 * <tr>
	 * <th>-d [dirname]</th>
	 * <td>./lib/</td>
	 * <td>once</td>
	 * <td>name of the directory for the class loader library</td>
	 * </tr>
	 * <tr>
	 * <th>-b [bootstrap]</th>
	 * <td>somehost:9000</td>
	 * <td>once</td>
	 * <td>Bootstrap to connect to</td>
	 * </tr>
	 * <tr>
	 * <th>-n [dirname]</th>
	 * <td>.pastry_data</td>
	 * <td>once</td>
	 * <td>node temporary directory</td>
	 * </tr>
	 * <tr>
	 * <th>-p [portnumber]</th>
	 * <td>9000</td>
	 * <td>once</td>
	 * <td>port to listen to</td>
	 * <tr>
	 * <tr>
	 * <th>-http [portnumber]</th>
	 * <td>--</td>
	 * <td>once</td>
	 * <td>Start the http connector at the given port. The port number is
	 * optional. If none is given, the connector will be started at port 8080.</td>
	 * </tr>
	 * </table>
	 * 
	 * @param argv
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] argv) {
		try {
			Hashtable<String, Object> parameters = parseParameters(argv);
			addDefaultValues(parameters);

			checkClasses();

			L2pClassLoader cl = setupClassLoader((Vector<String>) parameters
					.get("libdirs"));

			Vector<ServiceAgent> agentsFromXml = loadAgents(
					(Vector<String>) parameters.get("serviceXmls"),
					(Hashtable<String, String>) parameters.get("agentPasses"));
			Vector<ServiceAgent> newAgents = createAgents(cl,
					(Vector<String>) parameters.get("serviceClasses"));

			PastryNodeImpl n = startNode(cl, parameters);

			launchAgents(n, agentsFromXml);
			launchAgents(n, newAgents);

			System.out.println("node has been started! ...\n\n");

			if (parameters.containsKey("startHttp"))
				startHttpConnector(n, (Integer) parameters.get("startHttp"));

			try {
				Thread.sleep(5000);

				System.out.println("testing eve retrieval");
				Agent eve = n.getAgent(MockAgentFactory.getEve().getId());

				System.out.println("successfully fetched eve: " + eve);
			} catch (Exception e) {
			}

		} catch (IllegalArgumentException e) {
			printHelp(e.getMessage());
			System.exit(10);
		} catch (L2pServiceException e) {
			printHelp(e.getMessage());
			System.exit(200);
		} catch (AgentException e) {
			printHelp(e.getMessage());
			System.exit(10);
		} catch (NodeException e) {
			printHelp("Error starting the las2peer node: " + e.getMessage());
			System.exit(100);
		}

	}

	/**
	 * check for needed java classes or libraries
	 */
	private static void checkClasses() {
		String[] neededJars = new String[] { "simpleXML-0.1.jar",
				"xpp3-1.1.4.jar", "FreePastry-2.1.jar", "commonsCodec-1.7.jar" };

		boolean check = true;
		for (String checkJar : neededJars) {
			if (!new File(checkJar).exists()) {
				System.err.println("Warning: needed jar " + checkJar
						+ " not found!");
				check = false;
			}
		}

		if (check)
			return;

		String[] testClasses = new String[] { "rice.p2p.past.Past",
				"org.apache.commons.codec.binary.Base64",
				"i5.simpleXML.Parser", "org.xmlpull.v1.XmlPullParser" };

		for (String testClass : testClasses) {
			try {
				Class.forName(testClass);
			} catch (ClassNotFoundException e) {
				printHelp("Attention: either make sure, that all needed jars ("
						+ SimpleTools.join(neededJars, ", ")
						+ ") are available either in the execution directory or in the classpath");
				System.exit(10);
			}
		}

	}

	private static void startHttpConnector(Node node, final int iPort) {

		try {
			System.out.println("Starting Http Connector!");
			final HttpConnector connector = new HttpConnector();
			connector.setHttpPort(iPort);
			connector.start(node);

			System.out.println(" -> waiting a little");

			try {
				System.in.read();
			} catch (IOException e) {
			}

		} catch (FileNotFoundException e) {
			System.out.println(" --> Error finding connector logfile!" + e);
		} catch (ConnectorException e) {
			System.out.println(" --> problems starting the connector: " + e);
		}

	}

}
