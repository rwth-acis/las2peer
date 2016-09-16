package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.crypto.SecretKey;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

public class Envelope implements Serializable, XmlAble {

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

	public static String getAgentIdentifier(long agentId) {
		return "agent-" + Long.toString(agentId);
	}

	private final String identifier;
	private final long version;
	private final HashMap<PublicKey, byte[]> readerKeys;
	private final ArrayList<Long> readerGroupIds;
	private final byte[] rawContent;

	// just for the XML factory method
	private Envelope(String identifier, long version, HashMap<PublicKey, byte[]> readerKeys,
			ArrayList<Long> readerGroupIds, byte[] rawContent) {
		this.identifier = identifier;
		this.version = version;
		this.readerKeys = readerKeys;
		this.readerGroupIds = readerGroupIds;
		this.rawContent = rawContent;
	}

	protected Envelope(String identifier, Serializable content, List<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		this(identifier, START_VERSION, content, readers);
	}

	protected Envelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		this(previousVersion, content, previousVersion.getReaderKeys().keySet());
	}

	protected Envelope(Envelope previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		this(previousVersion.getIdentifier(), previousVersion.getVersion() + 1, content, readers);
	}

	protected Envelope(String identifier, long version, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		if (identifier == null) {
			throw new IllegalArgumentException("The identifier must not be null");
		}
		if (version < START_VERSION) {
			throw new IllegalArgumentException("Given version number is too low");
		}
		this.identifier = identifier;
		if (version > MAX_UPDATE_CYCLES) {
			throw new IllegalArgumentException(
					"Version number (" + version + ") is too high, max is " + MAX_UPDATE_CYCLES);
		}
		this.version = version;
		readerKeys = new HashMap<>();
		readerGroupIds = new ArrayList<>();
		if (readers != null && !readers.isEmpty()) {
			// we have a non empty set of readers, lets encrypt!
			SecretKey contentKey = CryptoTools.generateSymmetricKey();
			rawContent = CryptoTools.encryptSymmetric(content, contentKey);
			for (Object reader : readers) {
				if (reader instanceof GroupAgent) {
					Agent agent = (Agent) reader;
					readerGroupIds.add(agent.getId());
				}
				if (reader instanceof Agent) {
					Agent agent = (Agent) reader;
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
		// XXX return a shallow copy? Would a malicious action on this list have any impacts?
		return readerKeys;
	}

	public Serializable getContent() throws CryptoException, L2pSecurityException, SerializationException {
		if (isEncrypted()) {
			return getContent(Context.getCurrent().getMainAgent());
		} else {
			ClassLoader clsLoader = null;
			try {
				clsLoader = L2pThread.getServiceClassLoader();
			} catch (IllegalStateException e) {
				// XXX logging
			}
			return SerializeTools.deserialize(rawContent, clsLoader);
		}
	}

	public Serializable getContent(Agent reader) throws CryptoException, L2pSecurityException, SerializationException {
		byte[] decrypted = null;
		if (isEncrypted()) {
			SecretKey decryptedReaderKey = null;
			// fetch all groups
			for (Long groupId : readerGroupIds) {
				try {
					Agent agent = null;
					try {
						agent = Context.getCurrent().getAgent(groupId);
					} catch (IllegalStateException e) {
						Node node = reader.getRunningAtNode();
						if (node == null) {
							throw new IllegalStateException("Neither context nor node known");
						}
						agent = reader.getRunningAtNode().getAgent(groupId);
					}
					if (agent instanceof GroupAgent) {
						GroupAgent group = (GroupAgent) agent;
						byte[] encryptedReaderKey = readerKeys.get(group.getPublicKey());
						if (encryptedReaderKey != null && group.isMember(reader)) {
							// use group to decrypt content
							group.unlockPrivateKey(reader);
							decryptedReaderKey = group.decryptSymmetricKey(encryptedReaderKey);
							break;
						}
					} else {
						// XXX error logging
					}
				} catch (AgentNotKnownException e) {
					// XXX error logging
				}
			}
			if (decryptedReaderKey == null) {
				// no group matched
				byte[] encryptedReaderKey = readerKeys.get(reader.getPublicKey());
				if (encryptedReaderKey == null) {
					throw new CryptoException("given reader has no read permission");
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
			clsLoader = L2pThread.getServiceClassLoader();
		} catch (IllegalStateException e) {
			// XXX logging
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
		result.append("\t<las2peer:content encoding=\"Base64\">").append(Base64.getEncoder().encodeToString(rawContent))
				.append("</las2peer:content>\n");
		result.append(
				"\t<las2peer:keys encoding=\"base64\" encryption=\"" + CryptoTools.getAsymmetricAlgorithm() + "\">\n");
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
		for (Long groupId : readerGroupIds) {
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
	 * @param root
	 * @return envelope created from the given XML String serialization
	 * @throws MalformedXMLException
	 */
	public static Envelope createFromXml(Element root) throws MalformedXMLException {
		try {
			if (!root.getName().equals("envelope")) {
				throw new MalformedXMLException("not an envelope");
			}
			if (!root.hasAttribute("identifier")) {
				throw new MalformedXMLException("identifier attribute expected!");
			}
			if (!root.hasAttribute("version")) {
				throw new MalformedXMLException("version attribute expected!");
			}
			String identifier = root.getAttribute("identifier");
			long version = Long.parseLong(root.getAttribute("version"));
			Element content = root.getFirstChild();
			if (!content.getName().equals("content")) {
				throw new MalformedXMLException("envelope content expected");
			}
			if (!content.getAttribute("encoding").equals("Base64")) {
				throw new MalformedXMLException("base 64 encoding of the content expected");
			}
			byte[] rawContent = Base64.getDecoder().decode(content.getFirstChild().getText());
			Element keys = root.getChild(1);
			if (!keys.getName().equals("keys")) {
				throw new MalformedXMLException("not an envelope");
			}
			if (!keys.getAttribute("encoding").equals("base64")) {
				throw new MalformedXMLException(
						"base 64 encoding of the content expected - got: " + keys.getAttribute("encoding"));
			}
			if (!keys.getAttribute("encryption").equals(CryptoTools.getAsymmetricAlgorithm())) {
				throw new MalformedXMLException(
						CryptoTools.getAsymmetricAlgorithm() + " encryption of the content expected");
			}
			// reader keys
			HashMap<PublicKey, byte[]> readerKeys = new HashMap<>();
			for (Enumeration<Element> enKeys = keys.getChildren(); enKeys.hasMoreElements();) {
				Element key = enKeys.nextElement();
				if (!key.getName().equals("key")) {
					throw new MalformedXMLException("key expected");
				}
				String strPublicKey = key.getAttribute("public");
				try {
					PublicKey publicKey = CryptoTools.stringToPublicKey(strPublicKey);
					byte[] encryptedReaderKey = Base64.getDecoder().decode(key.getFirstChild().getText());
					readerKeys.put(publicKey, encryptedReaderKey);
				} catch (CryptoException e) {
					throw new MalformedXMLException("Could not convert string to public key", e);
				}

			}
			// groups
			Element groups = root.getChild(2);
			if (!groups.getName().equals("groups")) {
				throw new MalformedXMLException("groups tag expected");
			}
			ArrayList<Long> readerGroupIds = new ArrayList<>();
			for (Enumeration<Element> enGroups = groups.getChildren(); enGroups.hasMoreElements();) {
				Element group = enGroups.nextElement();
				if (!group.getName().equals("group")) {
					throw new MalformedXMLException("group expected");
				}
				if (!group.hasAttribute("id")) {
					throw new MalformedXMLException("group id expected");
				}
				long groupId = Long.valueOf(group.getAttribute("id"));
				readerGroupIds.add(groupId);
			}
			return new Envelope(identifier, version, readerKeys, readerGroupIds, rawContent);
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("problems with parsing the XML document", e);
		}
	}

	/**
	 * factory for generating an envelope from the given XML String representation
	 * 
	 * @param xml
	 * @return envelope created from the given XML String serialization
	 * @throws MalformedXMLException
	 */
	public static Envelope createFromXml(String xml) throws MalformedXMLException {
		try {
			return createFromXml(Parser.parse(xml, false));
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("problems with parsing the xml document", e);
		}
	}

}
