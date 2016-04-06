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
		try {
			node.invoke(service, MaliciousService.class.getName(), "disableSecurityManager", new Serializable[0]);
		} catch (Exception e) {
			// get the root cause exception
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

	/**
	 * In this test case the malicious service tries to access the file system
	 */
	@Test
	public void testFilesystem() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "readFile", new Serializable[0]);
		} catch (Exception e) {
			// get the root cause exception
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

	/**
	 * In this test case the malicious service spawns subthreads to prevent detection
	 */
	@Test
	public void testSubthreading() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "subthreads", new Serializable[0]);
		} catch (Exception e) {
			// get the root cause exception
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
		// subthreads can't throw exceptions till this point
		// the necessary fail() call is placed earlier
	}

	// TODO reflection is still dangerous
//	/**
//	 * In this test case the malicious service tries to overwrite a L2pSecurityManager member variable
//	 */
//	@Test
//	public void testReflection() {
//		try {
//			node.invoke(service, MaliciousService.class.getName(), "reflection", new Serializable[0]);
//		} catch (Exception e) {
//			// get the root cause exception
//			Throwable cause = e;
//			while (cause.getCause() != null) {
//				cause = cause.getCause();
//			}
//			if (cause instanceof SecurityException) {
//				// expected
//				System.out.println(cause.toString());
//				return;
//			}
//			e.printStackTrace();
//		}
//		fail("SecurityException expected!");
//	}

}
