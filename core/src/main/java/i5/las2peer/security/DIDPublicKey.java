package i5.las2peer.security;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DIDPublicKey implements DIDAttribute {
	private final String ID;
	private PublicKey publicKey;

	protected DIDPublicKey(String ID, PublicKey publicKey) throws IllegalArgumentException, InvalidKeyException {
		if (ID == null || ID.isEmpty()) {
			throw new IllegalArgumentException("ID must not be null or empty.");
		}
		if (publicKey == null || !publicKey.getAlgorithm().equals("RSA")) {
			throw new InvalidKeyException("Only RSA keys supported.");
		}

		this.ID = ID;
		this.publicKey = publicKey;
	}

	/**
	 *
	 * @param name name of
	 * @param value Base64-encoded public key
	 */
	protected DIDPublicKey(String name, byte[] value) throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
		String[] tokens = name.split("/", 5);
		if (tokens.length != 5 || !tokens[0].equals("did") || !tokens[1].equals("pub")) {
			throw new IllegalArgumentException("Input not in correct format.");
		}
		String type = tokens[2];
		this.ID = tokens[3];
		String encoding = tokens[4];

		if (!type.equals("RSA") || !encoding.equals("base64")) {
			throw new IllegalArgumentException("Only RSA keys encoded in Base64URL supported.");
		}

		KeyFactory kf = KeyFactory.getInstance(type);

		X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getUrlDecoder().decode(value));
		this.publicKey = kf.generatePublic(keySpecX509);
	}

	public String getID() {
		return ID;
	}

	public String getType() {
		return publicKey.getAlgorithm();
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public String getPublicKeyBase64URL() {
		return Base64.getUrlEncoder().encodeToString(publicKey.getEncoded());
	}

	public void setPublicKey(PublicKey publicKey) throws InvalidKeyException {
		if (publicKey == null || !publicKey.getAlgorithm().equals("RSA")) {
			throw new InvalidKeyException("Only RSA keys supported.");
		}
		this.publicKey = publicKey;
	}

	public String getEncodedName() {
		return String.format("did/pub/%s/%s/base64", getType(), getPublicKeyBase64URL());
	}

	public byte[] getEncodedValue() {
		return getPublicKeyBase64URL().getBytes();
	}
}
