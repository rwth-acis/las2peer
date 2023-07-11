package i5.las2peer.logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import i5.las2peer.api.Context;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.Agent;
import i5.las2peer.execution.ServiceThread;
import i5.las2peer.p2p.Node;

public final class L2pLogger extends Logger implements NodeObserver {

	public static final String GLOBAL_NAME = "i5.las2peer"; // this name should be equal to the las2peer package name.

	// default parameters
	public static final int DEFAULT_LIMIT_BYTES = 1 * 1000 * 1000; // max 1 MB log file size
	public static final int DEFAULT_LIMIT_FILES = 10; // max 10 log files in rotation
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final String DEFAULT_LOG_DIRECTORY = "log/";
	public static final String DEFAULT_LOGFILE_PREFIX = "las2peer.log";
	public static final Level DEFAULT_CONSOLE_LEVEL = Level.INFO;
	public static final Level DEFAULT_LOGFILE_LEVEL = Level.FINEST;
	public static final Level DEFAULT_OBSERVER_LEVEL = Level.FINER;
	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy MMM dd HH:mm:ss");

	// instance parameters
	private int limitBytes = DEFAULT_LIMIT_BYTES;
	private int limitFiles = DEFAULT_LIMIT_FILES;
	private String encoding = DEFAULT_ENCODING;
	private String logDir = DEFAULT_LOG_DIRECTORY;
	private String logfilePrefix; // default null => no file logging, only done by global instance
	private Level consoleLevel = DEFAULT_CONSOLE_LEVEL;
	private Level logfileLevel = DEFAULT_LOGFILE_LEVEL;
	private Level observerLevel = DEFAULT_OBSERVER_LEVEL;
	private SimpleDateFormat dateFormat = DEFAULT_DATE_FORMAT;
	private ConsoleFormatter consoleFormatter = new ConsoleFormatter();
	private LogfileFormatter logfileFormatter = new LogfileFormatter(this);

	private ConsoleHandler handlerConsole;
	private FileHandler handlerLogfile;

	// this is the global static instance, logging to default log file and console
	private static final L2pLogger GLOBAL_INSTANCE = new L2pLogger(GLOBAL_NAME, null);

	// initialize global static instance
	static {
		// suppress Java native console logging
		GLOBAL_INSTANCE.setUseParentHandlers(false);
		// global console logging
		GLOBAL_INSTANCE.handlerConsole = new ConsoleHandler();
		GLOBAL_INSTANCE.handlerConsole.setLevel(GLOBAL_INSTANCE.consoleLevel);
		try {
			GLOBAL_INSTANCE.handlerConsole.setEncoding(GLOBAL_INSTANCE.encoding);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Fatal Error! Can't set console log encoding to '" + GLOBAL_INSTANCE.encoding + "'! " + e
					+ " Using default: " + GLOBAL_INSTANCE.handlerConsole.getEncoding());
		}
		GLOBAL_INSTANCE.handlerConsole.setFormatter(GLOBAL_INSTANCE.consoleFormatter);
		GLOBAL_INSTANCE.addHandler(GLOBAL_INSTANCE.handlerConsole);
		// auto create log directory
		try {
			createDir(GLOBAL_INSTANCE.logDir);
		} catch (IOException e) {
			System.err.println("Fatal Error! Can't create log directory '" + GLOBAL_INSTANCE.logDir
					+ "'! File logging is about to fail!");
		}
		// global file logging
		try {
			GLOBAL_INSTANCE.setLogfilePrefix(DEFAULT_LOGFILE_PREFIX);
			GLOBAL_INSTANCE.handlerLogfile.setLevel(GLOBAL_INSTANCE.logfileLevel);
		} catch (IOException e) {
			System.err.println("Fatal Error! Can't use logging prefix '" + GLOBAL_INSTANCE.logfilePrefix
					+ "'! File logging is disabled!");
		}
		// since this is the global instance, drop not logged messages
		GLOBAL_INSTANCE.minimizeLogLevel();
	}

	protected static class ConsoleHandler extends StreamHandler {

		@Override
		public void publish(LogRecord record) {
			try {
				int level = record.getLevel().intValue();
				String message = getFormatter().format(record);
				if (level >= Level.WARNING.intValue()) {
					System.err.write(message.getBytes());
				} else if (level >= getLevel().intValue()) {
					System.out.write(message.getBytes());
				}
			} catch (Exception exception) {
				reportError(null, exception, ErrorManager.FORMAT_FAILURE);
			}
		}

	}

	/**
	 * This is the formatter used for the console output.
	 */
	protected static class ConsoleFormatter extends Formatter {

		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			// actual message
			sb.append(record.getMessage()).append("\n");
			// stack trace
			printStackTrace(sb, record.getThrown());
			return sb.toString();
		}

	}

	/**
	 * This is the formatter used for the log file output.
	 */
	protected static class LogfileFormatter extends Formatter {

		private final L2pLogger logger;

		public LogfileFormatter(L2pLogger logger) {
			this.logger = logger;
		}

		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			// timestamp
			sb.append(logger.dateFormat.format(new Date(record.getMillis()))).append(" ");
			// level
			sb.append(record.getLevel().getName());
			// sourceClassName replaced by loggerName, as source class is 
			// always L2pLogger itself
			// final String loggerName = record.getSourceClassName();
			final String loggerName = record.getLoggerName();
			sb.append(" ").append(loggerName);
			if (logger.getLevel().intValue() >= Level.FINER.intValue()) { // print the method name, too
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
	 * Prints a stack trace as nicely as {@code e.printStackTrace()}, but uses the logging system as output.
	 *
	 * @param e A {@code Throwable} thats stack trace should be printed.
	 */
	public synchronized void printStackTrace(Throwable e) {
		StringBuilder sb = new StringBuilder();
		printStackTrace(sb, e);
		severe(sb.toString().trim());
	}

	/**
	 * Appends the stack trace for the given {@link Throwable} to the given {@link StringBuilder}.
	 *
	 * @param sb {@code StringBuilder} as output for the stack trace.
	 * @param e A {@code Throwable} which stack trace should be appended. If {@code null} given, nothing is appended.
	 */
	protected static void printStackTrace(StringBuilder sb, Throwable e) {
		if (e != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			sb.append(sw.toString());
		}
	}

	/**
	 * Just calls the parent constructor and adds the new instance to the LogManager.
	 *
	 * @param name A name for the logger. This should be a dot-separated name and should normally be based on the
	 *            package name or class name of the subsystem, such as java.net or javax.swing. It may be null for
	 *            anonymous Loggers.
	 * @param resourceBundleName name of ResourceBundle to be used for localizing messages for this logger. May be null
	 *            if none of the messages require localization.
	 * @throws IllegalArgumentException If a logger with the given name is already registered.
	 */
	protected L2pLogger(String name, String resourceBundleName) throws IllegalArgumentException {
		super(name, resourceBundleName);
		// Changed logging level of Providers class as the jersey update caused a lot of new provider warnings to pop up when starting a node. This disables them (see https://github.com/eclipse-ee4j/jersey/issues/3700).
		L2pLogger.getLogger("org.glassfish.jersey.internal.inject.Providers").setLevel(Level.SEVERE);
		// if the logger is not added to the LogManager, the log files may not be closed correctly
		if (!LogManager.getLogManager().addLogger(this)) {
			// a logger with that name is already registered
			// therefore this instance is not added to the logger hierarchy and may only be used with caution
			throw new IllegalArgumentException("A logger with that name is already registered!");
		}
		// by default the logger itself logs everything, level filtering is done by its handlers
		setLevel(Level.ALL);
	}

	/**
	 * Same as {@link #setLogDirectory(String)} for the global static instance.
	 *
	 * @param directory A directory path given as String. {@code null} is equal to "" and the class loader directory.
	 * @throws IOException If the log file could not be written
	 */
	public static void setGlobalLogDirectory(String directory) throws IOException {
		GLOBAL_INSTANCE.setLogDirectory(directory);
	}

	/**
	 * Sets the directory to store log files.
	 *
	 * @param directory A directory path given as String. {@code null} is equal to "" and the class loader directory.
	 * @throws IOException If the log file could not be written
	 */
	public synchronized void setLogDirectory(String directory) throws IOException {
		if (directory == null || directory.isEmpty()) {
			logDir = "";
		} else {
			logDir = directory;
			if (!logDir.endsWith(File.separator)) {
				logDir = directory + File.separator;
			}
		}
		updateLogfileHandler();
	}

	/**
	 * Same as {@link #setLogfilePrefix(String)} for global static instance.
	 *
	 * @param prefix If {@code null} is given, file logging will be disabled.
	 * @throws IOException If the log file could not be written
	 */
	public static void setGlobalLogfilePrefix(String prefix) throws IOException {
		GLOBAL_INSTANCE.setLogfilePrefix(prefix);
	}

	/**
	 * Sets the prefix used to generate log files.
	 *
	 * @param prefix If {@code null} is given, file logging will be disabled.
	 * @throws IOException If the log file could not be written
	 */
	public synchronized void setLogfilePrefix(String prefix) throws IOException {
		if (prefix.equals(logfilePrefix)) {
			// already the same prefix
			return;
		}
		logfilePrefix = prefix;
		updateLogfileHandler();
	}

	/**
	 * This method must be called each time the log file target is changed.
	 *
	 * @throws IOException If the log file could not be written
	 */
	private synchronized void updateLogfileHandler() throws IOException {
		Level oldLevel = null;
		if (handlerLogfile != null) {
			oldLevel = handlerLogfile.getLevel();
			handlerLogfile.close();
			this.removeHandler(handlerLogfile);
			handlerLogfile = null;
		}
		if (logfilePrefix == null) {
			return;
		}
		// auto create log directory
		createDir(logDir);
		// file logging
		handlerLogfile = new FileHandler(logDir + logfilePrefix, limitBytes, limitFiles, true);
		try {
			handlerLogfile.setEncoding(encoding);
		} catch (SecurityException | UnsupportedEncodingException e) {
			System.err.println("Fatal Error! Can't set file log encoding to '" + encoding + "'! " + e
					+ " Using default: " + handlerConsole.getEncoding());
		}
		handlerLogfile.setFormatter(logfileFormatter);
		// default level: FINEST
		if (oldLevel != null) {
			handlerLogfile.setLevel(oldLevel);
		}
		this.addHandler(handlerLogfile);
	}

	/**
	 * This method ensures that the given directory is actually a directory and exists.
	 *
	 * @param dir A path given as String for the desired directory
	 * @throws IOException return null;
	 */
	private static void createDir(String dir) throws IOException {
		File fDir = new File(dir);
		if (fDir != null && !dir.isEmpty() && !fDir.isDirectory() && !fDir.mkdirs()) {
			throw new IOException("Can't create directory! Invalid path '" + fDir.getPath() + "'");
		}
	}

	/**
	 * Same as {@link #setConsoleLevel(Level)} for the global static instance.
	 *
	 * @param level The log level to set.
	 */
	public static void setGlobalConsoleLevel(Level level) {
		GLOBAL_INSTANCE.setConsoleLevel(level);
	}

	/**
	 * Sets the log level for the console output of this logger.
	 *
	 * @param level The log level to set.
	 */
	public synchronized void setConsoleLevel(Level level) {
		consoleLevel = level;
		handlerConsole.setLevel(consoleLevel);
	}

	/**
	 * Same as {@link #setLogfileLevel(Level)} for global static instance.
	 *
	 * @param level The log level to set.
	 */
	public static void setGlobalLogfileLevel(Level level) {
		GLOBAL_INSTANCE.setLogfileLevel(level);
	}

	/**
	 * Sets the log level for the log files used in this logger.
	 *
	 * @param level The log level to set.
	 */
	public synchronized void setLogfileLevel(Level level) {
		logfileLevel = level;
		if (handlerLogfile != null) {
			handlerLogfile.setLevel(logfileLevel);
		}
	}

	/**
	 * Updates the loggers own log level and sets it to the minimal value of all assigned handlers. This way the
	 * performance is slightly improved, because the logger itself drops messages not suitable for assigned handlers.
	 * Please pay attention that this will drop messages, that may be interesting for parent loggers or handlers, too.
	 * Usually this method should be only used with the global instance.
	 */
	private synchronized void minimizeLogLevel() {
		// set minimal level of all handlers and this logger instance
		Level minLevel = Level.OFF;
		for (Handler handler : getHandlers()) {
			Level level = handler.getLevel();
			if (level.intValue() < minLevel.intValue()) {
				minLevel = level;
			}
		}
		setLevel(minLevel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void log(LogRecord record) {
		super.log(record);
//		Thread t = Thread.currentThread();
//		if (t instanceof L2pThread) {
//			// TODO write output to specific node log file, too
//			Serializable nodeId = ((L2pThread) t).getContext().getLocalNode().getNodeId();
//		}
	}

	/**
	 * @deprecated Use {@link i5.las2peer.api.Context#monitorEvent(MonitoringEvent, String)}
	 * 
	 *             Writes a log message. The given event can be used to differentiate between different log messages.
	 *
	 * @param event used to differentiate between different log messages
	 * @param message A message to log in the monitoring data
	 */
	@Deprecated
	public static void logEvent(MonitoringEvent event, String message) {
		logEvent(null, event, message, null, null);
	}

	/**
	 * @deprecated Use {@link i5.las2peer.api.Context#monitorEvent(Object, MonitoringEvent, String, boolean)}
	 * 
	 *             Writes a log message. The given event can be used to differentiate between different log messages.
	 *
	 * @param event used to differentiate between different log messages
	 * @param actingUser can be set to null if unknown / not desired
	 * @param message A message to log in the monitoring data
	 */
	@Deprecated
	public static void logEvent(MonitoringEvent event, Agent actingUser, String message) {
		logEvent(null, event, message, Context.get().getServiceAgent(), actingUser);
	}

	/**
	 * @deprecated Use {@link i5.las2peer.api.Context#monitorEvent(Object, MonitoringEvent, String)}
	 * 
	 *             Logs a message to the l2p system using the observers.
	 *
	 *             Since this method will/should only be used in an L2pThread, the message will come from a service or a
	 *             helper, so a SERVICE_MESSAGE is assumed. Then this message will not be monitored by the monitoring
	 *             observer.
	 *
	 * @param from the calling class
	 * @param event used to differentiate between different log messages
	 * @param message A message to log in the monitoring data
	 */
	@Deprecated
	public static void logEvent(Object from, MonitoringEvent event, String message) {
		logEvent(from, event, message, null, null);
	}

	/**
	 * @deprecated Use {@link i5.las2peer.api.Context#monitorEvent(Object, MonitoringEvent, String, boolean)}
	 * 
	 *             Writes a log message. The given event can be used to differentiate between different log messages.
	 *             The serviceAgent and actingUser can be set to {@code null} if not known. Then this message will not
	 *             be monitored by the monitoring observer.
	 *
	 * @param from the calling class
	 * @param event used to differentiate between different log messages
	 * @param message A message to log in the monitoring data
	 * @param serviceAgent can be set to null if unknown / not desired
	 * @param actingUser can be set to null if unknown / not desired
	 */
	@Deprecated
	public static void logEvent(Object from, MonitoringEvent event, String message, Agent serviceAgent,
			Agent actingUser) {
		logEvent(ServiceThread.getCurrentContext().getCallerContext().getLocalNode(), from, event, message,
				serviceAgent, actingUser);
	}

	/**
	 * @deprecated Use {@link i5.las2peer.api.Context#monitorEvent(Object, MonitoringEvent, String, boolean)}
	 * 
	 *             Writes a log message. The given event can be used to differentiate between different log messages.
	 *             The serviceAgent and actingUser can be set to {@code null} if not known. Then this message will not
	 *             be monitored by the monitoring observer.
	 *
	 * @param node The node that should be noticed about this event
	 * @param from the calling class
	 * @param event used to differentiate between different log messages
	 * @param message A message to log in the monitoring data
	 * @param serviceAgent can be set to null if unknown / not desired
	 * @param actingUser can be set to null if unknown / not desired
	 */
	@Deprecated
	public static void logEvent(Node node, Object from, MonitoringEvent event, String message, Agent serviceAgent,
			Agent actingUser) {
		String msg = message;
		if (from != null) {
			msg = from.getClass().getSimpleName() + ": " + message;
		}
		node.observerNotice(event, node.getNodeId(), serviceAgent, null, actingUser, msg);
	}

	/**
	 * Same as #log(MonitoringEvent, String) without any remarks, just logs the plain event.
	 * 
	 * @param event A monitoring event to log
	 */
	public void log(MonitoringEvent event) {
		log(System.currentTimeMillis(), event, null, Context.get().getMainAgent().getIdentifier(), null, null, null);
	}

	/**
	 * Same as #log(MonitoringEvent, String, String, String) with context main agent as default source agent.
	 * 
	 * @param event A monitoring event to log
	 * @param remarks Arbitrary data to log along with the event
	 */
	public void log(MonitoringEvent event, String remarks) {
		log(System.currentTimeMillis(), event, null, Context.get().getMainAgent().getIdentifier(), null, null, remarks);
	}

	/**
	 * Same as {@link #log(Long, MonitoringEvent, String, String, String, String, String)} with current system timestamp
	 * and no source or destination node.
	 * 
	 * @param event A monitoring event to log
	 * @param sourceAgentId A source agent id for this monitoring event
	 * @param destinationAgentId A destination agent id for this monitoring event
	 * @param remarks Arbitrary data to log along with the event
	 */
	public void log(MonitoringEvent event, String sourceAgentId, String destinationAgentId, String remarks) {
		log(System.currentTimeMillis(), event, null, sourceAgentId, null, destinationAgentId, remarks);
	}

	/**
	 * @deprecated Use {@link #log(Long, MonitoringEvent, String, String, String, String, String)} instead. The coupling
	 *             between nodes and services is softened now.
	 * 
	 */
	@Deprecated
	@Override
	public void log(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks) {
		StringBuilder logLine = new StringBuilder();
		logLine.append(event + " (" + event.getCode() + ")\t");
		logLine.append(appendPart(sourceNode));
		logLine.append(appendPart(sourceAgentId));
		logLine.append(appendPart(destinationNode));
		logLine.append(appendPart(destinationAgentId));
		logLine.append(appendPart(remarks));
		// with default levels this hides the output from console and only writes it to logfile
		log(observerLevel, logLine.toString());
	}

	@Override
	public void logXESEvent(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks, String caseId, String activityName,
			String resourceId, String resourceType, String lifecyclePhase) {
		StringBuilder logLine = new StringBuilder();
		logLine.append(event + " (" + event.getCode() + ")\t");
		logLine.append(appendPart(sourceNode));
		logLine.append(appendPart(sourceAgentId));
		logLine.append(appendPart(destinationNode));
		logLine.append(appendPart(destinationAgentId));
		logLine.append(appendPart(remarks));
		logLine.append(appendPart(caseId));
		logLine.append(appendPart(activityName));
		logLine.append(appendPart(resourceId));
		logLine.append(appendPart(resourceType));
		logLine.append(appendPart(lifecyclePhase));
		// with default levels this hides the output from console and only writes it to
		// logfile
		log(observerLevel, logLine.toString());
	}

	/**
	 * Simple method for one log line entry. Null will be printed as "-". All values will be followed by a tab char.
	 *
	 * @param o
	 * @return a string
	 */
	private static String appendPart(Object o) {
		if (o == null) {
			return "-\t";
		} else {
			return "" + o + "\t";
		}
	}

	/**
	 * This method returns the default {@link Formatter} currently used to format log output for console.
	 *
	 * @return Returns the console formatter.
	 */
	public static Formatter getGlobalConsoleFormatter() {
		return GLOBAL_INSTANCE.consoleFormatter;
	}

	/**
	 * This method returns the default {@link Formatter} currently used to format log output for log files.
	 *
	 * @return Returns the log file formatter.
	 */
	public static Formatter getGlobalLogfileFormatter() {
		return GLOBAL_INSTANCE.logfileFormatter;
	}

	/**
	 * This method is used to retrieve a L2pLogger instance.
	 *
	 * @param cls Should be the class this instance is used with.
	 * @return Returns a L2pLogger instance for the given class.
	 * @throws ClassCastException If someone overloaded the loggers instance by adding some other logger implementation
	 *             with the same name. In this case you may use Java native method by calling
	 *             {@link Logger#getLogger(String)}.
	 */
	public static L2pLogger getInstance(Class<?> cls) throws ClassCastException {
		return getInstance(cls.getCanonicalName());
	}

	/**
	 * This method is used to retrieve a L2pLogger instance.
	 *
	 * @param name A name for the new logger instance. Should be the name of your current class by default. Like
	 *            L2pLogger.class.getCanonicalName()
	 * @return Returns a L2pLogger instance for the given name.
	 * @throws ClassCastException If someone overloaded the loggers instance by adding some other logger implementation
	 *             with the same name. In this case you may use Java native method by calling
	 *             {@link Logger#getLogger(String)}.
	 */
	public static L2pLogger getInstance(String name) throws ClassCastException {
		if (name == null || name.isEmpty() || "i5.las2peer".equals(name)) {
			throw new IllegalArgumentException("Invalid logger name '" + name + "' given!");
		}
		L2pLogger result;
		try {
			result = new L2pLogger(name, null);
		} catch (IllegalArgumentException e) {
			// a logger with that name is already registered
			result = (L2pLogger) LogManager.getLogManager().getLogger(name);
		}
		if (result == null) {
			throw new NullPointerException("Logger instance should not be null");
		}
		return result;
	}

}
