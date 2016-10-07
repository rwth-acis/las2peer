package i5.las2peer.persistency;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MissingNodesTest {

	private static final Random r = new Random();
	private ArrayList<PastryNodeImpl> nodes;

	@Before
	public void startNetwork() {
		try {
			// start test node
			nodes = TestSuite.launchNetwork(SharedStorage.DEFAULT_NUM_OF_REPLICAS + 1);
			System.out.println("Test network with " + nodes.size() + " nodes started");
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
	public void testStoreWithMissingNode() {
		try {
			// remove a node from network
			nodes.remove(r.nextInt(nodes.size())).shutDown();
			// store test content
			PastryNodeImpl active = nodes.get(r.nextInt(nodes.size()));
			EnvelopeVersion env = active.createUnencryptedEnvelope("test", "This is las2peer!");
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			long start = System.currentTimeMillis();
			active.storeEnvelope(env, smith);
			long delay = System.currentTimeMillis() - start;
			System.out.println("Storing envelope with missing node took " + delay + "ms");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testFetchWithMissingNode() {
		try {
			// store test content
			PastryNodeImpl active = nodes.get(r.nextInt(nodes.size()));
			EnvelopeVersion env = active.createUnencryptedEnvelope("test", "This is las2peer!");
			UserAgentImpl smith = MockAgentFactory.getAdam();
			smith.unlock("adamspass");
			active.storeEnvelope(env, smith);
			// shutdown a random node
			nodes.remove(r.nextInt(nodes.size())).shutDown();
			// fetch content with random node
			PastryNodeImpl fetching = nodes.get(r.nextInt(nodes.size()));
			long start = System.currentTimeMillis();
			EnvelopeVersion fetched = fetching.fetchEnvelope("test");
			long delay = System.currentTimeMillis() - start;
			// verify content
			Assert.assertEquals(env.getContent(), fetched.getContent());
			System.out.println("Fetching envelope with missing node took " + delay + "ms");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
