package i5.las2peer.p2p;

import java.nio.charset.StandardCharsets;

import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import rice.pastry.NodeIdFactory;

public class L2pNodeIdFactory implements NodeIdFactory {

	final byte[] raw;

	public L2pNodeIdFactory(String nodeIdSeed) {
		raw = nodeIdSeed.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public rice.pastry.Id generateNodeId() {
		try {
			byte[] digest = CryptoTools.getSecureHash(raw);
			return rice.pastry.Id.build(digest);
		} catch (CryptoException e) {
			throw new RuntimeException("Could not create node id", e);
		}
	}

}
