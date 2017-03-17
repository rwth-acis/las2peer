package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class UserAgentImplTest {

	@Test
	public void testUnlocking()
			throws NoSuchAlgorithmException, CryptoException, AgentAccessDeniedException, L2pSecurityException {
		String passphrase = "A passphrase to unlock";
		UserAgentImpl a = UserAgentImpl.createUserAgent(passphrase);

		try {
			a.decryptSymmetricKey(null); // not possible without unlocking the private key first
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
			// Should be thrown
		} catch (SerializationException e) {
			fail("SecurityException should have been thrown");
			e.printStackTrace();
		}

		try {
			a.unlock("bad passphrase");
			fail("SecurityException should have been thrown");
		} catch (AgentAccessDeniedException e) {
		}

		try {
			a.decryptSymmetricKey(null); // not possible without unlocking the private key first
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
			// Should be thrown
		} catch (SerializationException e) {
			fail("SecurityException should have been thrown");
			e.printStackTrace();
		}
		a.unlock(passphrase);

		try {
			a.decryptSymmetricKey(null); // should be possible now
		} catch (IllegalArgumentException e) {
			// Well...empty byte array..but ok since no security exception is thrown
		} catch (SerializationException e) {
			fail("Illegal argument exception should have been thrown");
			e.printStackTrace();
		}
	}

	@Test
	public void testPassphraseChange()
			throws NoSuchAlgorithmException, L2pSecurityException, CryptoException, AgentAccessDeniedException, AgentLockedException {
		String passphrase = "a passphrase";
		UserAgentImpl a = UserAgentImpl.createUserAgent(passphrase);

		String sndPass = "ein anderes Passphrase";
		try {
			a.changePassphrase(sndPass);
			fail("AgentLockedException expected");
		} catch (AgentLockedException e) {
		}
		a.unlock(passphrase);

		a.changePassphrase(sndPass);
		a.lockPrivateKey();

		try {
			a.unlock(passphrase);
			fail("AgentAccessDeniedException expected");
		} catch (AgentAccessDeniedException e) {
		}

		a.unlock(sndPass);
	}

	@Test
	public void testXml() throws NoSuchAlgorithmException, L2pSecurityException, MalformedXMLException, CryptoException,
			UserAgentException, AgentAccessDeniedException {
		String passphrase = "a pass";
		String email = "usera@example.org";
		String userData = "This is the user data attachement.";
		UserAgentImpl a = UserAgentImpl.createUserAgent(passphrase);
		a.unlock(passphrase);
		a.setEmail(email);
		a.setUserData(userData);

		String xml = a.toXmlString();
		System.out.println(xml);

		UserAgentImpl b = UserAgentImpl.createFromXml(xml);

		assertEquals(a.getIdentifier(), b.getIdentifier());
		assertEquals(email, b.getEmail());
		assertEquals(userData, b.getUserData());
	}

	public void testLogin() throws CryptoException, L2pSecurityException, MalformedXMLException, UserAgentException,
			AgentAccessDeniedException {
		UserAgentImpl a = UserAgentImpl.createUserAgent("test");
		a.unlock("test");

		a.setLoginName("login");

		String xml = a.toXmlString();

		UserAgentImpl andBack = UserAgentImpl.createFromXml(xml);

		assertEquals("login", andBack.getLoginName());
	}

	@Test
	public void testEmail() throws CryptoException, L2pSecurityException, MalformedXMLException, UserAgentException,
			AgentAccessDeniedException {
		UserAgentImpl a = UserAgentImpl.createUserAgent("test");
		a.unlock("test");

		a.setEmail("test@bla");

		String xml = a.toXmlString();

		UserAgentImpl andBack = UserAgentImpl.createFromXml(xml);

		assertEquals("test@bla", andBack.getEmail());
	}

	@Test
	public void testEmailAndLogin() throws CryptoException, L2pSecurityException, MalformedXMLException,
			UserAgentException, AgentAccessDeniedException {
		UserAgentImpl a = UserAgentImpl.createUserAgent("test");
		a.unlock("test");

		a.setEmail("test@bla");
		a.setLoginName("login");

		String xml = a.toXmlString();

		UserAgentImpl andBack = UserAgentImpl.createFromXml(xml);

		assertEquals("test@bla", andBack.getEmail());
		assertEquals("login", andBack.getLoginName());
	}

	@Test
	public void testLoginExceptions() throws L2pSecurityException, CryptoException, AgentAccessDeniedException {
		UserAgentImpl a = UserAgentImpl.createUserAgent("test");
		a.unlock("test");

		try {
			a.setLoginName("12323");
		} catch (UserAgentException e) {
			assertTrue(e.getMessage().contains("a-z"));
		}
		try {
			a.setLoginName("123");
		} catch (UserAgentException e) {
			assertTrue(e.getMessage().contains("longer"));
		}
	}

	@Test
	public void testEmailExceptions() throws L2pSecurityException, CryptoException, AgentAccessDeniedException {
		UserAgentImpl a = UserAgentImpl.createUserAgent("test");
		a.unlock("test");

		try {
			a.setEmail("afduaewd");
			fail("Exception expected");
		} catch (UserAgentException e) {
			// assertTrue(e.getMessage().contains("@"));
		}
	}

}
