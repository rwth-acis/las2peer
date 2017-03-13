package i5.las2peer.persistency;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.security.AgentContext;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AgentStorage;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlAble;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.crypto.SecretKey;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EnvelopeVersion implements Serializable, XmlAble {
	
	// TODO API @Thomas: store signing agent + reader list


	private static final L2pLogger logger = L2pLogger.getInstance(EnvelopeVersion.class);

	/**
	 * freshly created local instance, can be considered as {@code null}
	 */
	public static final long LATEST_VERSION = -1;
	/**
	 * freshly created local instance, can be considered as {@code null}
	 */
	public static final long NULL_VERSION = 0;
	/**
	 * first version, that reaches the network
	 */
	public static final long START_VERSION = 1;
	/**
	 * The following constant is used for practical purposes to prohibit some otherwise almost infinite loops. It
	 * describes the maximum number of write cycles to an envelope (identifier). Its value may be set to any value from
	 * 1 (not useful at all!) up to Long.MAX_VALUE (some parts will take almost forever).
	 */
	public static final long MAX_UPDATE_CYCLES = 1000000000L;

	private static final long serialVersionUID = 1L;

	public static String getAgentIdentifier(String agentId) {
		return "agent-" + agentId;
	}

	private final String identifier;
	private final long version;
	private final HashMap<PublicKey, byte[]> readerKeys;
	private final HashSet<String> readerGroupIds;
	private final byte[] rawContent;

	// just for the XML factory method
	private EnvelopeVersion(String identifier, long version, HashMap<PublicKey, byte[]> readerKeys,
			HashSet<String> readerGroupIds, byte[] rawContent) {
		this.identifier = identifier;
		this.version = version;
		this.readerKeys = readerKeys;
		this.readerGroupIds = readerGroupIds;
		this.rawContent = rawContent;
	}

	/**
	 * Creates a new version of an Envelope. The envelope uses by default the start version number.
	 * 
	 * @param identifier An unique identifier for the envelope.
	 * @param content The updated content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @throws IllegalArgumentException If the given identifier is null, the version number is below the start version
	 *             number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	protected EnvelopeVersion(String identifier, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		this(identifier, START_VERSION, content, readers, new HashSet<>());
	}

	/**
	 * Creates an continuous version instance for the given Envelope. This method copies the reader list from the
	 * previous envelope instance.
	 * 
	 * @param previousVersion The previous version of the envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @throws IllegalArgumentException If the given identifier is null, the version number is below the start version
	 *             number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	protected EnvelopeVersion(EnvelopeVersion previousVersion, Serializable content) throws IllegalArgumentException,
			SerializationException, CryptoException {
		this(previousVersion.getIdentifier(), previousVersion.getVersion() + 1, content, previousVersion.readerKeys
				.keySet(), previousVersion.readerGroupIds);
	}

	/**
	 * Creates an continuous version instance for the given Envelope.
	 * 
	 * @param previousVersion The previous version of the envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @throws IllegalArgumentException If the given identifier is null, the version number is below the start version
	 *             number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	protected EnvelopeVersion(EnvelopeVersion previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		this(previousVersion.getIdentifier(), previousVersion.getVersion() + 1, content, readers, new HashSet<>());
	}

	/**
	 * Creates an envelope with the given identifier, version, content and readable by the given reader list.
	 * 
	 * @param identifier An unique identifier for the envelope.
	 * @param version The version number for this envelope.
	 * @param content The actual content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @param readerGroups A set of group agent id's with read access.
	 * @throws IllegalArgumentException If the given identifier is null, the version number is below the start version
	 *             number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	protected EnvelopeVersion(String identifier, long version, Serializable content, Collection<?> readers,
			Set<String> readerGroups) throws IllegalArgumentException, SerializationException, CryptoException {
		if (identifier == null) {
			throw new IllegalArgumentException("The identifier must not be null");
		}
		if (version < START_VERSION) {
			throw new IllegalArgumentException("Given version number is too low");
		}
		this.identifier = identifier;
		if (version > MAX_UPDATE_CYCLES) {
			throw new IllegalArgumentException("Version number (" + version + ") is too high, max is "
					+ MAX_UPDATE_CYCLES);
		}
		this.version = version;
		readerKeys = new HashMap<>();
		readerGroupIds = new HashSet<>(readerGroups);
		if (readers != null && !readers.isEmpty()) {
			// we have a non empty set of readers, lets encrypt!
			SecretKey contentKey = CryptoTools.generateSymmetricKey();
			rawContent = CryptoTools.encryptSymmetric(content, contentKey);
			for (Object reader : readers) {
				if (reader instanceof GroupAgentImpl) {
					AgentImpl agent = (AgentImpl) reader;
					readerGroupIds.add(agent.getIdentifier());
				}
				if (reader instanceof AgentImpl) {
					AgentImpl agent = (AgentImpl) reader;
					byte[] readerKey = CryptoTools.encryptAsymmetric(contentKey, agent.getPublicKey());
					readerKeys.put(agent.getPublicKey(), readerKey);
				} else if (reader instanceof PublicKey) {
					PublicKey pubkey = (PublicKey) reader;
					byte[] readerKey = CryptoTools.encryptAsymmetric(contentKey, pubkey);
					readerKeys.put(pubkey, readerKey);
				}
			}
		} else {
			// unencrypted envelope
			rawContent = SerializeTools.serialize(content);
		}
	}

	/**
	 * Gets the identifier for this envelope.
	 *
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Gets the version of this envelope.
	 *
	 * @return Returns the version number.
	 */
	public long getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return identifier + "#" + version;
	}

	public boolean isEncrypted() {
		return readerKeys != null && !readerKeys.isEmpty();
	}

	public HashMap<PublicKey, byte[]> getReaderKeys() {
		return readerKeys;
	}

	public Set<String> getReaderGroupIds() {
		// return shallow copy to avoid manipulation
		return new HashSet<>(readerGroupIds);
	}

	public Serializable getContent() throws CryptoException, L2pSecurityException, SerializationException {
		if (isEncrypted()) {
			return getContent(AgentContext.getCurrent().getMainAgent());
		} else {
			ClassLoader clsLoader = null;
			try {
				clsLoader = Context.get().getServiceClassLoader();
			} catch (IllegalStateException e) {
				logger.log(Level.FINER, "Could not get service class loader" + e.toString());
			}
			return SerializeTools.deserialize(rawContent, clsLoader);
		}
	}

	public Serializable getContent(AgentImpl reader) throws CryptoException, L2pSecurityException,
			SerializationException {
		try {
			return getContent(reader, AgentContext.getCurrent().getLocalNode());
		} catch (IllegalStateException e) {
			return getContent(reader, reader.getRunningAtNode());
		}
	}

	public Serializable getContent(AgentImpl reader, AgentStorage agentStorage) throws CryptoException,
			L2pSecurityException, SerializationException {
		byte[] decrypted = null;
		if (isEncrypted()) {
			SecretKey decryptedReaderKey = null;
			// fetch all groups
			for (String groupId : readerGroupIds) {
				try {
					AgentImpl agent = agentStorage.getAgent(groupId);

					if (agent instanceof GroupAgentImpl) {
						GroupAgentImpl group = (GroupAgentImpl) agent;
						byte[] encryptedReaderKey = readerKeys.get(group.getPublicKey());
						if (encryptedReaderKey != null) {
							// use group to decrypt content
							group.unlockPrivateKeyRecursive(reader, agentStorage);
							decryptedReaderKey = group.decryptSymmetricKey(encryptedReaderKey);
							break;
						}
					} else {
						logger.log(Level.WARNING, "Non GroupAgent listed as reader group");
					}
				} catch (AgentException | L2pSecurityException | CryptoException | SerializationException e) {
					logger.log(Level.WARNING, "Issue with envelope reader", e);
				}
			}
			if (decryptedReaderKey == null) {
				// no group matched
				byte[] encryptedReaderKey = readerKeys.get(reader.getPublicKey());
				if (encryptedReaderKey == null) {
					throw new CryptoException("Agent (" + reader.getIdentifier() + ") has no read permission");
				}
				decryptedReaderKey = reader.decryptSymmetricKey(encryptedReaderKey);
			}
			// decrypt content
			decrypted = CryptoTools.decryptSymmetric(rawContent, decryptedReaderKey);
		} else {
			decrypted = rawContent;
		}
		ClassLoader clsLoader = null;
		try {
			clsLoader = Context.get().getServiceClassLoader();
		} catch (IllegalStateException e) {
			logger.log(Level.FINER, "Could not get service class loader" + e.toString());
		}
		return SerializeTools.deserialize(decrypted, clsLoader);
	}

	/**
	 * @return a XML (string) representation of this envelope
	 * @throws SerializationException
	 */
	@Override
	public String toXmlString() throws SerializationException {
		StringBuilder result = new StringBuilder();
		result.append("<las2peer:envelope identifier=\"" + identifier + "\" version=\"" + version + "\">\n");
		result.append("\t<las2peer:content encoding=\"Base64\">")
				.append(Base64.getEncoder().encodeToString(rawContent)).append("</las2peer:content>\n");
		result.append("\t<las2peer:keys encoding=\"base64\" encryption=\"" + CryptoTools.getAsymmetricAlgorithm()
				+ "\">\n");
		for (Entry<PublicKey, byte[]> readerKey : readerKeys.entrySet()) {
			try {
				result.append("\t\t<las2peer:key public=\"" + CryptoTools.publicKeyToString(readerKey.getKey()) + "\">"
						+ Base64.getEncoder().encodeToString(readerKey.getValue()) + "</las2peer:key>\n");
			} catch (CryptoException e) {
				throw new SerializationException("Could not encode key as string", e);
			}
		}
		result.append("\t</las2peer:keys>\n");
		result.append("\t<las2peer:groups>\n");
		for (String groupId : readerGroupIds) {
			if (groupId != null) {
				result.append("\t\t<las2peer:group id=\"" + groupId + "\"/>");
			}
		}
		result.append("\t</las2peer:groups>\n");
		result.append("</las2peer:envelope>\n");
		return result.toString();
	}

	/**
	 * factory for generating an envelope from the given XML String representation
	 * 
	 * @param rootElement
	 * @return envelope created from the given XML String serialization
	 * @throws MalformedXMLException
	 */
	public static EnvelopeVersion createFromXml(Element rootElement) throws MalformedXMLException {
		if (!rootElement.hasAttribute("identifier")) {
			throw new MalformedXMLException("identifier attribute expected!");
		}
		String identifier = rootElement.getAttribute("identifier");
		if (!rootElement.hasAttribute("version")) {
			throw new MalformedXMLException("version attribute expected!");
		}
		long version = Long.parseLong(rootElement.getAttribute("version"));
		// read content from XML
		Element content = XmlTools.getSingularElement(rootElement, "las2peer:content");
		if (!content.getAttribute("encoding").equals("Base64")) {
			throw new MalformedXMLException("base 64 encoding of the content expected");
		}
		byte[] rawContent = Base64.getDecoder().decode(content.getTextContent());
		// read reader keys from XML
		Element keys = XmlTools.getSingularElement(rootElement, "las2peer:keys");
		if (!keys.getAttribute("encoding").equalsIgnoreCase("base64")) {
			throw new MalformedXMLException("base 64 encoding of the content expected - got: "
					+ keys.getAttribute("encoding"));
		}
		if (!keys.getAttribute("encryption").equalsIgnoreCase(CryptoTools.getAsymmetricAlgorithm())) {
			throw new MalformedXMLException(CryptoTools.getAsymmetricAlgorithm()
					+ " encryption of the content expected");
		}
		HashMap<PublicKey, byte[]> readerKeys = new HashMap<>();
		NodeList enKeys = keys.getChildNodes();
		for (int n = 0; n < enKeys.getLength(); n++) {
			Node node = enKeys.item(n);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				// XXX logging
				continue;
			}
			Element key = (Element) node;
			if (!key.getNodeName().equals("las2peer:key")) {
				throw new MalformedXMLException("key expected");
			}
			String strPublicKey = key.getAttribute("public");
			try {
				PublicKey publicKey = CryptoTools.stringToPublicKey(strPublicKey);
				byte[] encryptedReaderKey = Base64.getDecoder().decode(key.getFirstChild().getTextContent());
				readerKeys.put(publicKey, encryptedReaderKey);
			} catch (CryptoException e) {
				throw new MalformedXMLException("Could not convert string to public key", e);
			}

		}
		// groups
		Element groups = XmlTools.getSingularElement(rootElement, "las2peer:groups");
		HashSet<String> readerGroupIds = new HashSet<>();
		NodeList enGroups = groups.getChildNodes();
		for (int n = 0; n < enGroups.getLength(); n++) {
			Node node = enKeys.item(n);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				// XXX logging
				continue;
			}
			Element group = (Element) node;
			if (!group.getNodeName().equals("las2peer:group")) {
				throw new MalformedXMLException("group expected");
			}
			if (!group.hasAttribute("id")) {
				throw new MalformedXMLException("group id expected");
			}
			String groupId = group.getAttribute("id");
			readerGroupIds.add(groupId);
		}
		return new EnvelopeVersion(identifier, version, readerKeys, readerGroupIds, rawContent);
	}

	/**
	 * factory for generating an envelope from the given XML String representation
	 * 
	 * @param xml
	 * @return envelope created from the given XML String serialization
	 * @throws MalformedXMLException
	 */
	public static EnvelopeVersion createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:envelope"));
	}

}
