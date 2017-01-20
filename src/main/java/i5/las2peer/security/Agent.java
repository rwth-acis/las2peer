package i5.las2peer.security;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.w3c.dom.Element;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.XmlTools;

/**
 * An Agent is the basic acting entity in the las2peer network. At the moment, an agent can represent a simple user, a
 * group, a service or a monitoring agent.
 * 
 */
public abstract class Agent implements XmlAble, Cloneable, MessageReceiver {

	/**
	 * encrypted private key
	 */
	private byte[] baEncrypedPrivate;

	/**
	 * public key for asymmetric encryption
	 */
	private PublicKey publicKey;

	/**
	 * private key for asymmetric encryption
	 */
	private PrivateKey privateKey = null;

	private Node runningAt = null;

	/**
	 * Creates a new agent.
	 * 
	 * @param pair
	 * @param key
	 * @throws L2pSecurityException
	 */
	protected Agent(KeyPair pair, SecretKey key) throws L2pSecurityException {
		this.publicKey = pair.getPublic();
		this.privateKey = pair.getPrivate();

		encryptPrivateKey(key);
		lockPrivateKey();
	}

	/**
	 * Creates a new agent.
	 * 
	 * @param publicKey
	 * @param encryptedPrivate
	 */
	protected Agent(PublicKey publicKey, byte[] encryptedPrivate) {
		this.publicKey = publicKey;
		this.privateKey = null;
		this.baEncrypedPrivate = encryptedPrivate.clone();
	}

	/**
	 * (Re-)Lock the private key.
	 */
	public void lockPrivateKey() {
		privateKey = null;
	}

	/**
	 * Unlocks the private key.
	 * 
	 * @param key A key that is used to unlock the agents private key.
	 * @throws L2pSecurityException If an issue with the given key occurs.
	 */
	public void unlockPrivateKey(SecretKey key) throws L2pSecurityException {
		try {
			privateKey = (PrivateKey) SerializeTools.deserialize(CryptoTools.decryptSymmetric(baEncrypedPrivate, key));
		} catch (SerializationException e) {
			throw new L2pSecurityException("unable do deserialize key", e);
		} catch (CryptoException e) {
			throw new L2pSecurityException("unable do decrypt key", e);
		}
	}

	/**
	 * Encrypts the private key into a byte array with strong encryption based on a passphrase. to unlock the key
	 * 
	 * @param key A key that is used to encrypt the agents private key.
	 * @throws L2pSecurityException If an issue with the given key occurs.
	 */
	public void encryptPrivateKey(SecretKey key) throws L2pSecurityException {
		if (isLocked()) {
			throw new L2pSecurityException("You have to unlock the key first!");
		}

		try {
			baEncrypedPrivate = CryptoTools.encryptSymmetric(privateKey, key);
		} catch (CryptoException e) {
			throw new L2pSecurityException("Unable to encrypt private key", e);
		} catch (SerializationException e) {
			throw new L2pSecurityException("unable to serialize private key", e);
		}
	}

	/**
	 * 
	 * @return true, if the private key of this agent is still locked
	 */
	public boolean isLocked() {
		return privateKey == null;
	}

	public String getSafeId() {
		return CryptoTools.publicKeyToSHA512(getPublicKey());
	}

	/**
	 * Returns the id of this agent. <i>This method is only implemented, since an Agent is also a
	 * {@link MessageReceiver}, thus has to implement this method. It was written for the {@link Mediator} class.</i>
	 * 
	 * @return id of the agent
	 */
	@Override
	public String getResponsibleForAgentSafeId() {
		return getSafeId();
	}

	/**
	 * 
	 * @return the cryptographic public key of this agent
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * 
	 * @return the cryptographic private key of this agent
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 */
	private PrivateKey getPrivateKey() throws L2pSecurityException {
		if (privateKey == null) {
			throw new L2pSecurityException("You have to unlock the key using a passphrase first!");
		}
		return privateKey;
	}

	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to decrypt the passed crypted content with the agent's private
	 * key.
	 * 
	 * @param crypted The encrypted content that is decrypted using the agents private key.
	 * @return Returns a {@link javax.crypto.SecretKey} decrypted from the crypted input and the agent's private key
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws CryptoException If an issue occurs with decryption.
	 * @throws SerializationException
	 */
	public SecretKey decryptSymmetricKey(byte[] crypted)
			throws L2pSecurityException, SerializationException, CryptoException {
		SecretKey symmetricGroupKey = (SecretKey) CryptoTools.decryptAsymmetric(crypted, this.getPrivateKey());
		return symmetricGroupKey;
	}

	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to create a {@link Signature java.security.Signature} and
	 * initializes the object for signing with the agent's private key.
	 * 
	 * @return a {@link Signature java.security.Signature}
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	public Signature createSignature() throws InvalidKeyException, L2pSecurityException, NoSuchAlgorithmException {
		Signature sig = Signature.getInstance(CryptoTools.getSignatureMethod());
		sig.initSign(this.getPrivateKey());
		return sig;
	}

	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to sign the passed data with the agent's private key.
	 * 
	 * @param plainData
	 * @return a signed version of the input
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws CryptoException
	 */
	public byte[] signContent(byte[] plainData) throws CryptoException, L2pSecurityException {
		byte[] signature = CryptoTools.signContent(plainData, this.getPrivateKey());
		return signature;
	}

	/**
	 * Gets the private key encrypted and encoded in base64.
	 * 
	 * mainly for <code>toXmlString()</code> methods of subclasses
	 * 
	 * @return encoded version or the private key
	 */
	protected String getEncodedPrivate() {
		return Base64.getEncoder().encodeToString(baEncrypedPrivate);
	}

	/**
	 * Hook to be called by the node where this agent is registered to, when the node receives a message destined to
	 * this agent.
	 * 
	 * @param message
	 * @param c
	 * @throws MessageException
	 */
	@Override
	public abstract void receiveMessage(Message message, AgentContext c) throws MessageException;

	/**
	 * Gets a locked copy of this agent.
	 * 
	 * @return a locked clone of this agent
	 * @throws CloneNotSupportedException
	 */
	public final Agent cloneLocked() throws CloneNotSupportedException {
		Agent result = (Agent) clone();
		result.lockPrivateKey();
		return result;
	}

	/**
	 * 
	 * Notifies this agent of unregistering from a node.
	 * 
	 */
	@Override
	public void notifyUnregister() {
		runningAt = null;
	}

	/**
	 * 
	 * Notifies this agent that it has been registered at a node. May be overridden in implementing classes.
	 * 
	 * <i>Make sure, overriding methods do a call of this method!</i>
	 * 
	 * @param n
	 * @throws AgentException
	 * 
	 */
	@Override
	public void notifyRegistrationTo(Node n) throws AgentException {
		if (this instanceof ServiceAgent) {
			n.observerNotice(Event.SERVICE_STARTUP, n.getNodeId(), this,
					"" + ((ServiceAgent) this).getServiceNameVersion());
		}
		runningAt = n;
	}

	/**
	 * Gets the node, this agent is running at.
	 * 
	 * @return the node, this agent is running at
	 */
	public Node getRunningAtNode() {
		return runningAt;
	}

	/**
	 * Factory: Create an agent from its XML file representation.
	 * 
	 * Depending on the type attribute of the root node, the type will be a {@link UserAgent}, {@link GroupAgent},
	 * {@link ServiceAgent}. Creation of {@link MonitoringAgent}s is not supported.
	 * 
	 * @param xmlFile
	 * @return an agent
	 * @throws MalformedXMLException
	 */
	public static Agent createFromXml(File xmlFile) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xmlFile, "las2peer:agent"));
	}

	/**
	 * Factory: Create an agent from its XML string representation.
	 * 
	 * Depending on the type attribute of the root node, the type will be a {@link UserAgent}, {@link GroupAgent},
	 * {@link ServiceAgent}. Creation of {@link MonitoringAgent}s is not supported.
	 * 
	 * @param xml
	 * @return an agent
	 * @throws MalformedXMLException
	 */
	public static Agent createFromXml(String xml) throws MalformedXMLException {
		return createFromXml(XmlTools.getRootElement(xml, "las2peer:agent"));
	}

	/**
	 * Factory: Create an agent from its XML representation.
	 * 
	 * Depending on the type attribute of the root node, the type will be a {@link UserAgent}, {@link GroupAgent},
	 * {@link ServiceAgent}. Creation of {@link MonitoringAgent}s is not supported.
	 * 
	 * @param rootElement
	 * @return an agent
	 * @throws MalformedXMLException
	 */
	public static Agent createFromXml(Element rootElement) throws MalformedXMLException {
		String type = rootElement.getAttribute("type");
		if ("user".equalsIgnoreCase(type)) {
			return UserAgent.createFromXml(rootElement);
		} else if ("group".equalsIgnoreCase(type)) {
			return GroupAgent.createFromXml(rootElement);
		} else if ("service".equalsIgnoreCase(type)) {
			return ServiceAgent.createFromXml(rootElement);
		} else if ("monitoring".equalsIgnoreCase(type)) {
			return MonitoringAgent.createFromXml(rootElement);
		} else {
			throw new MalformedXMLException("Unknown agent type: " + type);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (super.equals(other)) {
			return true;
		}
		if (other == null || !other.getClass().isInstance(this)) {
			return false;
		}
		return this.getSafeId().equalsIgnoreCase(((Agent) other).getSafeId());
	}

}
