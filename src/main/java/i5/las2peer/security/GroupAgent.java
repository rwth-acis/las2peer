package i5.las2peer.security;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Hashtable;

import javax.crypto.SecretKey;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.XmlTools;

/**
 * An agent representing a group of other agents.
 * 
 * The storage of the group information is stored encrypted in a similar manner to
 * {@link i5.las2peer.persistency.Envelope}:
 * 
 * The (symmetric) key to unlock the private key of the group is encrypted asymmetrically for each entitled agent (i.e.
 * <i>member</i> of the group).
 * 
 */
public class GroupAgent extends Agent {

	private SecretKey symmetricGroupKey = null;
	private Agent openedBy = null;
	private String name;
	private Serializable userData;

	/**
	 * hashtable storing the encrypted versions of the group secret key for each member
	 */
	private Hashtable<String, byte[]> htEncryptedKeyVersions = new Hashtable<>();

	@SuppressWarnings("unchecked")
	protected GroupAgent(PublicKey pubKey, byte[] encryptedPrivate, Hashtable<String, byte[]> htEncryptedKeys)
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
	protected GroupAgent(KeyPair keys, SecretKey secret, Agent[] members)
			throws L2pSecurityException, CryptoException, SerializationException {
		super(keys, secret);

		symmetricGroupKey = secret;
		for (Agent a : members) {
			addMember(a, false);
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
	public void unlockPrivateKey(Agent agent) throws L2pSecurityException, SerializationException, CryptoException {
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
	public void unlockPrivateKeyRecursive(Agent agent, AgentStorage agentStorage)
			throws L2pSecurityException, SerializationException, CryptoException {
		if (isMember(agent)) {
			unlockPrivateKey(agent);
			return;
		} else {
			for (String memberId : htEncryptedKeyVersions.keySet()) {
				try {
					Agent member = agentStorage.getAgent(memberId);
					if (member instanceof GroupAgent) {
						((GroupAgent) member).unlockPrivateKeyRecursive(agent, agentStorage);
						unlockPrivateKey(member);
						return;
					}
				} catch (AgentException | L2pSecurityException | SerializationException | CryptoException e) {
					// XXX logging
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
	private void decryptSecretKey(Agent agent) throws SerializationException, CryptoException, L2pSecurityException {
		byte[] crypted = htEncryptedKeyVersions.get(agent.getSafeId());

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
	public void addMember(Agent a) throws L2pSecurityException, CryptoException, SerializationException {
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
	private final void addMember(Agent a, boolean securityCheck)
			throws L2pSecurityException, CryptoException, SerializationException {
		if (securityCheck && isLocked()) {
			throw new L2pSecurityException("you have to unlock this group first!");
		}

		byte[] cryptedSecret = CryptoTools.encryptAsymmetric(symmetricGroupKey, a.getPublicKey());
		htEncryptedKeyVersions.put(a.getSafeId(), cryptedSecret);
	}

	/**
	 * check, if the given agent is member of this group
	 * 
	 * @param a
	 * @return true, if the given agent is a member of this group
	 */
	public boolean isMember(Agent a) {
		return isMember(a.getSafeId());
	}

	/**
	 * check, if the given agent is member of this group or any sub group
	 * 
	 * @param a
	 * @return true, if the given agent is a member of this group
	 */
	public boolean isMemberRecursive(Agent a) {
		return isMemberRecursive(a.getSafeId());
	}

	/**
	 * check, if the given agent (id) is member of this group
	 * 
	 * @param id
	 * @return true, if the given agent is a member if this group
	 */
	public boolean isMember(String id) {
		// TODO only for opened groups?
		return (htEncryptedKeyVersions.get(id) != null);
	}

	/**
	 * check, if the given agent (id) is member of this group
	 * 
	 * @param id
	 * @return true, if the given agent is a member if this group
	 */
	public boolean isMemberRecursive(String id) {
		if (isMember(id) == true) {
			return true;
		}
		for (String memberId : htEncryptedKeyVersions.keySet()) {
			try {
				Agent agent = AgentContext.getCurrent().getAgent(memberId);
				if (agent instanceof GroupAgent) {
					GroupAgent group = (GroupAgent) agent;
					if (group.isMemberRecursive(id) == true) {
						return true;
					}
				}
			} catch (AgentException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, "Can't get agent for id " + memberId);
			}
		}
		return false;
	}

	/**
	 * how many members does this group have
	 * 
	 * @return the number of group members
	 */
	public int getSize() {
		return htEncryptedKeyVersions.size();
	}

	/**
	 * get the number of member this group and all sub groups have
	 * 
	 * @return the total number of group members
	 */
	public int getSizeRecursive() {
		int result = 0;
		for (String memberId : htEncryptedKeyVersions.keySet()) {
			try {
				Agent agent = AgentContext.getCurrent().getAgent(memberId);
				if (agent instanceof GroupAgent) {
					GroupAgent group = (GroupAgent) agent;
					// the group agent itself is not counted
					result += group.getSizeRecursive();
				} else {
					result++;
				}
			} catch (AgentException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, "Can't get agent for id " + memberId);
			}
		}
		return result;
	}

	/**
	 * get an array with the ids of all direct group members without recursion
	 * 
	 * @return an array with the ids of all direct member agents
	 */
	public String[] getMemberList() {
		return htEncryptedKeyVersions.keySet().toArray(new String[0]);
	}

	/**
	 * get an array with the ids of all group members including recursion
	 * 
	 * @return an array with the ids of all member agents
	 */
	public String[] getMemberListRecursive() {
		ArrayList<String> result = new ArrayList<String>(htEncryptedKeyVersions.keySet());
		for (String id : result) {
			try {
				Agent agent = AgentContext.getCurrent().getAgent(id);
				if (agent instanceof GroupAgent) {
					GroupAgent group = (GroupAgent) agent;
					Collections.addAll(result, group.getMemberListRecursive());
				}
			} catch (AgentException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, "Can't get agent for id " + id);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * returns the Agent by whom the private Key of this Group has been unlocked
	 * 
	 * @return the agent, who opened the private key of the group
	 */
	public Agent getOpeningAgent() {
		return openedBy;
	}

	/**
	 * remove a member from this group
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 */
	public void removeMember(Agent a) throws L2pSecurityException {
		removeMember(a.getSafeId());
	}

	/**
	 * remove a member from this group and recursivly from all sub groups
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 */
	public void removeMemberRecursive(Agent a) throws L2pSecurityException {
		removeMemberRecursive(a.getSafeId());
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

	public void removeMemberRecursive(String id) throws L2pSecurityException {
		if (isLocked()) {
			throw new L2pSecurityException("You have to unlock this agent first!");
		}

		htEncryptedKeyVersions.remove(id);
		for (String memberId : htEncryptedKeyVersions.keySet()) {
			try {
				Agent agent = AgentContext.getCurrent().getAgent(memberId);
				if (agent instanceof GroupAgent) {
					GroupAgent group = (GroupAgent) agent;
					group.removeMemberRecursive(id);
				}
			} catch (AgentException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, "Can't get agent for id " + memberId);
			}
		}
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

			StringBuffer result = new StringBuffer("<las2peer:agent type=\"group\">\n" + "\t<id>" + getSafeId()
					+ "</id>\n" + "\t<publickey encoding=\"base64\">" + SerializeTools.serializeToBase64(getPublicKey())
					+ "</publickey>\n" + "\t<privatekey encoding=\"base64\" encrypted=\""
					+ CryptoTools.getSymmetricAlgorithm() + "\">" + getEncodedPrivate() + "</privatekey>\n"
					+ "\t<unlockKeys method=\"" + CryptoTools.getAsymmetricAlgorithm() + "\">\n" + keyList
					+ "\t</unlockKeys>\n");

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
	public static GroupAgent createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	/**
	 * factory - create an instance of GroupAgent based on a XML node
	 * 
	 * @param root
	 * @return a group agent
	 * @throws MalformedXMLException
	 */
	public static GroupAgent createFromXml(Element root) throws MalformedXMLException {
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
					throw new MalformedXMLException(
							"Node type (" + nodeType + ") is not type element (" + org.w3c.dom.Node.ELEMENT_NODE + ")");
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
			GroupAgent result = new GroupAgent(publicKey, encPrivate, htMemberKeys);

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
	public static GroupAgent createGroupAgent(Agent[] members)
			throws L2pSecurityException, CryptoException, SerializationException {
		return new GroupAgent(CryptoTools.generateKeyPair(), CryptoTools.generateSymmetricKey(), members);
	}

	@Override
	public void receiveMessage(Message message, AgentContext context) throws MessageException {
		// extract content from message
		Object content = null;
		try {
			message.open(this, getRunningAtNode());
			content = message.getContent();
		} catch (AgentException e1) {
			L2pLogger.logEvent(Event.SERVICE_ERROR, e1.getMessage());
		} catch (L2pSecurityException e2) {
			L2pLogger.logEvent(Event.SERVICE_ERROR, e2.getMessage());
		}
		if (content == null) {
			L2pLogger.logEvent(Event.SERVICE_ERROR, "The message content is null. Dropping message!");
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
			Agent member = null;
			try {
				member = getRunningAtNode().getAgent(memberId);
			} catch (AgentException e1) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, e1.getMessage());
			}
			if (member == null) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, "No agent for group member " + memberId + "! Skipping member.");
				continue;
			}
			try {
				Message msg = null;
				if (contentSerializable != null) {
					msg = new Message(this, member, contentSerializable);
				} else if (contentXmlAble != null) {
					msg = new Message(this, member, contentXmlAble);
				} else {
					L2pLogger.logEvent(Event.SERVICE_ERROR, "The message content is null. Dropping message!");
					return;
				}
				getRunningAtNode().sendMessage(msg, null);
			} catch (EncodingFailedException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, e.getMessage());
			} catch (L2pSecurityException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, e.getMessage());
			} catch (SerializationException e) {
				L2pLogger.logEvent(Event.SERVICE_ERROR, e.getMessage());
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

}
