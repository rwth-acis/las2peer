package i5.las2peer.logging;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class L2pLogger {

	// any changes to these parameters require a restart
	public static final int DEFAULT_LIMIT_BYTES = 10000; // max 10 kB log file size
	private static int limitBytes = DEFAULT_LIMIT_BYTES;
	public static final int DEFAULT_LIMIT_FILES = 5; // max 5 log files in rotation
	private static int limitFiles = DEFAULT_LIMIT_FILES;

	// if this instance is not used, the L2pLogger may not be initialized!
	public static final Logger INSTANCE = Logger.getLogger("i5.las2peer");

	private static final ConsoleHandler handlerConsole = new ConsoleHandler();
	private static FileHandler handlerFile;
	private static String strLogDir = "";
	private static String strPattern;

	static public void init() {
		// TODO read properties file from ./etc/logging.properties file
		// suppress default console logging
		Logger rootLogger = Logger.getLogger("");
		for (Handler handler : rootLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				rootLogger.removeHandler(handler);
			}
		}
		// console logging
		// default level: INFO
		try {
			handlerConsole.setEncoding("UTF-8");
		} catch (SecurityException | UnsupportedEncodingException e) {
			System.err.println("Fatal Error! Can't set console log encoding to UTF-8! Using default: "
					+ handlerConsole.getEncoding());
		}
		handlerConsole.setFormatter(new ConsoleFormatter());
		INSTANCE.addHandler(handlerConsole);
		updateLogLevel();
	}

	private static class ConsoleFormatter extends Formatter {

		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			// actual message
			// TODO colored output
			sb.append(record.getMessage()).append("\n");
			// stack trace
			printStackTrace(sb, record.getThrown());
			return sb.toString();
		}

	}

	/**
	 * Sets the directory to store log files.
	 * 
	 * @param directory A directory path given as String. {@code null} is equal to "" and the class loader directory.
	 */
	public static void setLogDirectory(String directory) {
		if (directory == null) {
			strLogDir = "";
		} else {
			strLogDir = directory;
			if (!strLogDir.endsWith(File.separator)) {
				strLogDir = directory + File.separator;
			}
		}
		updateLogFileHandler();
	}

	/**
	 * Sets the prefix used to generate log files.
	 * 
	 * @param prefix If {@code null} is given, file logging will be disabled.
	 */
	public static void setLogFilePrefix(String prefix) {
		strPattern = prefix;
		updateLogFileHandler();
	}

	private static void updateLogFileHandler() {
		Level oldLevel = null;
		if (handlerFile != null) {
			oldLevel = handlerFile.getLevel();
			INSTANCE.removeHandler(handlerFile);
			handlerFile = null;
		}
		if (strPattern == null) {
			return;
		}
		try {
			// auto create log dir
			File logDir = new File(strLogDir);
			if (logDir != null && !logDir.isDirectory() && !logDir.mkdirs()) {
				throw new IOException("Can't create log directory! Invalid path '" + logDir.getPath() + "'");
			}
			// file logging
			handlerFile = new FileHandler(strLogDir + strPattern, limitBytes, limitFiles, true);
			try {
				handlerFile.setEncoding("UTF-8");
			} catch (SecurityException | UnsupportedEncodingException e) {
				System.err.println("Fatal Error! Can't set file log encoding to UTF-8! Using default: "
						+ handlerConsole.getEncoding());
			}
			handlerFile.setFormatter(new FileFormatter());
			// default level: FINEST
			if (oldLevel != null) {
				handlerFile.setLevel(oldLevel);
			}
			INSTANCE.addHandler(handlerFile);
			updateLogLevel();
		} catch (SecurityException | IOException e) {
			System.err.println("Fatal Error! Can't init file logging! " + e.getMessage());
		}
	}

	private static class FileFormatter extends Formatter {

		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			// timestamp
			sb.append(DATE_FORMAT.format(new Date(record.getMillis()))).append(" ");
			// level
			sb.append(record.getLevel().getName()).append(" ");
			// class and method name
			sb.append(record.getSourceClassName());
			if (INSTANCE.getLevel().intValue() >= Level.FINER.intValue()) { // print the method name, too
				sb.append("#").append(record.getSourceMethodName());
			}
			sb.append(": ");
			// actual message
			sb.append(record.getMessage()).append("\n");
			// stack trace
			printStackTrace(sb, record.getThrown());
			return sb.toString();
		}

	}

	private L2pLogger() {
		// just private constructor to prevent instantiation
	}

	private static void updateLogLevel() {
		// set overall level to minimal value
		Level minLevel = handlerConsole.getLevel();
		for (Handler handler : INSTANCE.getHandlers()) {
			Level level = handler.getLevel();
			if (level.intValue() < minLevel.intValue()) {
				minLevel = level;
			}
		}
		INSTANCE.setLevel(minLevel);
	}

	private static void printStackTrace(StringBuilder sb, Throwable e) {
		if (e != null) {
			sb.append(e.toString()).append("\n");
			for (StackTraceElement stack : e.getStackTrace()) {
				sb.append("\t").append(stack.toString()).append("\n");
			}
		}
	}

	public static void setConsoleLogLevel(Level level) {
		handlerConsole.setLevel(level);
		updateLogLevel();
	}

	public static void setFileLogLevel(Level level) {
		if (handlerFile != null) {
			handlerFile.setLevel(level);
			updateLogLevel();
		}
	}

}
