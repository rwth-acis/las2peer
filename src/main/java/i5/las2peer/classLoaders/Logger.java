package i5.las2peer.classLoaders;

import i5.las2peer.logging.L2pLogger;

public class Logger {

	private static final L2pLogger logger = L2pLogger.INSTANCE;

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

	static void logFinding(ClassLoader who, String classname, Boolean success) {
		String whoString = getWhoString(who);
		logger.finer("f\t" + success + "\t" + whoString + "\t" + classname);
	}

	static void logMessage(ClassLoader who, String classname, String message) {
		String whoString = getWhoString(who);
		logger.finer("f\t" + "\t" + whoString + "\t" + classname + "\t" + message);
	}

	static void logLoading(ClassLoader who, String classname, Boolean success, Object add) {
		String whoString = getWhoString(who);
		logger.finer("l\t" + success + "\t" + whoString + "\t" + classname + "\t" + add);
	}

	static void logSubLibrary(ClassLoader who, LibraryClassLoader libraryLoader) {
		String whoString = getWhoString(who);
		logger.finer("library load\t" + whoString + "\t" + libraryLoader.getLibrary().getIdentifier());
	}

	private static String getWhoString(ClassLoader cl) {
		if (cl instanceof LibraryClassLoader) {
			return "lcl" + "\t" + ((LibraryClassLoader) cl).getLibrary().getIdentifier();
		} else if (cl instanceof BundleClassLoader) {
			return "bcl" + "\t" + ((BundleClassLoader) cl).getMainLibraryIdentifier();
		} else if (cl instanceof L2pClassLoader) {
			return "l2p-main" + "\t\t";
		} else {
			return "unkown";
		}
	}

}
