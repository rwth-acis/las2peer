package i5.las2peer.logging;

import java.io.IOException;
import java.util.logging.Level;

import org.junit.BeforeClass;
import org.junit.Test;

public class LogTest {

	private static final L2pLogger logger = L2pLogger.INSTANCE;

	@BeforeClass
	public static void init() {
		L2pLogger.init();
	}

	@Test
	public void testOutput() throws IOException {
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
	}

}
