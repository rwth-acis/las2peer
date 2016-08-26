package i5.las2peer.testing;

import java.util.ArrayList;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;

/**
 * This class provides methods for developers to simplify JUnit test creation.
 *
 */
public class TestSuite {

	private static final int bootstrapPort = 14571;

	/**
	 * This method starts a network consisting of the given number of nodes.
	 *
	 * @param numOfNodes The number of nodes that should be in the network.
	 * @return Returns a list with all nodes from the network.
	 * @throws Exception If an error occurs.
	 */
	public static ArrayList<PastryNodeImpl> launchNetwork(int numOfNodes) throws Exception {
		ArrayList<PastryNodeImpl> result = new ArrayList<>();
		// TODO bind to localhost
		// launch bootstrap node
		PastryNodeImpl bootstrapNode = new PastryNodeImpl(bootstrapPort, null, STORAGE_MODE.MEMORY, false, null, null);
		bootstrapNode.launch();
		result.add(bootstrapNode);
		System.out.println("bootstrap node launched with id " + bootstrapNode.getNodeId());
		// add more nodes
		for (int num = 1; num <= numOfNodes - 1; num++) {
			PastryNodeImpl node2 = new PastryNodeImpl(bootstrapPort + num, null, STORAGE_MODE.MEMORY, false, null,
					null);
			node2.launch();
			result.add(node2);
			System.out.println("network node launched with id " + bootstrapNode.getNodeId());
		}
		return result;
	}

}
