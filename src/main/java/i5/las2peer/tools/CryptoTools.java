package i5.las2peer.tools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

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



/**
 * Simple <i>static</i> class collecting useful cryptographic methods end encapsulating 
 * the access to the underlying cryptografic library.
 *   
 * @author Holger Jan&szlig;en
 *
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
	 * @return	used hash method
	 */
	public static String getHashMethod () {
		return hashMethod;
	}
	
	/**
	 * get the asymmetric encryption algorithm in use
	 * @return	asymetric algorithm
	 */
	public static String getAsymmetricAlgorithm() {
		return asymmetricAlgorithm;
	}
	
	/**
	 * get the symmetric algorithm in use
	 * @return	symetric algorithm
	 */
	public static String getSymmetricAlgorithm() {
		return symmetricAlgorithm;
	}
	
	/**
	 * get the signature method in use
	 * @return	signature method
	 */
	public static String getSignatureMethod() {
		return getHashMethod() + "with" + getAsymmetricAlgorithm();
	}
	
	/**
	 * get the factory method for symmetric keys
	 * @return	factory method in use
	 */
	public static String getSymmetricKeygenMethod () {
		return keyFactoryName;
	}
	
	
	/**
	 * set the preferred size for asymmetric keys
	 * @param size
	 */
	public static void setAsymmetricKeySize ( int size ) {
		asymmetricKeySize = size;
		clear();
	}
	
	
	/**
	 * set the preferred size for symmetric keys
	 * @param size
	 */
	public static void setSymmetricKeySize ( int size ) {
		symmetricKeySize = size;
		clear();
	}
	
	
	
	/**
	 * generate a symmetric key for the given passphrase using the given salt
	 * make sure to use real random salts e.g. via the {@link #generateSalt} method
	 * 
	 * @param passphrase
	 * @param salt
	 * 
	 * @return a symmetric key for the given passphrase
	 * 
	 * @throws CryptoException
	 */
	public static SecretKey generateKeyForPassphrase ( String passphrase, byte[] salt ) throws CryptoException {
		try {
			PBEKeySpec password = new PBEKeySpec(passphrase.toCharArray(), salt, 1000, 128);  
			SecretKeyFactory factory = SecretKeyFactory.getInstance(keyFactoryName);  
			PBEKey key = (PBEKey) factory.generateSecret(password);  
			return new SecretKeySpec(key.getEncoded(), symmetricAlgorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "Algorithm problems",e );
		} catch (InvalidKeySpecException e) {
			throw new CryptoException ( "problems with symmetric key",e );
		}
	}
	
	
	/**
	 * encrypt a serializable object using the given passphrase an salt
	 * 
	 * make sure to use real random salts e.g. via the {@link #generateSalt} method
	 * 
	 * @param object
	 * @param passphrase
	 * @param salt
	 * 
	 * @return	encrypted content
	 * 
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static byte[] encryptWithPassphrase ( Serializable object, String passphrase, byte[] salt ) throws CryptoException, SerializationException {		
		try {
			SecretKey encKey = generateKeyForPassphrase ( passphrase, salt );
			
			Cipher c = Cipher.getInstance( symmetricAlgorithm );
			c.init( Cipher.ENCRYPT_MODE, encKey );
			
			return c.doFinal ( SerializeTools.serialize( object ) );
		} catch (InvalidKeyException e) {
			throw new CryptoException ( "problems with symmetric key",e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "Algorithm problems",e );
		} catch (NoSuchPaddingException e) {
			throw new CryptoException ( "padding problems",e );
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException ( "block size problems",e );
		} catch (BadPaddingException e) {
			throw new CryptoException ( "padding problems",e );
		}
	}
	
	
	/**
	 * descrypt (and deserialize) the given encrypted data using the given passphrase and salt
	 * 
	 * @param content
	 * @param salt
	 * @param passphrase
	 * 
	 * @return decrypted and deserialized content
	 * 
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static Serializable depryptPassphaseObject ( byte[] content, byte[] salt, String passphrase ) throws CryptoException, SerializationException {
		try {
			SecretKey encKey = generateKeyForPassphrase ( passphrase, salt );
			
			Cipher c = Cipher.getInstance( symmetricAlgorithm );
			c.init( Cipher.DECRYPT_MODE, encKey );
			
			return SerializeTools.deserialize ( c.doFinal ( content ));
		} catch (InvalidKeyException e) {
			throw new CryptoException ( "problems with symmetric key",e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "Algorithm problems",e );
		} catch (NoSuchPaddingException e) {
			throw new CryptoException ( "padding problems",e );
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException ( "block size problems",e );
		} catch (BadPaddingException e) {
			throw new CryptoException ( "padding problems",e );
		}
	}
	
	/**
	 * generate a random salt
	 * 
	 * @return a random salt for later use
	 * 
	 * @throws CryptoException
	 * 
	 */
	public static byte[] generateSalt () throws CryptoException {
		try {
			SecureRandom rand = SecureRandom.getInstance(randomMethod);  
			byte [] salt = new byte[16];  
			rand.nextBytes(salt);
			
			return salt;
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "unable to get SecureRandom instance!", e);
		}
	}
	
	/**
	 * decrypt the given content with the given private key and try to deserialize
	 * the resulting byte array
	 * 
	 * @param data
	 * @param key
	 * 
	 * @return	decrypted and deserialized content as java object
	 * 
	 * @throws SerializationException
	 * @throws CryptoException
	 */
	public static Serializable decryptAsymmetric ( byte[] data, PrivateKey key ) throws SerializationException, CryptoException {
		try {
			Cipher c = Cipher.getInstance( asymmetricAlgorithm );
			c.init( Cipher.DECRYPT_MODE, key );		
			
			byte[] decoded = c.doFinal( data );
			
			return SerializeTools.deserialize(decoded);
		} catch (InvalidKeyException e) {
			throw new CryptoException("Key problems", e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("algorithm problems", e );
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("Padding problems", e );
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("Block size problems", e );
		} catch (BadPaddingException e) {
			throw new CryptoException("Padding problems", e );
		}
	}

	/**
	 * decrypt a symmetrically encrypted byte block using the given key
	 * 
	 * @param baCipherData
	 * @param key
	 * 
	 * @return	decrypted content as byte array
	 * 
	 * @throws CryptoException
	 */
	public static byte[] decryptSymmetric(byte[] baCipherData, SecretKey key ) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance ( symmetricAlgorithm );
			c.init ( Cipher.DECRYPT_MODE, key);
			return c.doFinal ( baCipherData );
		} catch (InvalidKeyException e) {
			throw new CryptoException ( "Key problems!", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "algorithm problems!", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException ( "padding problems!", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException ( "block size problems!", e);
		} catch (BadPaddingException e) {
			throw new CryptoException ( "padding problems!", e);
		}
	}
	
	
	/**
	 * encrypt the given data after serialization using the given public key
	 * 
	 * @param content
	 * @param key
	 * 
	 * @return	encrypted content as byte array
	 * 
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static byte[] encryptAsymmetric ( Serializable content, PublicKey key ) throws CryptoException, SerializationException {
		return encryptAsymmetric( SerializeTools.serialize( content ), key);
	}
	
	
	/**
	 * encrypt the given data asymmetrically using the given public key
	 * 
	 * @param content
	 * @param key
	 * 
	 * @return	encrypted content as byte array
	 * 
	 * @throws CryptoException
	 */
	public static byte[] encryptAsymmetric ( byte[] content, PublicKey key ) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance(asymmetricAlgorithm);
			c.init( Cipher.ENCRYPT_MODE, key );
			
			return c.doFinal ( content );
		} catch (InvalidKeyException e) {
			throw new CryptoException ( "Key Problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException ( "Algorithm Problems", e);
		} catch (NoSuchPaddingException e) {
			throw new CryptoException ( "Padding Problems", e);
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException ( "Blocksize Problems", e);
		} catch (BadPaddingException e) {
			throw new CryptoException ( "padding Problems", e);
		}
	}
	
	
	/**
	 * sign the given content with the given private key
	 * 
	 * @param content
	 * @param key
	 * 
	 * @return	signature as byte array
	 * 
	 * @throws CryptoException 
	 */
	public static byte[] signContent ( byte[] content, PrivateKey key ) throws CryptoException {
		try {
			Signature sig = Signature.getInstance(getSignatureMethod());
			
			sig.initSign(key);
			sig.update(content);
			
			return sig.sign();
		} catch (InvalidKeyException e) {
			throw new CryptoException("key problems", e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e );
		} catch (SignatureException e) {
			throw new CryptoException("Signature problems", e );
		}
	}
	
	/**
	 * tries to verify the given signature of the given content with the given public key
	 * 
	 * @param signature
	 * @param content
	 * @param key
	 * 
	 * @return	true, if verification is successful
	 * 
	 * @throws CryptoException
	 */
	public static boolean verifySignature ( byte[] signature, byte[] content, PublicKey key ) throws CryptoException {
		try {
			Signature sig = Signature.getInstance( getSignatureMethod());
			sig.initVerify ( key );
			sig.update( content );
			
			return sig.verify(signature);
		} catch (InvalidKeyException e) {
			throw new CryptoException("key problems", e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("Algorithm problems", e );
		} catch (SignatureException e) {
			throw new CryptoException("Signature problems", e );
		}
	}
	
	/**
	 * generate a new key for the symmetric crypto operations of this class
	 * @return	new symmetric key
	 */
	public static SecretKey generateSymmetricKey () {	
		if ( keyGeneratorSymmetric == null )
			initialize();
		return keyGeneratorSymmetric.generateKey();
	}
	
	/**
	 * generate a new asymmetric key pair
	 * @return new key pair
	 */
	public static KeyPair generateKeyPair () {
		if ( keyGeneratorAsymmetric == null)
			initialize();
		
		return keyGeneratorAsymmetric.generateKeyPair();
	}
	
	
	/**
	 * encrypt the given data symmetrically with the given key
	 * 
	 * @param baPlainData
	 * @param symmetricKey
	 * 
	 * @return encrypted content as byte array
	 * 
	 * @throws CryptoException 
	 */
	public static byte[] encryptSymmetric(byte[] baPlainData, SecretKey symmetricKey) throws CryptoException {
		try {
			Cipher c = Cipher.getInstance( getSymmetricAlgorithm() );
			c.init( Cipher.ENCRYPT_MODE, symmetricKey );

			return c.doFinal ( baPlainData );
		} catch (InvalidKeyException e) {
			throw new CryptoException("key problems", e );
		} catch (NoSuchAlgorithmException e) {
			throw new CryptoException("algorithm problems", e );
		} catch (NoSuchPaddingException e) {
			throw new CryptoException("padding problems", e );
		} catch (IllegalBlockSizeException e) {
			throw new CryptoException("blocksize problems", e );
		} catch (BadPaddingException e) {
			throw new CryptoException("padding problems", e );
		}
	}
	

	/**
	 * encrypt the given object after serialization with the givne key
	 * 
	 * @param plainData
	 * @param key
	 * 
	 * @return	encrypted content as byte array
	 * 
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static byte[] encryptSymmetric ( Serializable plainData, SecretKey key ) throws CryptoException, SerializationException {
		return encryptSymmetric( SerializeTools.serialize(plainData), key);
	}



	
	
	
	
	/*** statics **** */
	private static KeyGenerator keyGeneratorSymmetric = null;
	private static KeyPairGenerator keyGeneratorAsymmetric = null;
	
	/**
	 * (re) inistialize the key generators
	 */
	private static void initialize () {
		try {
			keyGeneratorSymmetric = KeyGenerator.getInstance ( getSymmetricAlgorithm() );
			keyGeneratorSymmetric.init ( symmetricKeySize );
			
			keyGeneratorAsymmetric = KeyPairGenerator.getInstance( getAsymmetricAlgorithm());
			keyGeneratorAsymmetric.initialize( asymmetricKeySize);
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException ( "critical: needed crypto algorithm not found!", e);
		}
	}
	
	/**
	 * clear the prepared key generators
	 */
	private static void clear () {
		keyGeneratorAsymmetric = null;
		keyGeneratorAsymmetric = null;
	}
	
	static {
		//initialize();
	}


	
	/**
	 * main (command line) method: create a key pair in the given file name
	 * prefix
	 * 
	 * @param argv
	 */
	public static void main ( String[] argv ) {
		if ( argv.length != 1 ) {
			System.err.println ( "Usage: java -cp ... i5.las2peer.tools.CryptoTools [fileprefix]\n\n"
					+"Will generate a file [fileprefix].public and [fileprefix].private"
					+"containing a serialized version of a public and a private key.");
			System.exit(1);
		}
		
		
		KeyPair kp = generateKeyPair ();
		
		
		try {
			FileOutputStream fosPublic = new FileOutputStream (argv[0] + ".public" );
			FileOutputStream fosPrivate = new FileOutputStream (argv[0] + ".private" );
			PrintStream psPublic = new PrintStream ( fosPublic );
			PrintStream psPrivate = new PrintStream ( fosPrivate );
			psPublic.print(SerializeTools.serializeToBase64(kp.getPublic()));
			psPrivate.print ( SerializeTools.serializeToBase64(kp.getPrivate()));
			fosPublic.close();
			fosPrivate.close();
		} catch (FileNotFoundException e) {
			System.err.println ("Unable to open output file: " + e);
		} catch (SerializationException e) {
			System.err.println( "Problems serializing key: " + e);
		} catch (IOException e) {
			System.out.println ( "I/O problems: " + e);
		}
	}
	
	
}
