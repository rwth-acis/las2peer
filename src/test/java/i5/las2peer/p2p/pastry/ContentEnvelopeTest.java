package i5.las2peer.p2p.pastry;

import static org.junit.Assert.assertEquals;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class ContentEnvelopeTest {

	@Test
	public void testSerialization() throws NoSuchAlgorithmException, L2pSecurityException, SerializationException,
			PastryStorageException, CryptoException {
		UserAgent adam = UserAgent.createUserAgent("passa");
		adam.unlockPrivateKey("passa");

		ContentEnvelope testee = new ContentEnvelope(adam);

		byte[] bytes = SerializeTools.serialize(testee);

		ContentEnvelope andBack = (ContentEnvelope) SerializeTools.deserialize(bytes);

		Agent contained = andBack.getContainedAgent();

		assertEquals(adam.getId(), contained.getId());
	}

}
