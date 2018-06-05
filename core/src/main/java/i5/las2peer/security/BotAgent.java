package i5.las2peer.security;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

import org.w3c.dom.Element;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.monitoring.MonitoringMessage;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

public class BotAgent extends UserAgentImpl implements UserAgent {
	private final static String BOT_MANAGER_SERVICE_CLASS_NAME = "i5.las2peer.services.socialBotManagerService.SocialBotManagerService";

	protected BotAgent(KeyPair pair, String passphrase, byte[] salt)
			throws AgentOperationFailedException, CryptoException {
		super(pair, passphrase, salt);
	}

	/**
	 * 
	 * Create a new MonitoringAgent protected by the given passphrase.
	 * 
	 * @param passphrase passphrase for the secret key of the new agent
	 * @return a new UserAgent
	 * @throws CryptoException
	 * @throws AgentOperationFailedException
	 * 
	 */
	public static BotAgent createBotAgent(String passphrase) throws CryptoException, AgentOperationFailedException {
		return new BotAgent(CryptoTools.generateKeyPair(), passphrase, CryptoTools.generateSalt());
	}

	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer("<las2peer:agent type=\"bot\">\n" + "\t<id>" + getIdentifier()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">" + SerializeTools.serializeToBase64(getPublicKey())
					+ "</publickey>\n" + "\t<privatekey encrypted=\"" + CryptoTools.getSymmetricAlgorithm()
					+ "\" keygen=\"" + CryptoTools.getSymmetricKeygenMethod() + "\">\n"
					+ "\t\t<salt encoding=\"base64\">" + Base64.getEncoder().encodeToString(getSalt()) + "</salt>\n"
					+ "\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n" + "\t</privatekey>\n");

			if (hasLoginName()) {
				result.append("\t<login>" + getLoginName() + "</login>\n");
			}
			if (hasEmail()) {
				result.append("\t<email>" + getEmail() + "</email>\n");
			}

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	/**
	 * 
	 * This method is called by the node this agent is running at. In this context, it is used to receive monitoring
	 * messages send by the {@link i5.las2peer.logging.monitoring.MonitoringObserver}s of the monitored nodes to the
	 * central processing service. Every other type of communication (to agents not resided at the monitoring node,
	 * other types of content than {@link i5.las2peer.logging.monitoring.MonitoringMessage}s will result in an
	 * Exception.
	 * 
	 * @param message
	 * @param context
	 * @throws MessageException
	 * 
	 */
	@Override
	public void receiveMessage(Message message, AgentContext context) throws MessageException {
		try {
			// Test for instance
			message.open(this, getRunningAtNode());
			Object content = message.getContent();
			if (content instanceof MonitoringMessage[]) {
				Serializable[] parameters = { (Serializable) content };
				try {
					// Try to send the content of the message to the Processing Service
					boolean success = (Boolean) getRunningAtNode().invoke(this, BOT_MANAGER_SERVICE_CLASS_NAME,
							"getMessages", parameters);
					if (!success) {
						System.out.println(
								"Bot: Something went wrong while invoking Processing Service to deliver a monitoring message!");
					}
				} catch (ServiceNotFoundException e) {
					System.out.println("Bot: I am not the Processing Service!");
				} catch (ServiceInvocationException e) {
					System.out.println("Bot: Something went wrong while invoking Processing Service!");
					e.printStackTrace();
				}
			} else {
				throw new MessageException("MonitoringAgents only receive monitoring messages!");
			}
		} catch (InternalSecurityException e) {
			throw new MessageException("Security problems handling the received message", e);
		} catch (AgentNotFoundException e) {
			// Do nothing..("this" is not known..would be strange, eh?)
		} catch (AgentException e) {
			throw new MessageException("Could not read the sender agent", e);
		}
	}

	protected BotAgent(PublicKey pubKey, byte[] encodedPrivate, byte[] salt) {
		super(pubKey, encodedPrivate, salt);
	}

	public static BotAgent createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	public static BotAgent createFromXml(Element root) throws MalformedXMLException {
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
			BotAgent result = new BotAgent(publicKey, encPrivate, salt);

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
