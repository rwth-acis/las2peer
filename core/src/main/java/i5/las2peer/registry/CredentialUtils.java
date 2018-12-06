package i5.las2peer.registry;

import i5.las2peer.registry.exceptions.BadEthereumCredentialsException;
import org.web3j.crypto.*;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

import static org.web3j.crypto.Hash.sha256;

/**
 * Wraps {@link org.web3j.crypto.Credentials} with convenience methods.
 */
public class CredentialUtils {
	/**
	 * Derive credentials from password-protected wallet file.
	 * @param walletFilePath path to standard JSON Ethereum wallet file
	 * @param password password for decrypting private key in wallet
	 * @return credentials object (with decrypted private key)
	 */
	public static Credentials fromWallet(String walletFilePath, String password) throws BadEthereumCredentialsException {
		if (password == null) {
			password = "";
		}
		try {
			return WalletUtils.loadCredentials(password, walletFilePath);
		} catch (IOException | CipherException e) {
			throw new BadEthereumCredentialsException("Could not load or decrypt wallet file", e);
		}
	}

	/**
	 * Derive credentials from private key.
	 * @param privateKey private key from which public key will be
	 *                   derived
	 */
	public static Credentials fromPrivateKey(String privateKey) {
		return Credentials.create(privateKey);
	}

	/**
	 * Derive credentials from BIP39 mnemonic and password.
	 * @param mnemonic string of words. As defined in BIP-0039, this
	 *                 wallet seed determines a key pair.
	 * @param password password added to the key generation process
	 * @return credentials object (with decrypted private key)
	 * @see <a href="https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki">Bitcoin BIP-0039 document</a>
	 */
	public static Credentials fromMnemonic(String mnemonic, String password) {
		byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
		ECKeyPair keyPair = ECKeyPair.create(sha256(seed));
		return Credentials.create(keyPair);
	}

	/**
	 * Create new BIP39 mnemonic string using secure randomness.
	 * @return string of words. Can be used to deterministically
	 * 		   generate a key pair.
	 */
	public static String createMnemonic() {
		byte[] initialEntropy = new byte[16];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(initialEntropy);
		return MnemonicUtils.generateMnemonic(initialEntropy);
	}

	/**
	 * Create wallet file and return its path.
	 * @param password password with which the private key is encrypted
	 * @param directoryPath directory in which the file will be created
	 * @return full path of the newly created wallet file
	 */
	public static String createWallet(String password, String directoryPath) throws IOException, CipherException {
		File destinationDirectory = new File(directoryPath);
		if (!destinationDirectory.isDirectory()) {
			throw new IllegalArgumentException("Path must be a directory.");
		}
		Bip39Wallet wallet = WalletUtils.generateBip39Wallet(password, destinationDirectory);
		return directoryPath + File.separator + wallet.getFilename();
	}
}
