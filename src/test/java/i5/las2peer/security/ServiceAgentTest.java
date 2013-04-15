package i5.las2peer.security;

import static org.junit.Assert.*;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;

import org.junit.Test;

public class ServiceAgentTest {
	private final static String servicename = "i5.las2peer.somePackage.AService";
	private final static String passphrase = "a passphrase";

	@Test
	public void testCreation() throws CryptoException, L2pSecurityException {
		ServiceAgent testee = ServiceAgent.generateNewAgent(servicename, passphrase);
	
		assertEquals ( servicename, testee.getServiceClassName());
		
		assertTrue ( testee.isLocked());

		try {
			testee.unlockPrivateKey("dummy");
			fail ( "L2pSecurityException expected!");
		} catch (L2pSecurityException e) {
			// intended
			assertTrue ( testee.isLocked());
		}
		
		testee.unlockPrivateKey(passphrase);
		
		assertFalse ( testee.isLocked());
	}
	
	@Test
	public void testXmlAndBack() throws CryptoException, L2pSecurityException, MalformedXMLException {
		ServiceAgent testee = ServiceAgent.generateNewAgent ( servicename, passphrase);
		
		String xml = testee.toXmlString();
		
		ServiceAgent andBack = ServiceAgent.createFromXml(xml);
		
		andBack.unlockPrivateKey(passphrase);
	}
	
	@Test
	public void testId () {
		assertEquals ( ServiceAgent.serviceClass2Id(servicename), ServiceAgent.serviceClass2Id(servicename));
		assertFalse ( ServiceAgent.serviceClass2Id(servicename) == ServiceAgent.serviceClass2Id(servicename + "x") );
	}

}
