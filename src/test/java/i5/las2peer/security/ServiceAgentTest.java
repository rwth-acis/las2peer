package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;

public class ServiceAgentTest {
	private static final String servicename = "i5.las2peer.somePackage.AService";
	private static final String serviceversion = "1.0";
	private static final String passphrase = "a passphrase";

	@Test
	public void testCreation() throws CryptoException, L2pSecurityException {
		ServiceAgent testee = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString(servicename + "@" + serviceversion), passphrase);

		assertEquals(servicename + "@" + serviceversion, testee.getServiceNameVersion().toString());

		assertTrue(testee.isLocked());

		try {
			testee.unlockPrivateKey("dummy");
			fail("L2pSecurityException expected!");
		} catch (L2pSecurityException e) {
			// intended
			assertTrue(testee.isLocked());
		}

		testee.unlockPrivateKey(passphrase);

		assertFalse(testee.isLocked());
	}

	@Test
	public void testXmlAndBack() throws CryptoException, L2pSecurityException, MalformedXMLException {
		ServiceAgent testee = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString(servicename + "@" + serviceversion), passphrase);

		String xml = testee.toXmlString();

		ServiceAgent andBack = ServiceAgent.createFromXml(xml);

		andBack.unlockPrivateKey(passphrase);
	}

	@Test
	public void testId() {
		assertEquals(ServiceAgent.serviceClass2Id(ServiceNameVersion.fromString(servicename + "@" + serviceversion)),
				ServiceAgent.serviceClass2Id(ServiceNameVersion.fromString(servicename + "@" + serviceversion)));
		assertFalse(ServiceAgent
				.serviceClass2Id(ServiceNameVersion.fromString(servicename + "@" + serviceversion)) == ServiceAgent
						.serviceClass2Id(ServiceNameVersion.fromString(servicename + "x" + "@" + serviceversion)));
	}

}
