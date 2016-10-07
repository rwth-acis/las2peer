package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class MessageEnvelopeTest {

	@Test
	public void testSimpleContent() throws SerializationException {
		String data = "some data to test";

		MessageEnvelope testee = new MessageEnvelope(null, data);

		byte[] serialized = SerializeTools.serialize(testee);
		MessageEnvelope andBack = (MessageEnvelope) SerializeTools.deserialize(serialized);

		assertEquals(data, andBack.getContent());
	}

	@Test
	public void testFromMessage() throws NoSuchAlgorithmException, L2pSecurityException, CryptoException,
			EncodingFailedException, SerializationException, MalformedXMLException, AgentNotKnownException,
			AgentAccessDeniedException {
		UserAgentImpl adam = UserAgentImpl.createUserAgent("passa");
		UserAgentImpl eve = UserAgentImpl.createUserAgent("passb");

		BasicAgentStorage storage = new BasicAgentStorage();
		storage.registerAgents(adam, eve);

		adam.unlock("passa");
		eve.unlock("passb");

		String data = "some data to test";

		Message m = new Message(adam, eve, data);

		MessageEnvelope testee = new MessageEnvelope(null, m);

		byte[] serialized = SerializeTools.serialize(testee);
		MessageEnvelope andBack = (MessageEnvelope) SerializeTools.deserialize(serialized);

		Message contained = andBack.getContainedMessage();
		contained.open(eve, storage);
		assertEquals(data, contained.getContent());
	}

}
