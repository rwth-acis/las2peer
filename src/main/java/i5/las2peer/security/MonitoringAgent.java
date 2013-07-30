package i5.las2peer.security;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.monitoring.MonitoringMessage;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;


/**
 * 
 * An MonitoringAgent is responsible for sending
 * monitoring information collected at the {@link i5.las2peer.logging.monitoring.MonitoringObserver}.
 * It should only be used for this task.
 * 
 * @author Peter de Lange
 *
 */
public class MonitoringAgent extends PassphraseAgent {
	
	
	/**
	 * 
	 * Creates a new MonitoringAgent.
	 * 
	 * @param id
	 * @param pair
	 * @param passphrase
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * 
	 */
	protected MonitoringAgent ( long id, KeyPair pair, String passphrase, byte[] salt ) throws L2pSecurityException, CryptoException{
		super ( id, pair, passphrase, salt );
	}
	
	
	@Override
	public String toXmlString() {
		try {
			StringBuffer result = new StringBuffer(
					"<las2peer:agent type=\"monitoring\">\n"
					+"\t<id>" + getId() + "</id>\n"
					+"\t<publickey encoding=\"base64\">"
					+ SerializeTools.serializeToBase64( getPublicKey()  ) 
					+"</publickey>\n"
					+"\t<privatekey encrypted=\""+CryptoTools.getSymmetricAlgorithm() +"\" keygen=\""+CryptoTools.getSymmetricKeygenMethod()+"\">\n"
					+"\t\t<salt encoding=\"base64\">" + Base64.encodeBase64String(getSalt()) + "</salt>\n"
					+"\t\t<data encoding=\"base64\">" + getEncodedPrivate() + "</data>\n"
					+"\t</privatekey>\n"
			);
			
			result.append( "</las2peer:agent>\n" );
			
			return result.toString();
		} catch (SerializationException e) {
			throw new RuntimeException ( "Serialization problems with keys");
		}
	}
	
	
	/**
	 * 
	 * Sets the state of the object from a string representation resulting from
	 * a previous {@link #toXmlString} call.
	 *
	 * Usually, a standard constructor is used to get a fresh instance of the
	 * class and to set the complete state via this method.
	 *
	 *
	 * @param xml a String
	 *
	 * @exception MalformedXMLException
	 *
	 */
	public static MonitoringAgent createFromXml (String xml) throws MalformedXMLException {
		try {
			Element root = Parser.parse( xml, false);
			if ( ! "monitoring".equals( root.getAttribute("type")))
				throw new MalformedXMLException("monitoring agent expeced" );
			if ( ! "agent".equals( root.getName()))
				throw new MalformedXMLException("agent expeced" );
			return createFromXml ( root );
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}
	
	
	/**
	 * Create a new MonitoringAgent protected by the given passphrase.
	 * 
	 * @param passphrase passphrase for the secret key of the new user
	 * 
	 * @return a new UserAgent
	 * 
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	public static MonitoringAgent createMonitoringAgent ( String passphrase ) throws CryptoException, L2pSecurityException {
		Random r = new Random();
		return createMonitoringAgent(r.nextLong(), passphrase);
	}
	
	
	/**
	 * Create a new MonitoringAgent with the given Id and protected by the given passphrase.
	 * 
	 * @param passphrase passphrase for the secret key of the new user
	 * 
	 * @return a new UserAgent
	 * 
	 * @throws CryptoException
	 * @throws L2pSecurityException
	 */
	public static MonitoringAgent createMonitoringAgent(long id, String passphrase) throws CryptoException, L2pSecurityException{
		byte[] salt = CryptoTools.generateSalt();
		return new MonitoringAgent( id, CryptoTools.generateKeyPair(), passphrase, salt );
	}
	
	
	/**
	 * Sets the state of the object from a string representation resulting from
	 * a previous {@link #toXmlString} call.
	 *
	 * @param root parsed xml document
	 *
	 * @exception MalformedXMLException
	 *
	 */	
	public static MonitoringAgent createFromXml ( Element root ) throws MalformedXMLException {
		try {
			Element elId = root.getFirstChild();
			long id = Long.parseLong( elId.getFirstChild().getText());
			
			Element pubKey = root.getChild(1);
			if ( !pubKey.getName().equals( "publickey" ))
				throw new MalformedXMLException("public key expected" );
			if ( ! pubKey.getAttribute("encoding").equals( "base64"))
				throw new MalformedXMLException("base64 encoding expected" );
			
			PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64 ( pubKey.getFirstChild().getText());
			
			Element privKey = root.getChild ( 2 );
			if ( !privKey.getName().equals("privatekey"))
				throw new MalformedXMLException("private key expected");
			if ( ! privKey.getAttribute("encrypted").equals( CryptoTools.getSymmetricAlgorithm() ))
				throw new MalformedXMLException(CryptoTools.getSymmetricAlgorithm() + " expected");
			if ( ! privKey.getAttribute("keygen").equals( CryptoTools.getSymmetricKeygenMethod() ))
				throw new MalformedXMLException(CryptoTools.getSymmetricKeygenMethod()  + " expected");
			
			Element elSalt= privKey.getFirstChild();
			if ( !elSalt.getName().equals("salt"))
				throw new MalformedXMLException("salt expected");
			if ( ! elSalt.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");
			
			byte[] salt = Base64.decodeBase64 (elSalt.getFirstChild().getText());
			
			Element data = privKey.getChild(1);
			if ( !data.getName().equals( "data" ))
				throw new MalformedXMLException("data expected");
			if ( ! data.getAttribute("encoding").equals("base64"))
				throw new MalformedXMLException("base64 encoding expected");
			byte[] encPrivate = Base64.decodeBase64( data.getFirstChild().getText());
			
			MonitoringAgent result = new MonitoringAgent ( id, publicKey, encPrivate, salt );
			
			return result;
		} catch (XMLSyntaxException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		} catch (SerializationException e) {
			throw new MalformedXMLException("Deserialization problems", e );
		}		
	}
	
	
	/**
	 * Creates a MonitoringAgent with a locked private key.
	 * 
	 * used within {@link #createFromXml}
	 * 
	 * @param id
	 * @param pubKey
	 * @param encodedPrivate
	 * @param salt
	 */
	protected MonitoringAgent ( long id, PublicKey pubKey, byte[] encodedPrivate, byte[] salt ) {
		super ( id, pubKey, encodedPrivate, salt );
	}
	
	
	@Override
	public void receiveMessage(Message message, Context context) throws MessageException {
		try {
			message.open(this, getRunningAtNode());
			Object content = message.getContent();
			
			if ( content instanceof MonitoringMessage[] ) {
				System.out.print("Received Message!"); //TODO: Processing!
			} else {
				throw new MessageException ( "Monitoring Agents only receive monitoring messages!");
			}
		} catch (L2pSecurityException e) {
			throw new MessageException ( "Security problems handling the received message", e);
		} catch (AgentNotKnownException e) {
		}
	}
	
	
	@Override
	public void notifyUnregister() {
		// TODO Auto-generated method stub
	}
	
}
