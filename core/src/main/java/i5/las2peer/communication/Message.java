package i5.las2peer.communication;

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

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AgentStorage;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlAble;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import rice.p2p.commonapi.NodeHandle;

/**
 * Base class for sending messages between {@link AgentImpl}s.
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
	private AgentImpl sender = null;

	/**
	 * id of the sending agent
	 */
	private String senderId;

	/**
	 * recipient of the message
	 */
	private AgentImpl recipient = null;

	/**
	 * id of the receiving agent
	 */
	private String recipientId = null;

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
	 * raw decrypted content of the message
	 */
	private byte[] baDecryptedContent;

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
	 * @param from A sending agent
	 * @param to A receiving agent
	 * @param data Arbitrary data to wrap in the message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 * 
	 */
	public Message(AgentImpl from, AgentImpl to, Serializable data)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		this(from, to, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message
	 * 
	 * @param from A sending agent
	 * @param to A receiving agent
	 * @param data Arbitrary data to wrap in the message
	 * @param timeOutMs timeout for the validity of the new message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(AgentImpl from, AgentImpl to, Serializable data, long timeOutMs)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		if (from == null || to == null) {
			throw new IllegalArgumentException("null not allowed as sender or recipient!");
		}
		sender = from;
		senderId = from.getIdentifier();
		recipient = to;
		recipientId = to.getIdentifier();
		content = data;

		timestampMs = new Date().getTime();
		validMs = timeOutMs;

		id = new Random().nextLong();

		encryptContent();

		try {
			signContent();
		} catch (AgentLockedException e) {
			throw new InternalSecurityException("Agent locked", e);
		}

		close();
	}

	/**
	 * create a new message with default timeout
	 * 
	 * @param from A sending agent
	 * @param to A receiving agent
	 * @param data Arbitrary data to wrap in the message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(AgentImpl from, AgentImpl to, XmlAble data)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		this(from, to, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message
	 * 
	 * @param from A sending agent
	 * @param to A receiving agent
	 * @param data Arbitrary data to wrap in the message
	 * @param timeoutMs timeout for the validity of the new message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(AgentImpl from, AgentImpl to, XmlAble data, long timeoutMs)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		sender = from;
		senderId = from.getIdentifier();
		recipient = to;
		recipientId = to.getIdentifier();
		content = data;
		validMs = timeoutMs;

		finalizeConstructor();
	}

	/**
	 * create a new message to a topic with default timeout
	 * 
	 * @param from A sending agent
	 * @param topic A target topic
	 * @param data Arbitrary data to wrap in the message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(AgentImpl from, long topic, Serializable data)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		this(from, topic, data, DEFAULT_TIMEOUT);
	}

	/**
	 * create a new message to all agents listening on the given topic
	 * 
	 * @param from A sending agent
	 * @param topic A target topic
	 * @param data Arbitrary data to wrap in the message
	 * @param timeoutMs A timeout to send message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(AgentImpl from, long topic, Serializable data, long timeoutMs)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		if (from == null) {
			throw new IllegalArgumentException("null not allowed as sender!");
		}

		sender = from;
		senderId = from.getIdentifier();
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
	 * @throws InternalSecurityException
	 * @throws SerializationException
	 */
	private void finalizeConstructor()
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		timestampMs = new Date().getTime();
		id = new Random().nextLong();

		if (!isTopic() && !(recipient instanceof AnonymousAgent)) {
			encryptContent();
		} else {
			baDecryptedContent = getContentString().getBytes(StandardCharsets.UTF_8);
			baEncryptedContent = baDecryptedContent;
		}

		try {
			signContent();
		} catch (AgentLockedException e) {
			throw new InternalSecurityException("Agent locked", e);
		}

		close();
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo A message reference
	 * @param data Arbitrary data to wrap in the message
	 * @param timeoutMs timeout for the validity of the new message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(Message responseTo, XmlAble data, long timeoutMs)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
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
	 * @param responseTo A message reference
	 * @param data Arbitrary data to wrap in the message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(Message responseTo, XmlAble data)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		this(responseTo, data, DEFAULT_TIMEOUT);
	}

	/**
	 * Generate a new message in response to the given one. Sender and recipient will be derived from the given message.
	 * 
	 * @param responseTo A message reference
	 * @param data Arbitrary data to wrap in the message
	 * @param timeoutMs A timeout to send message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(Message responseTo, Serializable data, long timeoutMs)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
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
	 * @param responseTo A message reference
	 * @param data Arbitrary data to wrap in the message
	 * @throws EncodingFailedException If the content serialization fails
	 * @throws InternalSecurityException the private key of the sender is not accessible for signing
	 * @throws SerializationException If the content serialization fails
	 */
	public Message(Message responseTo, Serializable data)
			throws EncodingFailedException, InternalSecurityException, SerializationException {
		this(responseTo, data, DEFAULT_TIMEOUT);
	}

	/**
	 * get the contents of this message as base 64 encoded string
	 * 
	 * @return message contents in XML format with all important attributes and the actual content as base64 encoded
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
			attrs += " recipient=\"" + recipient.getIdentifier() + "\"";
		} else {
			attrs += " topic=\"" + topicId + "\"";
		}

		return "<las2peer:messageContent" + " id=\"" + id + "\"" + " sender=\"" + sender.getIdentifier() + "\""
				+ " class=\"" + content.getClass().getCanonicalName() + "\"" + " type=\"" + typeAttr + "\""
				+ " timestamp=\"" + timestampMs + "\"" + " timeout=\"" + validMs + "\"" + attrs + ">" + sContent
				+ "</las2peer:messageContent>";
	}

	/**
	 * encrypt the content of this message (as base64 encoded string) with asymmetric encryption
	 * 
	 * @throws EncodingFailedException If the content serialization fails
	 */
	private void encryptContent() throws EncodingFailedException {
		if (recipient == null || recipient instanceof AnonymousAgent) {
			return;
		}

		try {
			SecretKey contentKey = CryptoTools.generateSymmetricKey();
			baContentKey = CryptoTools.encryptAsymmetric(contentKey, recipient.getPublicKey());

			String contentString = getContentString();
			baDecryptedContent = contentString.getBytes(StandardCharsets.UTF_8);
			baEncryptedContent = CryptoTools.encryptSymmetric(baDecryptedContent, contentKey);
		} catch (SerializationException e) {
			throw new EncodingFailedException("serialization problems with encryption", e);
		} catch (CryptoException e) {
			throw new EncodingFailedException("unable to encrypt the secret message key", e);
		}
	}

	/**
	 * sign the contents of this message
	 * 
	 * @throws SerializationException
	 * @throws EncodingFailedException
	 * @throws AgentLockedException
	 */
	private void signContent() throws SerializationException, EncodingFailedException, AgentLockedException {
		if (sender instanceof AnonymousAgent) {
			return;
		}

		try {
			Signature sig = sender.createSignature();
			sig.update(baDecryptedContent);

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
	public AgentImpl getSender() {
		return sender;
	}

	/**
	 * get the id of the sending agent
	 * 
	 * @return id of the sending agent
	 */
	public String getSenderId() {
		return senderId;
	}

	/**
	 * get the designated recipient of this message
	 * 
	 * only works after opening or creating of the message
	 * 
	 * @return (designated) receiver
	 */
	public AgentImpl getRecipient() {
		return recipient;
	}

	/**
	 * Gets the id of the recipient agent
	 * 
	 * @return id of the receiving agent
	 */
	public String getRecipientId() {
		return recipientId;
	}

	/**
	 * Gets the id of the receiving topic
	 * 
	 * @return Returns the topic id
	 */
	public Long getTopicId() {
		return topicId;
	}

	/**
	 * Check if this message is sent to a topic
	 * 
	 * @return Returns {@code true} if this message is sent to a topic
	 */
	public boolean isTopic() {
		return topicId != null;
	}

	/**
	 * Gets the id of this message
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
	 * @throws InternalSecurityException the message (envelope) has to be opened (decrypted) first
	 */
	public Object getContent() throws InternalSecurityException {
		if (!isOpen()) {
			throw new InternalSecurityException("You have to open the envelope first!");
		}

		return content;
	}

	/**
	 * open the envelope, i.e. decrypt the content with the private key of the receiving agent
	 * 
	 * The storage has to know an unlocked version of the recipient agent! (i.e. a
	 * {@link i5.las2peer.security.AgentContext} bound to him.
	 * 
	 * @param storage An agent storage to use
	 * @throws InternalSecurityException If the private key of the receiver has to be unlocked for decryption
	 * @throws AgentException If an issue with the sender agent occurs
	 */
	public void open(AgentStorage storage) throws InternalSecurityException, AgentException {
		open(null, storage);
	}

	/**
	 * open the envelope, i.e. decrypt the content with the private key of the receiving agent
	 * 
	 * the private key has to be unlocked first!
	 * 
	 * @param unlockedRecipient An unlocked recipient
	 * @param storage An agent storage to use
	 * @throws InternalSecurityException If the private key of the receiver has to be unlocked for decryption
	 * @throws AgentException If an issue with the sender agent occurs
	 * @throws AgentNotFoundException If an issue with the sender agent occurs
	 */
	public void open(AgentImpl unlockedRecipient, AgentStorage storage)
			throws InternalSecurityException, AgentException {
		if (isOpen()) {
			return;
		}

		sender = storage.getAgent(senderId);

		if (recipientId != null) { // topic messages are not encrypted
			if (unlockedRecipient != null && unlockedRecipient.getIdentifier().equalsIgnoreCase(recipientId)) {
				recipient = unlockedRecipient;
			} else {
				recipient = storage.getAgent(recipientId);
			}

			if (recipient.isLocked()) {
				throw new InternalSecurityException("private key of recipient is locked!");
			}
		}

		try {
			if (!isTopic() && !(recipient instanceof AnonymousAgent)) {
				SecretKey contentKey = recipient.decryptSymmetricKey(baContentKey);
				baDecryptedContent = CryptoTools.decryptSymmetric(baEncryptedContent, contentKey);
			} else { // topics are not encrypted
				baDecryptedContent = baEncryptedContent;
			}

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(baDecryptedContent));
			doc.getDocumentElement().normalize();
			Element root = doc.getDocumentElement();

			if (!root.hasAttribute("sender")) {
				throw new InternalSecurityException("content block needs sender attribute!");
			}
			if (!root.hasAttribute("recipient") && !root.hasAttribute("topic")) {
				throw new InternalSecurityException("content block needs recipient or topic attribute!");
			}
			if (!root.hasAttribute("timestamp")) {
				throw new InternalSecurityException("content block needs timestamp attribute!");
			}
			if (!root.hasAttribute("timeout")) {
				throw new InternalSecurityException("content block needs timeout attribute!");
			}
			if (!root.hasAttribute("id")) {
				throw new InternalSecurityException("content block needs id attribute!");
			}

			if (!root.getAttribute("sender").equalsIgnoreCase(sender.getIdentifier())) {
				throw new InternalSecurityException("message is signed for another sender!!");
			}
			if (root.hasAttribute("recipient") && (recipient == null
					|| !root.getAttribute("recipient").equalsIgnoreCase(recipient.getIdentifier()))) {
				throw new InternalSecurityException("message is signed for another recipient!!");
			}
			if (root.hasAttribute("topic") && Long.parseLong(root.getAttribute("topic")) != (topicId)) {
				throw new InternalSecurityException("message is signed for another topic!!");
			}
			if (Long.parseLong(root.getAttribute("timestamp")) != timestampMs) {
				throw new InternalSecurityException("message is signed for another timestamp!!");
			}
			if (Long.parseLong(root.getAttribute("timeout")) != validMs) {
				throw new InternalSecurityException("message is signed for another timeout value!!");
			}
			if (Long.parseLong(root.getAttribute("id")) != id) {
				throw new InternalSecurityException("message is signed for another id!");
			}

			if ((root.hasAttribute("responseTo") || responseToId != null)
					&& !responseToId.equals(Long.parseLong(root.getAttribute("responseTo")))) {
				throw new InternalSecurityException("message is signed as response to another message!");
			}

			if (root.getAttribute("type").equals("Serializable")) {
				content = SerializeTools.deserializeBase64(root.getTextContent());
			} else {
				content = XmlAble.createFromXml(root.getFirstChild().toString(), root.getAttribute("class"));
			}
		} catch (CryptoException e) {
			throw new InternalSecurityException("Crypto-Problems: Unable to open message content", e);
		} catch (SerializationException e) {
			throw new InternalSecurityException("deserializiation problems with decryption!", e);
		} catch (ClassNotFoundException e) {
			throw new InternalSecurityException("content class missing with decryption!", e);
		} catch (MalformedXMLException | ParserConfigurationException | SAXException | IOException e) {
			throw new InternalSecurityException("xml syntax problems with decryption!", e);
		}

		// verify signature
		verifySignature();
	}

	/**
	 * verify the signature of this message the content has to be available for this
	 * 
	 * @throws InternalSecurityException If signature verification fails
	 */
	public void verifySignature() throws InternalSecurityException {
		if (sender instanceof AnonymousAgentImpl) {
			return;
		}

		Signature sig;
		try {
			sig = Signature.getInstance(CryptoTools.getSignatureMethod());
			sig.initVerify(sender.getPublicKey());
			sig.update(baDecryptedContent);

			if (!sig.verify(baSignature)) {
				throw new InternalSecurityException("Signature invalid!");
			}
		} catch (InvalidKeyException e) {
			throw new InternalSecurityException("unable to verify signature: key problems", e);
		} catch (NoSuchAlgorithmException e) {
			throw new InternalSecurityException("unable to verify signature: algorithm problems", e);
		} catch (SignatureException e) {
			throw new InternalSecurityException("unable to verify signature: signature problems", e);
		}
	}

	/**
	 * close this message (envelope)
	 */
	public void close() {
		content = null;
		sender = null;
		recipient = null;
		baDecryptedContent = null;
	}

	/**
	 * @return true, if the content of this message is accessible
	 */
	public boolean isOpen() {
		return (content != null);
	}

	/**
	 * @return the time in ms the message is valid
	 */
	public long getValidMs() {
		return validMs;
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
	 * @param xml An XML data string
	 * @throws MalformedXMLException If the XML data string is malformed
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

			senderId = root.getAttribute("from");
			if (root.hasAttribute("to")) {
				recipientId = root.getAttribute("to");
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
	 * @param handle A sending node handle
	 */
	public void setSendingNodeId(NodeHandle handle) {
		sendingNodeId = handle;
	}

	/**
	 * set the id of the node sending this message The long-variant is to use in case of a LocalNode network.
	 * 
	 * @param id A sending node id
	 */
	public void setSendingNodeId(Long id) {
		sendingNodeId = id;
	}

	/**
	 * set the id of the recipient (used by the node when receiving messages from topics)
	 * 
	 * @param id A receiver id
	 */
	public void setRecipientId(String id) {
		recipientId = id;
	}

	/**
	 * set the id of the node sending this message
	 * 
	 * @param id A sending node id object
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
	 * @param xml An XML data string
	 * @return a message generated from the given XML document
	 * @throws MalformedXMLException If the XML data string is malformed
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
