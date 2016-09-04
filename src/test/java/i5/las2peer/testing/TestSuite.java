package i5.las2peer.testing;

import java.util.ArrayList;

import i5.las2peer.p2p.PastryNodeImpl;

/**
 * This class provides methods for developers to simplify JUnit test creation.
 *
 */
public class TestSuite {

	/**
	 * This method starts a network consisting of the given number of nodes.
	 *
	 * @param numOfNodes The number of nodes that should be in the network.
	 * @return Returns a list with all nodes from the network.
	 * @throws Exception If an error occurs.
	 */
	public static ArrayList<PastryNodeImpl> launchNetwork(int numOfNodes) throws Exception {
		ArrayList<PastryNodeImpl> result = new ArrayList<>();
		// launch bootstrap node
		PastryNodeImpl bootstrapNode = new PastryNodeImpl("");
		bootstrapNode.launch();
		final int bootstrapPort = bootstrapNode.getPort();
		result.add(bootstrapNode);
		System.out.println("bootstrap node launched with id " + bootstrapNode.getNodeId());
		// add more nodes
		for (int num = 1; num <= numOfNodes - 1; num++) {
			PastryNodeImpl node2 = new PastryNodeImpl("localhost:" + bootstrapPort);
			node2.launch();
			result.add(node2);
			System.out.println("network node launched with id " + bootstrapNode.getNodeId());
		}
		return result;
	}

}
