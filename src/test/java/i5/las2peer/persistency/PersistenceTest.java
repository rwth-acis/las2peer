package i5.las2peer.persistency;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

public class PersistenceTest {

	private ArrayList<PastryNodeImpl> nodes;

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
	public void testFilesystemPersistence() {
		try {
			// start network and write data into shared storage
			PastryNodeImpl node1 = nodes.get(0);
			EnvelopeVersion before = node1.createUnencryptedEnvelope("test", "This is las2peer!");
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			node1.storeEnvelope(before, smith);
			// shutdown network
			stopNetwork();
			Thread.sleep(500);
			// restart the network and read data
			nodes = TestSuite.launchNetwork(3, STORAGE_MODE.FILESYSTEM, false);
			System.out.println("test network restarted");
			node1 = nodes.get(0);
			EnvelopeVersion after = node1.fetchEnvelope("test");
			Assert.assertEquals(before.getContent(), after.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Ignore
	@Test
	public void testVersionSafety() {
		try {
			// start network and write data into shared storage
			PastryNodeImpl node1 = nodes.get(0);
			EnvelopeVersion env = node1.createUnencryptedEnvelope("test", "This is las2peer!");
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			node1.storeEnvelope(env, smith);
			// shutdown node 2
			PastryNodeImpl node2 = nodes.remove(1);
			node2.shutDown();
			// update envelope
			EnvelopeVersion updated = node1.createUnencryptedEnvelope(env, "This is las2peer again!");
			long start = System.currentTimeMillis();
			node1.storeEnvelope(updated, smith);
			long stop = System.currentTimeMillis();
			System.out.println(stop - start);
			// start node 2 again (still with version 1 of env in storage
			node2 = TestSuite.addNode(node1.getPort(), STORAGE_MODE.FILESYSTEM, 1L);
			nodes.set(1, node2);
			// read data
			EnvelopeVersion fetched = node2.fetchEnvelope("test");
			Assert.assertEquals(updated.getContent(), fetched.getContent());
			/*
			 * The test output should also end with the following lines:
			 * 
			 * Looking for metadata envelope with identifier 'test' and version 1 at id FDC232A97E1E3E66C6023E1306DC2C1CA9EFF2F9 ...
			Lookup got 6 past handles for identifier 'test' and version 1
			Looking for metadata envelope with identifier 'test' and version 2 at id 1B225008E192E98014E4F24EC6655D160F0AFB86 ...
			Lookup got 5 past handles for identifier 'test' and version 2
			Looking for metadata envelope with identifier 'test' and version 3 at id B7B3972C41D6E0E480F4292DA52AD6140B51BFD2 ...
			Lookup got 0 past handles for identifier 'test' and version 3
			 * 
			 * Version 1 should have one more handle than version 2 and version 3 has zero handles.
			 * 
			 */
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
