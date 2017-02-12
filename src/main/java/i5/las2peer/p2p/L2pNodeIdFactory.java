package i5.las2peer.p2p;

import java.nio.ByteBuffer;

import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import rice.pastry.NodeIdFactory;

public class L2pNodeIdFactory implements NodeIdFactory {

	final byte[] raw;

	public L2pNodeIdFactory(long nodeIdSeed) {
		// long to byte[]
		raw = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(nodeIdSeed).array();
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
