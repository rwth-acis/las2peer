package i5.las2peer.tools;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.testing.TestSuite;
import i5.las2peer.tools.helper.L2pNodeLauncherConfiguration;

public class L2pNodeLauncherConfigurationTest {

	@Test
	public void testMainArgsJoin() {
		try {
			L2pNodeLauncherConfiguration conf = L2pNodeLauncherConfiguration
					.createFromMainArgs("--startService('i5.las2peer.testService.TestService',", "'testpass')");
			Assert.assertNotNull(conf);
			Assert.assertEquals("--startService('i5.las2peer.testService.TestService','testpass')",
					conf.getCommands().get(0));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testMainArgsJoinFailClosing() {
		try {
			L2pNodeLauncherConfiguration.createFromMainArgs("--startService(xxx))");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected exception
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testMainArgsJoinFailOpening() {
		try {
			L2pNodeLauncherConfiguration.createFromMainArgs("--startService((xxx)");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected exception
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testMainArgsNoJoin() {
		try {
			L2pNodeLauncherConfiguration conf = L2pNodeLauncherConfiguration.createFromMainArgs("--startService(xxx)",
					"interactive");
			Assert.assertEquals(2, conf.getCommands().size());
		} catch (IllegalArgumentException e) {
			// expected exception
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testStorageMode() {
		try {
			// test large/small case
			Assert.assertEquals(STORAGE_MODE.MEMORY,
					L2pNodeLauncherConfiguration.createFromMainArgs("--storage-mode", "MEMORY").getStorageMode());
			Assert.assertEquals(STORAGE_MODE.MEMORY,
					L2pNodeLauncherConfiguration.createFromMainArgs("--storage-mode", "memory").getStorageMode());
			Assert.assertEquals(STORAGE_MODE.FILESYSTEM,
					L2pNodeLauncherConfiguration.createFromMainArgs("--storage-mode", "FILESYSTEM").getStorageMode());
			Assert.assertEquals(STORAGE_MODE.FILESYSTEM,
					L2pNodeLauncherConfiguration.createFromMainArgs("--storage-mode", "filesystem").getStorageMode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testAllArgs() {
		try {
			// test all arg constants
			L2pNodeLauncherConfiguration conf = L2pNodeLauncherConfiguration.createFromArrayArgs("--help", "--version",
					"--colored-shell", "--debug", "--port", "12345", "--bootstrap", "test:1234", "--observer",
					"--log-directory", "testdir", "--node-id-seed", "0000", "--service-directory", "servicedir",
					"--storage-mode", "memory");
			Assert.assertTrue(conf.isPrintHelp());
			Assert.assertTrue(conf.isPrintVersion());
			Assert.assertTrue(conf.isColoredOutput());
			Assert.assertTrue(conf.isDebugMode());
			Assert.assertEquals((Integer) 12345, conf.getPort());
			Assert.assertEquals("test:1234", conf.getBootstrap());
			Assert.assertTrue(conf.useMonitoringObserver());
			Assert.assertEquals("testdir", conf.getLogDir());
			Assert.assertEquals((Long) 0000l, conf.getNodeIdSeed());
			Set<String> expectedSet = new HashSet<>();
			expectedSet.add("servicedir");
			Assert.assertEquals(expectedSet, conf.getServiceDirectories());
			Assert.assertEquals(STORAGE_MODE.MEMORY, conf.getStorageMode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testAllShortArgs() {
		try {
			// test all short arg constants
			L2pNodeLauncherConfiguration conf = L2pNodeLauncherConfiguration.createFromArrayArgs("-h", "-v", "-c",
					"-debug", "-p", "12345", "-b", "test:1234", "-o", "-l", "testdir", "-n", "0000", "-s", "servicedir",
					"-m", "memory");
			Assert.assertTrue(conf.isPrintHelp());
			Assert.assertTrue(conf.isPrintVersion());
			Assert.assertTrue(conf.isColoredOutput());
			Assert.assertTrue(conf.isDebugMode());
			Assert.assertEquals((Integer) 12345, conf.getPort());
			Assert.assertEquals("test:1234", conf.getBootstrap());
			Assert.assertTrue(conf.useMonitoringObserver());
			Assert.assertEquals("testdir", conf.getLogDir());
			Assert.assertEquals((Long) 0000l, conf.getNodeIdSeed());
			Set<String> expectedSet = new HashSet<>();
			expectedSet.add("servicedir");
			Assert.assertEquals(expectedSet, conf.getServiceDirectories());
			Assert.assertEquals(STORAGE_MODE.MEMORY, conf.getStorageMode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNodeIdSeedNull() {
		try {
			L2pNodeLauncherConfiguration.createFromArrayArgs("--node-id-seed", null);
			Assert.fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// expected exception
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNodeIdSeedString() {
		try {
			L2pNodeLauncherConfiguration.createFromArrayArgs("--node-id-seed", "test");
			Assert.fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// expected exception
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNodePortInDebugMode() {
		try {
			L2pNodeLauncherConfiguration conf = new L2pNodeLauncherConfiguration();
			conf.setDebugMode(true);
			L2pNodeLauncher launcher = L2pNodeLauncher.launchConfiguration(conf);
			// random port in debug mode expected
			Assert.assertNotEquals(PastryNodeImpl.DEFAULT_BOOTSTRAP_PORT, launcher.getNode().getPort());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNodePortInDebugModeWithPort() {
		try {
			L2pNodeLauncherConfiguration conf = new L2pNodeLauncherConfiguration();
			conf.setDebugMode(true);
			int systemPort = PastryNodeImpl.getSystemDefinedPort();
			System.out.println("Using system defined (random) port " + systemPort + " to test");
			conf.setPort(systemPort);
			L2pNodeLauncher launcher = L2pNodeLauncher.launchConfiguration(conf);
			Assert.assertEquals(systemPort, launcher.getNode().getPort());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Ignore // because the port might be in use
	@Test
	public void testNodePortDefault() {
		try {
			TestSuite.wipeTestStorage();
			L2pNodeLauncherConfiguration conf = new L2pNodeLauncherConfiguration();
			conf.setStorageDirectory(TestSuite.TEST_STORAGE_DIR);
			L2pNodeLauncher launcher = L2pNodeLauncher.launchConfiguration(conf);
			Assert.assertEquals(PastryNodeImpl.DEFAULT_BOOTSTRAP_PORT, launcher.getNode().getPort());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testNodePortWithPort() {
		try {
			TestSuite.wipeTestStorage();
			L2pNodeLauncherConfiguration conf = new L2pNodeLauncherConfiguration();
			conf.setStorageDirectory(TestSuite.TEST_STORAGE_DIR);
			int systemPort = PastryNodeImpl.getSystemDefinedPort();
			System.out.println("Using system defined (random) port " + systemPort + " to test");
			conf.setPort(systemPort);
			L2pNodeLauncher launcher = L2pNodeLauncher.launchConfiguration(conf);
			Assert.assertEquals(systemPort, launcher.getNode().getPort());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
