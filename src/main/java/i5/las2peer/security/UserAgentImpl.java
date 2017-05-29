package i5.las2peer.security;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.PingPongContent;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

/**
 * An UserAgent represent a (End)user of the las2peer system.
 * 
 */
public class UserAgentImpl extends PassphraseAgentImpl implements UserAgent {

	private String sLoginName = null;
	private String sEmail = null;

	/**
	 * atm constructor for the MockAgent class, just don't know, how agent creation will take place later
	 * 
	 * @param pair
	 * @param passphrase
	 * @param salt
	 * @throws AgentOperationFailedException
	 * @throws CryptoException
	 */
	protected UserAgentImpl(KeyPair pair, String passphrase, byte[] salt)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
	}

	/**
	 * create an agent with a locked private key
	 * 
	 * used within {@link #createFromXml}
	 * 
	 * @param pubKey
	 * @param encryptedPrivate
	 * @param salt
	 */
	protected UserAgentImpl(PublicKey pubKey, byte[] encryptedPrivate, byte[] salt) {
		super(pubKey, encryptedPrivate, salt);
	}

	@Override
	public String getLoginName() {
		return sLoginName;
	}

	@Override
	public boolean hasLoginName() {
		return getLoginName() != null;
	}

	@Override
	public void setLoginName(String loginName) throws AgentLockedException, IllegalArgumentException {
		if (this.isLocked()) {
			throw new AgentLockedException();
		}

		if (loginName != null && loginName.length() < 4) {
			throw new IllegalArgumentException("please use a login name longer than three characters!");
		}

		if (loginName != null && !(loginName.matches("[a-zA-Z].*"))) {
			throw new IllegalArgumentException("please use a login name startung with a normal character (a-z or A-Z)");
		}

		// duplicate check is performed when storing/updating an UserAgent in a Node
		this.sLoginName = loginName;
	}

	@Override
	public void setEmail(String email) throws AgentLockedException, IllegalArgumentException {
		if (this.isLocked()) {
			throw new AgentLockedException();
		}

		// http://stackoverflow.com/questions/153716/verify-email-in-java
		Pattern rfc2822 = Pattern.compile(
				"^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");

		if (email != null && !email.contains("@") && !rfc2822.matcher(email).matches()) {
			throw new IllegalArgumentException("Invalid e-mail address");
		}

		// duplicate check is performed when storing/updating an UserAgent in a Node
		this.sEmail = email.toLowerCase();
	}

	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer("<las2peer:agent type=\"user\">\n" + "\t<id>" + getIdentifier()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">" + SerializeTools.serializeToBase64(getPublicKey())
					+ "</publickey>\n" + "\t<privatekey encrypted=\"" + CryptoTools.getSymmetricAlgorithm()
					+ "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod() + "\">\n"
					+ "\t\t<salt encoding=\"base64\">" + Base64.getEncoder().encodeToString(getSalt()) + "</salt>\n"
					+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n" + "\t</privatekey>\n");

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

	/**
	 * sets the state of the object from a string representation resulting from a previous {@link #toXmlString} call.
	 *
	 * Usually, a standard constructor is used to get a fresh instance of the class and the set the complete state via
	 * this method.
	 *
	 *
	 * @param xml a String
	 * @return Returns a new UserAgent instance
	 *
	 * @exception MalformedXMLException
	 *
	 */
	public static UserAgentImpl createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	/**
	 * Create a new UserAgent protected by the given passphrase.
	 * 
	 * @param passphrase passphrase for the secret key of the new user
	 * @return Returns a new UserAgent instance
	 * @throws CryptoException
	 * @throws AgentOperationFailedException
	 */
	public static UserAgentImpl createUserAgent(String passphrase)
			throws CryptoException, AgentOperationFailedException {
		byte[] salt = CryptoTools.generateSalt();
		return new UserAgentImpl(CryptoTools.generateKeyPair(), passphrase, salt);
	}

	/**
	 * Sets the state of the object from a string representation resulting from a previous {@link #toXmlString} call.
	 *
	 * @param root parsed XML document
	 * @return Returns a new UserAgent instance
	 * @throws MalformedXMLException
	 */
	public static UserAgentImpl createFromXml(Element root) throws MalformedXMLException {
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

			// required fields complete, create result
			UserAgentImpl result = new UserAgentImpl(publicKey, encPrivate, salt);

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

	@Override
	public void receiveMessage(Message message, AgentContext context) throws MessageException {
		try {
			message.open(this, getRunningAtNode());
			Object content = message.getContent();

			if (content instanceof PingPongContent) {
				Message answer = new Message(message, new PingPongContent());

				System.out.println("PingPong: sending answer!");

				getRunningAtNode().sendResponse(answer, message.getSendingNodeId());
			} else {
				System.out.println("got message: " + message.getContent().getClass() + " / " + message.getContent());
				System.out.println("response: " + message.getResponseToId());
				throw new MessageException("What to do with this message?!");
			}
		} catch (InternalSecurityException e) {
			throw new MessageException("Security problems handling the received message", e);
		} catch (EncodingFailedException e) {
			throw new MessageException("encoding problems with sending an answer", e);
		} catch (SerializationException e) {
			throw new MessageException("serialization problems with sending an answer", e);
		} catch (AgentException e) {
			// just fire and forget
		} catch (NodeNotFoundException e) {
			// just fire and forget
		}
	}

	@Override
	public void notifyUnregister() {
		// do nothing
	}

	/**
	 * get the email address assigned to this agent
	 * 
	 * @return an email address
	 */
	@Override
	public String getEmail() {
		return sEmail;
	}

	/**
	 * has this user a registered email address?
	 * 
	 * @return true, if an email address is assigned
	 */
	@Override
	public boolean hasEmail() {
		return getEmail() != null;
	}

}
