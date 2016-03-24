package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.pastry.PastryStorageException;
import i5.las2peer.tools.CryptoException;

public class UserAgentManagerTest {

	@Test
	public void testLogin() throws L2pSecurityException, CryptoException, AgentAlreadyRegisteredException, AgentException, PastryStorageException {
		Node node = LocalNode.launchNode();
		UserAgentManager l = node.getUserManager();

		UserAgent a = UserAgent.createUserAgent("pass");
		a.unlockPrivateKey("pass");
		a.setLoginName("login");
		
		node.storeAgent(a);

		assertEquals(a.getId(), l.getAgentIdByLogin("login"));

		UserAgent b = UserAgent.createUserAgent("pass");
		b.unlockPrivateKey("pass");
		b.setLoginName("login");

		try {
			node.storeAgent(b);
			fail("DuplicateLoginNameException expected");
		} catch (DuplicateLoginNameException e) {
			// intended
		}

		b.setLoginName("login2");
		node.updateAgent(b);

		assertEquals(b.getId(), l.getAgentIdByLogin("login2"));

		b.setLoginName("LOGIN");
		try {
			node.updateAgent(b);
			fail("DuplicateLoginNameException expected");
		} catch (DuplicateLoginNameException e) {
			// intended
		}
		assertEquals(a.getId(), l.getAgentIdByLogin("LOGIN"));

		try {
			l.getAgentIdByLogin("fdewfue");
			fail("AgentNotKnownException expected");
		} catch (AgentNotKnownException e) {
			// indended
		}
	}

	@Test
	public void testEmail() throws L2pSecurityException, CryptoException, PastryStorageException, AgentException {
		Node node = LocalNode.launchNode();
		UserAgentManager l = node.getUserManager();

		UserAgent a = UserAgent.createUserAgent("pass");
		a.unlockPrivateKey("pass");
		a.setEmail("email@example.com");

		node.storeAgent(a);

		assertEquals(a.getId(), l.getAgentIdByEmail("email@example.com"));

		UserAgent b = UserAgent.createUserAgent("pass");
		b.unlockPrivateKey("pass");
		b.setEmail("email@example.com");
		try {
			node.storeAgent(b);
			fail("DuplicateEmailException expected");
		} catch (DuplicateEmailException e) {
			// intended
		}

		b.setEmail("EMAIL@example.com");
		try {
			node.updateAgent(b);
			fail("DuplicateEmailException expected");
		} catch (DuplicateEmailException e) {
			// intended
		}

		b.setEmail("email2@example.com");
		node.updateAgent(b);
		assertEquals(b.getId(), l.getAgentIdByEmail("email2@example.com"));

		b.setEmail("email3@example.com");
		node.updateAgent(b);
		assertEquals(b.getId(), l.getAgentIdByEmail("email3@example.com"));
		assertEquals(b.getId(), l.getAgentIdByEmail("EMAIL3@example.com"));

		try {
			l.getAgentIdByEmail("fdewfue");
			fail("AgentNotKnownException expected");
		} catch (AgentNotKnownException e) {
			// indended
		}
	}

}
