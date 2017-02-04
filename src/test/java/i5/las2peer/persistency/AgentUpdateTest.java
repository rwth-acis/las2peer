package i5.las2peer.persistency;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.Agent;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;

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
			UserAgent smith = MockAgentFactory.getAdam();
			UserAgent neo = MockAgentFactory.getEve();
			neo.unlockPrivateKey("evespass");
			GroupAgent group = GroupAgent.createGroupAgent(new Agent[] { smith, neo });
			group.unlockPrivateKey(neo);
			// store agents
			firstNode.storeAgent(group);
			PastryNodeImpl secondNode = nodes.get(1);
			GroupAgent fetched = (GroupAgent) secondNode.getAgent(group.getSafeId());
			// check attributes of both GroupAgents
			Assert.assertEquals(group.getSafeId(), fetched.getSafeId());
			Assert.assertEquals(group.getName(), fetched.getName());
			Assert.assertEquals(group.getPublicKey(), fetched.getPublicKey());
			Assert.assertEquals(group.getSize(), fetched.getSize());
			Assert.assertArrayEquals(group.getMemberList(), fetched.getMemberList());
			// update GroupAgent on first node
			UserAgent morpheus = MockAgentFactory.getAbel();
			group.addMember(morpheus);
			firstNode.storeAgent(group);
			// fetch GroupAgent again on second node
			fetched = (GroupAgent) secondNode.getAgent(group.getSafeId());
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
