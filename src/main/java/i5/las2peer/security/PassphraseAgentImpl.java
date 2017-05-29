package i5.las2peer.security;

import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.PassphraseAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

/**
 * Base class for pass phrase protected agents.
 */
public abstract class PassphraseAgentImpl extends AgentImpl implements PassphraseAgent {

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
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws AgentOperationFailedException
	 * @throws CryptoException
	 */
	protected PassphraseAgentImpl(KeyPair pair, String passphrase, byte[] salt)
			throws AgentOperationFailedException, CryptoException {
		super(pair, CryptoTools.generateKeyForPassphrase(passphrase, salt));

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
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected PassphraseAgentImpl(PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(pubKey, encodedPrivate);
		this.salt = salt.clone();
	}

	@Override
	public void unlock(String passphrase) throws AgentAccessDeniedException, AgentOperationFailedException {
		try {
			SecretKey key = CryptoTools.generateKeyForPassphrase(passphrase, salt);
			super.unlockPrivateKey(key);
			this.passphrase = passphrase;
		} catch (CryptoException e) {
			throw new AgentAccessDeniedException("unable to create key from passphrase", e);
		}
	}

	/**
	 * encrypt the private key into a byte array with strong encryption based on a passphrase to unlock the key
	 * 
	 * @param passphrase
	 * @throws AgentOperationFailedException
	 * @throws AgentLockedException
	 */
	private void encryptPrivateKey(String passphrase) throws AgentOperationFailedException, AgentLockedException {
		try {
			salt = CryptoTools.generateSalt();
			super.encryptPrivateKey(CryptoTools.generateKeyForPassphrase(passphrase, salt));
		} catch (CryptoException e) {
			throw new AgentOperationFailedException("problems with key generation for passphrase", e);
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
	 * @throws AgentOperationFailedException
	 * @throws AgentLockedException
	 */
	public void changePassphrase(String passphrase) throws AgentOperationFailedException, AgentLockedException {
		if (isLocked()) {
			throw new AgentLockedException();
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
	 * @throws AgentLockedException 
	 */
	public String getPassphrase() throws AgentLockedException {
		if (isLocked()) {
			throw new AgentLockedException();
		}
		return this.passphrase;
	}

}
