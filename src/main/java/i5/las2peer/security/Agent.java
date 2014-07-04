package i5.las2peer.security;

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
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;


/**
 * An Agent is the basic acting entity in the LAS2peer network.
 * At the moment, an agent can represent a simple user, a group, a service or a monitoring agent.
 * 
 * 
 * 
 *
 */
public abstract class Agent implements XmlAble, Cloneable, MessageReceiver {
	
	private long id;
	
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
	 * @param id
	 * @param pair
	 * @param key
	 * @throws L2pSecurityException
	 */
	protected Agent ( long id, KeyPair pair, SecretKey key ) throws L2pSecurityException {
		publicKey = pair.getPublic();
		privateKey = pair.getPrivate();
		this.id = id;		
		
		encryptPrivateKey(key);
		lockPrivateKey();
	}
	
	
	/**
	 * Creates a new agent.
	 * 
	 * @param id
	 * @param publicKey
	 * @param encodedPrivate
	 */
	protected Agent ( long id, PublicKey publicKey, byte[] encodedPrivate ) {
		this.id = id;
		this.publicKey = publicKey;
		this.baEncrypedPrivate = encodedPrivate.clone();
		this.privateKey = null;
	}
	
		
	/**
	 * (Re-)Lock the private key.
	 */
	public void lockPrivateKey () {
		privateKey = null;
	}
	
	
	/**
	 * Unlocks the private key.
	 * 
	 * @param key
	 * 
	 * @throws L2pSecurityException
	 */
	public void unlockPrivateKey ( SecretKey key ) throws L2pSecurityException {
		try {
			privateKey = (PrivateKey) SerializeTools.deserialize(CryptoTools.decryptSymmetric(baEncrypedPrivate, key));
		} catch (SerializationException e) {
			throw new L2pSecurityException("unable do deserialize key", e );
		} catch (CryptoException e) {
			throw new L2pSecurityException("unable do decrypt key", e );
		}
	}
	
	
	/**
	 * Encrypts the private key into a byte array with strong encryption based on a passphrase.
	 * to unlock the key
	 * 
	 * @param key
	 * @throws L2pSecurityException
	 */
	public void encryptPrivateKey(SecretKey key) throws L2pSecurityException {
		if ( isLocked() )
			throw new L2pSecurityException("You have to unlock the key first!" );
		
		try {
			baEncrypedPrivate = CryptoTools.encryptSymmetric(privateKey, key);
		} catch (CryptoException e) {
			throw new L2pSecurityException ( "Unable to encrypt private key", e);
		} catch ( SerializationException e) {
			throw new L2pSecurityException ( "unable to serialize private key", e);
		}
	}
	
	
	/**
	 * 
	 * @return true, if the private key of this agent is still locked
	 */
	public boolean isLocked () {
		return privateKey == null;
	}
	
	
	/**
	 * Returns the id of this agent.
	 * 
	 * @return id of the agent
	 */
	public long getId () {
		return id;
	}
	
	
	/**
	 * Returns the id of this agent.
	 * <i>This method is only implemented, since an Agent is
	 * also a {@link MessageReceiver}, thus has to implement this method.
	 * It was written for the {@link Mediator} class.</i>
	 * 
	 * @return id of the agent
	 */
	@Override
	public long getResponsibleForAgentId() {
		return getId();
	}	
	
	
	/**
	 * 
	 * @return the cryptographic public key of this agent 
	 */
	public PublicKey getPublicKey () {
		return publicKey;
	}
	
	
	/**
	 * 
	 * @return the cryptographic private key of this agent
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 */
	private PrivateKey getPrivateKey () throws L2pSecurityException {
		if ( privateKey == null )
			throw new L2pSecurityException("You have to unlock the key using a passphrase first!");
		return privateKey;
	}
	
	
	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to decrypt the passed crypted content with the agent's private key.
	 * 
	 * @return a {@link javax.crypto.SecretKey} decrypted from the crypted input and the agent's private key
	 * 
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public SecretKey returnSecretKey (byte[] crypted) throws L2pSecurityException, SerializationException, CryptoException {
		SecretKey symmetricGroupKey = (SecretKey) CryptoTools.decryptAsymmetric(crypted, this.getPrivateKey());
		return symmetricGroupKey;
	}
	
	
	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to create a {@link Signature java.security.Signature}
	 * and initializes the object for signing with the agent's private key.
	 * 
	 * @return a {@link Signature java.security.Signature}
	 * 
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	public Signature createSignature () throws InvalidKeyException, L2pSecurityException, NoSuchAlgorithmException {
		Signature sig = Signature.getInstance( CryptoTools.getSignatureMethod() );
		sig.initSign(this.getPrivateKey());
		return sig;
	}
	
	
	/**
	 * Uses the {@link i5.las2peer.tools.CryptoTools} to sign the passed data with the agent's private key.
	 * 
	 * @return a signed version of the input
	 * 
	 * @throws L2pSecurityException the private key has not been unlocked yet
	 * @throws CryptoException 
	 * @throws SerializationException 
	 */
	public byte[] signContent (byte[] plainData) throws CryptoException, L2pSecurityException {
		byte[] signature = CryptoTools.signContent(plainData, this.getPrivateKey());
		return signature;
	}
	
	/**
	 * Gets the private key encrypted and encoded in base64.
	 * 
	 * mainly for <code>toXmlString()</code> methods of subclasses
	 *  
	 * @return	encoded version or the private key
	 */
	protected String getEncodedPrivate () {
		return Base64.encodeBase64String( baEncrypedPrivate );
	}
	
	
	/**
	 * Hook to be called by the node where this agent is registered to, when the
	 * node receives a message destined to this agent.
	 * 
	 * @param message
	 * @throws MessageException
	 */
	public abstract void receiveMessage( Message message, Context c ) throws MessageException;
	
	
	/**
	 * Gets a locked copy of this agent.
	 * 
	 * @return a locked clone of this agent
	 * @throws CloneNotSupportedException 
	 */
	public final Agent cloneLocked () throws CloneNotSupportedException {
		Agent result = (Agent) clone();
		result.lockPrivateKey();
		return result;
	}
	
	
	/**
	 * 
	 * Notifies this agent of unregistering from a node.
	 * 
	 */
	public void notifyUnregister() {
		if ( this instanceof ServiceAgent )
			runningAt.observerNotice(Event.SERVICE_SHUTDOWN, runningAt.getNodeId(), this, "" + ((ServiceAgent)this).getServiceClassName());
		runningAt = null;
	}
	
	
	/**
	 * 
	 * Notifies this agent that it has been registered at a node.
	 * May be overridden in implementing classes.
	 * 
	 * <i>Make sure, overriding methods do a call of this method!</i>
	 * 
	 * @param n
	 * 
	 * @throws AgentException
	 * 
	 */
	public void notifyRegistrationTo ( Node n ) throws AgentException {
		if ( this instanceof ServiceAgent )
			n.observerNotice(Event.SERVICE_STARTUP, n.getNodeId(), this, "" + ((ServiceAgent)this).getServiceClassName());
		runningAt = n;
	}
	
	
	/**
	 * Gets the node, this agent is running at.
	 * 
	 * @return the node, this agent is running at
	 */
	public Node getRunningAtNode () { return runningAt; } 
	
	
	/**
	 * Factory: Create an agent from its XML string representation.
	 * 
	 * Depending on the type attribute of the root node, the type will be
	 * a {@link UserAgent}, {@link GroupAgent}, {@link ServiceAgent}.
	 * Creation of {@link MonitoringAgent}s is not supported.
	 * 
	 * @param xml
	 * 
	 * @return an agent
	 * 
	 * @throws MalformedXMLException
	 */
	public static Agent createFromXml(String xml) throws MalformedXMLException {
		try {
			Element root = Parser.parse(xml, false);

			if ( !root.getName().equals("agent") )
				throw new MalformedXMLException ( "this is not an agent but a " + root.getName() );
			
			String type = root.getAttribute( "type");
			
			if ( "user".equals( type ))
				return UserAgent.createFromXml(root);
			else if ( "group".equals( type ) )
				return GroupAgent.createFromXml(root);
			else if ( "service".equals( type ) )
				return ServiceAgent.createFromXml(root);
			else if ("monitoring".equals( type ))
				return MonitoringAgent.createFromXml(root);
			else 
				throw new MalformedXMLException("Unknown agent type: " + type);
			
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}		
	}
	
	
}
