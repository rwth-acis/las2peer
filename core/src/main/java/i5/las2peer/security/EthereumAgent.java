package i5.las2peer.security;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.registryGateway.BadEthereumCredentialsException;
import i5.las2peer.registryGateway.CredentialUtils;
import i5.las2peer.registryGateway.ReadWriteRegistryClient;
import i5.las2peer.registryGateway.RegistryConfiguration;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import org.w3c.dom.Element;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class EthereumAgent extends UserAgentImpl {
	// this should probably be configured elsewhere
	private static final String DEFAULT_WALLET_DIRECTORY = "./etc/wallets";

	private String ethereumWalletPath;

	// it would be prettier to have a single password for both the user private key and the wallet
	// but they're generated separately for now, so whatever
	// FIXME actually, let's force it to be the same now
	// TODO change wallet passwords
	//private String ethereumWalletPassword;

	private ReadWriteRegistryClient registryClient;

	public EthereumAgent(PublicKey pubKey, byte[] encryptedPrivate, byte[] salt, String ethereumWalletPath) {
		super(pubKey, encryptedPrivate, salt);
		this.ethereumWalletPath = ethereumWalletPath;
	}

	protected EthereumAgent(KeyPair pair, String passphrase, byte[] salt, String ethereumWalletPath)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
		this.ethereumWalletPath = ethereumWalletPath;
	}

	// bit of unfortunate name here, but let's stick with it
	@Override
	public void lockPrivateKey() {
		super.lockPrivateKey();
		registryClient = null;
	}

	@Override
	public void unlock(String passphrase) throws AgentAccessDeniedException, AgentOperationFailedException {
		super.unlock(passphrase);
		try {
			registryClient = new ReadWriteRegistryClient(new RegistryConfiguration(),
				CredentialUtils.fromWallet(ethereumWalletPath, passphrase));
		} catch (BadEthereumCredentialsException e) {
			throw new AgentAccessDeniedException("Could not unlock Ethereum wallet. Ensure password is set to the"
				+ "agent's passphrase (if not change it!).", e);
		}
	}

	@Override
	public boolean isLocked() {
		boolean ethereumIsLocked = registryClient == null;
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
					+ "\t<ethereumwalletpath>" + ethereumWalletPath + "</ethereumwalletpath>\n");

			if (sLoginName != null) {
				result.append("\t<login>" + sLoginName + "</login>\n");
			}
			if (sEmail != null) {
				result.append("\t<email>" + sEmail + "</email>\n");
			}

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	public static EthereumAgent createEthereumAgent(String passphrase) throws CryptoException  {
		byte[] salt = CryptoTools.generateSalt();
		try {
			String walletPath = CredentialUtils.createWallet(passphrase, DEFAULT_WALLET_DIRECTORY);
			return new EthereumAgent(CryptoTools.generateKeyPair(), passphrase, salt, walletPath);
		} catch (Exception e) {
			throw new CryptoException("Wallet generation failed.", e);
		}
	}

	public ReadWriteRegistryClient getRegistryClient() {
		return registryClient;
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

			Element ethereumWalletPathElement = XmlTools.getSingularElement(root, "ethereumwalletpath");
			String ethereumWalletPath = ethereumWalletPathElement.getTextContent();

			// required fields complete, create result
			EthereumAgent result = new EthereumAgent(publicKey, encPrivate, salt, ethereumWalletPath);

			// read and set optional fields

			// optional login name
			Element login = XmlTools.getOptionalElement(root, "login");
			if (login != null) {
				result.sLoginName = login.getTextContent();
			}
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
