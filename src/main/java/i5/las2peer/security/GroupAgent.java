package i5.las2peer.security;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

/**
 * An agent representing a group of other agents.
 * 
 * The storage of the group information is stored encrypted in a similar manner to 
 * {@link i5.las2peer.persistency.Envelope}:
 * 
 * The (symmetric) key to unlock the private key of the group is encrypted asymmetrically for
 * each entitled agent (i.e. <i>member</i> of the group).
 * 
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class GroupAgent extends Agent {

	private SecretKey symmetricGroupKey = null;
	private Agent openedBy = null;

	
	
	/**
	 * hashtable storing the encrypted versions of the group secret key
	 * for each member
	 */
	private Hashtable<Long, byte[]> htEncryptedKeyVersions = new Hashtable<Long,byte[]> ();
	
	
	@SuppressWarnings("unchecked")
	protected GroupAgent(long id, PublicKey pubKey, byte[] encryptedPrivate, Hashtable<Long, byte[]> htEncryptedKeys ) throws L2pSecurityException {
		super (id, pubKey, encryptedPrivate );
		
		htEncryptedKeyVersions = (Hashtable<Long, byte[]>) htEncryptedKeys.clone();
	}
	
	/**
	 * constructor for the {@link #createGroupAgent} factory
	 * simply necessary, since the secret key has to be stated for the constructor of the superclass
	 *  
	 * @param id
	 * @param keys
	 * @param secret
	 * @param members
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	protected GroupAgent ( long id, KeyPair keys, SecretKey secret, Agent[] members ) throws L2pSecurityException, CryptoException, SerializationException {
		super ( id, keys, secret );
		
		symmetricGroupKey = secret;
		for ( Agent a : members )
			addMember ( a, false );
		
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
	public void unlockPrivateKey ( Agent agent ) throws L2pSecurityException, SerializationException, CryptoException {
		decryptSecretKey ( agent );
		openedBy = agent;
		super.unlockPrivateKey(symmetricGroupKey);
	}
	
	/**
	 * decrypt the secret key of this group for the given agent (which is hopefully a member) 
	 * @param agent
	 * @throws SerializationException
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	private void decryptSecretKey ( Agent agent ) throws SerializationException, CryptoException, L2pSecurityException {
		byte[] crypted = htEncryptedKeyVersions.get ( agent.getId() );
		
		if ( crypted == null )
			throw new L2pSecurityException ( "the given agent is not listed as a group member!");
		
		symmetricGroupKey = (SecretKey) agent.returnSecretKey(crypted);
	}
	
	/**
	 * add a member to this group
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public void addMember ( Agent a ) throws L2pSecurityException, CryptoException, SerializationException {
		addMember ( a, true );
	}
	
	/**
	 * private version of adding members, mainly just for the constructor to
	 * add members without unlocking the private key of the group
	 * 
	 * @param a
	 * @param securityCheck
	 * @throws L2pSecurityException
	 * @throws SerializationException 
	 * @throws CryptoException 
	 */
	private final void addMember ( Agent a, boolean securityCheck ) throws L2pSecurityException, CryptoException, SerializationException {
		if ( securityCheck && isLocked ())
			throw new L2pSecurityException("you have to unlock this group first!" );
		
		byte[] cryptedSecret = CryptoTools.encryptAsymmetric(symmetricGroupKey, a.getPublicKey());
		htEncryptedKeyVersions.put( a.getId(), cryptedSecret);		
	}
	
	/**
	 * check, if the given agent is member of this group
	 * @param a
	 * @return true, if the given agent is a member of this group
	 */
	public boolean isMember( Agent a ) {
		// only for opened groups?
		return ( htEncryptedKeyVersions.get(a.getId()) != null);
	}
	
	
	/**
	 * check, if the given agent (id) is member of this group
	 * @param id
	 * @return true, if the given agent is a member if this group
	 */
	public boolean isMember ( long id ) {
		return ( htEncryptedKeyVersions.get ( id ) != null);
	}
	
	/**
	 * how many members does this group have
	 * 
	 * @return	the number of group members
	 */
	public int getSize() {
		return htEncryptedKeyVersions.size();
	}
	
	
	/**
	 * get an array with the ids of all group members
	 * 
	 * @return	an array with the ids of all member agents
	 */
	public Long[] getMemberList () {
		return htEncryptedKeyVersions.keySet().toArray(new Long[0]);
	}
	
	/**
	 * returns the Agent by whom the private Key of this Group has been unlocked
	 * 
	 * @return	the agent, who opened the private key of the group
	 */
	public Agent getOpeningAgent () {
		return openedBy;
	}
	
	/**
	 * remove a member from this group 
	 * 
	 * @param a
	 * @throws L2pSecurityException
	 */
	public void removeMember ( Agent a ) throws L2pSecurityException {
		removeMember ( a.getId());
	}
	
	
	/**
	 * remove a member from this group 
	 * 
	 * @param id
	 * @throws L2pSecurityException
	 */
	public void removeMember ( long id ) throws L2pSecurityException {
		if ( isLocked())
			throw new L2pSecurityException ( "You have to unlock this agent first!");
		
		htEncryptedKeyVersions.remove( id );
		
		// todo: switch SecretKey?
	}
	
	@Override
	public void lockPrivateKey () {
		super.lockPrivateKey();
		this.symmetricGroupKey = null;
		openedBy = null;
	}
	
	
	@Override
	public String toXmlString() {
		try {
			String keyList = "";
			
			for ( Long id : htEncryptedKeyVersions.keySet() ) {
				keyList += 
					"\t\t<keyentry forAgent=\""+id+"\" encoding=\"base64\">" 
							+ Base64.encodeBase64String(htEncryptedKeyVersions.get(id)) 
							+ "</keyentry>\n";
			}
			
			return 
				"<las2peer:agent type=\"group\">\n"
				+"\t<id>" + getId() + "</id>\n"
				+"\t<publickey encoding=\"base64\">"
				+ SerializeTools.serializeToBase64( getPublicKey()  ) 
				+"</publickey>\n"
				+"\t<privatekey encoding=\"base64\" encrypted=\""+CryptoTools.getSymmetricAlgorithm()+"\">"
				+getEncodedPrivate()
				+"</privatekey>\n"
				+"\t<unlockKeys method=\""+CryptoTools.getAsymmetricAlgorithm()+"\">\n"
				+ keyList
				+"\t</unlockKeys>\n"
				+"</las2peer:agent>\n";
		} catch (SerializationException e) {
			throw new RuntimeException ( "Serialization problems with keys");
		}		
	}

	
	/**
	 * factory - create an instance of GroupAgent from its xml representation
	 * 
	 * @param xml
	 * @return	a group agent 
	 * @throws MalformedXMLException
	 */
	public static GroupAgent createFromXml ( String xml ) throws MalformedXMLException {
		try {
			Element root = Parser.parse( xml, false);

			if ( ! "group".equals( root.getAttribute("type")) )
				throw new MalformedXMLException("group agent expeced" );
			if ( ! "agent".equals ( root.getName() ))
				throw new MalformedXMLException("agent expected");
		
			return createFromXml ( root );
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}
	
	
	/**
	 * factory - create an instance of GroupAgent based on a xml node
	 * 
	 * @param root
	 * @return	a group agent
	 * @throws MalformedXMLException
	 */
	public static GroupAgent createFromXml ( Element root ) throws MalformedXMLException {
		try {
			Element elId = root.getFirstChild();
			Element pubKey = root.getChild(1);
			Element privKey = root.getChild ( 2 );
			Element encryptedKeys = root.getChild(3);

			
			if ( !pubKey.getName().equals( "publickey" ))
				throw new MalformedXMLException("public key expected" );
			if ( ! pubKey.getAttribute("encoding").equals( "base64"))
				throw new MalformedXMLException("base64 encoding expected" );
						
			if ( !privKey.getName().equals( "privatekey" ))
				throw new MalformedXMLException("private key expected" );
			if ( ! privKey.getAttribute("encrypted").equals( CryptoTools.getSymmetricAlgorithm() ))
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected" );

			if ( ! encryptedKeys.getName().equals( "unlockKeys"))
				throw new MalformedXMLException("unlockKeys expected" );			
			if ( ! encryptedKeys.getAttribute("method").equals( CryptoTools.getAsymmetricAlgorithm() ))
				throw new MalformedXMLException("base64 encoding expected" );
						
			
			long id = Long.parseLong( elId.getFirstChild().getText());			
			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64 ( pubKey.getFirstChild().getText());
			byte[] encPrivate = Base64.decodeBase64(privKey.getFirstChild().getText());
			
			Hashtable<Long, byte[]> htMemberKeys = new Hashtable<Long, byte[]> ();
			for ( Enumeration<Element> enKeys = encryptedKeys.getChildren(); enKeys.hasMoreElements(); ) {
				Element elKey = enKeys.nextElement();
				
				if ( ! elKey.getName().equals( "keyentry"))
					throw new MalformedXMLException("unlockKeys expected" );
				if ( ! elKey.hasAttribute("forAgent"))
					throw new MalformedXMLException("forAgent attribute expected" );
				if ( ! elKey.getAttribute("encoding").equals( "base64"))
					throw new MalformedXMLException("base64 encoding expected" );
				
				long agentId = Long.parseLong( elKey.getAttribute("forAgent"));
				byte[] content = Base64.decodeBase64( elKey.getFirstChild().getText() );
				htMemberKeys.put( agentId,  content);
			}
			return new GroupAgent ( id, publicKey, encPrivate, htMemberKeys );
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e );
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
	public static GroupAgent createGroupAgent (Agent[] members ) throws L2pSecurityException, CryptoException, SerializationException {
		Random r = new Random();
		return new GroupAgent(r.nextLong(), CryptoTools.generateKeyPair(), CryptoTools.generateSymmetricKey(), members);
	}

	@Override
	public void receiveMessage(Message message, Context context) throws MessageException {
		// extract content from message
		Object content = null;
		try {
			message.open(this, getRunningAtNode());
			content = message.getContent();
		} catch (AgentNotKnownException e1) {
			Context.logError(this, e1.getMessage());
		} catch (L2pSecurityException e2) {
			Context.logError(this, e2.getMessage());
		}
		if (content == null) {
			Context.logError(this, "The message content is null. Dropping message!");
			return;
		}
		Serializable contentSerializable = null;
		XmlAble contentXmlAble = null;
		if (content instanceof Serializable) {
			contentSerializable = (Serializable) content;
		} else if (content instanceof XmlAble) {
			contentXmlAble = (XmlAble) content;
		} else {
			throw new MessageException("The content of the received message is neither Serializable nor XmlAble but "+content.getClass());
		}
		// send message content to each member of this group
		for (Long memberId : getMemberList()) {
			Agent member = null;
			try {
				member = getRunningAtNode().getAgent(memberId);
			} catch (AgentNotKnownException e1) {
				Context.logMessage(this, e1.getMessage());
			}
			if (member == null) {
				Context.logMessage(this, "No agent for group member " + memberId + " found! Skipping member.");
				continue;
			}
			try {
				Message msg = null;
				if (contentSerializable != null) {
					msg = new Message(this, member, contentSerializable);
				} else if (contentXmlAble != null) {
					msg = new Message(this, member, contentXmlAble);
				} else {
					Context.logError(this, "The message content is null. Dropping message!");
					return;
				}
				getRunningAtNode().sendMessage(msg, null);
			} catch (EncodingFailedException e) {
				Context.logError(this, e.getMessage());
			} catch (L2pSecurityException e) {
				Context.logError(this, e.getMessage());
			} catch (SerializationException e) {
				Context.logError(this, e.getMessage());
			}
		}
	}

	@Override
	public void notifyUnregister() {
		// TODO Auto-generated method stub
		
	}
	
}
 