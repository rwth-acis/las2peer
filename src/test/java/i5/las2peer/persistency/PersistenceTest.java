package i5.las2peer.persistency;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

public class PersistenceTest {

	private ArrayList<PastryNodeImpl> nodes;

	@Rule
	public TestName name = new TestName();

	@Before
	public void startNetwork() {
		try {
			// start test node
			nodes = TestSuite.launchNetwork(3, STORAGE_MODE.FILESYSTEM, true);
			System.out.println("Test network started");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@After
	public void stopNetwork() {
		if (nodes != null) {
			for (PastryNodeImpl node : nodes) {
				node.shutDown();
			}
			nodes = null;
		}
	}

	@Test
	public void testStartStopNetwork() {
		// just as time reference ...
	}

	@Test
	public void testFilesystemPersistence() {
		try {
			// start network and write data into shared storage
			PastryNodeImpl node1 = nodes.get(0);
			Envelope before = node1.createUnencryptedEnvelope("test", "This is las2peer!");
			UserAgent smith = MockAgentFactory.getAdam();
			smith.unlockPrivateKey("adamspass");
			node1.storeEnvelope(before, smith);
			// shutdown network
			stopNetwork();
			Thread.sleep(500);
			// restart the network and read data
			nodes = TestSuite.launchNetwork(3, STORAGE_MODE.FILESYSTEM, false);
			System.out.println("test network restarted");
			node1 = nodes.get(0);
			Envelope after = node1.fetchEnvelope("test");
			Assert.assertEquals(before.getContent(), after.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	// TODO test currently not working because of LAS-359, time to join the network is too high
//	@Test
//	public void testVersioning() {
//		try {
//			// start network and write data into shared storage
//			PastryNodeImpl node1 = nodes.get(0);
//			Envelope env = node1.createUnencryptedEnvelope("test", "This is las2peer!");
//			UserAgent smith = MockAgentFactory.getAdam();
//			smith.unlockPrivateKey("adamspass");
//			node1.storeEnvelope(env, smith);
//			// shutdown node 2
//			PastryNodeImpl node2 = nodes.remove(1);
//			node2.shutDown();
//			Thread.sleep(500);
//			// update envelope
//			env = node1.createUnencryptedEnvelope(env, "This is las2peer again!");
//			node1.storeEnvelope(env, smith);
//			// start node 2 again (still with version 1 of env in storage
//			node2 = TestSuite.addNode(node1.getPort(), STORAGE_MODE.FILESYSTEM, 1L);
//			nodes.set(1, node2);
//			// read data
//			Envelope fetched = node2.fetchEnvelope("test");
//			Assert.assertEquals(env.getContent(), fetched.getContent());
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail(e.toString());
//		}
//	}

}
