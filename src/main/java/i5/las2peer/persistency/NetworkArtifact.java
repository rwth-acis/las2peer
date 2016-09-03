package i5.las2peer.persistency;

import java.security.PublicKey;

import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;

/**
 * A network artifact is a bunch of arbitrary data in the shared storage. It is signed to help detect manipulations.
 * Note that this signature is self signed, one has to check if the used key is in a list of trusted keys.
 */
public abstract class NetworkArtifact extends ContentHashPastContent {

	public static final int MAX_SIZE = 500 * 1000; // = 500 KB

	private static final long serialVersionUID = 1L;

	private final int partIndex;
	// TODO test different content types, String seems the fastest, also check BASE64, Serializable, byte[]
	private final byte[] content;
	private final byte[] signature;
	private final PublicKey key;
	private final byte[] keySignature;

	protected NetworkArtifact(Id id, int partIndex, byte[] content, Agent author)
			throws CryptoException, L2pSecurityException {
		super(id);
		int size = content.length;
		if (size > MAX_SIZE) {
			throw new IllegalArgumentException(
					"Given content has " + size + " bytes and is too big for maximum size " + MAX_SIZE);
		}
		this.partIndex = partIndex;
		this.content = content;
		this.signature = author.signContent(content);
		this.key = author.getPublicKey();
		this.keySignature = author.signContent(key.getEncoded());
	}

	public int getPartIndex() {
		return partIndex;
	}

	public byte[] getContent() throws VerificationFailedException {
		verify();
		return content;
	}

	public PublicKey getAuthorPublicKey() throws VerificationFailedException {
		verifyAuthorKey();
		return key;
	}

	public void verify() throws VerificationFailedException {
		// first we verify the contained public key is correct
		verifyAuthorKey();
		// after we verify the actual content is correct
		CryptoTools.verifySignature(signature, content, key);
	}

	private void verifyAuthorKey() throws VerificationFailedException {
		CryptoTools.verifySignature(keySignature, key.getEncoded(), key);
	}

	public boolean hasSameAuthor(NetworkArtifact other) {
		if (this == other) {
			return true;
		} else if (other == null) {
			return false;
		} else {
			return this.key.equals(other.key);
		}
	}

	@Override
	public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
		try {
			this.verify();
		} catch (VerificationFailedException e) {
			throw new PastException(e.toString());
		}
		return super.checkInsert(id, existingContent);
	}

	@Override
	public String toString() {
		return getId().toStringFull();
	}

}
