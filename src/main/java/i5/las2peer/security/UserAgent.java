package i5.las2peer.security;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.communication.PingPongContent;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

/**
 * An UserAgent represent a (End)user of the LAS2peer system.
 * 
 */
public class UserAgent extends PassphraseAgent {

	private String sLoginName = null;
	private String sEmail = null;
	private Serializable userData = null;

	/**
	 * atm constructor for the MockAgent class, just don't know, how agent creation will take place later 
	 * 
	 * @param id
	 * @param pair
	 * @param passphrase
	 * @throws L2pSecurityException
	 * @throws CryptoException 
	 */
	protected UserAgent(long id, KeyPair pair, String passphrase, byte[] salt) throws L2pSecurityException,
			CryptoException {
		super(id, pair, passphrase, salt);

	}

	/**
	 * create an agent with a locked private key
	 * 
	 * used within {@link #createFromXml}
	 * 
	 * @param id
	 * @param pubKey
	 * @param encryptedPrivate
	 * @param salt
	 */
	protected UserAgent(long id, PublicKey pubKey, byte[] encryptedPrivate, byte[] salt) {
		super(id, pubKey, encryptedPrivate, salt);
	}

	/**
	 * get the login name stored for this user agent
	 * @return the user login name
	 */
	public String getLoginName() {
		return sLoginName;
	}

	/**
	 * has this user a login name
	 * @return true, if a login name is assigned
	 */
	public boolean hasLogin() {
		return sLoginName != null;
	}

	/**
	 * select a login name for this agent
	 * @param loginName
	 * 
	 * @throws L2pSecurityException
	 * @throws UserAgentException
	 */
	public void setLoginName(String loginName) throws L2pSecurityException, UserAgentException {
		if (this.isLocked())
			throw new L2pSecurityException("unlock needed first!");

		if (loginName != null && loginName.length() < 4)
			throw new UserAgentException("please use a login name longer than three characters!");

		if (loginName != null && !(loginName.matches("[a-zA-Z].*")))
			throw new UserAgentException("please use a login name startung with a normal character (a-z or A-Z)");

		// TODO!!!!!!!
		// duplicate check
		this.sLoginName = loginName;
	}

	/**
	 * select an email address to assign to this user agent
	 * @param email
	 * @throws L2pSecurityException 
	 * @throws UserAgentException 
	 */
	public void setEmail(String email) throws L2pSecurityException, UserAgentException {
		if (this.isLocked())
			throw new L2pSecurityException("unlock needed first!");

		if (email != null && !email.contains("@"))
			throw new UserAgentException("This email address contains no @ character...");

		// TODO!!!!!!!
		// duplicate check
		this.sEmail = email.toLowerCase();
	}

	/**
	 * Attaches the given object directly to this agent. The
	 * user data represent a field of this user agent and
	 * should be used with small values (&lt; 1MB) only.
	 * Larger byte amounts could handicap the agent handling
	 * inside the network.
	 * 
	 * @param object The user data object to be serialized and attached.
	 * @throws L2pSecurityException When the user agent is still locked.
	 */
	public void setUserData(Serializable object) throws L2pSecurityException {
		if (this.isLocked()) {
			throw new L2pSecurityException("unlock needed first!");
		}
		this.userData = object;
	}

	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer(
					"<las2peer:agent type=\"user\">\n"
							+ "\t<id>" + getId() + "</id>\n"
							+ "\t<publickey encoding=\"base64\">"
							+ SerializeTools.serializeToBase64(getPublicKey())
							+ "</publickey>\n"
							+ "\t<privatekey encrypted=\"" + CryptoTools.getSymmetricAlgorithm() + "\" keygen=\""
							+ CryptoTools.getSymmetricKeygenMethod() + "\">\n"
							+ "\t\t<salt encoding=\"base64\">" + Base64.encodeBase64String(getSalt()) + "</salt>\n"
							+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n"
							+ "\t</privatekey>\n"
					);

			if (sLoginName != null)
				result.append("\t<login>" + sLoginName + "</login>\n");
			if (sEmail != null)
				result.append("\t<email>" + sEmail + "</email>\n");
			if (userData != null) {
				result.append("\t<userdata>" + SerializeTools.serializeToBase64(userData) + "</userdata>\n");
			}

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	/**
	 * sets the state of the object from a string representation resulting from
	 * a previous {@link #toXmlString} call.
	 *
	 * Usually, a standard constructor is used to get a fresh instance of the
	 * class and the set the complete state via this method.
	 *
	 *
	 * @param    xml                 a  String
	 *
	 * @exception   MalformedXMLException
	 *
	 */
	public static UserAgent createFromXml(String xml) throws MalformedXMLException {
		try {
			Element root = Parser.parse(xml, false);

			if (!"user".equals(root.getAttribute("type")))
				throw new MalformedXMLException("user agent expected");

			if (!"agent".equals(root.getName()))
				throw new MalformedXMLException("agent expected");
			return createFromXml(root);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}

	/**
	 * Create a new UserAgent protected by the given passphrase.
	 * 
	 * @param passphrase passphrase for the secret key of the new user
	 * 
	 * @return a new UserAgent
	 * 
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	public static UserAgent createUserAgent(String passphrase) throws CryptoException, L2pSecurityException {
		Random r = new Random();

		byte[] salt = CryptoTools.generateSalt();

		return new UserAgent(r.nextLong(), CryptoTools.generateKeyPair(), passphrase, salt);
	}

	/**
	 * Create a new UserAgent with a given id protected by the given passphrase.
	 * 
	 * @param id agent id of new user
	 * @param passphrase passphrase for the secret key of the new user
	 * 
	 * @return a new UserAgent
	 * 
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	public static UserAgent createUserAgent(long id, String passphrase) throws CryptoException, L2pSecurityException {

		byte[] salt = CryptoTools.generateSalt();

		return new UserAgent(id, CryptoTools.generateKeyPair(), passphrase, salt);
	}

	/**
	 * Sets the state of the object from a string representation resulting from
	 * a previous {@link #toXmlString} call.
	 *
	 * @param    root		parsed xml document
	 *
	 * @exception   MalformedXMLException
	 *
	 */
	public static UserAgent createFromXml(Element root) throws MalformedXMLException {
		try {
			Element elId = root.getFirstChild();
			long id = Long.parseLong(elId.getFirstChild().getText());

			Element pubKey = root.getChild(1);
			if (!pubKey.getName().equals("publickey"))
				throw new MalformedXMLException("public key expected");
			if (!pubKey.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");

			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getFirstChild().getText());

			Element privKey = root.getChild(2);
			if (!privKey.getName().equals("privatekey"))
				throw new MalformedXMLException("private key expected");
			if (!privKey.getAttribute("encrypted").equals(CryptoTools.getSymmetricAlgorithm()))
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			if (!privKey.getAttribute("keygen").equals(CryptoTools.getSymmetricKeygenMethod()))
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod() + " expected");

			Element elSalt = privKey.getFirstChild();
			if (!elSalt.getName().equals("salt"))
				throw new MalformedXMLException("salt expected");
			if (!elSalt.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");

			byte[] salt = Base64.decodeBase64(elSalt.getFirstChild().getText());

			Element data = privKey.getChild(1);
			if (!data.getName().equals("data"))
				throw new MalformedXMLException("data expected");
			if (!data.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");
			byte[] encPrivate = Base64.decodeBase64(data.getFirstChild().getText());

			UserAgent result = new UserAgent(id, publicKey, encPrivate, salt);

			int cnt = 3;
			Element login = root.getChild(cnt);
			if (login != null && login.getName().equals("login")) {
				result.sLoginName = login.getFirstChild().getText();
				cnt++;
			}

			Element email = root.getChild(cnt);
			if (email != null) {
				if (!email.getName().equals("email")) {
					throw new MalformedXMLException("email or login element expected!");
				}
				result.sEmail = email.getFirstChild().getText();
				cnt++;
			}

			Element xmlUserData = root.getChild(cnt);
			if (xmlUserData != null && xmlUserData.getName().equals("userdata")) {
				String base64UserData = xmlUserData.getFirstChild().getText();
				result.userData = SerializeTools.deserializeBase64(base64UserData);
				cnt++;
			}

			return result;
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		}
	}

	@Override
	public void receiveMessage(Message message, Context context) throws MessageException {
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
		} catch (L2pSecurityException e) {
			throw new MessageException("Security problems handling the received message", e);
		} catch (EncodingFailedException e) {
			throw new MessageException("encoding problems with sending an answer", e);
		} catch (SerializationException e) {
			throw new MessageException("serialization problems with sending an answer", e);
		} catch (AgentNotKnownException e) {
			// just fire and forget
		} catch (NodeNotFoundException e) {
			// just fire and forget
		}

	}

	@Override
	public void notifyUnregister() {
		// TODO Auto-generated method stub	
	}

	/**
	 * get the email address assigned to this agent
	 * @return an email address
	 */
	public String getEmail() {
		return sEmail;
	}

	/**
	 * has this user a registered email address?
	 * @return true, if an email address is assigned
	 */
	public boolean hasEmail() {
		return sEmail != null;
	}

	/**
	 * get the user data assigned to this agent
	 * 
	 * @return Returns the user data object
	 */
	public Serializable getUserData() {
		return this.userData;
	}

}
