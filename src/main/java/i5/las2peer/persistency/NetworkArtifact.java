package i5.las2peer.persistency;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

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
	private final byte[] encodedAuthorKey; // the PublicKey class contains enum and can't be serialized by Java
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
		this.encodedAuthorKey = author.getPublicKey().getEncoded();
		this.keySignature = author.signContent(encodedAuthorKey);
	}

	public int getPartIndex() {
		return partIndex;
	}

	public byte[] getContent() throws VerificationFailedException {
		verify();
		return content;
	}

	public PublicKey getAuthorPublicKey() throws VerificationFailedException {
		try {
			PublicKey decoded = KeyFactory.getInstance(CryptoTools.getAsymmetricAlgorithm())
					.generatePublic(new X509EncodedKeySpec(encodedAuthorKey));
			CryptoTools.verifySignature(keySignature, encodedAuthorKey, decoded);
			return decoded;
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new VerificationFailedException("Decoding authors public key failed!", e);
		}
	}

	public void verify() throws VerificationFailedException {
		// first we decode and verify the contained public key is correct
		PublicKey authorPublicKey = getAuthorPublicKey();
		// after we verify the actual content is correct
		CryptoTools.verifySignature(signature, content, authorPublicKey);
	}

	public boolean hasSameAuthor(NetworkArtifact other) {
		if (this == other) {
			return true;
		} else if (other == null) {
			return false;
		} else {
			return Arrays.equals(encodedAuthorKey, other.encodedAuthorKey);
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
