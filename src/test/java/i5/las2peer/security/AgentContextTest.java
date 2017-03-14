package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

public class AgentContextTest {

	@Test
	public void testRequestAgent() throws MalformedXMLException, IOException, L2pSecurityException, CryptoException,
			SerializationException, AgentException, NodeException, AgentAccessDeniedException {
		LocalNode node = LocalNode.newNode();

		GroupAgentImpl group1 = MockAgentFactory.getGroup1();
		GroupAgentImpl groupA = MockAgentFactory.getGroupA();
		GroupAgentImpl groupSuper = GroupAgentImpl.createGroupAgent(new AgentImpl[] { group1, groupA });
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

		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");

		AgentContext context = new AgentContext(node, eve);

		try {
			GroupAgentImpl a = (GroupAgentImpl) context.requestAgent(group1.getIdentifier());
			assertEquals(a.getIdentifier(), group1.getIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}

		try {
			context.requestAgent(groupA.getIdentifier());
			fail("exception expected");
		} catch (Exception e) {
		}

		try {
			GroupAgentImpl a = (GroupAgentImpl) context.requestAgent(groupSuper.getIdentifier());
			assertEquals(a.getIdentifier(), groupSuper.getIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}
	}

	@Test
	public void testHasAccess() throws MalformedXMLException, IOException, L2pSecurityException, CryptoException,
			SerializationException, AgentException, NodeException, AgentAccessDeniedException {
		LocalNode node = LocalNode.newNode();

		GroupAgentImpl group1 = MockAgentFactory.getGroup1();
		GroupAgentImpl groupA = MockAgentFactory.getGroupA();
		GroupAgentImpl groupSuper = GroupAgentImpl.createGroupAgent(new AgentImpl[] { group1, groupA });
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

		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");

		AgentContext context = new AgentContext(node, eve);

		try {
			boolean result = context.hasAccess(group1.getIdentifier());
			assertTrue(result);
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}

		try {
			boolean result = context.hasAccess(groupA.getIdentifier());
			assertFalse(result);
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}

		try {
			boolean result = context.hasAccess(groupSuper.getIdentifier());
			assertTrue(result);
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception thrown: " + e);
		}
	}
}
