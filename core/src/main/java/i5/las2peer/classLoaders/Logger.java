package i5.las2peer.classLoaders;

import i5.las2peer.logging.L2pLogger;

public class Logger {

	private static final L2pLogger logger = L2pLogger.getInstance(Logger.class.getName());

	static {
		String logFile = System.getenv("CL_LOGFILE");
		if (logFile != null && !logFile.isEmpty()) {
			// TODO use a dedicated log file for classloading
			logger.warning(
					"Dedicated class loader log file is specified, but currently not implemented! Please see default log file for log output.");
//			try {
//				logStream = new PrintStream(new FileOutputStream(logFile));
//				System.out.println("Logging classloading to " + logFile);
//			} catch (FileNotFoundException e) {
//				System.out.println("Error opening cl logfile: " + e);
//			}
		}
	}

	static void logFinding(Object who, String classname, Boolean success) {
		String whoString = getWhoString(who);
		logger.finer("f\t" + success + "\t" + whoString + "\t" + classname);
	}

	static void logMessage(Object who, String classname, String message) {
		String whoString = getWhoString(who);
		logger.finer("f\t" + "\t" + whoString + "\t" + classname + "\t" + message);
	}

	static void logLoading(Object who, String classname, Boolean success) {
		String whoString = getWhoString(who);
		logger.finer("l\t" + success + "\t" + whoString + "\t" + classname);
	}

	static void logSubLibrary(Object who, ServiceClassLoader libraryLoader) {
		String whoString = getWhoString(who);
		logger.finer("library load\t" + whoString + "\t" + libraryLoader.getLibrary().getIdentifier());
	}

	private static String getWhoString(Object cl) {
		if (cl instanceof ServiceClassLoader) {
			return "lcl" + "\t" + ((ServiceClassLoader) cl).getLibrary().getIdentifier();
		} else if (cl instanceof ClassManager) {
			return "l2p-main" + "\t\t";
		} else {
			return "unkown";
		}
	}

	static void logGetResource(Object who, String resourceName, Boolean success, Boolean lookUp) {
		String whoString = getWhoString(who);
		logger.finer("l\t" + success + "\t" + whoString + "\t" + resourceName + "\t" + lookUp);
	}

}
