package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;

public class GroupAgentTest {

	private static final String ADAMSPASS = "adamspass";
	private static final String EVESSPASS = "evesspass";
	private static final String KAINSPASS = "kainspass";
	private static final String ABELSPASS = "abelspass";

	private UserAgent adam;
	private UserAgent eve;
	private UserAgent kain;
	private UserAgent abel;

	@Before
	public void setUp() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException {
		adam = UserAgent.createUserAgent(ADAMSPASS);
		eve = UserAgent.createUserAgent(EVESSPASS);
		kain = UserAgent.createUserAgent(KAINSPASS);
		abel = UserAgent.createUserAgent(ABELSPASS);
	}

	@Test
	public void testXmlAndBack() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException,
			SerializationException, MalformedXMLException {
		GroupAgent testee = GroupAgent.createGroupAgent(new Agent[] { adam, eve });
		assertEquals(2, testee.getSize());
		assertFalse(testee.isMember(kain));
		assertFalse(testee.isMember(abel.getId()));
		assertTrue(testee.isMember(adam));
		assertTrue(testee.isMember(eve.getId()));

		assertTrue(testee.isLocked());

		adam.unlockPrivateKey(ADAMSPASS);
		testee.unlockPrivateKey(adam);
		String groupname = "Test Group";
		testee.setName(groupname);
		String userData = "This is the user data attachement.";
		testee.setUserData(userData);

		String xml = testee.toXmlString();
		System.out.println(xml);

		GroupAgent fromXml = GroupAgent.createFromXml(xml);

		assertEquals(2, fromXml.getSize());
		assertFalse(testee.isMember(kain));
		assertFalse(testee.isMember(abel.getId()));
		assertTrue(testee.isMember(adam));
		assertTrue(testee.isMember(eve.getId()));

		assertTrue(fromXml.isLocked());

		assertEquals(fromXml.getName(), groupname);
		assertEquals(fromXml.getUserData(), userData);
	}

	@Test
	public void testUnlocking() throws L2pSecurityException, CryptoException, SerializationException {
		GroupAgent testee = GroupAgent.createGroupAgent(new Agent[] { adam, eve });

		try {
			testee.addMember(kain);
			fail("SecurityException should have been thrown!");
		} catch (L2pSecurityException e) {
		}

		try {
			testee.unlockPrivateKey(adam);
			fail("SecurityException should have been thrown!");
		} catch (L2pSecurityException e) {
		}

		adam.unlockPrivateKey(ADAMSPASS);
		testee.unlockPrivateKey(adam);
		assertSame(adam, testee.getOpeningAgent());
		assertFalse(testee.isLocked());

		try {
			testee.unlockPrivateKey(eve);
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
		}

		testee.lockPrivateKey();
		assertTrue(testee.isLocked());
		assertNull(testee.getOpeningAgent());

	}

	@Test
	public void testAdding() throws L2pSecurityException, CryptoException, SerializationException {
		GroupAgent testee = GroupAgent.createGroupAgent(new Agent[] { adam, eve });
		abel.unlockPrivateKey(ABELSPASS);

		try {
			testee.unlockPrivateKey(abel);
			fail("SecurityException should have been thrown");
		} catch (L2pSecurityException e) {
		}

		eve.unlockPrivateKey(EVESSPASS);
		testee.unlockPrivateKey(eve);

		assertFalse(testee.isMember(abel));

		testee.addMember(abel);
		testee.lockPrivateKey();

		assertTrue(testee.isMember(abel));

		testee.unlockPrivateKey(abel);
	}

	@Test
	public void testSubGrouping() throws SerializationException, CryptoException, L2pSecurityException {
		GroupAgent subGroup = GroupAgent.createGroupAgent(new Agent[] { adam, eve });
		GroupAgent superGroup = GroupAgent.createGroupAgent(new Agent[] { abel, subGroup });

		assertTrue(superGroup.isMember(subGroup));

		eve.unlockPrivateKey(EVESSPASS);
		try {
			superGroup.unlockPrivateKey(subGroup);
			fail("SecurityException should have been thrown!");
		} catch (L2pSecurityException e) {
		}

		try {
			superGroup.unlockPrivateKey(eve);
			fail("SecurityException should have been thrown!");
		} catch (L2pSecurityException e) {
		}

		subGroup.unlockPrivateKey(eve);

		superGroup.unlockPrivateKey(subGroup);
		assertSame(subGroup, superGroup.getOpeningAgent());
	}

}
