package i5.las2peer.security;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.sandbox.L2pSecurityManager;
import i5.las2peer.testing.TestSuite;

public class SandboxTest {

	private PastryNodeImpl node;
	private ServiceAgentImpl service;

	@BeforeClass
	public static void initSecurityManager() {
		// print some important system properties
		System.out.println(System.getProperty("java.home"));
		System.out.println(System.getProperty("java.class.path"));
		L2pSecurityManager.enableSandbox();
	}

	@Before
	public void startNetwork() {
		try {
			node = TestSuite.launchNetwork(1).get(0);
			node.launch();
			node.startService(new ServiceNameVersion(MaliciousService.class.getName(), "1.0"), "testpasswd");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Can't bring up network!");
		}
	}

	@After
	public void stopNetwork() {
		if (node != null) {
			node.shutDown();
		}
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

	/**
	 * In this test case the malicious service tries to modify the whitelisted system property java.class.path in order
	 * to whitelist some more directories.
	 */
	@Test
	public void testClassPath() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "changeClassPath", new Serializable[0]);
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
	 * In this test case the malicious service tries to change the system property java.security.policy to an empty file
	 * in order to disable all policy file checks.
	 */
	@Test
	public void testPolicyProperty() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "changePolicyProperty", new Serializable[0]);
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
	 * In this test case the malicious service tries to replace the policy file by moving another file over it.
	 */
	@Test
	public void testPolicyFile() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "overwritePolicyFile", new Serializable[0]);
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
	 * In this test case the malicious service tries to open (block) ports in order to prevent other system services
	 * from execution.
	 */
	@Test
	public void testBlockingPorts() {
		try {
			node.invoke(service, MaliciousService.class.getName(), "blockPorts", new Serializable[0]);
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
	 * In this test case the access to files and directories is tested, which should be whitelisted in the sandbox.
	 */
	@Test
	public void testAccessGranted() {
		try {
			new File("lib/").canRead();
			new File("service/").canRead();
			new File("etc/").canRead();
			new File("config/").canRead();
			new File("properties/").canRead();
			new File("service/").canRead();
			new File("log/").canRead();
			new File("node-storage/").canRead();
			new File("user.params").canRead();
		} catch (SecurityException e) {
			e.printStackTrace();
			fail("No SecurityException expected!");
		}
	}

}
