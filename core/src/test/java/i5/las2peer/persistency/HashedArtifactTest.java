package i5.las2peer.persistency;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.testing.TestSuite;
import i5.las2peer.tools.CryptoTools;

public class HashedArtifactTest {

	@Test
	public void testStoreAndFetch() {
		try {
			ArrayList<PastryNodeImpl> nodes = TestSuite.launchNetwork(SharedStorage.DEFAULT_NUM_OF_REPLICAS + 1);
			PastryNodeImpl firstNode = nodes.get(0);
			byte[] testData = new String("This is las2peer!").getBytes(StandardCharsets.UTF_8);
			byte[] hash = CryptoTools.getSecureHash(testData);
			firstNode.storeHashedContent(testData);
			byte[] fetched = firstNode.fetchHashedContent(hash);
			Assert.assertArrayEquals(testData, fetched);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testStoreOverwrite() {
		try {
			ArrayList<PastryNodeImpl> nodes = TestSuite.launchNetwork(SharedStorage.DEFAULT_NUM_OF_REPLICAS + 1);
			PastryNodeImpl firstNode = nodes.get(0);
			byte[] testData = new String("This is las2peer!").getBytes(StandardCharsets.UTF_8);
			byte[] hash = CryptoTools.getSecureHash(testData);
			firstNode.storeHashedContent(testData);
			firstNode.storeHashedContent(testData);
			byte[] fetched = firstNode.fetchHashedContent(hash);
			Assert.assertArrayEquals(testData, fetched);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
