package i5.las2peer.security;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.registry.exceptions.BadEthereumCredentialsException;
import i5.las2peer.registry.CredentialUtils;
import i5.las2peer.registry.ReadWriteRegistryClient;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import org.w3c.dom.Element;
import org.web3j.crypto.Credentials;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

/**
 * User agent for las2peer networks with blockchain-based registry.
 *
 * In addition to the usual key pair, this agent has an Ethereum wallet
 * (which is also a key pair) with an associated Ethereum address.
 *
 * For now, this wallet is stored separately on disk. (It's just a JSON
 * file.)
 */
// TODO: don't store the wallet file path. store its contents
public class EthereumAgent extends UserAgentImpl {
	// this should probably be configured elsewhere
	private static final String DEFAULT_WALLET_DIRECTORY = "./etc/wallets";

	private String ethereumWalletPath;

	private ReadWriteRegistryClient registryClient;
	private String ethereumAddress;

	protected EthereumAgent(KeyPair pair, String passphrase, byte[] salt, String ethereumWalletPath, String loginName)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
		checkLoginNameValidity(loginName);
		sLoginName = loginName;
		this.ethereumWalletPath = ethereumWalletPath;
	}

	protected EthereumAgent(PublicKey pubKey, byte[] encryptedPrivate, byte[] salt, String ethereumWalletPath,
			String loginName) {
		super(pubKey, encryptedPrivate, salt);
		checkLoginNameValidity(loginName);
		sLoginName = loginName;
		this.ethereumWalletPath = ethereumWalletPath;
	}

	// as in the superclass, it would be nicer not to use an exception
	@Override
	void checkLoginNameValidity(String loginName) throws IllegalArgumentException {
		super.checkLoginNameValidity(loginName);
		if (loginName.length() > 32) {
			throw new IllegalArgumentException("Login name must be at most 32 characters");
		}
	}

	// bit of unfortunate name here, but let's stick with it

	/**
	 * Removes decrypted private key and the registry client (which
	 * contains user credentials).
	 */
	@Override
	public void lockPrivateKey() {
		super.lockPrivateKey();
		registryClient = null;
	}

	@Override
	public void unlock(String passphrase) throws AgentAccessDeniedException, AgentOperationFailedException {
		super.unlock(passphrase);
		try {
			Credentials credentials = CredentialUtils.fromWallet(ethereumWalletPath, passphrase);
			registryClient = new ReadWriteRegistryClient(new RegistryConfiguration(), credentials);
			ethereumAddress = CredentialUtils.fromWallet(ethereumWalletPath, passphrase).getAddress();
		} catch (BadEthereumCredentialsException e) {
			throw new AgentAccessDeniedException("Could not unlock Ethereum wallet. Ensure password is set to the"
				+ "agent's passphrase (if not change it!).", e);
		}
	}

	@Override
	public boolean isLocked() {
		boolean ethereumIsLocked = (registryClient == null);
		boolean userAgentIsLocked = super.isLocked();
		if (userAgentIsLocked == ethereumIsLocked) {
			return userAgentIsLocked;
		} else {
			// this hopefully only happens during initialization, where this is called from a superclass's constructor
			if (ethereumWalletPath == null) {
				// we're almost certainly in the middle of initialization
				return userAgentIsLocked;
			} else {
				throw new RuntimeException("FIXME: inconsistent lock state should not be reachable -- figure out where this happens!");
			}
		}
	}

	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer("<las2peer:agent type=\"ethereum\">\n" + "\t<id>" + getIdentifier()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">" + SerializeTools.serializeToBase64(getPublicKey())
					+ "</publickey>\n" + "\t<privatekey encrypted=\"" + CryptoTools.getSymmetricAlgorithm()
					+ "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod() + "\">\n"
					+ "\t\t<salt encoding=\"base64\">" + Base64.getEncoder().encodeToString(getSalt()) + "</salt>\n"
					+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n" + "\t</privatekey>\n"
					+ "\t<login>" + sLoginName + "</login>\n"
					+ "\t<ethereumwalletpath>" + ethereumWalletPath + "</ethereumwalletpath>\n");

			if (sEmail != null) {
				result.append("\t<email>" + sEmail + "</email>\n");
			}

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	/**
	 * Creates new agent with given passphrase and login name.
	 * Wallet file will be created in default location (for now).
	 * @param passphrase passphrase with which both the agent key pair
	 *                   and the Ethereum wallet are encrypted
	 * @param loginName name matching [a-zA-Z].{3,31} (hopefully UTF-8
	 *                  characters, let's not get too crazy)
	 * @return new EthereumAgent instance
	 * @throws CryptoException if there is an internal error during
	 *                         wallet creation
	 */
	public static EthereumAgent createEthereumAgent(String passphrase, String loginName) throws CryptoException  {
		byte[] salt = CryptoTools.generateSalt();
		try {
			String walletPath = CredentialUtils.createWallet(passphrase, DEFAULT_WALLET_DIRECTORY);
			return new EthereumAgent(CryptoTools.generateKeyPair(), passphrase, salt, walletPath, loginName);
		} catch (Exception e) {
			throw new CryptoException("Wallet generation failed.", e);
		}
	}

	/**
	 * Gets registry client that uses the agent's credentials.
	 * May be <code>null</code>; use {@link #unlock(String)}.
	 */
	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
	}

	/** @return address of the Ethereum wallet associated with the agent */
	public String getEthereumAddress() {
		return ethereumAddress;
	}

	public static EthereumAgent createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	public static EthereumAgent createFromXml(Element root) throws MalformedXMLException {
		try {
			// read id field from XML
			Element elId = XmlTools.getSingularElement(root, "id");
			String id = elId.getTextContent();
			// read public key from XML
			Element pubKey = XmlTools.getSingularElement(root, "publickey");
			if (!pubKey.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getTextContent());
			if (!id.equalsIgnoreCase(CryptoTools.publicKeyToSHA512(publicKey))) {
				throw new MalformedXMLException("id does not match with public key");
			}
			// read private key from XML
			Element privKey = XmlTools.getSingularElement(root, "privatekey");
			if (!privKey.getAttribute("encrypted").equals(CryptoTools.getSymmetricAlgorithm())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			}
			if (!privKey.getAttribute("keygen").equals(CryptoTools.getSymmetricKeygenMethod())) {
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod() + " expected");
			}
			Element dataPrivate = XmlTools.getSingularElement(privKey, "data");
			byte[] encPrivate = Base64.getDecoder().decode(dataPrivate.getTextContent());
			// read salt from XML
			Element elSalt = XmlTools.getSingularElement(root, "salt");
			if (!elSalt.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			byte[] salt = Base64.getDecoder().decode(elSalt.getTextContent());

			Element loginElement = XmlTools.getSingularElement(root, "login");
			String login = loginElement.getTextContent();

			Element ethereumWalletPathElement = XmlTools.getSingularElement(root, "ethereumwalletpath");
			String ethereumWalletPath = ethereumWalletPathElement.getTextContent();

			// required fields complete, create result
			EthereumAgent result = new EthereumAgent(publicKey, encPrivate, salt, ethereumWalletPath, login);

			// read and set optional fields
			// note: login name is not optional here
			// optional email address
			Element email = XmlTools.getOptionalElement(root, "email");
			if (email != null) {
				result.sEmail = email.getTextContent();
			}

			return result;
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		}
	}
}
