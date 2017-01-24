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

/**
 * A network artifact is a bunch of arbitrary data in the shared storage. It is signed to help detect manipulations.
 * Note that this signature is self signed, one has to check if the used key is in a list of trusted keys.
 */
public abstract class NetworkArtifact extends AbstractArtifact {

	private static final long serialVersionUID = 1L;

	private final int partIndex;
	private final byte[] signature;
	private final byte[] encodedAuthorKey; // the PublicKey class contains enum and can't be serialized by Java
	private final byte[] keySignature;

	protected NetworkArtifact(Id id, int partIndex, byte[] content, Agent author)
			throws CryptoException, L2pSecurityException, VerificationFailedException {
		super(id, content);
		this.partIndex = partIndex;
		this.signature = author.signContent(content);
		this.encodedAuthorKey = author.getPublicKey().getEncoded();
		this.keySignature = author.signContent(encodedAuthorKey);
		verify();
	}

	public int getPartIndex() {
		return partIndex;
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

	@Override
	public void verify() throws VerificationFailedException {
		if (content == null) {
			return;
		}
		try {
			super.verify();
			if (partIndex < 0) {
				throw new VerificationFailedException("Part index must be zero or greater");
			}
			// first we decode and verify the contained public key is correct
			PublicKey authorPublicKey = getAuthorPublicKey();
			// after we verify the actual content is correct
			CryptoTools.verifySignature(signature, content, authorPublicKey);
		} catch (Exception e) {
			throw new VerificationFailedException("Exception during verification", e);

		}
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

}
