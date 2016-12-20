package i5.las2peer.communication;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentStorage;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.XmlTools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import rice.p2p.commonapi.NodeHandle;

/**
 * Base class for sending messages between {@link Agent}s.
 * 
 * The content of the message will be encrypted symmetrically with a randomly generated key, this key will be encrypted
 * asymmetrically for the recipient of the message.
 * 
 * Additionally, the contents will be signed with the private key of the sender.
 * 
 * Therefore, it is necessary, that the generating Thread has access to the private key of the sending agent.
 * 
 * When specifying a topic, the message will be sent to all agents listening to the topic. Since these agents are not
 * known, the message will not be encrypted.
 * 
 */
public class Message implements XmlAble, Cloneable {

	public static final long DEFAULT_TIMEOUT = 30 * 1000; // 30 seconds

	/**
	 * sender of the message
	 */
	private Agent sender = null;

	/**
	 * id of the sending agent
	 */
	private long senderId;

	/**
	 * recipient of the message
	 */
	private Agent recipient = null;

	/**
	 * id of the receiving agent
	 */
	private Long recipientId = null;

	/**
	 * id of the receiving topic
	 */
	private Long topicId = null;

	/**
	 * message content (if opened)
	 */
	private Object content = null;

	/**
	 * (asymmetrically) encrypted content of the message
	 */
	private byte[] baEncryptedContent;

	/**
	 * signature of the message content
	 */
	private byte[] baSignature;

	/**
	 * symmetric key for the content of this message encrypted for the recipient
	 */
	private byte[] baContentKey;

	/**
	 * timestamp of the message generation
	 */
	private long timestampMs;

	/**
	 * how long in milliseconds is this message valid (timestamp of timeout is timestamp + validMs)
	 */
	private long validMs;

	/**
	 * a simple message id for reference, i.e. answer messages
	 */
	private long id;

	/**
	 * id of a message, this one is a response to
	 */
	private Long responseToId = null;

	private Serializable sendingNodeId = null;

	/**
	 * constructor for the {@link XmlAble} facilities
	 */
	public Message() {
		// just for XmlAble
	}

	/**
	 * create a new message with default timeout
	 * 
	 * @param from
	 * @param to
	 * @param data
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 * 
	 */
	public Message(Agent from, Agent to, Serializable data) throws EncodingFailedException, L2pSecurityException,
			SerializationException {
		this(from, to, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message
	 * 
	 * @param from
	 * @param to
	 * @param data
	 * @param timeOutMs timeout for the validity of the new message
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Agent from, Agent to, Serializable data, long timeOutMs) throws EncodingFailedException,
			L2pSecurityException, SerializationException {
		if (from == null || to == null) {
			throw new IllegalArgumentException("null not allowed as sender or recipient!");
		}
		sender = from;
		senderId = from.getId();
		recipient = to;
		recipientId = to.getId();
		content = data;

		timestampMs = new Date().getTime();
		validMs = timeOutMs;

		id = new Random().nextLong();

		encryptContent();

		signContent();

		close();
	}

	/**
	 * create a new message with default timeout
	 * 
	 * @param from
	 * @param to
	 * @param data
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Agent from, Agent to, XmlAble data) throws EncodingFailedException, L2pSecurityException,
			SerializationException {
		this(from, to, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message
	 * 
	 * @param from
	 * @param to
	 * @param data
	 * @param timeoutMs timeout for the validity of the new message
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Agent from, Agent to, XmlAble data, long timeoutMs) throws EncodingFailedException,
			L2pSecurityException, SerializationException {
		sender = from;
		senderId = from.getId();
		recipient = to;
		recipientId = to.getId();
		content = data;
		validMs = timeoutMs;

		finalizeConstructor();
	}

	/**
	 * create a new message to a topic with default timeout
	 * 
	 * @param from
	 * @param topic
	 * @param data
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 */
	public Message(Agent from, long topic, Serializable data) throws EncodingFailedException, L2pSecurityException,
			SerializationException {
		this(from, topic, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message to all agents listening on the given topic
	 * 
	 * @param from
	 * @param topic
	 * @param data
	 * @param timeoutMs
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 */
	public Message(Agent from, long topic, Serializable data, long timeoutMs) throws EncodingFailedException,
			L2pSecurityException, SerializationException {
		if (from == null) {
			throw new IllegalArgumentException("null not allowed as sender!");
		}

		sender = from;
		senderId = from.getId();
		topicId = topic;
		content = data;
		validMs = timeoutMs;

		timestampMs = new Date().getTime();

		// id = new Random().nextLong();

		finalizeConstructor();
	}

	/**
	 * common to all constructors
	 * 
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 */
	private void finalizeConstructor() throws EncodingFailedException, L2pSecurityException, SerializationException {
		timestampMs = new Date().getTime();
		id = new Random().nextLong();

		if (!isTopic()) {
			encryptContent();
		} else {
			baEncryptedContent = getContentString().getBytes(StandardCharsets.UTF_8);
		}

		signContent();

		close();
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo
	 * @param data
	 * @param timeoutMs timeout for the validity of the new message
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Message responseTo, XmlAble data, long timeoutMs) throws EncodingFailedException,
			L2pSecurityException, SerializationException {
		if (!responseTo.isOpen()) {
			throw new IllegalStateException("the original message has to be open to create a response to it!");
		}

		if (responseTo.getRecipient() == null) {
			throw new IllegalStateException("the original message has to have an recipient attached");
		}

		sender = responseTo.getRecipient();
		senderId = responseTo.getRecipientId();
		recipient = responseTo.getSender();
		recipientId = responseTo.getSenderId();
		validMs = timeoutMs;
		content = data;

		responseToId = responseTo.getId();

		finalizeConstructor();
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo
	 * @param data
	 * @throws SerializationException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws EncodingFailedException
	 */
	public Message(Message responseTo, XmlAble data) throws EncodingFailedException, L2pSecurityException,
			SerializationException {
		this(responseTo, data, DEFAULT_TIMEOUT);
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo
	 * @param data
	 * @param timeoutMs
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Message responseTo, Serializable data, long timeoutMs) throws EncodingFailedException,
			L2pSecurityException, SerializationException {
		if (!responseTo.isOpen()) {
			throw new IllegalStateException("the original message has to be open to create a response to it!");
		}

		if (responseTo.getRecipient() == null) {
			throw new IllegalStateException("the original message has to have an recipient attached");
		}

		sender = responseTo.getRecipient();
		senderId = responseTo.getRecipientId();
		recipient = responseTo.getSender();
		recipientId = responseTo.getSenderId();

		validMs = timeoutMs;
		content = data;

		responseToId = responseTo.getId();

		finalizeConstructor();
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo
	 * @param data
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException
	 */
	public Message(Message responseTo, Serializable data) throws EncodingFailedException, L2pSecurityException,
			SerializationException {
		this(responseTo, data, DEFAULT_TIMEOUT);
	}

	/**
	 * get the contents of this message as base 64 encoded string
	 * 
	 * @return message contents in xml format with all important attributes and the actual content as base64 encoded
	 *         string
	 * 
	 * @throws SerializationException
	 */
	private String getContentString() throws SerializationException {
		String typeAttr;
		String sContent;
		if (content instanceof XmlAble) {
			typeAttr = "XmlAble";
			sContent = ((XmlAble) content).toXmlString();
		} else {
			typeAttr = "Serializable";
			sContent = Base64.getEncoder().encodeToString(SerializeTools.serialize((Serializable) content));
		}

		String attrs = "";
		if (responseToId != null) {
			attrs += " responseTo=\"" + responseToId + "\"";
		}

		if (!isTopic()) {
			attrs += " recipient=\"" + recipient.getId() + "\"";
		} else {
			attrs += " topic=\"" + topicId + "\"";
		}

		return "<las2peer:messageContent" + " id=\"" + id + "\"" + " sender=\"" + sender.getId() + "\"" + " class=\""
				+ content.getClass().getCanonicalName() + "\"" + " type=\"" + typeAttr + "\"" + " timestamp=\""
				+ timestampMs + "\"" + " timeout=\"" + validMs + "\"" + attrs + ">" + sContent
				+ "</las2peer:messageContent>";
	}

	/**
	 * encrypt the content of this message (as base64 encoded string) with asymmetric encryption
	 * 
	 * @throws EncodingFailedException
	 */
	private void encryptContent() throws EncodingFailedException {
		if (recipient == null) {
			return;
		}

		try {
			SecretKey contentKey = CryptoTools.generateSymmetricKey();
			baContentKey = CryptoTools.encryptAsymmetric(contentKey, recipient.getPublicKey());

			String contentString = getContentString();
			baEncryptedContent = CryptoTools.encryptSymmetric(contentString.getBytes(StandardCharsets.UTF_8),
					contentKey);
		} catch (SerializationException e) {
			throw new EncodingFailedException("serialization problems with encryption", e);
		} catch (CryptoException e) {
			throw new EncodingFailedException("unable to encrypt the secret message key", e);
		}
	}

	/**
	 * sign the contents of this message
	 * 
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 */
	private void signContent() throws L2pSecurityException, SerializationException, EncodingFailedException {
		try {
			byte[] contentBytes = getContentString().getBytes(StandardCharsets.UTF_8);

			Signature sig = sender.createSignature();
			sig.update(contentBytes);

			baSignature = sig.sign();
		} catch (InvalidKeyException e) {
			throw new EncodingFailedException("Key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new EncodingFailedException("Algorithm problems", e);
		} catch (SignatureException e) {
			throw new EncodingFailedException("Signature problems", e);
		}
	}

	/**
	 * get the sending agent of this message
	 * 
	 * only works after opening or creation the message
	 * 
	 * @return sending agent
	 */
	public Agent getSender() {
		return sender;
	}

	/**
	 * get the id of the sending agent
	 * 
	 * @return id of the sending agent
	 */
	public long getSenderId() {
		return senderId;
	}

	/**
	 * get the designated recipient of this message
	 * 
	 * only works after opening or creating of the message
	 * 
	 * @return (designated) receiver
	 */
	public Agent getRecipient() {
		return recipient;
	}

	/**
	 * get the id of the recipient agent
	 * 
	 * @return id of the receiving agent
	 */
	public Long getRecipientId() {
		return recipientId;
	}

	/**
	 * get the id of the receiving topic
	 * 
	 * @return
	 */
	public Long getTopicId() {
		return topicId;
	}

	/**
	 * check if this message is sent to a topic
	 * 
	 * @return
	 */
	public boolean isTopic() {
		return topicId != null;
	}

	/**
	 * get the id of this message
	 * 
	 * @return id
	 */
	public long getId() {
		return id;
	}

	/**
	 * 
	 * @return id of the message, this one is a response to
	 */
	public Long getResponseToId() {
		if (responseToId == null) {
			return null;
		}
		return new Long(responseToId);
	}

	/**
	 * 
	 * @return true, if this message is a response to another one
	 */
	public boolean isResponse() {
		return responseToId != null;
	}

	/**
	 * get the content of this message may be Serializable or XmlAble
	 * 
	 * @return actual content of the message
	 * @throws L2pSecurityException the message (envelope) has to be opened (decrypted) first
	 */
	public Object getContent() throws L2pSecurityException {
		if (!isOpen()) {
			throw new L2pSecurityException("You have to open the envelope first!");
		}

		return content;
	}

	/**
	 * open the envelope, i.e. decrypt the content with the private key of the receiving agent
	 * 
	 * The storage has to know an unlocked version of the recipient agent! (i.e. a
	 * {@link i5.las2peer.security.AgentContext} bound to him.
	 * 
	 * @param storage
	 * @throws L2pSecurityException
	 * @throws AgentNotKnownException
	 */
	public void open(AgentStorage storage) throws L2pSecurityException, AgentNotKnownException {
		open(null, storage);
	}

	/**
	 * open the envelope, i.e. decrypt the content with the private key of the receiving agent
	 * 
	 * the private key has to be unlocked first!
	 * 
	 * @param unlockedRecipient
	 * @param storage
	 * 
	 * 
	 * @throws L2pSecurityException the private key of the receiver has to be unlocked for decryption
	 * @throws AgentNotKnownException
	 */
	public void open(Agent unlockedRecipient, AgentStorage storage) throws L2pSecurityException, AgentNotKnownException {
		if (isOpen()) {
			return;
		}

		sender = storage.getAgent(senderId);

		if (recipientId != null) { // topic messages are not encrypted
			if (unlockedRecipient != null && unlockedRecipient.getId() == recipientId) {
				recipient = unlockedRecipient;
			} else {
				recipient = storage.getAgent(recipientId);
			}

			if (recipient.isLocked()) {
				throw new L2pSecurityException("private key of recipient is locked!");
			}
		}

		try {
			byte[] rawContent;

			if (!isTopic()) {
				SecretKey contentKey = recipient.decryptSymmetricKey(baContentKey);
				rawContent = CryptoTools.decryptSymmetric(baEncryptedContent, contentKey);
			} else { // topics are not encrypted
				rawContent = baEncryptedContent;
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(rawContent));
			doc.getDocumentElement().normalize();
			Element root = doc.getDocumentElement();

			if (!root.hasAttribute("sender")) {
				throw new L2pSecurityException("content block needs sender attribute!");
			}
			if (!root.hasAttribute("recipient") && !root.hasAttribute("topic")) {
				throw new L2pSecurityException("content block needs recipient or topic attribute!");
			}
			if (!root.hasAttribute("timestamp")) {
				throw new L2pSecurityException("content block needs timestamp attribute!");
			}
			if (!root.hasAttribute("timeout")) {
				throw new L2pSecurityException("content block needs timeout attribute!");
			}
			if (!root.hasAttribute("id")) {
				throw new L2pSecurityException("content block needs id attribute!");
			}

			if (Long.parseLong(root.getAttribute("sender")) != (sender.getId())) {
				throw new L2pSecurityException("message is signed for another sender!!");
			}
			if (root.hasAttribute("recipient")
					&& (recipient == null || Long.parseLong(root.getAttribute("recipient")) != (recipient.getId()))) {
				throw new L2pSecurityException("message is signed for another recipient!!");
			}
			if (root.hasAttribute("topic") && Long.parseLong(root.getAttribute("topic")) != (topicId)) {
				throw new L2pSecurityException("message is signed for another topic!!");
			}
			if (Long.parseLong(root.getAttribute("timestamp")) != timestampMs) {
				throw new L2pSecurityException("message is signed for another timestamp!!");
			}
			if (Long.parseLong(root.getAttribute("timeout")) != validMs) {
				throw new L2pSecurityException("message is signed for another timeout value!!");
			}
			if (Long.parseLong(root.getAttribute("id")) != id) {
				throw new L2pSecurityException("message is signed for another id!");
			}

			if ((root.hasAttribute("responseTo") || responseToId != null)
					&& !responseToId.equals(Long.parseLong(root.getAttribute("responseTo")))) {
				throw new L2pSecurityException("message is signed as response to another message!");
			}

			if (root.getAttribute("type").equals("Serializable")) {
				content = SerializeTools.deserializeBase64(root.getTextContent());
			} else {
				content = XmlAble.createFromXml(root.getFirstChild().toString(), root.getAttribute("class"));
			}
		} catch (CryptoException e) {
			throw new L2pSecurityException("Crypto-Problems: Unable to open message content", e);
		} catch (SerializationException e) {
			throw new L2pSecurityException("deserializiation problems with decryption!", e);
		} catch (ClassNotFoundException e) {
			throw new L2pSecurityException("content class missing with decryption!", e);
		} catch (MalformedXMLException | ParserConfigurationException | SAXException | IOException e) {
			throw new L2pSecurityException("xml syntax problems with decryption!", e);
		}

		// verify signature
		verifySignature();
	}

	/**
	 * verify the signature of this message the content has to be available for this
	 * 
	 * @throws L2pSecurityException
	 */
	public void verifySignature() throws L2pSecurityException {
		Signature sig;
		try {
			byte[] contentBytes = getContentString().getBytes(StandardCharsets.UTF_8);

			sig = Signature.getInstance(CryptoTools.getSignatureMethod());
			sig.initVerify(sender.getPublicKey());
			sig.update(contentBytes);

			if (!sig.verify(baSignature)) {
				String log = ""; // TODO LAS-353 logging; remove when resolved
				log += "--------------------- [BEGIN] ---------------------" + "\n";
				log += "Signature invalid. Please report the following output to LAS-353." + "\n";
				log += "--------------------- [Message XML] ---------------------" + "\n";
				log += this.toXmlString() + "\n";
				log += "--------------------- [Decrypted content (as String)] ---------------------" + "\n";
				log += getContentString() + "\n";
				log += "--------------------- [Sender PublicKey] ---------------------" + "\n";
				log += sender.getPublicKey() + "\n";
				log += "--------------------- [Sender Agent] ---------------------" + "\n";
				log += this.getSender().toXmlString() + "\n";
				log += "--------------------- [Receiver Agent] ---------------------" + "\n";
				log += this.getRecipient().toXmlString() + "\n";
				log += "--------------------- [Receiver Agent Passphrase] ---------------------" + "\n";
				log += ((PassphraseAgent) this.getRecipient()).getPassphrase() + "\n";
				log += "--------------------- [END] ---------------------" + "\n";
				L2pLogger.getInstance(Message.class).log(Level.SEVERE, log);
				throw new L2pSecurityException("Signature invalid!");
			}
		} catch (InvalidKeyException e) {
			throw new L2pSecurityException("unable to verify signature: key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new L2pSecurityException("unable to verify signature: algorithm problems", e);
		} catch (SignatureException e) {
			throw new L2pSecurityException("unable to verify signature: signature problems", e);
		} catch (SerializationException e) {
			throw new L2pSecurityException("unable to verify signature: serialization problems", e);
		}
	}

	/**
	 * close this message (envelope)
	 */
	public void close() {
		content = null;
		sender = null;
		recipient = null;
	}

	/**
	 * @return true, if the content of this message is accessible
	 */
	public boolean isOpen() {
		return (content != null);
	}

	/**
	 * @return unix timestamp of message creation
	 */
	public long getTimestamp() {
		return timestampMs;
	}

	/**
	 * @return timestamp of message creation as Date object
	 */
	public Date getTimestampDate() {
		return new Date(timestampMs);
	}

	/**
	 * @return the date of timeout for this message
	 */
	public Date getTimeoutDate() {
		return new Date(timestampMs + validMs);
	}

	/**
	 * 
	 * @return timestamp of the timeout for this message
	 */
	public long getTimeoutTs() {
		return timestampMs + validMs;
	}

	/**
	 * @return true, if this message is expired
	 */
	public boolean isExpired() {
		return timestampMs + validMs - new Date().getTime() < 0;
	}

	/**
	 * from XmlAble return a XML representation of this instance
	 * 
	 */
	@Override
	public String toXmlString() {
		String response = "";
		if (responseToId != null) {
			response = " responseTo=\"" + responseToId + "\"";
		}

		String sending = "";
		if (sendingNodeId != null) {
			if (sendingNodeId instanceof Long || sendingNodeId instanceof NodeHandle) {
				try {
					sending = "\t<sendingNode encoding=\"base64\">" + SerializeTools.serializeToBase64(sendingNodeId)
							+ "</sendingNode>\n";
				} catch (SerializationException e) {
				}
			}
		}

		String receiver;
		String contentKey = "";
		String encryption = "";
		if (!isTopic()) {
			receiver = "to=\"" + recipientId + "\"";
			encryption = " encryption=\"" + CryptoTools.getSymmetricAlgorithm() + "\"";
			contentKey = "\t<contentKey encryption=\"" + CryptoTools.getAsymmetricAlgorithm()
					+ "\" encoding=\"base64\">" + Base64.getEncoder().encodeToString(baContentKey) + "</contentKey>\n";
		} else {
			receiver = "topic=\"" + topicId + "\"";
		}

		return "<las2peer:message" + " id=\"" + id + "\"" + response + " from=\"" + senderId + "\" " + receiver
				+ " generated=\"" + timestampMs + "\" timeout=\"" + validMs + "\">\n" + sending + "\t<content"
				+ encryption + " encoding=\"base64\">" + Base64.getEncoder().encodeToString(baEncryptedContent)
				+ "</content>\n" + contentKey + "\t<signature encoding=\"base64\" method=\""
				+ CryptoTools.getSignatureMethod() + "\">" + Base64.getEncoder().encodeToString(baSignature)
				+ "</signature>\n" + "</las2peer:message>\n";
	}

	/**
	 * for XmlAble: set the state of this object from the given xml document
	 * 
	 * @param xml
	 * @throws MalformedXMLException
	 */
	public void setStateFromXml(String xml) throws MalformedXMLException {
		try {
			Element root = XmlTools.getRootElement(xml, "las2peer:message");

			Element sending = XmlTools.getOptionalElement(root, "sendingNode");
			if (sending != null) {
				if (!"base64".equals(sending.getAttribute("encoding"))) {
					throw new MalformedXMLException("base64 encoding of sending node expected!");
				}
				sendingNodeId = SerializeTools.deserializeBase64(sending.getTextContent());
			}

			Element content = XmlTools.getSingularElement(root, "content");
			Element contentKey = XmlTools.getOptionalElement(root, "contentKey");
			Element signature = XmlTools.getSingularElement(root, "signature");

			if (!root.hasAttribute("from")) {
				throw new MalformedXMLException("needed from attribute missing!");
			}
			if (!root.hasAttribute("to") && !root.hasAttribute("topic")) {
				throw new MalformedXMLException("needed to or topic attribute missing!");
			}
			if (!root.hasAttribute("topic") && contentKey == null) {
				throw new MalformedXMLException("content key missing!");
			}
			if (!root.hasAttribute("generated")) {
				throw new MalformedXMLException("needed generated attribute missing!");
			}
			if (!root.hasAttribute("timeout")) {
				throw new MalformedXMLException("needed timeout attribute missing!");
			}
			if (!root.hasAttribute("id")) {
				throw new MalformedXMLException("needed id attribute missing!");
			}

			if (!content.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			if (contentKey != null && !contentKey.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			if (!signature.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException("base64 encoding expected");
			}

			senderId = Long.parseLong(root.getAttribute("from"));
			if (root.hasAttribute("to")) {
				recipientId = Long.parseLong(root.getAttribute("to"));
			}
			if (root.hasAttribute("topic")) {
				topicId = Long.parseLong(root.getAttribute("topic"));
			}
			// sender = AgentStorage.getAgent( Long.parseLong(root.getAttribute ( "from")));
			// recipient = AgentStorage.getAgent( Long.parseLong(root.getAttribute ( "to")));

			baEncryptedContent = Base64.getDecoder().decode(content.getTextContent());
			baSignature = Base64.getDecoder().decode(signature.getTextContent());
			if (contentKey != null) {
				baContentKey = Base64.getDecoder().decode(contentKey.getTextContent());
			}

			timestampMs = Long.parseLong(root.getAttribute("generated"));
			validMs = Long.parseLong(root.getAttribute("timeout"));
			id = Long.parseLong(root.getAttribute("id"));

			if (root.hasAttribute("responseTo")) {
				responseToId = Long.parseLong(root.getAttribute("responseTo"));
			}
		} catch (NumberFormatException e) {
			throw new MalformedXMLException("to or from attribute is not a long!", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("deserialization problems (sending node id)", e);
		}
	}

	/**
	 * set the if of the node sending this message The NodeHandle-variant is for Pastry based networks.
	 * 
	 * @param handle
	 */
	public void setSendingNodeId(NodeHandle handle) {
		sendingNodeId = handle;
	}

	/**
	 * set the id of the node sending this message The long-variant is to use in case of a LocalNode network.
	 * 
	 * @param id
	 */
	public void setSendingNodeId(Long id) {
		sendingNodeId = id;
	}

	/**
	 * set the id of the recipient (used by the node when receiving messages from topics)
	 * 
	 * @param id
	 */
	public void setRecipientId(Long id) {
		recipientId = id;
	}

	/**
	 * set the id of the node sending this message
	 * 
	 * @param id
	 */
	public void setSendingNodeId(Object id) {
		if (id instanceof NodeHandle) {
			setSendingNodeId((NodeHandle) id);
		} else if (id instanceof Long) {
			setSendingNodeId((Long) id);
		} else {
			throw new IllegalArgumentException("Illegal Node Id class " + id.getClass().getName());
		}
	}

	/**
	 * get the id of the sending node The type depends on on the Node implementation (Long for
	 * {@link i5.las2peer.p2p.LocalNode} and NodeHandle for {@link i5.las2peer.p2p.PastryNodeImpl}
	 * 
	 * @return id of the sending las2peer node
	 */
	public Serializable getSendingNodeId() {
		return sendingNodeId;
	}

	/**
	 * factory: create a message from an XML document
	 * 
	 * @param xml
	 * @return a message generated from the given XML document
	 * @throws MalformedXMLException
	 */
	public static Message createFromXml(String xml) throws MalformedXMLException {
		Message result = new Message();
		result.setStateFromXml(xml);
		return result;
	}

	@Override
	public Message clone() throws CloneNotSupportedException {
		return (Message) super.clone();
	}

}
