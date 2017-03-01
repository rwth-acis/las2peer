package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;
<<<<<<< HEAD
import i5.las2peer.api.security.AgentAccessDeniedException;
=======

import org.junit.Assert;
import org.junit.Test;

>>>>>>> future-v0.7
import i5.las2peer.communication.Message;
import i5.las2peer.security.BasicAgentStorage;
<<<<<<< HEAD
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
=======
import i5.las2peer.security.UserAgent;
>>>>>>> future-v0.7
import i5.las2peer.tools.SerializeTools;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class MessageEnvelopeTest {

	@Test
	public void testSimpleContent() {
		try {
			String data = "some data to test";

			MessageEnvelope testee = new MessageEnvelope(null, data);

			byte[] serialized = SerializeTools.serialize(testee);
			MessageEnvelope andBack = (MessageEnvelope) SerializeTools.deserialize(serialized);

			assertEquals(data, andBack.getContent());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	public void testFromMessage() {
		try {
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
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
