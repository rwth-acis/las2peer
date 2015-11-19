package i5.las2peer.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

public class LogTest {

	private static final Logger logger = Logger.getLogger(LogTest.class.getName());

	@BeforeClass
	public static void init() {
		L2pLogger.init();
	}

	@Test
	public void testOutput() {
		logger.finest("silent start");
		logger.info("here we go");
		logger.warning("fatal error incoming");
		logger.severe("fatal error");
		logger.info("told u so");
		logger.finest("only in log file");
		L2pLogger.setConsoleLogLevel(Level.FINE);
		logger.fine("fine");
		L2pLogger.setFileLogLevel(Level.INFO);
		logger.finer("this goes directly to /dev/null");
		String str = null;
		logger.severe(str);
		logger.log(Level.SEVERE, null, new NullPointerException());
		logger.log(Level.SEVERE, null, new NullPointerException("layer 8 problem"));
		logger.log(Level.SEVERE, "this is the worst!", new NullPointerException());
		logger.log(Level.SEVERE, "this is the worst!", new NullPointerException("layer 8 problem"));
		L2pLogger.setLogDirectory("log");
		L2pLogger.setLogFilePrefix("testlog.log");
		logger.severe("Does this even create directories?!");
		L2pLogger.setLogDirectory("");
		logger.severe("top level directory");
	}

}
