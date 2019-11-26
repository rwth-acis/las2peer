package i5.las2peer.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EnvelopeGenerator;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.testing.MockAgentFactory;

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

		EnvelopeVersion test = EnvelopeVersion.createFromXml(output);

		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");

		Integer content = (Integer) test.getContent(new AgentContext(null, eve));

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
	public void testOpenFail() throws SerializationException, DecodingFailedException, InternalSecurityException,
			MalformedXMLException, IOException {
		EnvelopeGenerator
				.main(new String[] { "src/main/java/i5/las2peer/testing/eve.xml", "failure", "Integer", "102" });

		String error = standardError.toString();
		String output = standardOut.toString();

		assertEquals("", output);

		assertTrue(error.contains("unable to unlock agent"));
	}

}
