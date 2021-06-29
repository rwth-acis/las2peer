package i5.las2peer.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class GroupAgentImplTest {

	private static final String ADAMSPASS = "adamspass";
	private static final String EVESSPASS = "evesspass";
	private static final String KAINSPASS = "kainspass";
	private static final String ABELSPASS = "abelspass";

	private UserAgentImpl adam;
	private UserAgentImpl eve;
	private UserAgentImpl kain;
	private UserAgentImpl abel;

	@Before
	public void setUp() throws NoSuchAlgorithmException, AgentException, CryptoException {
		adam = UserAgentImpl.createUserAgent(ADAMSPASS);
		eve = UserAgentImpl.createUserAgent(EVESSPASS);
		kain = UserAgentImpl.createUserAgent(KAINSPASS);
		abel = UserAgentImpl.createUserAgent(ABELSPASS);
	}

	@Test
	public void testXmlAndBack()
			throws NoSuchAlgorithmException, InternalSecurityException, CryptoException, SerializationException,
			MalformedXMLException, AgentAccessDeniedException, AgentOperationFailedException, AgentLockedException {
		GroupAgentImpl testee = GroupAgentImpl.createGroupAgent(new AgentImpl[] { adam, eve }, "testXmlAndBack");
		assertEquals(2, testee.getSize());
		assertFalse(testee.hasMember(kain));
		assertFalse(testee.hasMember(abel.getIdentifier()));
		assertTrue(testee.hasMember(adam));
		assertTrue(testee.hasMember(eve.getIdentifier()));

		testee.apply();

		assertEquals(2, testee.getSize());
		assertFalse(testee.hasMember(kain));
		assertFalse(testee.hasMember(abel.getIdentifier()));
		assertTrue(testee.hasMember(adam));
		assertTrue(testee.hasMember(eve.getIdentifier()));

		assertTrue(testee.isLocked());

		adam.unlock(ADAMSPASS);
		testee.unlock(adam);

		String xml = testee.toXmlString();
		System.out.println(xml);

		GroupAgentImpl fromXml = GroupAgentImpl.createFromXml(xml);

		assertEquals(2, fromXml.getSize());
		assertFalse(testee.hasMember(kain));
		assertFalse(testee.hasMember(abel.getIdentifier()));
		assertTrue(testee.hasMember(adam));
		assertTrue(testee.hasMember(eve.getIdentifier()));

		assertTrue(fromXml.isLocked());
	}

	@Test
	public void testUnlocking() throws InternalSecurityException, CryptoException, SerializationException,
			AgentAccessDeniedException, AgentOperationFailedException, AgentLockedException {
		GroupAgentImpl testee = GroupAgentImpl.createGroupAgent(new AgentImpl[] { adam, eve }, "testUnlocking");

		try {
			testee.addMember(kain);
			fail("AgentLockedException should have been thrown!");
		} catch (AgentLockedException e) {
		}

		try {
			testee.unlock(adam);
			fail("AgentLockedException should have been thrown!");
		} catch (AgentLockedException e) {
		}

		adam.unlock(ADAMSPASS);
		testee.unlock(adam);
		assertSame(adam, testee.getOpeningAgent());
		assertFalse(testee.isLocked());

		try {
			testee.unlock(eve);
			fail("AgentLockedException should have been thrown");
		} catch (AgentLockedException e) {
		}

		testee.lockPrivateKey();
		assertTrue(testee.isLocked());
		assertNull(testee.getOpeningAgent());

	}

	@Test
	public void testAdding() throws InternalSecurityException, CryptoException, SerializationException,
			AgentAccessDeniedException, AgentOperationFailedException, AgentLockedException {
		GroupAgentImpl testee = GroupAgentImpl.createGroupAgent(new AgentImpl[] { adam, eve }, "testAdding");
		abel.unlock(ABELSPASS);

		try {
			testee.unlock(abel);
			fail("AgentAccessDeniedException should have been thrown");
		} catch (AgentAccessDeniedException e) {
		}

		eve.unlock(EVESSPASS);
		testee.unlock(eve);

		assertFalse(testee.hasMember(abel));

		testee.addMember(abel);
		testee.lockPrivateKey();

		assertTrue(testee.hasMember(abel));

		testee.unlock(abel);
	}

	@Test
	public void testSubGrouping() throws SerializationException, CryptoException, InternalSecurityException,
			AgentAccessDeniedException, AgentOperationFailedException, AgentLockedException {
		GroupAgentImpl subGroup = GroupAgentImpl.createGroupAgent(new AgentImpl[] { adam, eve }, "testSubGrouping1");
		GroupAgentImpl superGroup = GroupAgentImpl.createGroupAgent(new AgentImpl[] { abel, subGroup },
				"testSubGrouping2");

		assertTrue(superGroup.hasMember(subGroup));

		eve.unlock(EVESSPASS);
		try {
			superGroup.unlock(subGroup);
			fail("AgentLockedException should have been thrown!");
		} catch (AgentLockedException e) {
		}

		try {
			superGroup.unlock(eve);
			fail("AgentAccessDeniedException should have been thrown!");
		} catch (AgentAccessDeniedException e) {
		}

		subGroup.unlock(eve);

		superGroup.unlock(subGroup);
		assertSame(subGroup, superGroup.getOpeningAgent());
	}

	public void testApply() throws InternalSecurityException, CryptoException, SerializationException,
			AgentAccessDeniedException, AgentOperationFailedException, AgentLockedException {
		GroupAgentImpl agent = GroupAgentImpl.createGroupAgent(new AgentImpl[] { adam, eve }, "testApply");
		assertTrue(agent.hasMember(adam));
		assertTrue(agent.hasMember(eve));
		assertEquals(2, agent.getSize());
		assertTrue(Arrays.asList(agent.getMemberList()).contains(adam.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(eve.getIdentifier()));

		agent.addMember(abel);

		assertTrue(agent.hasMember(adam));
		assertTrue(agent.hasMember(eve));
		assertTrue(agent.hasMember(abel));
		assertEquals(3, agent.getSize());
		assertTrue(Arrays.asList(agent.getMemberList()).contains(adam.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(eve.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(abel.getIdentifier()));

		agent.revokeMember(adam);

		assertFalse(agent.hasMember(adam));
		assertTrue(agent.hasMember(eve));
		assertTrue(agent.hasMember(abel));
		assertEquals(2, agent.getSize());
		assertFalse(Arrays.asList(agent.getMemberList()).contains(adam.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(eve.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(abel.getIdentifier()));

		agent.revokeMember(abel);

		assertFalse(agent.hasMember(adam));
		assertTrue(agent.hasMember(eve));
		assertFalse(agent.hasMember(abel));
		assertEquals(1, agent.getSize());
		assertFalse(Arrays.asList(agent.getMemberList()).contains(adam.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(eve.getIdentifier()));
		assertFalse(Arrays.asList(agent.getMemberList()).contains(abel.getIdentifier()));

		agent.addMember(adam);

		assertTrue(agent.hasMember(adam));
		assertTrue(agent.hasMember(eve));
		assertFalse(agent.hasMember(abel));
		assertEquals(2, agent.getSize());
		assertTrue(Arrays.asList(agent.getMemberList()).contains(adam.getIdentifier()));
		assertTrue(Arrays.asList(agent.getMemberList()).contains(eve.getIdentifier()));
		assertFalse(Arrays.asList(agent.getMemberList()).contains(abel.getIdentifier()));

		try {
			agent.apply();
			fail("Exception expected");
		} catch (Exception e) {
		}

		try {
			agent.unlock(adam);
			agent.apply();
		} catch (Exception e) {
			fail("Got unexpected exception: " + e);
		}
	}

}
