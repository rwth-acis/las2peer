package i5.las2peer.registryGateway;

import org.web3j.crypto.*;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

import static org.web3j.crypto.Hash.sha256;

/**
 * Wraps org.web3j.crypto.Credentials with convenience methods.
 */
public class CredentialUtils {
	/**
	 * Derive credentials from password-protected wallet file.
	 */
	public static Credentials fromWallet(String walletFile, String password) throws BadEthereumCredentialsException {
		if (password == null) {
			password = "";
		}
		try {
			return WalletUtils.loadCredentials(password, walletFile);
		} catch (IOException | CipherException e) {
			throw new BadEthereumCredentialsException("Could not load or decrypt wallet file", e);
		}
	}

	/**
	 * Derive credentials from private key.
	 */
	public static Credentials fromPrivateKey(String privateKey) {
		return Credentials.create(privateKey);
	}

	/**
	 * Derive credentials from BIP39 mnemonic and password.
	 */
	public static Credentials fromMnemonic(String mnemonic, String password) {
		byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
		ECKeyPair keyPair = ECKeyPair.create(sha256(seed));
		return Credentials.create(keyPair);
	}

	/**
	 * Create new BIP39 mnemonic string using secure randomness.
	 */
	public static String createMnemonic() {
		byte[] initialEntropy = new byte[16];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(initialEntropy);
		return MnemonicUtils.generateMnemonic(initialEntropy);
	}

	/**
	 * Create wallet file and return its path.
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
