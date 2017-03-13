package i5.las2peer.security;

import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.GroupAgent;
import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlAble;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.crypto.SecretKey;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An agent representing a group of other agents.
 * 
 * The storage of the group information is stored encrypted in a similar manner to
 * {@link i5.las2peer.persistency.EnvelopeVersion}:
 * 
 * The (symmetric) key to unlock the private key of the group is encrypted asymmetrically for each entitled agent (i.e.
 * <i>member</i> of the group).
 * 
 */
public class GroupAgentImpl extends AgentImpl implements GroupAgent {

	private SecretKey symmetricGroupKey = null;
	private AgentImpl openedBy = null;
	private String name;
	private Serializable userData;

	/**
	 * hashtable storing the encrypted versions of the group secret key for each member
	 */
	private Hashtable<String, byte[]> htEncryptedKeyVersions = new Hashtable<>();

	private Map<String, AgentImpl> membersToAdd = new HashMap<>();
	private Map<String, AgentImpl> membersToRemove = new HashMap<>();

	@SuppressWarnings("unchecked")
	protected GroupAgentImpl(PublicKey pubKey, byte[] encryptedPrivate, Hashtable<String, byte[]> htEncryptedKeys)
			throws L2pSecurityException {
		super(pubKey, encryptedPrivate);

		htEncryptedKeyVersions = (Hashtable<String, byte[]>) htEncryptedKeys.clone();
	}

	/**
	 * constructor for the {@link #createGroupAgent} factory simply necessary, since the secret key has to be stated for
	 * the constructor of the superclass
	 * 
	 * @param keys
	 * @param secret
	 * @param members
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	protected GroupAgentImpl(KeyPair keys, SecretKey secret, Agent[] members) throws L2pSecurityException,
			CryptoException, SerializationException {
		super(keys, secret);

		symmetricGroupKey = secret;
		for (Agent a : members) {
			addMember((AgentImpl)a, false);
		}

		lockPrivateKey();
	}

	/**
	 * unlock the private key of this group for the given agent (which is hopefully a member)
	 * 
	 * @param agent
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws CryptoException
	 */
	public void unlockPrivateKey(AgentImpl agent) throws L2pSecurityException, SerializationException, CryptoException {
		decryptSecretKey(agent);
		openedBy = agent;
		super.unlockPrivateKey(symmetricGroupKey);
	}

	/**
	 * unlock the GroupAgent using transitive memberships
	 * 
	 * @param agentStorage to load the agents from
	 * @param agent
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws CryptoException
	 */
	// TODO API remove
	public void unlockPrivateKeyRecursive(AgentImpl agent, AgentStorage agentStorage) throws L2pSecurityException,
			SerializationException, CryptoException {
		if (isMember(agent)) {
			unlockPrivateKey(agent);
			return;
		} else {
			for (String memberId : htEncryptedKeyVersions.keySet()) {
				try {
					AgentImpl member = agentStorage.getAgent(memberId);
					if (member instanceof GroupAgentImpl) {
						((GroupAgentImpl) member).unlockPrivateKeyRecursive(agent, agentStorage);
						unlockPrivateKey(member);
						return;
					}
				} catch (AgentException | L2pSecurityException | SerializationException | CryptoException e) {
				}
			}
		}

		throw new L2pSecurityException("The given agent has no access to this group!");
	}

	/**
	 * decrypt the secret key of this group for the given agent (which is hopefully a member)
	 * 
	 * @param agent
	 * @throws SerializationException
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	private void decryptSecretKey(AgentImpl agent) throws SerializationException, CryptoException, L2pSecurityException {
		byte[] crypted = htEncryptedKeyVersions.get(agent.getIdentifier());

		if (crypted == null) {
			throw new L2pSecurityException("the given agent is not listed as a group member!");
		}

		symmetricGroupKey = agent.decryptSymmetricKey(crypted);
	}

	/**
	 * add a member to this group
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public void addMember(AgentImpl a) throws L2pSecurityException, CryptoException, SerializationException {
		addMember(a, true);
	}

	/**
	 * private version of adding members, mainly just for the constructor to add members without unlocking the private
	 * key of the group
	 * 
	 * @param a
	 * @param securityCheck
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws CryptoException
	 */
	private final void addMember(AgentImpl a, boolean securityCheck) throws L2pSecurityException, CryptoException,
			SerializationException {
		if (securityCheck && isLocked()) {
			throw new L2pSecurityException("you have to unlock this group first!");
		}

		byte[] cryptedSecret = CryptoTools.encryptAsymmetric(symmetricGroupKey, a.getPublicKey());
		htEncryptedKeyVersions.put(a.getIdentifier(), cryptedSecret);
	}

	/**
	 * check, if the given agent is member of this group
	 * 
	 * @param a
	 * @return true, if the given agent is a member of this group
	 */
	public boolean isMember(AgentImpl a) {
		return isMember(a.getIdentifier());
	}

	/**
	 * check, if the given agent (id) is member of this group
	 * 
	 * @param id
	 * @return true, if the given agent is a member if this group
	 */
	public boolean isMember(String id) {
		return hasMember(id);
	}

	/**
	 * how many members does this group have
	 * 
	 * @return the number of group members
	 */
	@Override
	public int getSize() {
		return htEncryptedKeyVersions.size() + membersToAdd.size() - membersToRemove.size();
	}

	/**
	 * get an array with the ids of all direct group members without recursion
	 * 
	 * @return an array with the ids of all direct member agents
	 */
	@Override
	public String[] getMemberList() {
		ArrayList<String> elements= new ArrayList<>();
		elements.addAll(htEncryptedKeyVersions.keySet());
		elements.removeAll(membersToRemove.keySet());
		elements.addAll(membersToAdd.keySet());
		return elements.toArray(new String[0]);
	}

	/**
	 * returns the Agent by whom the private Key of this Group has been unlocked
	 * 
	 * @return the agent, who opened the private key of the group
	 */
	public AgentImpl getOpeningAgent() {
		return openedBy;
	}

	/**
	 * remove a member from this group
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 */
	public void removeMember(AgentImpl a) throws L2pSecurityException {
		removeMember(a.getIdentifier());
	}

	/**
	 * remove a member from this group
	 * 
	 * @param id
	 * @throws L2pSecurityException
	 */
	public void removeMember(String id) throws L2pSecurityException {
		if (isLocked()) {
			throw new L2pSecurityException("You have to unlock this agent first!");
		}

		htEncryptedKeyVersions.remove(id);
	}

	@Override
	public void lockPrivateKey() {
		super.lockPrivateKey();
		this.symmetricGroupKey = null;
		openedBy = null;
	}

	@Override
	public String toXmlString() {
		try {
			String keyList = "";

			for (String id : htEncryptedKeyVersions.keySet()) {
				keyList += "\t\t<keyentry forAgent=\"" + id + "\" encoding=\"base64\">"
						+ Base64.getEncoder().encodeToString(htEncryptedKeyVersions.get(id)) + "</keyentry>\n";
			}

			StringBuffer result = new StringBuffer("<las2peer:agent type=\"group\">\n" + "\t<id>" + getIdentifier()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">"
					+ SerializeTools.serializeToBase64(getPublicKey()) + "</publickey>\n"
					+ "\t<privatekey encoding=\"base64\" encrypted=\"" + CryptoTools.getSymmetricAlgorithm() + "\">"
					+ getEncodedPrivate() + "</privatekey>\n" + "\t<unlockKeys method=\""
					+ CryptoTools.getAsymmetricAlgorithm() + "\">\n" + keyList + "\t</unlockKeys>\n");

			if (name != null) {
				result.append("<groupname>" + name + "</groupname>");
			}
			if (userData != null) {
				result.append("<userdata>" + SerializeTools.serializeToBase64(userData) + "</userdata>");
			}

			result.append("</las2peer:agent>\n");

			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException("Serialization problems with keys");
		}
	}

	/**
	 * factory - create an instance of GroupAgent from its XML representation
	 * 
	 * @param xml
	 * @return a group agent
	 * @throws MalformedXMLException
	 */
	public static GroupAgentImpl createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	/**
	 * factory - create an instance of GroupAgent based on a XML node
	 * 
	 * @param root
	 * @return a group agent
	 * @throws MalformedXMLException
	 */
	public static GroupAgentImpl createFromXml(Element root) throws MalformedXMLException {
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
			byte[] encPrivate = Base64.getDecoder().decode(privKey.getTextContent());
			// read member keys from XML
			Element encryptedKeys = XmlTools.getSingularElement(root, "unlockKeys");
			if (!encryptedKeys.getAttribute("method").equals(CryptoTools.getAsymmetricAlgorithm())) {
				throw new MalformedXMLException("base64 encoding expected");
			}
			Hashtable<String, byte[]> htMemberKeys = new Hashtable<>();
			NodeList enGroups = encryptedKeys.getElementsByTagName("keyentry");
			for (int n = 0; n < enGroups.getLength(); n++) {
				org.w3c.dom.Node node = enGroups.item(n);
				short nodeType = node.getNodeType();
				if (nodeType != org.w3c.dom.Node.ELEMENT_NODE) {
					throw new MalformedXMLException("Node type (" + nodeType + ") is not type element ("
							+ org.w3c.dom.Node.ELEMENT_NODE + ")");
				}
				Element elKey = (Element) node;
				if (!elKey.hasAttribute("forAgent")) {
					throw new MalformedXMLException("forAgent attribute expected");
				}
				if (!elKey.getAttribute("encoding").equals("base64")) {
					throw new MalformedXMLException("base64 encoding expected");
				}

				String agentId = elKey.getAttribute("forAgent");
				byte[] content = Base64.getDecoder().decode(elKey.getTextContent());
				htMemberKeys.put(agentId, content);
			}
			GroupAgentImpl result = new GroupAgentImpl(publicKey, encPrivate, htMemberKeys);

			// read and set optional fields
			Element groupname = XmlTools.getOptionalElement(root, "groupname");
			if (groupname != null) {
				result.name = groupname.getTextContent();
			}
			Element userdata = XmlTools.getOptionalElement(root, "userdata");
			if (userdata != null) {
				result.userData = SerializeTools.deserializeBase64(userdata.getTextContent());
			}

			return result;
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e);
		} catch (L2pSecurityException e) {
			throw new MalformedXMLException("Security Problems creating an agent from the xml string", e);
		}
	}

	/**
	 * create a new group agent instance
	 * 
	 * @param members
	 * @return a group agent
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static GroupAgentImpl createGroupAgent(Agent[] members) throws L2pSecurityException, CryptoException,
			SerializationException {
		return new GroupAgentImpl(CryptoTools.generateKeyPair(), CryptoTools.generateSymmetricKey(), members);
	}

	@Override
	public void receiveMessage(Message message, AgentContext context) throws MessageException {
		// extract content from message
		Object content = null;
		try {
			message.open(this, getRunningAtNode());
			content = message.getContent();
		} catch (AgentException e1) {
			getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e1.getMessage());
		} catch (L2pSecurityException e2) {
			getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e2.getMessage());
		}
		if (content == null) {
			getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR,
					"The message content is null. Dropping message!");
			return;
		}
		Serializable contentSerializable = null;
		XmlAble contentXmlAble = null;
		if (content instanceof Serializable) {
			contentSerializable = (Serializable) content;
		} else if (content instanceof XmlAble) {
			contentXmlAble = (XmlAble) content;
		} else {
			throw new MessageException("The content of the received message is neither Serializable nor XmlAble but "
					+ content.getClass());
		}
		// send message content to each member of this group
		for (String memberId : getMemberList()) {
			AgentImpl member = null;
			try {
				member = getRunningAtNode().getAgent(memberId);
			} catch (AgentException e1) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e1.getMessage());
			}
			if (member == null) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR,
						"No agent for group member " + memberId + " found! Skipping member.");
				continue;
			}
			try {
				Message msg = null;
				if (contentSerializable != null) {
					msg = new Message(this, member, contentSerializable);
				} else if (contentXmlAble != null) {
					msg = new Message(this, member, contentXmlAble);
				} else {
					getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR,
							"The message content is null. Dropping message!");
					return;
				}
				getRunningAtNode().sendMessage(msg, null);
			} catch (EncodingFailedException e) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e.getMessage());
			} catch (L2pSecurityException e) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e.getMessage());
			} catch (SerializationException e) {
				getRunningAtNode().observerNotice(MonitoringEvent.SERVICE_ERROR, e.getMessage());
			}
		}
	}

	@Override
	public void notifyUnregister() {
		// do nothing
	}

	/**
	 * Sets a name for this group(-agent)
	 * 
	 * @param groupname A name to be used for this group. This is no identifier! May have duplicates.
	 * @throws L2pSecurityException When the user agent is still locked.
	 */
	public void setName(String groupname) throws L2pSecurityException {
		if (this.isLocked()) {
			throw new L2pSecurityException("unlock needed first!");
		}
		this.name = groupname;
	}

	/**
	 * Gets the name for this group.
	 * 
	 * @return Returns the group name or {@code null} if no name was assigned.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Attaches the given object directly to this agent. The user data represent a field of this user agent and should
	 * be used with small values (&lt; 1MB) only. Larger byte amounts could handicap the agent handling inside the
	 * network.
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

	/**
	 * get the user data assigned to this agent
	 * 
	 * @return Returns the user data object
	 */
	public Serializable getUserData() {
		return this.userData;
	}

	@Override
	public void addMember(Agent agent) {
		if (!htEncryptedKeyVersions.containsKey(agent.getIdentifier())) {
			membersToAdd.put(agent.getIdentifier(), (AgentImpl)agent);
		}
		membersToRemove.remove(agent.getIdentifier());

	}

	@Override
	public void revokeMember(Agent agent) {
		if (htEncryptedKeyVersions.containsKey(agent.getIdentifier())) {
			membersToRemove.put(agent.getIdentifier(), (AgentImpl)agent);
		}
		else if (membersToAdd.containsKey((AgentImpl)agent)) {
			membersToAdd.remove(agent.getIdentifier());
		}
	}

	@Override
	public boolean hasMember(Agent agent) {
		return hasMember(agent.getIdentifier());
	}
	
	@Override
	public boolean hasMember(String agentId) {
		return (htEncryptedKeyVersions.get(agentId) != null || membersToAdd.containsKey(agentId))
				&& !membersToRemove.containsKey(agentId);
	}

	@Override
	public void unlock(Agent agent) throws AgentAccessDeniedException, AgentOperationFailedException {
		try {
			unlockPrivateKey((AgentImpl) agent);
		} catch (L2pSecurityException | CryptoException e) {
			throw new AgentAccessDeniedException("Permission denied", e);
		} catch (SerializationException e) {
			throw new AgentOperationFailedException("Agent corrupted", e);
		}
	}

	public void apply() throws AgentAccessDeniedException, AgentOperationFailedException {
		try {
			for (AgentImpl agent : membersToRemove.values()) {
				removeMember(agent);
			}
			
			for (AgentImpl agent : membersToAdd.values()) {
				addMember(agent);
			}
		} catch (L2pSecurityException e) {
			throw new AgentAccessDeniedException("Agent is locked!");
		} catch (CryptoException | SerializationException e) {
			throw new AgentOperationFailedException("Agent corrupted!", e);
		}
	}

}
