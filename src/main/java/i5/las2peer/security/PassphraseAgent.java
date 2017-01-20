package i5.las2peer.security;

import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

/**
 * Base class for pass phrase protected agents.
 */
public abstract class PassphraseAgent extends Agent {

	/**
	 * random salt for the encryption of the private key (necessary for generating a strong key from a given passphrase)
	 */
	private byte[] salt;

	/**
	 * current passphrase
	 */
	private String passphrase;

	/**
	 * atm constructor for the MockAgent class, just don't know, how agent creation will take place later
	 * 
	 * @param id
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 */
	protected PassphraseAgent(long id, KeyPair pair, String passphrase, byte[] salt)
			throws L2pSecurityException, CryptoException {
		super(id, pair, CryptoTools.generateKeyForPassphrase(passphrase, salt));

		this.salt = salt.clone();

		this.passphrase = null;

		// done in constructor of superclass
		// lockPrivateKey();
	}

	/**
	 * create an agent with a locked private key
	 * 
	 * used within {@link #createFromXml}
	 * 
	 * @param id
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected PassphraseAgent(long id, PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(id, pubKey, encodedPrivate);
		this.salt = salt.clone();
	}

	/**
	 * unlock the private key
	 * 
	 * @param passphrase
	 * @throws L2pSecurityException
	 */
	public void unlockPrivateKey(String passphrase) throws L2pSecurityException {
		try {
			SecretKey key = CryptoTools.generateKeyForPassphrase(passphrase, salt);
			super.unlockPrivateKey(key);
			this.passphrase = passphrase;
		} catch (CryptoException e) {
			throw new L2pSecurityException("unable to create key from passphrase", e);
		}
	}

	/**
	 * encrypt the private key into a byte array with strong encryption based on a passphrase to unlock the key
	 * 
	 * @param passphrase
	 * @throws L2pSecurityException
	 */
	private void encryptPrivateKey(String passphrase) throws L2pSecurityException {
		try {
			salt = CryptoTools.generateSalt();
			super.encryptPrivateKey(CryptoTools.generateKeyForPassphrase(passphrase, salt));
		} catch (CryptoException e) {
			throw new L2pSecurityException("problems with key generation for passphrase", e);
		}
	}

	/**
	 * provide access to salt for subclasses (security risk? - probably not)
	 * 
	 * @return the random salt used to encode the private key
	 */
	protected byte[] getSalt() {
		return salt;
	}

	/**
	 * Change the passphrase for unlocking the private key. The key has to be unlocked first, of course.
	 * 
	 * @param passphrase
	 * @throws L2pSecurityException
	 */
	public void changePassphrase(String passphrase) throws L2pSecurityException {
		if (isLocked()) {
			throw new L2pSecurityException("You have to unlock the key first!");
		}
		encryptPrivateKey(passphrase);
	}

	@Override
	public void lockPrivateKey() {
		super.lockPrivateKey();
		this.passphrase = null;
	}

	/**
	 * get the current passphrase
	 * 
	 * @return
	 * @throws L2pSecurityException
	 */
	public String getPassphrase() throws L2pSecurityException {
		if (isLocked()) {
			throw new L2pSecurityException("You have to unlock the key first!");
		}
		return this.passphrase;
	}

}
