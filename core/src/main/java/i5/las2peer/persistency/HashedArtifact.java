package i5.las2peer.persistency;

import java.util.Base64;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import rice.p2p.commonapi.Id;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;
import rice.pastry.commonapi.PastryIdFactory;

/**
 * This class represents a bunch of arbitrary data in the shared storage. The content is stored using its secure hash as
 * identifier. To ensure data integrity one has to check if the hash from the identifier matches the secure hash of its
 * content.
 *
 */
public class HashedArtifact extends AbstractArtifact {

	private static final long serialVersionUID = 1L;

	private static final String IDENTIFIER_PREFIX = "hashed-";

	private static final L2pLogger logger = L2pLogger.getInstance(HashedArtifact.class);

	public HashedArtifact(PastryIdFactory idFactory, byte[] content) throws CryptoException {
		super(buildIdFromHash(idFactory, CryptoTools.getSecureHash(content)), content);
	}

	public static Id buildIdFromHash(PastryIdFactory idFactory, byte[] hash) throws CryptoException {
		return idFactory.buildId(IDENTIFIER_PREFIX + Base64.getEncoder().encodeToString(hash));
	}

	@Override
	public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
		try {
			this.verify();
		} catch (VerificationFailedException e) {
			throw new PastException(e.toString());
		}
		if (existingContent != null) {
			// actually the insert operation should be canceled with an exception here, because the content already
			// exists, but FreePastry doesn't support good exception handling.
			logger.info("Hashed content already exists");
			return existingContent;
		} else {
			return this;
		}
	}

}
