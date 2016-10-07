package i5.las2peer.persistency;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class AgentUpdateTest {

	/**
	 * This test launches a network with two nodes. The first node creates an GroupAgent and modifies it. The second
	 * node verifies that all changes are correctly stored in the network.
	 */
	@Test
	public void testGroupAgentUpdate() {
		try {
			// launch network
			ArrayList<PastryNodeImpl> nodes = TestSuite.launchNetwork(2);
			PastryNodeImpl firstNode = nodes.get(0);
			// create agents
			UserAgentImpl smith = MockAgentFactory.getAdam();
			UserAgentImpl neo = MockAgentFactory.getEve();
			neo.unlock("evespass");
			GroupAgentImpl group = GroupAgentImpl.createGroupAgent(new AgentImpl[] { smith, neo });
			group.unlockPrivateKey(neo);
			// store agents
			firstNode.storeAgent(group);
			PastryNodeImpl secondNode = nodes.get(1);
			GroupAgentImpl fetched = (GroupAgentImpl) secondNode.getAgent(group.getSafeId());
			// check attributes of both GroupAgents
			Assert.assertEquals(group.getSafeId(), fetched.getSafeId());
			Assert.assertEquals(group.getName(), fetched.getName());
			Assert.assertEquals(group.getPublicKey(), fetched.getPublicKey());
			Assert.assertEquals(group.getSize(), fetched.getSize());
			Assert.assertArrayEquals(group.getMemberList(), fetched.getMemberList());
			// update GroupAgent on first node
			UserAgentImpl morpheus = MockAgentFactory.getAbel();
			group.addMember(morpheus);
			firstNode.storeAgent(group);
			// fetch GroupAgent again on second node
			fetched = (GroupAgentImpl) secondNode.getAgent(group.getSafeId());
			// check (again) attributes of both GroupAgents
			Assert.assertEquals(group.getSafeId(), fetched.getSafeId());
			Assert.assertEquals(group.getName(), fetched.getName());
			Assert.assertEquals(group.getPublicKey(), fetched.getPublicKey());
			Assert.assertEquals(group.getSize(), fetched.getSize());
			Assert.assertArrayEquals(group.getMemberList(), fetched.getMemberList());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

}
