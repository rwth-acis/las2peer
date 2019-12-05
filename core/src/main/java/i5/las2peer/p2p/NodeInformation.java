package i5.las2peer.p2p;

import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.persistency.VerificationFailedException;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import i5.las2peer.serialization.XmlAble;
import i5.las2peer.serialization.XmlTools;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.FileContentReader;

/**
 * A NodeInformation gives basic information about a node.
 * 
 */
public class NodeInformation implements XmlAble {

	private String organization = null;
	private String adminName = null;
	private String adminEmail = null;
	private String description = "A standard las2peer node -- no further information is provided.";

	private List<ServiceNameVersion> hostedServices = new ArrayList<ServiceNameVersion>();

	private PublicKey nodeKey;
	private Serializable nodeHandle;

	private byte[] signature;

	/**
	 * create a new standard node information
	 */
	public NodeInformation() {
	}

	/**
	 * create a standard node information for a node hosting the given services
	 * 
	 * @param hostedServiceAgents A bunch of service agents hosted on this node
	 */
	public NodeInformation(ServiceAgentImpl[] hostedServiceAgents) {
		this();

		setServices(hostedServiceAgents);
	}

	/**
	 * get the organization, this node is hosted by
	 * 
	 * @return organization name
	 */
	public String getOrganization() {
		return organization;
	}

	/**
	 * get the name of the admin stored in this information
	 * 
	 * @return admin real name
	 */
	public String getAdminName() {
		return adminName;
	}

	/**
	 * get the admin email address of this node information
	 * 
	 * @return email address
	 */
	public String getAdminEmail() {
		return adminEmail;
	}

	/**
	 * get the description entry
	 * 
	 * @return a node description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * get an array with the class names of all services hosted at the node described with this information
	 * 
	 * @return array with service class names
	 */
	public List<ServiceNameVersion> getHostedServices() {
		return hostedServices;
	}

	/**
	 * set the hosted service classes of this node information
	 * 
	 * @param serviceAgents
	 */
	void setServices(ServiceAgentImpl[] serviceAgents) {
		hostedServices = new ArrayList<ServiceNameVersion>();
		//hostedServices = new ServiceNameVersion[serviceAgents.length];

		for (int i = 0; i < serviceAgents.length; i++) {
			hostedServices.add( serviceAgents[i].getServiceNameVersion() );
		}
	}

	/**
	 * for the node itself: set the signature before sending
	 * 
	 * @param signature A signature to set
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	public byte[] getSignature() {
		return signature;
	}

	/**
	 * for the node itself: deliver the handle
	 * 
	 * @param nodeHandle A node handle to set
	 */
	public void setNodeHandle(Serializable nodeHandle) {
		this.nodeHandle = nodeHandle;
	}

	/**
	 * for the node itself: deliver the key
	 * 
	 * @param nodeKey A node public key to set
	 */
	public void setNodeKey(PublicKey nodeKey) {
		this.nodeKey = nodeKey;
	}

	/**
	 * verify the signature
	 * 
	 * @throws InternalSecurityException If verifying the signature fails
	 */
	public void verifySignature() throws InternalSecurityException {
		if (signature == null) {
			throw new InternalSecurityException("No Signature!");
		}

		if (nodeKey == null) {
			throw new InternalSecurityException("No node key!");
		}

		try {
			if (!CryptoTools.verifySignature(signature, getSignatureContent(), nodeKey)) {
				throw new InternalSecurityException("signaure faulty!");
			}
		} catch (VerificationFailedException e) {
			throw new InternalSecurityException("unable to verify signature", e);
		}
	}

	/**
	 * the content for the signature
	 * 
	 * @return an array containing the signature content as bytes
	 */
	public byte[] getSignatureContent() {
		String toSign = nodeKey.toString() + getAdminEmail() + nodeHandle.toString();
		return toSign.getBytes();
	}

	/**
	 * get the handle of the described node
	 * 
	 * @return a node handle, either Long or NodeHandle
	 */
	public Object getNodeHandle() {
		return nodeHandle;
	}

	/**
	 * get the public encryption key of the node
	 * 
	 * @return the public node key
	 */
	public PublicKey getNodeKey() {
		return nodeKey;
	}

	/**
	 * check, if all relevant information is given
	 * 
	 * @return true, if at least node key, signature and node handle are provided.
	 */
	public boolean isComplete() {
		return nodeKey != null && signature != null && nodeHandle != null;
	}

	@Override
	public String toXmlString() {
		StringBuffer result = new StringBuffer("<las2peerNode>\n");

		if (organization != null) {
			result.append("\t<organization>");
			result.append(organization);
			result.append("</organization>\n");
		}

		if (adminName != null) {
			result.append("\t<adminName>");
			result.append(adminName);
			result.append("</adminName>\n");
		}

		if (adminEmail != null) {
			result.append("\t<adminEmail>");
			result.append(adminEmail);
			result.append("</adminEmail>\n");
		}

		result.append("\t<description>");
		result.append(description);
		result.append("</description>\n");

		if (hostedServices != null && hostedServices.size() > 0) {
			result.append("\t<services>\n");

			for (ServiceNameVersion service : hostedServices) {
				result.append("\t\t<serviceClass>");
				result.append(service.toString());
				result.append("</serviceClass>\n");
			}

			result.append("\t</services>\n");
		}

		try {
			if (nodeKey != null) {
				result.append("\t<nodeKey encoding=\"base64\">");
				result.append(SerializeTools.serializeToBase64(nodeKey));
				result.append("</nodeKey>\n");
			}

			if (signature != null) {
				result.append("\t<signature encoding=\"base64\">");
				result.append(SerializeTools.serializeToBase64(signature));
				result.append("</signature>\n");
			}

			if (nodeHandle != null) {
				result.append("\t<nodeHandle>\n");
				result.append("\t\t<plain><![CDATA[");
				result.append(nodeHandle.toString());
				result.append("]]></plain>\n");
				result.append("\t\t<serialized encoding=\"base64\">");
				result.append(SerializeTools.serializeToBase64(nodeHandle));
				result.append("</serialized>\n");
				result.append("\t</nodeHandle>\n");
			}
		} catch (SerializationException e) {
			throw new RuntimeException("critical: should not occur!");
		}

		result.append("</las2peerNode>\n");
		//logger.info("[niXML] preparing NodeInfo.xml: \n" + result.toString());
		return result.toString();
	}

	/**
	 * return all information stored here as XML string
	 */
	@Override
	public String toString() {
		return toXmlString();
	}

	/**
	 * factory: create a NodeInformation instance from a XML file
	 * 
	 * @param filename A name for a file containing the XML data
	 * @return the node information contained in the given XML file
	 * @throws MalformedXMLException If the XML data is malformed
	 * @throws IOException If reading the file fails
	 */
	public static NodeInformation createFromXmlFile(String filename) throws MalformedXMLException, IOException {
		return createFromXml(FileContentReader.read(filename));
	}

	/**
	 * factory: create a NodeInformation instance from a XML file and set the hosted services
	 * 
	 * @param filename A filename to read from
	 * @param serviceAgents A bunch of service agents hosted on this node
	 * @return a node information
	 * @throws MalformedXMLException If the XML data is malformed
	 * @throws IOException If reading the file fails
	 */
	public static NodeInformation createFromXmlFile(String filename, ServiceAgentImpl[] serviceAgents)
			throws MalformedXMLException, IOException {
		NodeInformation result = createFromXmlFile(filename);
		result.setServices(serviceAgents);

		return result;
	}

	/**
	 * factory create a node information instance from an XML string
	 * 
	 * @param xml An XML data string
	 * @return node information contained in the given XML string
	 * @throws MalformedXMLException If the XML data string is malformed
	 */
	public static NodeInformation createFromXml(String xml) throws MalformedXMLException {
		Element root = XmlTools.getRootElement(xml, "las2peerNode");

		NodeInformation result = new NodeInformation();

		try {
			NodeList children = root.getChildNodes();
			for (int c = 0; c < children.getLength(); c++) {
				Node node = children.item(c);
				if (node.getNodeType() != Node.ELEMENT_NODE) {
					// XXX logging
					continue;
				}
				Element child = (Element) node;

				if (child.getTagName().equals("adminName")) {
					result.adminName = child.getTextContent();
				} else if (child.getTagName().equals("adminEmail")) {
					result.adminEmail = child.getTextContent();
				} else if (child.getTagName().equals("organization")) {
					result.organization = child.getTextContent();
				} else if (child.getTagName().equals("description")) {
					result.description = child.getTextContent();
				} else if (child.getTagName().equals("nodeHandle")) {
					Element serializedNodeHandle = XmlTools.getSingularElement(child, "serialized");
					result.nodeHandle = SerializeTools.deserializeBase64(serializedNodeHandle.getTextContent());
				} else if (child.getTagName().equals("nodeKey")) {
					result.nodeKey = (PublicKey) SerializeTools.deserializeBase64(child.getTextContent());
				} else if (child.getTagName().equals("signature")) {
					result.signature = (byte[]) SerializeTools.deserializeBase64(child.getTextContent());
				} else if (child.getTagName().equals("services")) {
					List<ServiceNameVersion> serviceClasses = new ArrayList<ServiceNameVersion>();

					NodeList services = child.getElementsByTagName("serviceClass");
					// logger.info("[niXML] found services tag with " + services.getLength() + " nodes: \n" + child.getTextContent());
					for (int s = 0; s < services.getLength(); s++) {
						Node serviceNode = services.item(s);
						String tagContents = serviceNode.getTextContent();
						// logger.info("[niXML] > parsing child tag #" + s + ": " + tagContents);
						ServiceNameVersion snv = ServiceNameVersion.fromString(tagContents);
						// logger.info("[niXML] > found ServiceNameVersion: " + snv.getName() + " @ " + snv.getVersion().toString());
						serviceClasses.add(snv);
					}

					result.hostedServices = serviceClasses;//.toArray(new ServiceNameVersion[0]);
				} else {
					throw new MalformedXMLException("unknown xml element: " + child.getTagName());
				}

			}
		} catch (SerializationException e) {
			throw new MalformedXMLException("unable to deserialize contents", e);
		}

		return result;
	}

	/**
	 * command line tool for generating a description XML file
	 * 
	 * @param argv A bunch of args
	 */
	public static void main(String argv[]) {
		if (argv.length < 4) {
			System.out.println(
					"Usage: java i5.las2peer.p2p.NodeInformation adminName adminEmail organization description");
			System.exit(0);
		}

		NodeInformation result = new NodeInformation();

		result.description = argv[3];
		result.adminEmail = argv[1];
		result.adminName = argv[0];
		result.organization = argv[2];

		System.out.println(result.toXmlString());
	}

}
