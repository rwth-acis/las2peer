package i5.las2peer.security;

import static org.junit.Assert.*;

import java.io.IOException;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;

import org.junit.Test;

public class BasicAgentStorageTest {

	@Test
	public void testStorage() throws AgentNotKnownException, L2pSecurityException, MalformedXMLException, IOException {
		
		BasicAgentStorage testee = new BasicAgentStorage();
		UserAgent eve = MockAgentFactory.getEve();
		
		eve.unlockPrivateKey("evespass");
		
		
		assertFalse ( testee.hasAgent(eve.getId()));
		testee.registerAgent(eve);
		
		assertTrue ( testee.hasAgent( eve.getId()));
		
		assertNotSame ( eve, testee.getAgent( eve.getId()));
		
		assertFalse ( eve.isLocked() );
		
		
		Agent eve2 = testee.getAgent ( eve.getId());
		assertTrue ( eve2.isLocked());
		
		((UserAgent) eve2).unlockPrivateKey("evespass");
		assertFalse ( eve2.isLocked());
		
		assertTrue ( testee.getAgent( eve.getId()).isLocked()  );
	}

}
