package i5.las2peer.persistency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import i5.las2peer.security.Agent;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SimpleTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

public class EnvelopeTest {

	private static UserAgent eve, adam;

	@Before
	public void setUp() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException {
		adam = UserAgent.createUserAgent("adamspass");
		eve = UserAgent.createUserAgent("evespass");
		eve.unlockPrivateKey("evespass");
		adam.unlockPrivateKey("adamspass");
	}

	@Test
	public void testStringContent() throws NoSuchAlgorithmException, UnsupportedEncodingException,
			MalformedXMLException, XMLSyntaxException, L2pSecurityException, EnvelopeException {
		if (eve == null)
			throw new NullPointerException("Agents not initialized!");

		String content = "Dies ist ein String!";

		Envelope testee = new Envelope(content, eve);

		String xml = testee.toXmlString();
		// TODO: assertions for xml String
		// at least allow parsing!
		@SuppressWarnings("unused")
		Element testXml = Parser.parse(xml);

		System.out.println("simple String");
		System.out.println(xml);

		Envelope andBack = Envelope.createFromXml(xml);

		try {
			andBack.open(adam);
			fail("adam should not be able to open this envelope!");
		} catch (L2pSecurityException e) {
			// that's correct
		}

		try {
			andBack.getContentAsBinary();
			fail("DecodingFailedException should have been thrown - envelope has not been obend yet!");
		} catch (DecodingFailedException e) {
			// that's correct
		}

		andBack.open(eve);

		String contentBack = andBack.getContentAsString();
		assertEquals(content, contentBack);
	}

	@Test
	public void testSerializable()
			throws L2pSecurityException, EnvelopeException, MalformedXMLException, SerializationException {
		String s = "Hallo";
		int i = 100;
		TestContent t = new TestContent(s, i);

		Envelope testee = new Envelope(t, eve);

		String xml = testee.toXmlString();

		Envelope andBack = Envelope.createFromXml(xml);

		andBack.open(eve);
		TestContent c = (TestContent) andBack.getContentAsSerializable();

		assertEquals(t.getString(), c.getString());
		assertEquals(t.getInt(), c.getInt());

		System.out.println("Serializable");
		System.out.println(xml);
	}

	@Test
	public void testAgentRemoval() throws UnsupportedEncodingException, EncodingFailedException,
			DecodingFailedException, L2pSecurityException {
		String content = "blabla";
		Envelope testee = new Envelope(content, new Agent[] { eve });

		testee.close();
		assertNull(testee.getOpeningAgent());

		try {
			testee.open(adam);
			fail("adam should not be able to open this envelope!");
		} catch (L2pSecurityException e) {
			// that's correct
		}

		testee.open(eve);
		assertEquals(eve, testee.getOpeningAgent());
		testee.addReader(adam);
		testee.removeReader(eve);

		testee.close();

		try {
			testee.open(eve);
			fail("eve should not be able to open this envelope anymore!");
		} catch (L2pSecurityException e) {
			// that's correct
		}

		testee.open(adam);
		assertEquals(adam, testee.getOpeningAgent());
	}

	@Test
	public void testOpen() throws EncodingFailedException, UnsupportedEncodingException, DecodingFailedException,
			MalformedXMLException, L2pSecurityException {
		String data = "darf's auch ein bisschen mehr sein?";

		Envelope testee = new Envelope(data, adam);
		assertFalse(testee.isOpen());
		assertTrue(testee.isClosed());

		testee.open(adam);
		assertFalse(testee.isClosed());
		assertTrue(testee.isOpen());

		String xml = testee.toXmlString();
		try {
			testee.open(eve);
			fail("eve should not be able to open this envelope!");
		} catch (L2pSecurityException e) {
		}
		assertFalse(testee.isOpen());
		assertTrue(testee.isClosed());

		testee.open(adam);
		assertTrue(testee.isOpen());
		assertFalse(testee.isClosed());

		Envelope fromXml = Envelope.createFromXml(xml);
		assertFalse(fromXml.isOpen());
		assertTrue(fromXml.isClosed());

		testee = new Envelope(data, new Agent[] { adam, eve });
		assertFalse(testee.isOpen());
		assertNull(testee.getOpeningAgent());
	}

	@Test
	public void testSignatures() throws UnsupportedEncodingException, EncodingFailedException, DecodingFailedException,
			MalformedXMLException, VerificationFailedException, L2pSecurityException {
		String content = "irgendwas sinnvolles";

		Envelope testee = new Envelope(content, eve);
		testee.open(eve);
		try {
			testee.addSignature(adam);
			fail("adam is not a reader and should not be able to sign the envelope");
		} catch (IllegalStateException e) {
		}

		testee = new Envelope(content, new Agent[] { eve, adam });
		testee.open(eve);
		try {
			testee.addSignature(adam);
			fail("IllegalStateException expected");
		} catch (IllegalStateException e) {
		}

		testee.open(adam);
		try {
			testee.addSignature(eve);
			fail("eve has not opened this envelope and should not be able to sign the contents");
		} catch (IllegalStateException e) {
		}

		testee.addSignature(adam);
		assertTrue(testee.isSignedBy(adam));
		testee.close();
		assertTrue(testee.isSignedBy(adam));

		try {
			testee.verifySignature(adam);
			fail("IllegalStateException should have been thrown");
		} catch (IllegalStateException e) {
		}

		testee.open(eve);
		assertTrue(testee.isOpen());
		testee.addSignature(eve);
		String xml = testee.toXmlString();

		testee.open(eve);
		assertTrue(testee.isOpen());

		try {
			testee.removeSignature(adam);
			fail("adam has not opened this envelope and should not be able to remove his signature");
		} catch (EncodingFailedException e) {
		}
		testee.removeSignature(eve);
		assertFalse(testee.isSignedBy(eve));
		assertTrue(testee.isSignedBy(adam));

		try {
			testee.verifySignature(eve);
			fail("verification for eve should fail");
		} catch (VerificationFailedException e) {
		}

		System.out.println(xml);

		Envelope fromXml = Envelope.createFromXml(xml);
		assertTrue(fromXml.isSignedBy(eve));
		assertTrue(fromXml.isSignedBy(adam));

		try {
			fromXml.verifySignature(adam);
			fail("IllegalStateException should have been thrown!");
		} catch (IllegalStateException e) {
		}

		fromXml.open(eve);

		assertTrue(fromXml.isSignedBy(eve));
		assertTrue(fromXml.isSignedBy(adam));
	}

	@Test
	public void testOpeningAgent() throws UnsupportedEncodingException, EncodingFailedException,
			DecodingFailedException, L2pSecurityException {
		String content = "Ja wass denn?!";

		Envelope testee = new Envelope(content, eve);
		assertFalse(testee.isOpen());
		testee.open(eve);

		assertTrue(testee.isOpen());
		assertSame(eve, testee.getOpeningAgent());
		testee.close();
		assertNull(testee.getOpeningAgent());

		testee = new Envelope(content, new Agent[] { eve, adam });
		assertTrue(testee.isClosed());
		assertNull(testee.getOpeningAgent());
		testee.open(adam);
		assertSame(adam, testee.getOpeningAgent());
	}

	@Test
	public void testId() throws MalformedXMLException, UnsupportedEncodingException, EncodingFailedException,
			DecodingFailedException {
		String content = "bla";
		Envelope testee = new Envelope(content, eve);
		String xml = testee.toXmlString();

		Envelope fromXml = Envelope.createFromXml(xml);
		assertEquals(testee.getId(), fromXml.getId());

		assertTrue(testee.getId() != 0);
	}

	@Test
	public void testUniqueClassIds() {
		HashSet<Long> hsStored = new HashSet<Long>();
		HashSet<String> hsKeys = new HashSet<String>();

		for (int i = 0; i < 500; i++) {
			String key;
			do {
				key = SimpleTools.createRandomString(10);
			} while (hsKeys.contains(key));
			hsKeys.add(key);

			long id1 = Envelope.getClassEnvelopeId("a class", key);
			long id2 = Envelope.getClassEnvelopeId("a 2nd class", key);

			assertFalse(hsStored.contains(id1));
			assertFalse(hsStored.contains(id2));
			hsStored.add(id1);
			hsStored.add(id2);

			for (int j = 0; j < 20; j++)
				assertEquals(id1, Envelope.getClassEnvelopeId("a class", key));
		}
	}

	@Test
	public void testGroups() throws MalformedXMLException, IOException, L2pSecurityException, SerializationException,
			CryptoException, EnvelopeException {

		Agent[] owners = new Agent[] { MockAgentFactory.getEve(), MockAgentFactory.getGroup1(),
				MockAgentFactory.getGroupA() };
		Envelope testee = new Envelope("a string", owners);

		assertEquals(2, testee.getReaderGroups().length);

		String xml = testee.toXmlString();

		System.out.println(xml);

		Envelope andBack = Envelope.createFromXml(xml);
		assertEquals(2, testee.getReaderGroups().length);

		GroupAgent g = MockAgentFactory.getGroup1();
		UserAgent adam = MockAgentFactory.getAdam();
		adam.unlockPrivateKey("adamspass");
		g.unlockPrivateKey(adam);

		andBack.open(g);

		assertEquals("a string", andBack.getContentAsString());
	}

	@Test
	public void testArrayStorage()
			throws MalformedXMLException, IOException, EnvelopeException, SerializationException, L2pSecurityException {
		UserAgent eve = MockAgentFactory.getEve();

		Random r = new Random();
		Long[] data = new Long[r.nextInt(80) + 10];

		for (int i = 0; i < data.length; i++)
			data[i] = r.nextLong();

		Envelope test = new Envelope(data, eve);

		String xml = test.toXmlString();

		Envelope andBack = Envelope.createFromXml(xml);

		eve.unlockPrivateKey("evespass");
		andBack.open(eve);

		Long[] data2 = andBack.getContent(Long[].class);

		assertNotNull(data2);
		assertEquals(data.length, data2.length);
		for (int i = 0; i < data.length; i++) {
			assertEquals(data[i], data2[i]);
		}
	}

	@Test
	public void testCheckOverwrite() throws L2pSecurityException, EncodingFailedException, SerializationException,
			InterruptedException, DecodingFailedException, MalformedXMLException, IOException {
		UserAgent eve = MockAgentFactory.getEve();

		Envelope test = Envelope.createClassIdEnvelope(new Long(100), "temp", eve);
		String xml = test.toXmlString();
		test = Envelope.createFromXml(xml);

		Thread.sleep(1000);

		Envelope test2 = Envelope.createClassIdEnvelope(new Long(200), "temp", eve);

		assertFalse(test.getReferalTimestamp() == test2.getReferalTimestamp());

		try {
			test.checkOverwrite(test2);
			fail("OverwriteException expected");
		} catch (OverwriteException e) {
		}
		try {
			test2.checkOverwrite(test);
			fail("OverwriteException expected");
		} catch (OverwriteException e) {
		}

		eve.unlockPrivateKey("evespass");
		test.open(eve);
		test.setOverWriteBlindly(true);

		test.checkOverwrite(test2);
	}

	@Test
	public void testLoadedTimestamp()
			throws EncodingFailedException, SerializationException, MalformedXMLException, IOException {
		UserAgent eve = MockAgentFactory.getEve();
		Envelope testee = Envelope.createClassIdEnvelope(new Long(100), "test", eve);
		long timestamp = testee.getTimestamp();

		String xml = testee.toXmlString();

		testee = Envelope.createFromXml(xml);

		assertEquals(timestamp, testee.getReferalTimestamp());
	}

	@Test
	public void testUpdateSwitch()
			throws EnvelopeException, MalformedXMLException, IOException, SerializationException, L2pSecurityException {
		UserAgent eve = MockAgentFactory.getEve();
		Envelope testee = Envelope.createClassIdEnvelope(new StringBuffer("test"), "test", eve);

		try {
			testee.updateContent(new DummyContent("test2"));
			fail("L2pSecurityException expected (not opened)");
		} catch (L2pSecurityException e) {
			// intended
		}

		eve.unlockPrivateKey("evespass");
		testee.open(eve);

		testee.updateContent(new DummyContent("test3"));

		assertEquals("test3", testee.getContent(DummyContent.class).toString());

		testee.lockContent();

		try {
			testee.updateContent(new DummyContent("test4"));
			fail("L2pSecurityException expected (overwrite disabled)");
		} catch (L2pSecurityException e) {
			// intended
		}

		testee.getContent(DummyContent.class).append("-abc");

		assertEquals("test3-abc", testee.getContent(DummyContent.class).toString());
	}

}
