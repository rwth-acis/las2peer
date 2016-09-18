package i5.las2peer.persistency;

import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import rice.p2p.commonapi.Id;
import rice.pastry.commonapi.PastryIdFactory;

public class MetadataArtifact extends NetworkArtifact {

	private static final long serialVersionUID = 1L;

	public MetadataArtifact(PastryIdFactory idFactory, String identifier, long version, byte[] serialize, Agent author)
			throws CryptoException, L2pSecurityException {
		super(buildMetadataId(idFactory, identifier, version), 0, serialize, author);
	}

	public static Id buildMetadataId(PastryIdFactory idFactory, String identifier, long version) {
		return idFactory.buildId(getMetadataIdentifier(identifier, version));
	}

	public static String getMetadataIdentifier(String identifier, long version) {
		return "metadata-" + identifier + "#" + version;
	}

}
