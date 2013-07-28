package i5.las2peer.p2p;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.FileContentReader;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.Vector;


/**
 * A NodeInformation gives basic information about a node.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class NodeInformation implements XmlAble {

	private String organization = null;
	private String adminName = null;
	private String adminEmail = null;
	private String description = "A standard Las2Peer node -- no further information is provided.";

	private String[] hostedServices = new String[0];
	
	private PublicKey nodeKey;
	private Serializable nodeHandle;
	
	private byte[] signature;
	
	
	/**
	 * create a new standard node information
	 */
	public NodeInformation () {
	}
	
	
	/**
	 * create a standard node information for a node hosting the given services
	 * 
	 * @param hostedServiceAgents
	 */
	public NodeInformation ( ServiceAgent[] hostedServiceAgents ) {
		this ();
		
		setServices ( hostedServiceAgents );
	}
	
	
	/**
	 * get the organization, this node is hosted by
	 * @return organization name
	 */
	public String getOrganization () { return organization; }
	
	/**
	 * get the name of the admin stored in this information
	 * 
	 * @return admin real name
	 */
	public String getAdminName () { return adminName; }
	
	/**
	 * get the admin email address of this node information
	 * 
	 * @return email address
	 */
	public String getAdminEmail () { return adminEmail; }
	
	/**
	 * get the description entry
	 * @return a node description 
	 */
	public String getDescription () { return description; }
	
	/**
	 * get an array with the class names of all services hosted at the node described with this information
	 * 
	 * @return array with service class names
	 */
	public String[] getHostedServices () {
		return hostedServices.clone();
	}
	
	
	
	/**
	 * set the hosted service classes of this node information
	 * 
	 * @param serviceAgents
	 */
	void setServices ( ServiceAgent[] serviceAgents ) {
		hostedServices = new String[ serviceAgents.length];
		
		for ( int lauf =0; lauf < hostedServices.length; lauf++) {
			hostedServices[lauf] = serviceAgents[lauf].getServiceClassName();
		}		
	}
	
	/**
	 * for the node itself: set the signature before sending
	 * @param signature
	 */
	public void setSignature( byte[] signature ) {
		this.signature = signature;
	}
	
	/**
	 * for the node itself: deliver the handle
	 * @param nodeHandle
	 */
	public void setNodeHandle ( Serializable nodeHandle ) {
		this.nodeHandle = nodeHandle;
	}
	
	
	/**
	 * for the node itself: deliver the key
	 * @param nodeKey
	 */
	public void setNodeKey ( PublicKey nodeKey ) {
		this.nodeKey = nodeKey;
	}
	
	/**
	 * verify the signature 
	 * @throws L2pSecurityException 
	 */
	public void verifySignature () throws L2pSecurityException {
		if ( signature == null )
			throw new L2pSecurityException ( "No Signature!");
		
		if ( nodeKey == null)
			throw new L2pSecurityException("No node key!" );
		
		try {
			if ( ! CryptoTools.verifySignature(signature, getSignatureContent(),  nodeKey) )
				throw new L2pSecurityException("signaure faulty!");
		} catch (CryptoException e) {
			throw new L2pSecurityException("unable to verify signature", e);
		}
	}
	
	/**
	 * the content for the signature
	 * 
	 * @return an array containing the signature content as bytes
	 */
	public byte[] getSignatureContent () {
		String toSign = nodeKey.toString() + getAdminEmail() + nodeHandle.toString();
		return toSign.getBytes();
	}
	
	
	/**
	 * get the handle of the described node
	 * 
	 * @return a node handle, either Long or NodeHandle
	 */
	public Object getNodeHandle () {
		return nodeHandle;
	}
	
	/**
	 * get the public encryption key of the node
	 * @return the public node key
	 */
	public PublicKey getNodeKey () {
		return nodeKey;
	}
	
	
	/**
	 * check, if all relevant information is given
	 * @return true, if at least node key, signature and node handle are provided.
	 */
	public boolean isComplete () {
		return nodeKey != null && signature != null && nodeHandle != null;
	}
	
	
	@Override
	public String toXmlString() {
		StringBuffer result = new StringBuffer ( "<las2peerNode>\n");
		
		if ( organization != null )
			result.append("\t<organization>").append(organization).append("</organization>\n");
		
		if ( adminName != null )
			result.append("\t<adminName>").append(adminName).append("</adminName>\n");
		
		if ( adminEmail != null )
			result.append("\t<adminEmail>").append(adminEmail).append("</adminEmail>\n");
		
		result.append ("\t<description>").append(description).append("</description>\n");
		
		if ( hostedServices != null && hostedServices.length > 0 ) {
			result.append ("\t<services>\n");
			
			for ( String service : hostedServices )
				result.append ("\t\t<serviceClass>").append(service).append("</serviceClass>\n");
			
			result.append ("\t</services>\n");
		}
		
		
		try {
			if ( nodeKey != null )
				result.append ( "\t<nodeKey encoding=\"base64\">").append ( SerializeTools.serializeToBase64(nodeKey)).append("</nodeKey>\n");
			
			if ( signature != null)
				result.append ( "\t<signature encoding=\"base64\">").append (SerializeTools.serializeToBase64(signature)).append("</signature>\n");
				
			if ( nodeHandle != null )
				result.append ( "\t<nodeHandle>\n")
					.append ("\t\t<plain><![CDATA[")
					.append ( nodeHandle.toString() )
					.append ("]]></plain>\n")
					.append ("\t\t<serialized encoding=\"base64\">").append ( SerializeTools.serializeToBase64(nodeHandle))
					.append ( "</serialized>\n" )
					.append( "\t</nodeHandle>\n");
		} catch (SerializationException e) {
			throw new RuntimeException ( "critical: should not occur!" );
		}
		
			
		
		result.append ( "</las2peerNode>\n");
		
		return result.toString();
	}
	
	/**
	 * return all information stored here as XML string
	 */
	@Override
	public String toString () {
		return toXmlString();
	}
	
	
	/**
	 * factory: create a NodeInformation instance from a XML file
	 * 
	 * @param filename
	 * 
	 * @return the node information contained in the given XML file
	 * 
	 * @throws MalformedXMLException
	 * @throws IOException
	 * @throws XMLSyntaxException 
	 */
	public static NodeInformation createFromXmlFile ( String filename ) throws MalformedXMLException, IOException, XMLSyntaxException {
		return createFromXml ( FileContentReader.read( filename ));
	}
	
	

	/**
	 * factory: create a NodeInformation instance from a XML file and set the hosted services
	 * 
	 * @param filename
	 * @param serviceAgents
	 * 
	 * @return	a node information
	 * 
	 * 
	 * @throws XMLSyntaxException
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static NodeInformation createFromXmlFile ( String filename, ServiceAgent[] serviceAgents ) throws XMLSyntaxException, MalformedXMLException, IOException {
		NodeInformation result = createFromXmlFile ( filename );
		result.setServices(serviceAgents);
		
		return result;
	}
	
	
	/**
	 * factory create a node information instance from an XML string
	 * 
	 * @param xml
	 * 
	 * @return node information contained in the given XML string
	 * 
	 * @throws MalformedXMLException
	 * @throws XMLSyntaxException
	 */
	public static NodeInformation createFromXml ( String xml ) throws MalformedXMLException, XMLSyntaxException {
		Element root = Parser.parse ( xml, false );
		if ( !root.getName().equals("las2peerNode"))
			throw new MalformedXMLException("not a node information but a " + root.getName());

		NodeInformation result = new NodeInformation ();
		
		try {
			Enumeration<Element > children = root.getChildren();
			while ( children.hasMoreElements()) {
				Element child = children.nextElement();
				
				if ( child.getName().equals( "adminName") )
					result.adminName = child.getFirstChild().getText();
				else if ( child.getName().equals( "adminEmail") )
					result.adminEmail = child.getFirstChild().getText();
				else if ( child.getName().equals( "description") )
					result.description = child.getFirstChild().getText();
				else if ( child.getName().equals( "nodeHandle"))
					result.nodeHandle = SerializeTools.deserializeBase64(child.getChild(1).getFirstChild().getText());
				else if ( child.getName().equals( "nodeKey"))
					result.nodeKey = (PublicKey) SerializeTools.deserializeBase64(child.getFirstChild().getText());
				else if ( child.getName().equals( "signature"))
					result.signature = (byte[]) SerializeTools.deserializeBase64(child.getFirstChild().getText());
				else if ( child.getName().equals( "services") ) {
					Vector<String> serviceClasses = new Vector<String> ();
					
					Enumeration<Element> services = child.getChildren();
					while ( services.hasMoreElements()) {
						Element service = services.nextElement();
						if ( ! service.getName().equals ( "serviceClass"))
							throw new MalformedXMLException(service + " is not a service class element");
						serviceClasses.add ( service.getFirstChild().getText());
					}
					
					result.hostedServices = serviceClasses.toArray( new String[0] );
				} else
					throw new MalformedXMLException("unkown xml element: " + child.getName());
				
			}
		} catch (SerializationException e) {
			throw new MalformedXMLException("unable to deserialize contents", e);
		}
		
		
		
		return result;
	}

	
	/**
	 * command line tool for generating a description XML file
	 *  
	 * @param argv
	 */
	public static void main ( String argv[] ) {
		if ( argv.length < 4 ) {
			System.out.println ( "Usage: java i5.las2peer.p2p.NodeInformation adminName adminEmail organization description");
			System.exit(0);
		}
		
		NodeInformation result = new NodeInformation ();
		
		result.description = argv[3];
		result.adminEmail = argv[1];
		result.adminName = argv[0];
		result.organization = argv[2];
		
		System.out.println ( result.toXmlString() );
	}
	
	
}
