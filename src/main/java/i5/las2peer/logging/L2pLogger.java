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
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import i5.las2peer.api.Service;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;

public final class L2pLogger extends Logger implements NodeObserver {

	// any changes to these parameters require a restart
	public static final int DEFAULT_LIMIT_BYTES = 1 * 1000 * 1000; // max 1 MB log file size
	private static int limitBytes = DEFAULT_LIMIT_BYTES;
	public static final int DEFAULT_LIMIT_FILES = 10; // max 10 log files in rotation
	private static int limitFiles = DEFAULT_LIMIT_FILES;
	public static final String DEFAULT_ENCODING = "UTF-8";
	private static String encoding = DEFAULT_ENCODING;
	public static final String DEFAULT_LOGDIR = "log/";
	private static String strLogDir = DEFAULT_LOGDIR;
	public static final String DEFAULT_LOGFILE_PREFIX = "las2peer.log";

	// if this instance is not used, the L2pLogger may not be initialized!
	public static L2pLogger INSTANCE = new L2pLogger("i5.logger", null);

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");
	private static ConsoleFormatter consoleDefaultFormatter = new ConsoleFormatter();
	private static final ConsoleHandler handlerConsole = new ConsoleHandler();
	private static FileFormatter fileDefaultFormatter = new FileFormatter();
	private static FileHandler handlerFile;
	private static String strPattern;

	/**
	 * This method has to be called in the {@code main} class of any code the L2pLogger is used in.
	 */
	public static synchronized void init() {
		// if the logger is not added to the LogManager, the log files may not be closed correctly
		LogManager.getLogManager().addLogger(INSTANCE);
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
			handlerConsole.setEncoding(encoding);
		} catch (SecurityException | UnsupportedEncodingException e) {
			System.err.println("Fatal Error! Can't set console log encoding to '" + encoding + "'! " + e
					+ " Using default: " + handlerConsole.getEncoding());
		}
		handlerConsole.setFormatter(consoleDefaultFormatter);
		INSTANCE.addHandler(handlerConsole);
		updateLogLevel();
		// file logging
		// default level: ALL
		try {
			setLogFilePrefix(DEFAULT_LOGFILE_PREFIX);
		} catch (IOException e) {
			System.err.println("Fatal Error! Can't use logging prefix '" + strPattern + "'! File logging is disabled!");
		}
	}

	/**
	 * This is the formatter used for the console output.
	 */
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
	 * @throws IOException
	 */
	public static synchronized void setLogDirectory(String directory) throws IOException {
		if (directory == null || directory.isEmpty()) {
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
	 * @throws IOException
	 */
	public static synchronized void setLogFilePrefix(String prefix) throws IOException {
		if (prefix.equals(strPattern)) {
			// already the same prefix
			return;
		}
		strPattern = prefix;
		updateLogFileHandler();
	}

	/**
	 * This method must be called each time the log file target is changed.
	 * 
	 * @throws IOException
	 */
	private static synchronized void updateLogFileHandler() throws IOException {
		Level oldLevel = null;
		if (handlerFile != null) {
			oldLevel = handlerFile.getLevel();
			handlerFile.close();
			INSTANCE.removeHandler(handlerFile);
			handlerFile = null;
		}
		if (strPattern == null) {
			return;
		}
		// auto create log dir
		File logDir = new File(strLogDir);
		if (logDir != null && !strLogDir.isEmpty() && !logDir.isDirectory() && !logDir.mkdirs()) {
			throw new IOException("Can't create log directory! Invalid path '" + logDir.getPath() + "'");
		}
		// file logging
		handlerFile = new FileHandler(strLogDir + strPattern, limitBytes, limitFiles, false);
		try {
			handlerFile.setEncoding(encoding);
		} catch (SecurityException | UnsupportedEncodingException e) {
			System.err.println("Fatal Error! Can't set file log encoding to '" + encoding + "'! " + e
					+ " Using default: " + handlerConsole.getEncoding());
		}
		handlerFile.setFormatter(fileDefaultFormatter);
		// default level: FINEST
		if (oldLevel != null) {
			handlerFile.setLevel(oldLevel);
		}
		INSTANCE.addHandler(handlerFile);
		updateLogLevel();
	}

	/**
	 * This is the formatter used for the log file output.
	 */
	private static class FileFormatter extends Formatter {

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

	/**
	 * Just calls the parent constructor.
	 */
	protected L2pLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
	}

	/**
	 * Updates the loggers own log level and sets it to the minimal value of all assigned handlers.
	 */
	private static synchronized void updateLogLevel() {
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

	/**
	 * Prints a stack trace as nicely as {@code Exception.printStackTrace()}
	 * 
	 * @param sb {@code StringBuilder} as output for the stack trace.
	 * @param e A {@code Throwable} thats stack trace should be printed.
	 */
	private static void printStackTrace(StringBuilder sb, Throwable e) {
		if (e != null) {
			sb.append(e.toString()).append("\n");
			for (StackTraceElement stack : e.getStackTrace()) {
				sb.append("\t").append(stack.toString()).append("\n");
			}
		}
	}

	/**
	 * Sets the log level for the console output of this logger.
	 * 
	 * @param level The log level to set.
	 */
	public static synchronized void setConsoleLogLevel(Level level) {
		handlerConsole.setLevel(level);
		updateLogLevel();
	}

	/**
	 * Sets the log level for the log files used in this logger.
	 * 
	 * @param level The log level to set.
	 */
	public static synchronized void setFileLogLevel(Level level) {
		if (handlerFile != null) {
			handlerFile.setLevel(level);
			updateLogLevel();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void log(LogRecord record) {
		super.log(record);
		// TODO check if it's a node context and write output to specific node log file
	}

	/**
	 * Writes a log message. The given event can be used to differentiate between different log messages. The
	 * serviceAgent and actingUser can be set to {@code null} if not known. Then this message will not be monitored by
	 * the monitoring observer.
	 * 
	 * This method replaces {@link Context#logMessage(Object, int, String, Agent, Agent)}
	 * {@link Context#logMessage(Object, String)} {@link Context#logError(Object, int, String, Agent, Agent)}
	 * {@link Context#logError(Object, String)} {@link Service#logError(String message)}
	 * 
	 * @param from the calling class
	 * @param event an event
	 * @param message
	 * @param serviceAgent
	 * @param actingUser
	 */
	public static void logEvent(Object from, Event event, String message, Agent serviceAgent, Agent actingUser) {
		Thread t = Thread.currentThread();
		if (t instanceof L2pThread) {
			Node node = ((L2pThread) t).getContext().getLocalNode();
			node.observerNotice(event, node.getNodeId(), serviceAgent, null, actingUser,
					from.getClass().getSimpleName() + ": " + message);
		} else {
			throw new IllegalStateException("Not executed in a L2pThread environment!");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void log(Long timestamp, Event event, String sourceNode, Long sourceAgentId, String destinationNode,
			Long destinationAgentId, String remarks) {
		StringBuilder logLine = new StringBuilder(DATE_FORMAT.format(new Date(timestamp)) + "\t");
		logLine.append(event + " (" + event.getCode() + ")\t");
		logLine.append(appendPart(sourceNode));
		logLine.append(appendPart(sourceAgentId));
		logLine.append(appendPart(destinationNode));
		logLine.append(appendPart(destinationAgentId));
		logLine.append(appendPart(remarks));
		// with default levels this hides the output from console and only writes it to logfile
		INSTANCE.log(Level.FINE, logLine.toString());
	}

	/**
	 * Simple method for one log line entry. Null will be printed as "-". All values will be followed by a tab char.
	 * 
	 * @param o
	 * @return a string
	 */
	private static String appendPart(Object o) {
		if (o == null)
			return "-\t";
		else
			return "" + o + "\t";
	}

	/**
	 * This method returns the default {@link Formatter} currently used to format log output for console.
	 * 
	 * @return Returns the console formatter.
	 */
	public static Formatter getConsoleDefaultFormatter() {
		return consoleDefaultFormatter;
	}

	/**
	 * This method returns the default {@link Formatter} currently used to format log output for log files.
	 * 
	 * @return Returns the log file formatter.
	 */
	public static Formatter getFileDefaultFormatter() {
		return fileDefaultFormatter;
	}

}
