package i5.las2peer.security;

import static org.junit.Assert.fail;

import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.PastryNodeImpl.STORAGE_MODE;

public class SandboxTest {

	private PastryNodeImpl node;
	private ServiceAgent service;

	@BeforeClass
	public static void initSecurityManager() {
		System.setSecurityManager(new L2pSecurityManager());
	}

	@Before
	public void startNetwork() {
		try {
			node = new PastryNodeImpl(14501, null, STORAGE_MODE.memory);
			node.launch();
			service = ServiceAgent.createServiceAgent(MaliciousService.class.getName(), "testpasswd");
			service.unlockPrivateKey("testpasswd");
			node.registerReceiver(service);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Can't bring up network!");
		}
	}

	@After
	public void stopNetwork() {
		node.shutDown();
	}

	/**
	 * In this test case the malicious service tries to disable the SecurityManager
	 * 
	 * This test case is essential for all others
	 */
	@Test
	public void testDisableSecurityManager() {
		callTestMethod("disableSecurityManager");
	}

	/**
	 * In this test case the malicious service tries to access the file system
	 */
	@Test
	public void testFilesystem() {
		callTestMethod("readFile");
	}

	/**
	 * In this test case the malicious service tries to open server sockets
	 */
	@Test
	public void testNetwork() {
		callTestMethod("openBackdoor");
	}

	/**
	 * In this test case the malicious service spawns subthreads to prevent L2pThread detection
	 */
	@Test
	public void testSubthreading() {
		callTestMethod("subthreads");
	}

	/**
	 * In this test case the malicious service tries to overload a SecurityManager method
	 */
	@Test
	public void testOverloading() {
		callTestMethod("overload");
	}

	private void callTestMethod(String methodName) {
		try {
			node.invokeLocally(service.getId(), MaliciousService.class.getName(), methodName, new Serializable[0]);
		} catch (Exception e) {
			// get the root cause exception
//			e.printStackTrace();
			Throwable cause = e;
			while (cause.getCause() != null) {
				cause = cause.getCause();
			}
			if (cause instanceof SecurityException) {
				// expected
				System.out.println(cause.toString());
				return;
			}
			e.printStackTrace();
		}
		fail("SecurityException expected!");
	}

}
