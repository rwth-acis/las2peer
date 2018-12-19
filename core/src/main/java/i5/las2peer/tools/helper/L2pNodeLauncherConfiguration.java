package i5.las2peer.tools.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;

public class L2pNodeLauncherConfiguration {

	public static final String DEFAULT_PROPERTIES_FILENAME = "etc/launcher-configuration.ini";

	public static final String ARG_HELP = "--help";
	public static final String ARG_SHORT_HELP = "-h";

	public static final String ARG_VERSION = "--version";
	public static final String ARG_SHORT_VERSION = "-v";

	// not used anymore
	public static final String ARG_WINDOWS_SHELL = "--windows-shell";
	public static final String ARG_SHORT_WINDOWS_SHELL = "-w";

	public static final String ARG_COLORED_SHELL = "--colored-shell";
	public static final String ARG_SHORT_COLORED_SHELL = "-c";

	public static final String ARG_DEBUG = "--debug";
	public static final String ARG_SHORT_DEBUG = "-debug";

	public static final String ARG_SHORT_BIND_ADDRESS = "-if";
	public static final String ARG_BIND_ADDRESS = "--bind-address";

	public static final String ARG_SHORT_PORT = "-p";
	public static final String ARG_PORT = "--port";

	public static final String ARG_SHORT_BOOTSTRAP = "-b";
	public static final String ARG_BOOTSTRAP = "--bootstrap";

	public static final String ARG_SHORT_OBSERVER = "-o";
	public static final String ARG_OBSERVER = "--observer";

	public static final String ARG_SHORT_LOG_DIRECTORY = "-l";
	public static final String ARG_LOG_DIRECTORY = "--log-directory";

	public static final String ARG_SHORT_NODE_ID_SEED = "-n";
	public static final String ARG_NODE_ID_SEED = "--node-id-seed";

	public static final String ARG_SHORT_SERVICE_DIRECTORY = "-s";
	public static final String ARG_SERVICE_DIRECTORY = "--service-directory";

	public static final String ARG_SHORT_STORAGE_MODE = "-m";
	public static final String ARG_STORAGE_MODE = "--storage-mode";

	public static final String ARG_SHORT_STORAGE_DIRECTORY = "-sd";
	public static final String ARG_STORAGE_DIRECTORY = "--storage-directory";

	public static final String ARG_SANDBOX = "--sandbox";
	public static final String ARG_SHORT_SANDBOX = "-sb";

	public static final String ARG_ETHEREUM_MNEMONIC = "--ethereum-mnemonic";

	public static final String ARG_ETHEREUM_PASSWORD = "--ethereum-password";

	private static final L2pLogger logger = L2pLogger.getInstance(L2pNodeLauncherConfiguration.class);

	private String filename;

	// special options - must be read and handled before regular options
	private boolean printHelp;
	private boolean printVersion;
	private boolean coloredOutput;

	// regular options
	private boolean debugMode;
	private InetAddress bindAddress;
	private Integer port;
	private List<String> bootstrap;
	private STORAGE_MODE storageMode;
	private String storageDirectory;
	private Boolean useMonitoringObserver;
	private String logDir;
	private final Set<String> serviceDirectories = new HashSet<>();
	private Long nodeIdSeed;
	private final List<String> commands = new LinkedList<>();
	private boolean sandbox;
	private String ethereumMnemonic;
	private String ethereumPassword;

	public L2pNodeLauncherConfiguration() {
		// set default values
	}

	/**
	 * Usually the OS splits up the parameters on spaces. This breaks commands like startService(xxx, yyy), which may
	 * use spaces to separate their own arguments.
	 * 
	 * This method uses the given brakets to join system arguments like [startService(xxx,] and [yyy)] into
	 * [startService(xxx, yyy)].
	 * 
	 * Usually it should be used in main(String[] args) methods.
	 * 
	 * @param argv
	 * @return Returns the configuration created from given args
	 */
	public static L2pNodeLauncherConfiguration createFromMainArgs(String... argv) {
		L2pNodeLauncherConfiguration result = new L2pNodeLauncherConfiguration();
		result.setFromMainArgs(argv);
		return result;
	}

	public void setFromMainArgs(String... argv) {
		List<String> argList = new ArrayList<>();
		String joined = "";
		for (String arg : argv) {
			int opening = arg.length() - arg.replace("(", "").length(); // nice way to count opening brackets
			int closing = arg.length() - arg.replace(")", "").length();
			if (opening == closing && joined.isEmpty()) {
				// just an argument
				argList.add(arg);
			} else {
				// previous arg was unbalanced, attach this arg
				joined += arg;
				int openingJoined = joined.length() - joined.replace("(", "").length();
				int closingJoined = joined.length() - joined.replace(")", "").length();
				if (openingJoined == closingJoined) {
					// now it's balanced
					argList.add(joined);
					joined = "";
				} else if (openingJoined < closingJoined) {
					throw new IllegalArgumentException("command \"" + joined + "\" has too many closing brackets!");
				} // needs more args to balance
			}
		}
		if (!joined.isEmpty()) {
			throw new IllegalArgumentException("command \"" + joined + "\" has too many opening brackets!");
		}
		setFromIterableArgs(argList);
	}

	public static L2pNodeLauncherConfiguration createFromArrayArgs(String... args) {
		return createFromIterableArgs(Arrays.asList(args));
	}

	/**
	 * Creates a launch configuration for the {@link i5.las2peer.tools.L2pNodeLauncher} from the given bunch of
	 * arguments.
	 * 
	 * @param args A bunch of arguments that should be used as configuration values.
	 * @return Returns the configuration.
	 * @throws IllegalArgumentException If an issue occurs parsing an argument.
	 */
	public static L2pNodeLauncherConfiguration createFromIterableArgs(Iterable<String> args)
			throws IllegalArgumentException {
		L2pNodeLauncherConfiguration result = new L2pNodeLauncherConfiguration();
		result.setFromIterableArgs(args);
		return result;
	}

	/**
	 * Sets launch configuration from the given bunch of arguments.
	 * 
	 * @param args A bunch of arguments that should be used as configuration values.
	 * @throws IllegalArgumentException If an issue occurs parsing an argument.
	 */
	public void setFromIterableArgs(Iterable<String> args) throws IllegalArgumentException {
		Iterator<String> itArg = args.iterator();
		while (itArg.hasNext()) {
			String arg = itArg.next();
			if (arg.equalsIgnoreCase(ARG_SHORT_HELP) || arg.equalsIgnoreCase(ARG_HELP)) {
				setPrintHelp(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_VERSION) || arg.equalsIgnoreCase(ARG_VERSION)) {
				setPrintVersion(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_WINDOWS_SHELL) || arg.equalsIgnoreCase(ARG_WINDOWS_SHELL)) {
				throw new IllegalArgumentException(
						"Illegal argument '" + arg + "', because colored output is disabled by default");
			} else if (arg.equalsIgnoreCase(ARG_SHORT_COLORED_SHELL) || arg.equalsIgnoreCase(ARG_COLORED_SHELL)) {
				setColoredOutput(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_DEBUG) || arg.equalsIgnoreCase(ARG_DEBUG)) {
				setDebugMode(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_SANDBOX) || arg.equalsIgnoreCase(ARG_SANDBOX)) {
				setSandbox(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_BIND_ADDRESS) || arg.equalsIgnoreCase(ARG_BIND_ADDRESS)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because ip address expected after it");
				} else {
					String sBindAddress = itArg.next();
					try {
						InetAddress addr = InetAddress.getByName(sBindAddress);
						// in case of an exception this structure doesn't override an already set value
						setBindAddress(addr);
					} catch (Exception ex) {
						throw new IllegalArgumentException(
								"Illegal argument '" + arg + "', because " + sBindAddress + " is not an ip address");
					}

				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_PORT) || arg.equalsIgnoreCase(ARG_PORT)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because port number expected after it");
				} else {
					String sPort = itArg.next();
					try {
						int p = Integer.valueOf(sPort);
						// in case of an exception this structure doesn't override an already set port number
						setPort(p);
					} catch (NumberFormatException ex) {
						throw new IllegalArgumentException(
								"Illegal argument '" + arg + "', because " + sPort + " is not an integer");
					}
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_BOOTSTRAP) || arg.equalsIgnoreCase(ARG_BOOTSTRAP)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because comma separated bootstrap list expected after it");
				} else {
					addBootstrap(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_OBSERVER) || arg.equalsIgnoreCase(ARG_OBSERVER)) {
				setUseMonitoringObserver(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_LOG_DIRECTORY) || arg.equalsIgnoreCase(ARG_LOG_DIRECTORY)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because log directory expected after it");
				} else {
					setLogDir(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_NODE_ID_SEED) || arg.equalsIgnoreCase(ARG_NODE_ID_SEED)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because node id seed expected after it");
				} else {
					String sNodeId = itArg.next();
					try {
						long idSeed = Long.valueOf(sNodeId);
						// in case of an exception this structure doesn't override an already set node id seed
						setNodeIdSeed(idSeed);
					} catch (NumberFormatException ex) {
						throw new IllegalArgumentException(
								"Illegal argument '" + arg + "', because '" + sNodeId + "' is not an integer");
					}
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_SERVICE_DIRECTORY)
					|| arg.equalsIgnoreCase(ARG_SERVICE_DIRECTORY)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because service directory expected after it");
				} else {
					getServiceDirectories().add(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_STORAGE_MODE) || arg.equalsIgnoreCase(ARG_STORAGE_MODE)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because storage mode expected after it");
				} else {
					String val = itArg.next();
					try {
						setStorageMode(STORAGE_MODE.valueOf(val.toUpperCase()));
						// remove only on successful storage mode parsing
					} catch (IllegalArgumentException e) {
						// not a valid storage mode
						throw new IllegalArgumentException(
								"Illegal argument '" + arg + "', because storage mode '" + val + "' is unknown");
					}
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_STORAGE_DIRECTORY)
					|| arg.equalsIgnoreCase(ARG_STORAGE_DIRECTORY)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because storage directory expected after it");
				} else {
					setStorageDirectory(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_ETHEREUM_MNEMONIC)) {
				if (!itArg.hasNext()) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because BIP39 Ethereum mnemonic expected after it");
				} else {
					setEthereumMnemonic(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_ETHEREUM_PASSWORD)) {
				if (!itArg.hasNext()) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because password for Ethereum mnemonic expected after it");
				} else {
					setEthereumPassword(itArg.next());
				}
			} else {
				getCommands().add(arg);
			}
		}
	}

	public void setFromFile() throws IOException {
		try {
			setFromFile(DEFAULT_PROPERTIES_FILENAME);
		} catch (FileNotFoundException e) {
			logger.log(Level.FINE, "Could not load default properties file", e);
		}
	}

	public void setFromFile(String filename) throws IOException {
		this.filename = filename;
		FileInputStream fis = new FileInputStream(filename);
		setFromInput(fis);
		fis.close();
	}

	public void setFromInput(InputStream inputStream) throws IOException {
		ConfigFile conf = new ConfigFile(inputStream);
		String strBindAddress = conf.get("bindAddress");
		if (strBindAddress != null) {
			setBindAddress(InetAddress.getByName(strBindAddress));
		}
		String strPort = conf.get("port");
		if (strPort != null) {
			setPort(Integer.valueOf(strPort));
		}
		if (this.bootstrap == null || this.bootstrap.isEmpty()) {
			List<String> bootstrap = conf.getAll("bootstrap");
			if (bootstrap != null) {
				setBootstrap(bootstrap);
			}
		}
		String strStorageMode = conf.get("storageMode");
		if (strStorageMode != null) {
			setStorageMode(STORAGE_MODE.valueOf(strStorageMode.toUpperCase()));
		}
		String strStorageDirectory = conf.get("storageDirectory");
		if (strStorageDirectory != null) {
			setStorageDirectory(strStorageDirectory);
		}
		String strUseMonitoringObserver = conf.get("useMonitoringObserver");
		if (strUseMonitoringObserver != null) {
			setUseMonitoringObserver(Boolean.valueOf(strUseMonitoringObserver));
		}
		String strLogDir = conf.get("logDir");
		if (strLogDir != null) {
			setLogDir(strLogDir);
		}
		List<String> strServiceDirectories = conf.getAll("serviceDirectories");
		if (strServiceDirectories != null) {
			getServiceDirectories().addAll(strServiceDirectories);
		}
		String strNodeIdSeed = conf.get("nodeIdSeed");
		if (strNodeIdSeed != null) {
			setNodeIdSeed(Long.valueOf(strNodeIdSeed));
		}
		String strEthereumMnemonic = conf.get("ethereumMnemonic");
		if (strEthereumMnemonic != null) {
			setEthereumMnemonic(strEthereumMnemonic);
		}
		String strEthereumPassword = conf.get("ethereumPassword");
		if (strEthereumPassword != null) {
			setEthereumPassword(strEthereumPassword);
		}
		List<String> commands = conf.getAll("commands");
		if (commands != null) {
			getCommands().addAll(commands);
		}
	}

	public void writeToFile() {
		if (filename == null) { // e. g. in tests
			logger.info("configuration file name is not set, configuration not written to file");
		} else {
			writeToFile(filename);
		}
	}

	public void writeToFile(String filename) {
		try {
			ConfigFile conf = new ConfigFile(filename);
			conf.put("port", getPort());
			conf.put("bootstrap", getBootstrap());
			conf.put("storageMode", getStorageMode());
			conf.put("storageDirectory", getStorageDirectory());
			conf.put("useMonitoringObserver", useMonitoringObserver());
			conf.put("logDir", getLogDir());
			conf.put("serviceDirectories", getServiceDirectories());
			conf.put("nodeIdSeed", getNodeIdSeed());
			conf.put("commands", getCommands());
			// auto create parent directory
			File parent = new File(filename).getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			// write configuration to file
			FileOutputStream fos = new FileOutputStream(filename);
			conf.store(fos);
			fos.close();
		} catch (IOException e) {
			logger.log(Level.INFO, "Could not write launcher configuration to file '" + filename + "'", e);
		}
	}

	public boolean isPrintHelp() {
		return printHelp;
	}

	public void setPrintHelp(boolean printHelp) {
		this.printHelp = printHelp;
	}

	public boolean isPrintVersion() {
		return printVersion;
	}

	public void setPrintVersion(boolean printVersion) {
		this.printVersion = printVersion;
	}

	public boolean isColoredOutput() {
		return coloredOutput;
	}

	public void setColoredOutput(boolean coloredOutput) {
		this.coloredOutput = coloredOutput;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	public void setSandbox(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public InetAddress getBindAddress() {
		return bindAddress;
	}

	public void setBindAddress(InetAddress bindAddress) {
		this.bindAddress = bindAddress;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public List<String> getBootstrap() {
		return bootstrap;
	}

	public void setBootstrap(List<String> bootstrap) {
		this.bootstrap = bootstrap;
	}

	public void setBootstrap(String bootstrap) {
		this.bootstrap = null;
		if (bootstrap != null) {
			addBootstrap(bootstrap);
		}
	}

	public void addBootstrap(String bootstrap) {
		if (this.bootstrap == null) {
			this.bootstrap = new LinkedList<>();
		}
		this.bootstrap.add(bootstrap);
	}

	public STORAGE_MODE getStorageMode() {
		return storageMode;
	}

	public void setStorageMode(STORAGE_MODE storageMode) {
		this.storageMode = storageMode;
	}

	public Boolean useMonitoringObserver() {
		return useMonitoringObserver != null && useMonitoringObserver;
	}

	public void setUseMonitoringObserver(boolean useMonitoringObserver) {
		this.useMonitoringObserver = useMonitoringObserver;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public Set<String> getServiceDirectories() {
		return serviceDirectories;
	}

	public Long getNodeIdSeed() {
		return nodeIdSeed;
	}

	public void setNodeIdSeed(Long nodeIdSeed) {
		this.nodeIdSeed = nodeIdSeed;
	}

	public List<String> getCommands() {
		return commands;
	}

	public String getStorageDirectory() {
		return storageDirectory;
	}

	public void setStorageDirectory(String storageDirectory) {
		this.storageDirectory = storageDirectory;
	}

	public String getEthereumMnemonic() {
		return ethereumMnemonic;
	}

	public void setEthereumMnemonic(String ethereumMnemonic) {
		this.ethereumMnemonic = ethereumMnemonic;
	}

	public String getEthereumPassword() {
		return ethereumPassword;
	}

	public void setEthereumPassword(String ethereumPassword) {
		this.ethereumPassword = ethereumPassword;
	}

}
