package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class ContextTest {

	// @Test
	public void testCreation() throws L2pSecurityException, MalformedXMLException, IOException {

		// idea has changes, context agent may be locked!

		LocalNode node = LocalNode.launchNode();
		UserAgent eve = MockAgentFactory.getEve();
		try {
			new AgentContext(node, eve);
			fail("L2pSecurityException expected");
		} catch (L2pSecurityException e) {
		}

		eve.unlockPrivateKey("evespass");
		AgentContext testee = new AgentContext(node, eve);

		assertSame(eve, testee.getMainAgent());
	}

	@Test
	public void testRequestGroupAgent() throws MalformedXMLException, IOException, L2pSecurityException,
			CryptoException, SerializationException, AgentException, NodeException {
		LocalNode node = LocalNode.newNode();

		GroupAgent group1 = MockAgentFactory.getGroup1();
		GroupAgent groupA = MockAgentFactory.getGroupA();
		GroupAgent groupSuper = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
		try {
			node.storeAgent(group1);
		} catch (AgentAlreadyRegisteredException e) {
		}
		try {
			node.storeAgent(groupA);
		} catch (AgentAlreadyRegisteredException e) {
		}
		node.storeAgent(groupSuper);

		node.launch();

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");

		AgentContext context = new AgentContext(node, eve);

		try {
			GroupAgent a = context.requestGroupAgent(group1.getId());
			assertEquals(a.getId(), group1.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}

		try {
			context.requestGroupAgent(groupA.getId());
			fail("exception expected");
		} catch (Exception e) {
		}

		try {
			GroupAgent a = context.requestGroupAgent(groupSuper.getId());
			assertEquals(a.getId(), groupSuper.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}
	}

	@Test
	public void testOpenEnvelope() {
		try {
			LocalNode node = LocalNode.newNode();

			GroupAgent group1 = MockAgentFactory.getGroup1();
			GroupAgent groupA = MockAgentFactory.getGroupA();
			GroupAgent groupSuper = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
			GroupAgent groupSuper2 = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
			try {
				node.storeAgent(group1);
			} catch (AgentAlreadyRegisteredException e) {
			}
			try {
				node.storeAgent(groupA);
			} catch (AgentAlreadyRegisteredException e) {
			}
			node.storeAgent(groupSuper);
			node.storeAgent(groupSuper2);

			node.launch();

			UserAgent adam = MockAgentFactory.getAdam();
			adam.unlockPrivateKey("adamspass");
			UserAgent eve = MockAgentFactory.getEve();
			eve.unlockPrivateKey("evespass");

			groupA.unlockPrivateKey(adam);
			groupSuper.unlockPrivateKey(groupA);
			groupSuper2.unlockPrivateKey(groupA);

			AgentContext context = new AgentContext(node, eve);

			Envelope envelope1 = context.createEnvelope("id", "content", groupSuper, groupSuper2);
			Envelope envelopeA = context.createEnvelope("id", "content", groupA);
			context.storeEnvelope(envelope1, groupSuper2);

			try {
				envelopeA.getContent(context.getMainAgent());
				fail("exception expected");
			} catch (Exception e) {
			}

			node.shutDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
