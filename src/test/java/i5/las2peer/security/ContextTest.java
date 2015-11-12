package i5.las2peer.security;

import static org.junit.Assert.*;

import java.io.IOException;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;

import org.junit.Test;

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

}
