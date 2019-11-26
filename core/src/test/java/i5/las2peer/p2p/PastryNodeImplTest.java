package i5.las2peer.p2p;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.testing.TestSuite;

public class PastryNodeImplTest {

	// TODO add more PastryNodeImpl tests

	@Test
	public void testSystemDefinedPort() {
		PastryNodeImpl testNode = null;
		try {
			testNode = TestSuite.launchNetwork(1).get(0);
			Assert.assertNotEquals(0, testNode.getPort());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		} finally {
			testNode.shutDown();
			System.out.println("Node stopped");
		}
	}

	@Test
	public void testNodeRestart() {
		PastryNodeImpl testNode = null;
		try {
			testNode = TestSuite.launchNetwork(1).get(0);
			testNode.shutDown();
			testNode.launch();
			testNode.shutDown();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		} finally {
			testNode.shutDown();
			System.out.println("Node stopped");
		}
	}

	@Test
	public void testStartNetwork() {
		ArrayList<PastryNodeImpl> nodes = null;
		try {
			// start nodes
			nodes = TestSuite.launchNetwork(3);
			System.out.println("Test network started");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			// stop nodes
			if (nodes != null) {
				for (PastryNodeImpl node : nodes) {
					node.shutDown();
					System.out.println("Node " + node.getPort() + " stopped");
				}
				nodes = null;
			}
		}

	}

}
