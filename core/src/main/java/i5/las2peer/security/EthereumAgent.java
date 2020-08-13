package i5.las2peer.security;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.CredentialUtils;
import i5.las2peer.registry.ReadWriteRegistryClient;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.web3j.crypto.Credentials;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

/**
 * User agent for las2peer networks with blockchain-based registry.
 *
 * In addition to the usual key pair, this agent has a separate Ethereum key
 * pair with an associated Ethereum address. (The address can be derived from
 * the private key.)
 *
 * In contrast to the regular las2peer key pair, the Ethereum key pair is not
 * stored as an encrypted private key, but instead as a mnemonic string which
 * serves the same function but is a standardized, human-readable
 * representation.
 */
public class EthereumAgent extends UserAgentImpl {
	// this should probably be configured elsewhere

	/**
	 * BIP39 mnemonic used together with the agent's password to generate the
	 * private key for Ethereum.
	 *
	 * (Instead of this, we could use the encrypted private key directly, like
	 * the regular agent key pair, but using the mnemonic abstraction allows
	 * the user to potentially use other wallet software/services and allows
	 * us to delegate the tricky crypto operations to a compatible library.)
	 */
	// should the salt be used, too? can't hurt, but not sure if there's an
	// advantage. I'm fairly certain Web3J uses its own salt and whatnot
	private String ethereumMnemonic;
	/** Ethereum account address. (This is not secret.) */
	private String ethereumAddress;

	/** Ethereum credentials; used for Ethereum message signing. */
	private Credentials credentials;

	/** This agent's registry client to make direct smart contract calls. */
	private ReadWriteRegistryClient registryClient;

	private static final L2pLogger logger = L2pLogger.getInstance(EthereumAgent.class);

	protected EthereumAgent(KeyPair pair, String passphrase, byte[] salt, String loginName, String ethereumMnemonic)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
		checkLoginNameValidity(loginName);
		this.sLoginName = loginName;
		this.ethereumMnemonic = ethereumMnemonic;
		this.ethereumAddress = CredentialUtils.fromMnemonic(ethereumMnemonic, passphrase).getAddress();
		logger.fine("creating ethereum agent [" + ethereumAddress + "]");
	}

	protected EthereumAgent(PublicKey pubKey, byte[] encryptedPrivate, byte[] salt, String loginName, String ethereumMnemonic, String ethereumAddress) {
		super(pubKey, encryptedPrivate, salt);
		checkLoginNameValidity(loginName);
		this.sLoginName = loginName;
		this.ethereumMnemonic = ethereumMnemonic;
		this.ethereumAddress = ethereumAddress;
		logger.fine("creating ethereum agent [" + ethereumAddress + "]");
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
		this.credentials = null;
		this.registryClient = null;
	}

	@Override
	public void unlock(String passphrase) throws AgentAccessDeniedException, AgentOperationFailedException {
		super.unlock(passphrase);

		credentials = CredentialUtils.fromMnemonic(ethereumMnemonic, passphrase);
		registryClient = new ReadWriteRegistryClient(new RegistryConfiguration(), credentials);

		ethereumAddress = credentials.getAddress();
		logger.fine("unlocked ethereum agent ["+ethereumAddress +"]");
	}

	@Override
	public boolean isLocked() {
		boolean ethereumIsLocked = (registryClient == null);
		boolean userAgentIsLocked = super.isLocked();
		if (userAgentIsLocked == ethereumIsLocked) {
			return userAgentIsLocked;
		} else {
			// this hopefully only happens during initialization, where this is called from a superclass's constructor
			if (ethereumMnemonic == null) {
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
					+ "\t<ethereumaddress>" + ethereumAddress + "</ethereumaddress>\n"
					+ "\t<ethereummnemonic>" + ethereumMnemonic + "</ethereummnemonic>\n");

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
	 * Creates new agent with a given login name, passphrase, and registry client.
	 * @param loginName name matching [a-zA-Z].{3,31} (hopefully UTF-8
	 *                  characters, let's not get too crazy)
	 * @param passphrase passphrase with which both the agent key pair
	 *                   and the Ethereum key pair are encrypted
	 * @param regClient read-/write-capable Ethereum registry client to register the login name to the
	 *                  Ethereum blockchain with the UserRegistry contract.
	 * @return new EthereumAgent instance
	 * @throws CryptoException if there is an internal error during Ethereum key creation
	 */
	// TODO: when does it throw an AgentOperationFailedException?
	public static @NotNull
	EthereumAgent createEthereumAgentWithClient(
			String loginName,
			String passphrase,
			ReadWriteRegistryClient regClient
	) throws CryptoException, AgentOperationFailedException {
		String mnemonic = CredentialUtils.createMnemonic();

		return createEthereumAgent(loginName, passphrase, regClient, mnemonic);
	}

	/**
	 * Creates new agent with a given login name, passphrase, registry client, and mnemonic.
	 * Can be used whenever an existing wallet should be used instead of a new one.
	 * @param loginName name matching [a-zA-Z].{3,31} (hopefully UTF-8
	 *                  characters, let's not get too crazy)
	 * @param passphrase passphrase with which both the agent key pair
	 *                   and the Ethereum key pair are encrypted
	 * @param regClient read-/write-capable Ethereum registry client to register the login name to the
	 *                  Ethereum blockchain with the UserRegistry contract.
	 * @param ethereumMnemonic BIP 39-compliant mnemonic from which the key pair for the Ethereum wallet will be
	 *                         created.
	 * @return new EthereumAgent instance
	 * @throws CryptoException if there is an internal error during Ethereum key creation
	 */
	// TODO: when does it throw an AgentOperationFailedException?
	public static @NotNull
	EthereumAgent createEthereumAgent(
			String loginName,
			String passphrase,
			ReadWriteRegistryClient regClient,
			String ethereumMnemonic
	) throws CryptoException, AgentOperationFailedException {
		byte[] salt = CryptoTools.generateSalt();
		KeyPair keyPair = CryptoTools.generateKeyPair();

		return new EthereumAgent(keyPair, passphrase, salt, loginName, ethereumMnemonic);
	}

	/**
	 * Gets registry client that uses the agent's credentials.
	 * May be <code>null</code>; use {@link #unlock(String)}.
	 */
	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
	}

	/** @return address of the Ethereum key pair associated with the agent */
	public String getEthereumAddress() {
		return ethereumAddress;
	}

	public String getEthereumMnemonic() {
		return ethereumMnemonic;
	}

	public Credentials getEthereumCredentials() throws AgentLockedException {
		if (isLocked()) {
			throw new AgentLockedException();
		}
		return this.credentials;
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

			Element ethereumMnemonicElement = XmlTools.getSingularElement(root, "ethereummnemonic");
			String ethereumMnemonic = ethereumMnemonicElement.getTextContent();

			Element ethereumAddressElement = XmlTools.getSingularElement(root, "ethereumaddress");
			String ethereumAddress = ethereumAddressElement.getTextContent();

			// required fields complete, create result
			EthereumAgent result = new EthereumAgent(publicKey, encPrivate, salt, login, ethereumMnemonic, ethereumAddress);

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
