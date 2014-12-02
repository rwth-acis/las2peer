package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class UserAgentTest {

	@Test
	public void testUnlocking() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException {
		String passphrase = "A passphrase to unlock";
		UserAgent a = UserAgent.createUserAgent(passphrase);

		try {
			a.returnSecretKey(null); //not possible without unlocking the private key first
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
			//Should be thrown
		} catch (SerializationException e) {
			fail("SecurityException should have been thrown");
			e.printStackTrace();
		}

		try {
			a.unlockPrivateKey("bad passphrase");
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
		}

		try {
			a.returnSecretKey(null); //not possible without unlocking the private key first
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
			//Should be thrown
		} catch (SerializationException e) {
			fail("SecurityException should have been thrown");
			e.printStackTrace();
		}
		a.unlockPrivateKey(passphrase);

		try {
			a.returnSecretKey(null); //should be possible now
		} catch (IllegalArgumentException e) {
			// Well...empty byte array..but ok since no security exception is thrown
		} catch (SerializationException e) {
			fail("Illegal argument exception should have been thrown");
			e.printStackTrace();
		}
	}

	@Test
	public void testPassphraseChange() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException {
		String passphrase = "a passphrase";
		UserAgent a = UserAgent.createUserAgent(passphrase);

		String sndPass = "ein anderes Passphrase";
		try {
			a.changePassphrase(sndPass);
			fail("SecurityException expected");
		} catch (L2pSecurityException e) {
		}
		a.unlockPrivateKey(passphrase);

		a.changePassphrase(sndPass);
		a.lockPrivateKey();

		try {
			a.unlockPrivateKey(passphrase);
			fail("SecurityException expected");
		} catch (L2pSecurityException e) {
		}

		a.unlockPrivateKey(sndPass);
	}

	@Test
	public void testXml() throws NoSuchAlgorithmException, L2pSecurityException, MalformedXMLException, CryptoException, UserAgentException {
		String passphrase = "a pass";
		UserAgent a = UserAgent.createUserAgent(passphrase);
		a.unlockPrivateKey(passphrase);
		a.setEmail("usera@example.org");
		a.setUserData("This is the user data attachement.");

		String xml = a.toXmlString();
		System.out.println(xml);

		UserAgent b = UserAgent.createFromXml(xml);

		assertEquals(a.getId(), b.getId());
		assertEquals(a.getEmail(), b.getEmail());
		assertEquals(a.getUserData(), b.getUserData());
	}

	public void testLogin() throws CryptoException, L2pSecurityException, MalformedXMLException, UserAgentException {
		UserAgent a = UserAgent.createUserAgent("test");
		a.unlockPrivateKey("test");

		a.setLoginName("login");

		String xml = a.toXmlString();

		UserAgent andBack = UserAgent.createFromXml(xml);

		assertEquals("login", andBack.getLoginName());
	}

	@Test
	public void testEmail() throws CryptoException, L2pSecurityException, MalformedXMLException, UserAgentException {
		UserAgent a = UserAgent.createUserAgent("test");
		a.unlockPrivateKey("test");

		a.setEmail("test@bla");

		String xml = a.toXmlString();

		UserAgent andBack = UserAgent.createFromXml(xml);

		assertEquals("test@bla", andBack.getEmail());
	}

	@Test
	public void testEmailAndLogin() throws CryptoException, L2pSecurityException, MalformedXMLException,
			UserAgentException {
		UserAgent a = UserAgent.createUserAgent("test");
		a.unlockPrivateKey("test");

		a.setEmail("test@bla");
		a.setLoginName("login");

		String xml = a.toXmlString();

		UserAgent andBack = UserAgent.createFromXml(xml);

		assertEquals("test@bla", andBack.getEmail());
		assertEquals("login", andBack.getLoginName());
	}

	@Test
	public void testLoginExceptions() throws L2pSecurityException, CryptoException {
		UserAgent a = UserAgent.createUserAgent("test");
		a.unlockPrivateKey("test");

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
	public void testEmailExceptions() throws L2pSecurityException, CryptoException {
		UserAgent a = UserAgent.createUserAgent("test");
		a.unlockPrivateKey("test");

		try {
			a.setEmail("afduaewd");
		} catch (UserAgentException e) {
			assertTrue(e.getMessage().contains("@"));
		}
	}

}
