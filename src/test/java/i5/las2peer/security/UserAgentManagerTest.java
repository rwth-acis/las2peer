package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.EmailAlreadyTakenException;
import i5.las2peer.api.security.LoginNameAlreadyTakenException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.Node;

public class UserAgentManagerTest {

	@Test
	public void testLogin() {
		try {
			Node node = LocalNode.launchNode();
			UserAgentManager l = node.getUserManager();

			UserAgentImpl a = UserAgentImpl.createUserAgent("pass");
			a.unlock("pass");
			a.setLoginName("login");

			node.storeAgent(a);

			assertEquals(a.getIdentifier(), l.getAgentIdByLogin("login"));

			UserAgentImpl b = UserAgentImpl.createUserAgent("pass");
			b.unlock("pass");
			b.setLoginName("login");

			try {
				node.storeAgent(b);
				fail("LoginNameAlreadyTakenException expected");
			} catch (LoginNameAlreadyTakenException e) {
				// intended
			}

			b.setLoginName("login2");
			node.storeAgent(b);

			assertEquals(b.getIdentifier(), l.getAgentIdByLogin("login2"));

			b.setLoginName("LOGIN");
			try {
				node.storeAgent(b);
				fail("LoginNameAlreadyTakenException expected");
			} catch (LoginNameAlreadyTakenException e) {
				// intended
			}
			assertEquals(a.getIdentifier(), l.getAgentIdByLogin("LOGIN"));

			try {
				l.getAgentIdByLogin("fdewfue");
				fail("AgentNotFoundException expected");
			} catch (AgentNotFoundException e) {
				// intended
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testEmail() {
		try {
			Node node = LocalNode.launchNode();
			UserAgentManager l = node.getUserManager();

			UserAgentImpl a = UserAgentImpl.createUserAgent("pass");
			a.unlock("pass");
			a.setEmail("email@example.com");

			node.storeAgent(a);

			assertEquals(a.getIdentifier(), l.getAgentIdByEmail("email@example.com"));

			UserAgentImpl b = UserAgentImpl.createUserAgent("pass");
			b.unlock("pass");
			b.setEmail("email@example.com");
			try {
				node.storeAgent(b);
				fail("EmailAlreadyTakenException expected");
			} catch (EmailAlreadyTakenException e) {
				// intended
			}

			b.setEmail("EMAIL@example.com");
			try {
				node.storeAgent(b);
				fail("EmailAlreadyTakenException expected");
			} catch (EmailAlreadyTakenException e) {
				// intended
			}

			b.setEmail("email2@example.com");
			node.storeAgent(b);
			assertEquals(b.getIdentifier(), l.getAgentIdByEmail("email2@example.com"));

			b.setEmail("email3@example.com");
			node.storeAgent(b);
			assertEquals(b.getIdentifier(), l.getAgentIdByEmail("email3@example.com"));
			assertEquals(b.getIdentifier(), l.getAgentIdByEmail("EMAIL3@example.com"));

			try {
				l.getAgentIdByEmail("fdewfue");
				fail("AgentNotFoundException expected");
			} catch (AgentNotFoundException e) {
				// intended
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testAnonymous() {
		try {
			Node node = LocalNode.launchNode();
			String a = node.getAgentIdForLogin("anonymous");
			assertEquals(a, node.getAnonymous().getIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
