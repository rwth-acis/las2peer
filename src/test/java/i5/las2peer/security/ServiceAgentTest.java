package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.ServiceDiscoveryContent;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

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
	public void testServiceDiscovery() throws CryptoException, L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException, MalformedXMLException, IOException, EncodingFailedException, SerializationException,
			InterruptedException, TimeoutException {

		// start node
		LocalNode node = LocalNode.launchNode();

		ServiceAgent testServiceAgent0 = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
		testServiceAgent0.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent0);

		ServiceAgent testServiceAgent1 = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.1"), "a pass");
		testServiceAgent1.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent1);

		ServiceAgent testServiceAgent2 = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.0"), "a pass");
		testServiceAgent2.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent2);

		ServiceAgent testServiceAgent3 = ServiceAgent
				.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a pass");
		testServiceAgent3.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent3);

		PassphraseAgent userAgent = MockAgentFactory.getAdam();
		userAgent.unlockPrivateKey("adamspass");
		node.registerReceiver(userAgent);

		// invoke (fits)
		Message request = new Message(userAgent, ServiceAgent.serviceNameToTopicId("i5.las2peer.api.TestService"),
				new ServiceDiscoveryContent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1"), false),
				30000);

		Message[] answers = node.sendMessageAndCollectAnswers(request, 4);

		assertEquals(answers.length, 2);

		boolean found10 = false, found11 = false;
		for (Message m : answers) {
			m.open(userAgent, node);
			ServiceDiscoveryContent c = (ServiceDiscoveryContent) m.getContent();
			if (c.getService().getVersion().toString().equals("1.0")) {
				found10 = true;
			} else if (c.getService().getVersion().toString().equals("1.1")) {
				found11 = true;
			}
		}
		assertTrue(found10);
		assertTrue(found11);

		// invoke (exact)

		request = new Message(userAgent, ServiceAgent.serviceNameToTopicId("i5.las2peer.api.TestService"),
				new ServiceDiscoveryContent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.1"), true),
				30000);

		answers = node.sendMessageAndCollectAnswers(request, 4);

		assertEquals(answers.length, 1);

		answers[0].open(userAgent, node);
		ServiceDiscoveryContent c = (ServiceDiscoveryContent) answers[0].getContent();
		assertTrue(c.getService().getVersion().toString().equals("1.1"));

	}
}
