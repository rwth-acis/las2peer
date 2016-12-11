package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.communication.Message;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.SerializeTools;

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

	@Test
	public void testFromMessage() {
		try {
			UserAgent adam = UserAgent.createUserAgent("passa");
			UserAgent eve = UserAgent.createUserAgent("passb");

			BasicAgentStorage storage = new BasicAgentStorage();
			storage.registerAgents(adam, eve);

			adam.unlockPrivateKey("passa");
			eve.unlockPrivateKey("passb");

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
