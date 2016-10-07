package i5.las2peer.p2p;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.VerificationFailedException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.FileContentReader;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import i5.las2peer.tools.XmlTools;

import java.io.IOException;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A NodeInformation gives basic information about a node.
 */
public class NodeInformation implements XmlAble {

	private String organization = null;
	private String adminName = null;
	private String adminEmail = null;
	private String description = "A standard las2peer node -- no further information is provided.";

	private ServiceNameVersion[] hostedServices = new ServiceNameVersion[0];

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
	 * @param hostedServiceAgents
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
	public ServiceNameVersion[] getHostedServices() {
		return hostedServices.clone();
	}

	/**
	 * set the hosted service classes of this node information
	 * 
	 * @param serviceAgents
	 */
	void setServices(ServiceAgentImpl[] serviceAgents) {
		hostedServices = new ServiceNameVersion[serviceAgents.length];

		for (int i = 0; i < hostedServices.length; i++) {
			hostedServices[i] = serviceAgents[i].getServiceNameVersion();
		}
	}

	/**
	 * for the node itself: set the signature before sending
	 * 
	 * @param signature
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

	/**
	 * for the node itself: deliver the handle
	 * 
	 * @param nodeHandle
	 */
	public void setNodeHandle(Serializable nodeHandle) {
		this.nodeHandle = nodeHandle;
	}

	/**
	 * for the node itself: deliver the key
	 * 
	 * @param nodeKey
	 */
	public void setNodeKey(PublicKey nodeKey) {
		this.nodeKey = nodeKey;
	}

	/**
	 * verify the signature
	 * 
	 * @throws L2pSecurityException
	 */
	public void verifySignature() throws L2pSecurityException {
		if (signature == null) {
			throw new L2pSecurityException("No Signature!");
		}

		if (nodeKey == null) {
			throw new L2pSecurityException("No node key!");
		}

		try {
			if (!CryptoTools.verifySignature(signature, getSignatureContent(), nodeKey)) {
				throw new L2pSecurityException("signaure faulty!");
			}
		} catch (VerificationFailedException e) {
			throw new L2pSecurityException("unable to verify signature", e);
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
			result.append("\t<organization>").append(organization).append("</organization>\n");
		}

		if (adminName != null) {
			result.append("\t<adminName>").append(adminName).append("</adminName>\n");
		}

		if (adminEmail != null) {
			result.append("\t<adminEmail>").append(adminEmail).append("</adminEmail>\n");
		}

		result.append("\t<description>").append(description).append("</description>\n");

		if (hostedServices != null && hostedServices.length > 0) {
			result.append("\t<services>\n");

			for (ServiceNameVersion service : hostedServices) {
				result.append("\t\t<serviceClass>").append(service.toString()).append("</serviceClass>\n");
			}

			result.append("\t</services>\n");
		}

		try {
			if (nodeKey != null) {
				result.append("\t<nodeKey encoding=\"base64\">").append(SerializeTools.serializeToBase64(nodeKey))
						.append("</nodeKey>\n");
			}

			if (signature != null) {
				result.append("\t<signature encoding=\"base64\">").append(SerializeTools.serializeToBase64(signature))
						.append("</signature>\n");
			}

			if (nodeHandle != null) {
				result.append("\t<nodeHandle>\n").append("\t\t<plain><![CDATA[").append(nodeHandle.toString())
						.append("]]></plain>\n").append("\t\t<serialized encoding=\"base64\">")
						.append(SerializeTools.serializeToBase64(nodeHandle)).append("</serialized>\n")
						.append("\t</nodeHandle>\n");
			}
		} catch (SerializationException e) {
			throw new RuntimeException("critical: should not occur!");
		}

		result.append("</las2peerNode>\n");

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
	 * @param filename
	 * @return the node information contained in the given XML file
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static NodeInformation createFromXmlFile(String filename) throws MalformedXMLException, IOException {
		return createFromXml(FileContentReader.read(filename));
	}

	/**
	 * factory: create a NodeInformation instance from a XML file and set the hosted services
	 * 
	 * @param filename
	 * @param serviceAgents
	 * @return a node information
	 * 
	 * 
	 * @throws MalformedXMLException
	 * @throws IOException
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
	 * @param xml
	 * @return node information contained in the given XML string
	 * @throws MalformedXMLException
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
					Vector<String> serviceClasses = new Vector<String>();
					NodeList services = child.getChildNodes();
					for (int s = 0; s < services.getLength(); s++) {
						Node serviceNode = services.item(s);
						if (node.getNodeType() != Node.ELEMENT_NODE) {
							// XXX logging
							continue;
						}
						Element service = (Element) serviceNode;
						if (!service.getTagName().equals("serviceClass")) {
							throw new MalformedXMLException(service + " is not a service class element");
						}
						serviceClasses.add(service.getTextContent());
					}

					result.hostedServices = serviceClasses.toArray(new ServiceNameVersion[0]);
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
	 * @param argv
	 */
	public static void main(String argv[]) {
		if (argv.length < 4) {
			System.out
					.println("Usage: java i5.las2peer.p2p.NodeInformation adminName adminEmail organization description");
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
