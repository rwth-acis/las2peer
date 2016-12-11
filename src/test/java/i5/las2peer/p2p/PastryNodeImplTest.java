package i5.las2peer.p2p;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.testing.TestSuite;

public class PastryNodeImplTest {

	// TODO add more PastryNodeImpl tests

	@Test
	public void testSystemDefinedPort() {
		try {
			PastryNodeImpl testNode = TestSuite.launchNetwork(1).get(0);
			Assert.assertNotEquals(testNode.getPort(), 0);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
