package i5.las2peer.tools;

import static org.junit.Assert.*;

import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnvelopeGeneratorTest {

	private final ByteArrayOutputStream standardOut = new ByteArrayOutputStream();
	private final ByteArrayOutputStream standardError = new ByteArrayOutputStream();

	@Before
	public void detourSystemStreams() throws IOException {
		System.setOut(new PrintStream(standardOut));
		System.setErr(new PrintStream(standardError));
	}

	@After
	public void clearSystemStreams() {
		System.setOut(null);
		System.setErr(null);
	}

	@Test
	public void testGeneration() throws Exception {
		EnvelopeGenerator.main(
				new String[] { "src/main/java/i5/las2peer/testing/eve.xml", "evespass", "java.lang.Integer", "102" });
		String output = standardOut.toString();
		String error = standardError.toString();

		assertTrue(output.length() > 0);
		assertEquals("", error);

		Envelope test = Envelope.createFromXml(output);

		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		test.open(eve);

		Integer content = (Integer) test.getContentAsSerializable();

		assertEquals(102, content.intValue());
	}

	@Test
	public void testFileMissing() {
		EnvelopeGenerator.main(new String[] { "bdauwid", "", "", "" });
		String error = standardError.toString();
		String output = standardOut.toString();
		assertEquals("", output);
		assertTrue(error.contains("unable to read contents"));
	}

	@Test
	public void testOpenFail() throws SerializationException, DecodingFailedException, L2pSecurityException,
			MalformedXMLException, IOException {
		EnvelopeGenerator
				.main(new String[] { "src/main/java/i5/las2peer/testing/eve.xml", "failure", "Integer", "102" });

		String error = standardError.toString();
		String output = standardOut.toString();

		assertEquals("", output);

		assertTrue(error.contains("unable to unlock agent"));
	}

}
