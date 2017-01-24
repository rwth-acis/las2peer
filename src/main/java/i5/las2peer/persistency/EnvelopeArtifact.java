package i5.las2peer.persistency;

import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import rice.p2p.commonapi.Id;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;
import rice.pastry.commonapi.PastryIdFactory;

public class EnvelopeArtifact extends NetworkArtifact {

	private static final long serialVersionUID = 1L;

	public EnvelopeArtifact(PastryIdFactory idFactory, String identifier, int partIndex, byte[] rawPart, Agent author)
			throws CryptoException, L2pSecurityException, VerificationFailedException {
		super(buildId(idFactory, identifier, partIndex), partIndex, rawPart, author);
	}

	@Override
	public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
		if (existingContent != null) {
			NetworkArtifact existingArtifact = (NetworkArtifact) existingContent;
			if (!hasSameAuthor(existingArtifact)) {
				throw new PastException("Write access blocked! Different authors");
			}
		}
		// don't tell the super class about existing copy -> mutable content
		return super.checkInsert(id, null);
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	public static Id buildId(PastryIdFactory idFactory, String identifier, int partIndex) {
		return idFactory.buildId("artifact-" + identifier + "#" + partIndex);
	}

}
