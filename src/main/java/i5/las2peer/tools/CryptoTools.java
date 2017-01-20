package i5.las2peer.tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import i5.las2peer.persistency.VerificationFailedException;

/**
 * Simple <i>static</i> class collecting useful cryptographic methods end encapsulating the access to the underlying
 * cryptografic library.
 */
public class CryptoTools {

	private static final String DEFAULT_HASH_METHOD = "SHA1";
	private static String hashMethod = DEFAULT_HASH_METHOD;

	private static String DEFAULT_ASYMMETRIC_ALGORITHM = "RSA";
	private static String asymmetricAlgorithm = DEFAULT_ASYMMETRIC_ALGORITHM;

	private static String DEFAULT_SYMMETIC_ALGORITHM = "AES";
	private static String symmetricAlgorithm = DEFAULT_SYMMETIC_ALGORITHM;

	private static final int DEFAULT_ASYM_KEYSIZE = 2048;
	private static int asymmetricKeySize = DEFAULT_ASYM_KEYSIZE;

	private static final int DEFAULT_SYM_KEYSIZE = 256;
	private static int symmetricKeySize = DEFAULT_SYM_KEYSIZE;

	private static final String DEFAULT_RANDOM_METHOD = "SHA1PRNG";
	private static String randomMethod = DEFAULT_RANDOM_METHOD;

	private static final String DEFAULT_KEY_FACTORY_NAME = "PBKDF2WithHmacSHA1";
	private static String keyFactoryName = DEFAULT_KEY_FACTORY_NAME;

	/**
	 * used hash method
	 * 
	 * @return used hash method
	 */
	public static String getHashMethod() {
		return hashMethod;
	}

	/**
	 * get the asymmetric encryption algorithm in use
	 * 
	 * @return asymetric algorithm
	 */
	public static String getAsymmetricAlgorithm() {
		return asymmetricAlgorithm;
	}

	/**
	 * get the symmetric algorithm in use
	 * 
	 * @return symetric algorithm
	 */
	public static String getSymmetricAlgorithm() {
		return symmetricAlgorithm;
	}

	/**
	 * get the signature method in use
	 * 
	 * @return signature method
	 */
	public static String getSignatureMethod() {
		return getHashMethod() + "with" + getAsymmetricAlgorithm();
	}

	/**
	 * get the factory method for symmetric keys
	 * 
	 * @return factory method in use
	 */
	public static String getSymmetricKeygenMethod() {
		return keyFactoryName;
	}

	/**
	 * set the preferred size for asymmetric keys
	 * 
	 * @param size The key size that is used to create asymmetric keys.
	 */
	public static void setAsymmetricKeySize(int size) {
		asymmetricKeySize = size;
		clear();
	}

	/**
	 * set the preferred size for symmetric keys
	 * 
	 * @param size The key size that is used to create symmetric keys.
	 */
	public static void setSymmetricKeySize(int size) {
		symmetricKeySize = size;
		clear();
	}

	/**
	 * generate a symmetric key for the given passphrase using the given salt make sure to use real random salts e.g.
	 * via the {@link #generateSalt} method
	 * 
	 * @param passphrase The secret that is used to generate the key.
	 * @param salt A salt that is used with the given passphrase.
	 * @return a symmetric key for the given passphrase
	 * @throws CryptoException If the selected algorithm does not exist or an issue with the given key occurs.
	 */
	public static SecretKey generateKeyForPassphrase(String passphrase, byte[] salt) throws CryptoException {
		try {
			PBEKeySpec password = new PBEKeySpec(passphrase.toCharArray(), salt, 1000, 128);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(keyFactoryName);
			PBEKey key = (PBEKey) factory.generateSecret(password);
			return new SecretKeySpec(key.getEncoded(), symmetricAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e);
		} catch (InvalidKeySpecException e) {
			throw new CryptoException("problems with symmetric key", e);
		}
	}

	/**
	 * encrypt a serializable object using the given passphrase an salt
	 * 
	 * make sure to use real random salts e.g. via the {@link #generateSalt} method
	 * 
	 * @param object The data that is encrypted.
	 * @param passphrase The secret that is used to encrypt the given data.
	 * @param salt A salt that is used with the given passphrase.
	 * @return encrypted content
	 * @throws CryptoException If an issue occurs with encryption.
	 * @throws SerializationException If an issue occurs with deserializing the given data.
	 */
	public static byte[] encryptWithPassphrase(Serializable object, String passphrase, byte[] salt)
			throws CryptoException, SerializationException {
		try {
			SecretKey encKey = generateKeyForPassphrase(passphrase, salt);

			Cipher c = Cipher.getInstance(symmetricAlgorithm);
			c.init(Cipher.ENCRYPT_MODE, encKey);

			return c.doFinal(SerializeTools.serialize(object));
		} catch (InvalidKeyException e) {
			throw new CryptoException("problems with symmetric key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("padding problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("block size problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("padding problems", e);
		}
	}

	/**
	 * descrypt (and deserialize) the given encrypted data using the given passphrase and salt
	 * 
	 * @param content The data that is decrypted.
	 * @param salt A salt that is used with the given passphrase.
	 * @param passphrase The secret that is used to decrypt the given data.
	 * @return decrypted and deserialized content
	 * @throws CryptoException If an issue occurs with decryption.
	 * @throws SerializationException If an issue occurs with deserializing the given data.
	 */
	public static Serializable depryptPassphaseObject(byte[] content, byte[] salt, String passphrase)
			throws CryptoException, SerializationException {
		try {
			SecretKey encKey = generateKeyForPassphrase(passphrase, salt);

			Cipher c = Cipher.getInstance(symmetricAlgorithm);
			c.init(Cipher.DECRYPT_MODE, encKey);

			return SerializeTools.deserialize(c.doFinal(content));
		} catch (InvalidKeyException e) {
			throw new CryptoException("problems with symmetric key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("padding problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("block size problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("padding problems", e);
		}
	}

	/**
	 * generate a random salt
	 * 
	 * @return a random salt for later use
	 * @throws CryptoException If the selected salt algorithm does not exist.
	 * 
	 */
	public static byte[] generateSalt() throws CryptoException {
		try {
			SecureRandom rand = SecureRandom.getInstance(randomMethod);
			byte[] salt = new byte[16];
			rand.nextBytes(salt);

			return salt;
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("unable to get SecureRandom instance!", e);
		}
	}

	/**
	 * decrypt the given content with the given private key and try to deserialize the resulting byte array
	 * 
	 * @param data The encrypted data that is decrypted.
	 * @param key The key that is used to decrypt the given data.
	 * @return decrypted and deserialized content as java object
	 * @throws SerializationException If an issue occurs with deserializing the given data.
	 * @throws CryptoException If an decryption issue occurs.
	 */
	public static Serializable decryptAsymmetric(byte[] data, PrivateKey key)
			throws SerializationException, CryptoException {
		try {
			Cipher c = Cipher.getInstance(asymmetricAlgorithm);
			c.init(Cipher.DECRYPT_MODE, key);

			byte[] decoded = c.doFinal(data);

			return SerializeTools.deserialize(decoded);
		} catch (InvalidKeyException e) {
			throw new CryptoException("Key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("algorithm problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("Padding problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("Block size problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("Padding problems", e);
		}
	}

	/**
	 * decrypt a symmetrically encrypted byte block using the given key
	 * 
	 * @param baCipherData The encrypted data that is decrypted.
	 * @param key The key that is used to decrypt the given data.
	 * @return decrypted content as byte array
	 * @throws CryptoException If an issue occurs with decryption.
	 */
	public static byte[] decryptSymmetric(byte[] baCipherData, SecretKey key) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance(symmetricAlgorithm);
			c.init(Cipher.DECRYPT_MODE, key);
			return c.doFinal(baCipherData);
		} catch (InvalidKeyException e) {
			throw new CryptoException("Key problems!", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("algorithm problems!", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("padding problems!", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("block size problems!", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("padding problems!", e);
		}
	}

	/**
	 * encrypt the given data after serialization using the given public key
	 * 
	 * @param content The object that is encrypted.
	 * @param key The key that is used to encrypt the given object.
	 * @return encrypted content as byte array
	 * @throws CryptoException If an issue occurs with encryption.
	 * @throws SerializationException If an issue occurs with deserializing the given data.
	 */
	public static byte[] encryptAsymmetric(Serializable content, PublicKey key)
			throws CryptoException, SerializationException {
		return encryptAsymmetric(SerializeTools.serialize(content), key);
	}

	/**
	 * encrypt the given data asymmetrically using the given public key
	 * 
	 * @param content The object that is encrypted.
	 * @param key The key that is used to encrypt the given object.
	 * @return encrypted content as byte array
	 * @throws CryptoException If an issue occurs with encryption.
	 */
	public static byte[] encryptAsymmetric(byte[] content, PublicKey key) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance(asymmetricAlgorithm);
			c.init(Cipher.ENCRYPT_MODE, key);

			return c.doFinal(content);
		} catch (InvalidKeyException e) {
			throw new CryptoException("Key Problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm Problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("Padding Problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("Blocksize Problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("padding Problems", e);
		}
	}

	/**
	 * sign the given content with the given private key
	 * 
	 * @param content The content that is signed with the given key.
	 * @param key The key that is used to sign the given content.
	 * @return signature as byte array
	 * @throws CryptoException If an issue occurs with the given key or selected algorithm.
	 */
	public static byte[] signContent(byte[] content, PrivateKey key) throws CryptoException {
		try {
			Signature sig = Signature.getInstance(getSignatureMethod());

			sig.initSign(key);
			sig.update(content);

			return sig.sign();
		} catch (InvalidKeyException e) {
			throw new CryptoException("key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e);
		} catch (SignatureException e) {
			throw new CryptoException("Signature problems", e);
		}
	}

	/**
	 * tries to verify the given signature of the given content with the given public key
	 * 
	 * @param signature The (possibly malicious) signature that is attached to the content.
	 * @param content The (possibly malicious) content that is verified.
	 * @param key The key that is verfied as the trusted signer.
	 * @return true, if verification is successful
	 * @throws VerificationFailedException If an issue occurs with the given key or selected algorithm.
	 */
	public static boolean verifySignature(byte[] signature, byte[] content, PublicKey key)
			throws VerificationFailedException {
		try {
			Signature sig = Signature.getInstance(getSignatureMethod());
			sig.initVerify(key);
			sig.update(content);
			return sig.verify(signature);
		} catch (InvalidKeyException e) {
			throw new VerificationFailedException("key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new VerificationFailedException("Algorithm problems", e);
		} catch (SignatureException e) {
			throw new VerificationFailedException("Signature problems", e);
		}
	}

	/**
	 * generate a new key for the symmetric crypto operations of this class
	 * 
	 * @return new symmetric key
	 */
	public static SecretKey generateSymmetricKey() {
		if (keyGeneratorSymmetric == null) {
			initialize();
		}
		return keyGeneratorSymmetric.generateKey();
	}

	/**
	 * generate a new asymmetric key pair
	 * 
	 * @return new key pair
	 */
	public static KeyPair generateKeyPair() {
		if (keyGeneratorAsymmetric == null) {
			initialize();
		}

		return keyGeneratorAsymmetric.generateKeyPair();
	}

	/**
	 * encrypt the given data symmetrically with the given key
	 * 
	 * @param baPlainData The data that is encrypted.
	 * @param symmetricKey The key that is used to encrypt the given data.
	 * @return encrypted content as byte array
	 * @throws CryptoException If an issue occurs with encryption.
	 */
	public static byte[] encryptSymmetric(byte[] baPlainData, SecretKey symmetricKey) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance(getSymmetricAlgorithm());
			c.init(Cipher.ENCRYPT_MODE, symmetricKey);

			return c.doFinal(baPlainData);
		} catch (InvalidKeyException e) {
			throw new CryptoException("key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("algorithm problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("padding problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("blocksize problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException("padding problems", e);
		}
	}

	/**
	 * encrypt the given object after serialization with the givne key
	 * 
	 * @param plainData The data that is encrypted.
	 * @param key The key that is used to encrypt the given data.
	 * @return encrypted content as byte array
	 * @throws CryptoException If an issue occurs with encryption.
	 * @throws SerializationException If an issue occurs with deserializing the given data.
	 */
	public static byte[] encryptSymmetric(Serializable plainData, SecretKey key)
			throws CryptoException, SerializationException {
		return encryptSymmetric(SerializeTools.serialize(plainData), key);
	}

	public static PrivateKey privateKeyToString(String key64) throws CryptoException {
		byte[] clear = Base64.getDecoder().decode(key64);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
		try {
			KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
			PrivateKey priv = fact.generatePrivate(keySpec);
			Arrays.fill(clear, (byte) 0);
			return priv;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoException("Could not read private key from given base64 string", e);
		}
	}

	public static PublicKey stringToPublicKey(String base64) throws CryptoException {
		byte[] data = Base64.getDecoder().decode(base64);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		try {
			KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
			return fact.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoException("Could not read public key from given base64 string", e);
		}
	}

	public static String privateKeyToString(PrivateKey priv) throws CryptoException {
		try {
			KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
			PKCS8EncodedKeySpec spec = fact.getKeySpec(priv, PKCS8EncodedKeySpec.class);
			byte[] packed = spec.getEncoded();
			String key64 = Base64.getEncoder().encodeToString(packed);
			Arrays.fill(packed, (byte) 0);
			return key64;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoException("Could not convert private key into base64 string", e);
		}
	}

	public static String publicKeyToString(PublicKey publ) throws CryptoException {
		try {
			KeyFactory fact = KeyFactory.getInstance(asymmetricAlgorithm);
			X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
			return Base64.getEncoder().encodeToString(spec.getEncoded());
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CryptoException("Could not convert public key into base64 string", e);
		}
	}

	/*** statics **** */
	private static KeyGenerator keyGeneratorSymmetric = null;
	private static KeyPairGenerator keyGeneratorAsymmetric = null;

	/**
	 * (re) inistialize the key generators
	 */
	private static void initialize() {
		try {
			keyGeneratorSymmetric = KeyGenerator.getInstance(getSymmetricAlgorithm());
			keyGeneratorSymmetric.init(symmetricKeySize);

			keyGeneratorAsymmetric = KeyPairGenerator.getInstance(getAsymmetricAlgorithm());
			keyGeneratorAsymmetric.initialize(asymmetricKeySize);

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("critical: needed crypto algorithm not found!", e);
		}
	}

	/**
	 * clear the prepared key generators
	 */
	private static void clear() {
		keyGeneratorAsymmetric = null;
		keyGeneratorAsymmetric = null;
	}

	static {
		// initialize();
	}

	/**
	 * main (command line) method: create a key pair in the given file name prefix
	 * 
	 * @param argv See usage output for details.
	 */
	public static void main(String[] argv) {
		if (argv.length != 1) {
			System.err.println("Usage: java -cp ... i5.las2peer.tools.CryptoTools [fileprefix]\n\n"
					+ "Will generate a file [fileprefix].public and [fileprefix].private"
					+ "containing a serialized version of a public and a private key.");
			System.exit(1);
		}

		KeyPair kp = generateKeyPair();

		try {
			FileOutputStream fosPublic = new FileOutputStream(argv[0] + ".public");
			FileOutputStream fosPrivate = new FileOutputStream(argv[0] + ".private");
			PrintStream psPublic = new PrintStream(fosPublic);
			PrintStream psPrivate = new PrintStream(fosPrivate);
			psPublic.print(SerializeTools.serializeToBase64(kp.getPublic()));
			psPrivate.print(SerializeTools.serializeToBase64(kp.getPrivate()));
			fosPublic.close();
			fosPrivate.close();
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open output file: " + e);
		} catch (SerializationException e) {
			System.err.println("Problems serializing key: " + e);
		} catch (IOException e) {
			System.out.println("I/O problems: " + e);
		}
	}

}
