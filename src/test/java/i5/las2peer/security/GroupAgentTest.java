package i5.las2peer.security;

import static org.junit.Assert.*;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.security.NoSuchAlgorithmException;

import org.junit.*;

public class GroupAgentTest {
	private UserAgent adam;
	private UserAgent eve ;
	private UserAgent kain;
	private UserAgent abel ;

	
	@Before
	public void setUp () throws NoSuchAlgorithmException, L2pSecurityException, CryptoException {
		adam = UserAgent.createUserAgent("adamspass");
		eve = UserAgent.createUserAgent("evesspass");
		kain = UserAgent.createUserAgent("kainspass");
		abel = UserAgent.createUserAgent("abelspass");
	}
	
	
	
	
	@Test
	public void testXmlAndBack() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException, SerializationException, MalformedXMLException {
		
		GroupAgent testee = GroupAgent.createGroupAgent(new Agent[] { adam, eve });
		assertEquals ( 2, testee.getSize());
		assertFalse ( testee.isMember(kain));
		assertFalse ( testee.isMember(abel.getId()));
		assertTrue ( testee.isMember(adam));
		assertTrue ( testee.isMember(eve.getId()));
		
		assertTrue ( testee.isLocked() );
		
		String xml = testee.toXmlString();
		System.out.println ( xml );
		
		GroupAgent fromXml = GroupAgent.createFromXml(xml);
		
		assertEquals ( 2, fromXml.getSize());
		assertFalse ( testee.isMember(kain));
		assertFalse ( testee.isMember(abel.getId()));
		assertTrue ( testee.isMember(adam));
		assertTrue ( testee.isMember(eve.getId()));
		
		assertTrue ( fromXml.isLocked() );
	}
	
	@Test
	public void testUnlocking () throws L2pSecurityException, CryptoException, SerializationException {
		GroupAgent testee = GroupAgent.createGroupAgent ( new Agent[] { adam, eve });
		
		try {
			testee.addMember(kain);
			fail ( "SecurityException should have been thrown!");
		} catch ( L2pSecurityException e) {}

		try {
			testee.unlockPrivateKey(adam);
			fail ( "SecurityException should have been thrown!");
		} catch ( L2pSecurityException e  ) {}
		
		adam.unlockPrivateKey("adamspass");
		testee.unlockPrivateKey( adam );
		assertSame ( adam, testee.getOpeningAgent ());
		assertFalse ( testee.isLocked());
		
		try {
			testee.unlockPrivateKey( eve );
			fail ( "SecurityException should have been thrown");
		} catch ( L2pSecurityException e ) {}

		testee.lockPrivateKey();
		assertTrue ( testee.isLocked());
		assertNull ( testee.getOpeningAgent());
		
		
	}
	
	@Test
	public void testAdding () throws L2pSecurityException, CryptoException, SerializationException {
		GroupAgent testee = GroupAgent.createGroupAgent ( new Agent[] { adam, eve });
		abel.unlockPrivateKey("abelspass");
		
		try {
			testee.unlockPrivateKey(abel);
			fail ( "SecurityException should have been thrown");
		} catch ( L2pSecurityException e ) {}
		
		eve.unlockPrivateKey("evesspass");
		testee.unlockPrivateKey(eve);
		
		assertFalse ( testee.isMember(abel));
		
		testee.addMember(abel);
		testee.lockPrivateKey();
		
		assertTrue ( testee.isMember ( abel ));
		
		
		testee.unlockPrivateKey(abel);
	}
	
	@Test
	public void testSubGrouping () throws SerializationException, CryptoException, L2pSecurityException {
		GroupAgent subGroup = GroupAgent.createGroupAgent( new Agent[]{adam, eve} );
		GroupAgent superGroup = GroupAgent.createGroupAgent( new Agent[] { abel, subGroup });
		
		assertTrue ( superGroup.isMember(subGroup));
		
		eve.unlockPrivateKey("evesspass");
		try {
			superGroup.unlockPrivateKey(subGroup);
			fail ( "SecurityException should have been thrown!");
		} catch ( L2pSecurityException e ) {}
		
		try {
			superGroup.unlockPrivateKey(eve);
			fail ( "SecurityException should have been thrown!");
		} catch ( L2pSecurityException e ) {}
		
		subGroup.unlockPrivateKey(eve);
		
		superGroup.unlockPrivateKey(subGroup);
		assertSame ( subGroup, superGroup.getOpeningAgent() );
	}
	

}
