package i5.las2peer.security;

import static org.junit.Assert.*;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.tools.CryptoException;

import org.junit.Test;

public class UserAgentListTest {

	@Test
	public void testLogin() throws L2pSecurityException, CryptoException, AgentNotKnownException, UserAgentException {
		UserAgentList l = new UserAgentList ();
		
		UserAgent a = UserAgent.createUserAgent("pass");
		a.unlockPrivateKey("pass");
		a.setLoginName("login");
		
		l.updateUser(a);
		
		assertEquals ( a.getId(), l.getLoginId ( "login"));
		
		UserAgent b = UserAgent.createUserAgent("pass");
		b.unlockPrivateKey("pass");
		b.setLoginName ("login");
		
		try {
			l.updateUser(b);
			fail ( "DuplicateLoginNameException expected");
		} catch (DuplicateLoginNameException e) {
			// intended
		}
		
		b.setLoginName ( "login2");
		l.updateUser ( b );
		
		assertEquals ( b.getId(), l.getLoginId ( "login2"));
		
		b.setLoginName( "LOGIN");
		l.updateUser( b );
		assertEquals ( b.getId(), l.getLoginId ( "LOGIN"));
		
		try {
			l.getLoginId ( "fdewfue");
			fail ( "AgentNotKnownException expected");
		} catch ( AgentNotKnownException e ) {
			// indended
		}
	}

	
	@Test
	public void testEmail() throws L2pSecurityException, CryptoException, AgentNotKnownException, UserAgentException {
		UserAgentList l = new UserAgentList ();
		
		UserAgent a = UserAgent.createUserAgent("pass");
		a.unlockPrivateKey("pass");
		a.setEmail("email@");
		
		l.updateUser(a);
		
		assertEquals ( a.getId(), l.getEmailId ( "email@"));
		
		UserAgent b = UserAgent.createUserAgent("pass");
		b.unlockPrivateKey("pass");
		b.setEmail ("email@");
		try {
			l.updateUser(b);
			fail ( "DuplicateLoginNameException expected");
		} catch (DuplicateEmailException e) {
			// intended
		}
		
		b.setEmail ("EMAIL@");
		try {
			l.updateUser(b);
			fail ( "DuplicateLoginNameException expected");
		} catch (DuplicateEmailException e) {
			// intended
		}
		
		b.setEmail ( "email2@");
		l.updateUser ( b );
		assertEquals ( b.getId(), l.getEmailId ( "email2@"));
		
		b.setEmail ( "email3@");
		l.updateUser( b );
		assertEquals ( b.getId(), l.getEmailId ( "email3@"));
		assertEquals ( b.getId(), l.getEmailId ( "EMAIL3@"));
		
		try {
			l.getEmailId ( "fdewfue");
			fail ( "AgentNotKnownException expected");
		} catch ( AgentNotKnownException e ) {
			// indended
		}
	}
	
	
}
