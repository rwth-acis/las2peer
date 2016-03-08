package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class ContextTest {

	@Test
	public void testLocal() throws L2pSecurityException, MalformedXMLException, IOException {
		LocalNode node = LocalNode.launchNode();

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");

		Context testee = new Context(node, eve);
		assertTrue(testee.isLocal());
		assertNull(testee.getNodeReference());

		testee = new Context(node, eve, 10);
		assertFalse(testee.isLocal());

		assertEquals(10, testee.getNodeReference());
	}

	// @Test
	public void testCreation() throws L2pSecurityException, MalformedXMLException, IOException {

		// idea has changes, context agent may be locked!

		LocalNode node = LocalNode.launchNode();
		UserAgent eve = MockAgentFactory.getEve();
		try {
			new Context(node, eve);
			fail("L2pSecurityException expected");
		} catch (L2pSecurityException e) {
		}

		eve.unlockPrivateKey("evespass");
		Context testee = new Context(node, eve);

		assertSame(eve, testee.getMainAgent());
	}

	@Test
	public void testRequestGroupAgent()
			throws MalformedXMLException, IOException, L2pSecurityException, CryptoException,
			SerializationException, AgentException, NodeException {
		LocalNode node = LocalNode.newNode();

		GroupAgent group1 = MockAgentFactory.getGroup1();
		GroupAgent groupA = MockAgentFactory.getGroupA();
		GroupAgent groupSuper = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
		try {
			node.storeAgent(group1);
		} catch (AgentAlreadyRegisteredException e) {}
		try {
			node.storeAgent(groupA);
		} catch (AgentAlreadyRegisteredException e) {}
		node.storeAgent(groupSuper);

		node.launch();

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");

		Context context = new Context(node, eve);

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
	public void testOpenEnvelope()
			throws MalformedXMLException, IOException, L2pSecurityException, CryptoException,
			SerializationException, AgentException, EncodingFailedException, DecodingFailedException, NodeException {
		LocalNode node = LocalNode.newNode();

		GroupAgent group1 = MockAgentFactory.getGroup1();
		GroupAgent groupA = MockAgentFactory.getGroupA();
		GroupAgent groupSuper = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
		GroupAgent groupSuper2 = GroupAgent.createGroupAgent(new Agent[] { group1, groupA });
		try {
			node.storeAgent(group1);
		} catch (AgentAlreadyRegisteredException e) {}
		try {
			node.storeAgent(groupA);
		} catch (AgentAlreadyRegisteredException e) {}
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

		Context context = new Context(node, eve);
		
		Envelope envelope1 = Envelope.createClassIdEnvelope("content", "id", new Agent[] {groupSuper, groupSuper2});
		Envelope envelopeA = Envelope.createClassIdEnvelope("content", "id", groupA);
		envelope1.open(groupSuper2);
		envelope1.addSignature(groupSuper2);
		envelope1.close();
		envelopeA.close();

		try {
			context.openEnvelope(envelope1);
			assertTrue(envelope1.isOpen());
			assertTrue(envelope1.getOpeningAgent().getId() == groupSuper2.getId()); // check if signing agent is preferred
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}

		try {
			context.openEnvelope(envelopeA);
			assertFalse(envelopeA.isOpen());
			fail("exception expected");
		} catch (Exception e) {
		}
		
		node.shutDown();
	}
}
