package i5.las2peer.httpConnector;

import static org.junit.Assert.*;

import java.io.IOException;

import i5.las2peer.api.ConnectorException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import org.junit.Test;

public class HttpSessionTest {

	
	
	
	
	@Test
	public void testConstructor() throws L2pSecurityException, MalformedXMLException, IOException {
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		Mediator evesMediator = new Mediator ( eve );
		
		HttpSession testee = new HttpSession (evesMediator, "aConnectionString", "a pass");
		
		assertEquals ( eve.getId(), testee.getAgentId());
		assertTrue ( testee.isAttached());
		assertFalse ( testee.isPersistent());
		
		assertTrue ( testee.getTimeout() <= 0 );
		assertTrue ( testee.getPersistentTimeout() <= 0);
		
		assertFalse ( testee.isExpired());
		assertFalse ( testee.isOutdated());

	}
	
	
	@Test
	public void testPersistency () throws InterruptedException, L2pSecurityException, MalformedXMLException, IOException {
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		Mediator evesMediator = new Mediator ( eve );
		
		HttpSession testee = new HttpSession (evesMediator, "aConnectionString", "a pass");

		testee.setPersistent();
		assertTrue ( testee.isPersistent());

		testee.setPersistentTimeout(2000);
		testee.touch();
		assertFalse ( testee.isOutdated());
		
		Thread.sleep(1000);
		assertFalse ( testee.isOutdated ());
		
		Thread.sleep( 1500);
		assertTrue ( testee.isOutdated());
	}
	
	@Test
	public void testConnectionString () throws AddressNotAllowedException, L2pSecurityException, MalformedXMLException, IOException {
		String connectionString = "a connection string";
		
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		Mediator evesMediator = new Mediator ( eve );
		
		HttpSession testee = new HttpSession (evesMediator, connectionString, "a pass");
		
		testee.checkRemoteAccess(connectionString);
		
		try {
			testee.checkRemoteAccess("caoefefh");
			fail ( "AddressNotAllowedException expected");
		} catch ( AddressNotAllowedException e ) {}
		
	}
	
	
	@Test
	public void testTimeout () throws MalformedXMLException, IOException, InterruptedException, L2pSecurityException {
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		Mediator evesMediator = new Mediator ( eve );
		
		HttpSession testee = new HttpSession (evesMediator, "aConnectionString", "a pass");
		testee.setPersistent();
		testee.setTimeout ( 2000 );
		testee.setPersistentTimeout(  5000);
		
		assertFalse ( testee.isExpired());
		assertFalse ( testee.isOutdated());
		
		Thread.sleep ( 2500 );
		assertTrue ( testee.isExpired () );
		assertFalse ( testee.isOutdated());
		
		Thread.sleep( 3000 );
		assertTrue ( testee.isExpired () );
		assertTrue ( testee.isOutdated ());
		
		testee.touch();
		assertFalse ( testee.isExpired());
		assertFalse ( testee.isOutdated());
	}
	
	@Test
	public void testAttaching () throws ConnectorException, MalformedXMLException, IOException, L2pSecurityException {
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		Mediator evesMediator = new Mediator ( eve );
		
		HttpSession testee = new HttpSession (evesMediator, "aConnectionString", "a pass");
		
		try {
			testee.detach ();
			fail ( "ConnectorException expected");
		} catch ( ConnectorException e ) {}
		
	
		testee.setPersistent();
		testee.detach();
		
		assertFalse ( testee.isAttached ());
		
		testee.attach ();
		assertTrue ( testee.isAttached () );
		
		
	}

}
