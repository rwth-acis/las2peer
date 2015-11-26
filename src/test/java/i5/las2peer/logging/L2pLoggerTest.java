package i5.las2peer.logging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

public class L2pLoggerTest {

	@Test
	public void testParent() {
		Logger logger = Logger.getLogger("i5.las2peer.logging.LogTest");
		Logger parent = logger.getParent();
		assertNotNull(parent);
		assertTrue(parent instanceof L2pLogger);
	}

	@Test
	public void testParent2() {
		// actual logger name may be initialized by other tests
		L2pLogger logger = L2pLogger.getInstance("i5.las2peer.logging.LogTest2");
		assertTrue(logger instanceof L2pLogger);
		Logger parent = logger.getParent();
		assertNotNull(parent);
		assertTrue(parent instanceof L2pLogger);
	}

	// TODO transform into actual test method
	public static void main(String[] args) throws IOException {
		L2pLogger logger = L2pLogger.getInstance("i5.las2peer.log.LogTest");
		logger.finest("silent start");
		logger.info("here we go");
		logger.warning("fatal error incoming");
		logger.severe("fatal error");
		logger.info("told u so");
		logger.finest("only in log file");
		L2pLogger.setGlobalConsoleLevel(Level.FINE);
		logger.fine("fine");
		L2pLogger.setGlobalLogfileLevel(Level.INFO);
		logger.finer("this goes directly to /dev/null");
		logger.log(Level.SEVERE, "this is the worst!", new NullPointerException("layer 8 problem"));
	}

}
