package i5.las2peer.tools.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;

public class L2pNodeLauncherConfiguration {

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

	// special options - must be read and handled before regular options
	private boolean printHelp;
	private boolean printVersion;
	private boolean coloredOutput;

	// regular options
	private boolean debugMode;
	private Integer port;
	private String bootstrap;
	private STORAGE_MODE storageMode;
	private String storageDirectory;
	private boolean useMonitoringObserver;
	private String logDir;
	private final Set<String> serviceDirectories = new HashSet<>();
	private Long nodeIdSeed;
	private final List<String> commands = new LinkedList<>();

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
	 * @return
	 */
	public static L2pNodeLauncherConfiguration createFromMainArgs(String... argv) {
		List<String> result = new ArrayList<>();
		String joined = "";
		for (String arg : argv) {
			int opening = arg.length() - arg.replace("(", "").length(); // nice way to count opening brackets
			int closing = arg.length() - arg.replace(")", "").length();
			if (opening == closing && joined.isEmpty()) {
				// just an argument
				result.add(arg);
			} else {
				// previous arg was unbalanced, attach this arg
				joined += arg;
				int openingJoined = joined.length() - joined.replace("(", "").length();
				int closingJoined = joined.length() - joined.replace(")", "").length();
				if (openingJoined == closingJoined) {
					// now it's balanced
					result.add(joined);
					joined = "";
				} else if (openingJoined < closingJoined) {
					throw new IllegalArgumentException("command \"" + joined + "\" has too many closing brackets!");
				} // needs more args to balance
			}
		}
		if (!joined.isEmpty()) {
			throw new IllegalArgumentException("command \"" + joined + "\" has too many opening brackets!");
		}
		return createFromIterableArgs(result);
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
		Iterator<String> itArg = args.iterator();
		while (itArg.hasNext()) {
			String arg = itArg.next();
			if (arg.equalsIgnoreCase(ARG_SHORT_HELP) || arg.equalsIgnoreCase(ARG_HELP)) {
				result.setPrintHelp(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_VERSION) || arg.equalsIgnoreCase(ARG_VERSION)) {
				result.setPrintVersion(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_WINDOWS_SHELL) || arg.equalsIgnoreCase(ARG_WINDOWS_SHELL)) {
				throw new IllegalArgumentException(
						"Illegal argument '" + arg + "', because colored output is disabled by default");
			} else if (arg.equalsIgnoreCase(ARG_SHORT_COLORED_SHELL) || arg.equalsIgnoreCase(ARG_COLORED_SHELL)) {
				result.setColoredOutput(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_DEBUG) || arg.equalsIgnoreCase(ARG_DEBUG)) {
				result.setDebugMode(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_PORT) || arg.equalsIgnoreCase(ARG_PORT)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because port number expected after it");
				} else {
					String sPort = itArg.next();
					try {
						int p = Integer.valueOf(sPort);
						// in case of an exception this structure doesn't override an already set port number
						result.setPort(p);
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
					result.setBootstrap(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_OBSERVER) || arg.equalsIgnoreCase(ARG_OBSERVER)) {
				result.setUseMonitoringObserver(true);
			} else if (arg.equalsIgnoreCase(ARG_SHORT_LOG_DIRECTORY) || arg.equalsIgnoreCase(ARG_LOG_DIRECTORY)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because log directory expected after it");
				} else {
					result.setLogDir(itArg.next());
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
						result.setNodeIdSeed(idSeed);
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
					result.getServiceDirectories().add(itArg.next());
				}
			} else if (arg.equalsIgnoreCase(ARG_SHORT_STORAGE_MODE) || arg.equalsIgnoreCase(ARG_STORAGE_MODE)) {
				if (itArg.hasNext() == false) {
					throw new IllegalArgumentException(
							"Illegal argument '" + arg + "', because storage mode expected after it");
				} else {
					String val = itArg.next();
					try {
						result.setStorageMode(STORAGE_MODE.valueOf(val.toUpperCase()));
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
					result.setStorageDirectory(itArg.next());
				}
			} else {
				result.getCommands().add(arg);
			}
		}
		return result;
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

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getBootstrap() {
		return bootstrap;
	}

	public void setBootstrap(String bootstrap) {
		this.bootstrap = bootstrap;
	}

	public STORAGE_MODE getStorageMode() {
		return storageMode;
	}

	public void setStorageMode(STORAGE_MODE storageMode) {
		this.storageMode = storageMode;
	}

	public boolean useMonitoringObserver() {
		return useMonitoringObserver;
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

}
