package i5.las2peer.classLoaders.libraries;

import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.XmlTools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map.Entry;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class is stored in the network and represents a network library.
 *
 */
public class LoadedNetworkLibrary extends LoadedLibrary implements XmlAble {

	private final PastryNodeImpl node;

	/**
	 * maps the dependency filenames to their contents base64 encoded secure hashes
	 */
	private final HashMap<String, byte[]> dependencies;

	public LoadedNetworkLibrary(PastryNodeImpl node, LibraryIdentifier lib, HashMap<String, byte[]> dependencies) {
		super(lib);
		this.node = node;
		this.dependencies = dependencies;
	}

	@Override
	public URL getResourceAsUrl(String resourceName) throws ResourceNotFoundException {
		// TODO implement get resource as URL in network library
		// return las2peer:// URL or https:// URL?
		throw new ResourceNotFoundException("getResourceAsUrl not implemented, yet", getIdentifier().toString());
	}

	@Override
	InputStream getResourceAsStream(String resourceName) throws ResourceNotFoundException {
		try {
			byte[] bytes = getResourceAsBinary(resourceName);
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			return bais;
		} catch (IOException e) {
			throw new ResourceNotFoundException(resourceName, getIdentifier().toString(), e);
		}
	}

	@Override
	long getSizeOfResource(String resourceName) throws ResourceNotFoundException {
		try {
			return getResourceAsBinary(resourceName).length;
		} catch (IOException e) {
			throw new ResourceNotFoundException(resourceName, getIdentifier().toString(), e);
		}
	}

	@Override
	public byte[] getResourceAsBinary(String resourceName) throws IOException, ResourceNotFoundException {
		byte[] resourceHash = dependencies.get(resourceName);
		if (resourceHash == null) {
			throw new ResourceNotFoundException(resourceName, this.getLibraryIdentifier().toString());
		}
		try {
			return node.fetchHashedContent(resourceHash);
		} catch (EnvelopeNotFoundException e) {
			throw new ResourceNotFoundException(resourceName, getIdentifier().toString(), e);
		} catch (EnvelopeException e) {
			throw new IOException("Could not read class from hashed content", e);
		}
	}

	@Override
	public String toXmlString() throws SerializationException {
		StringBuilder result = new StringBuilder();
		LibraryIdentifier libId = getIdentifier();
		result.append("<las2peer:networklibrary identifier=\"" + XmlTools.escapeAttributeValue(libId.toString())
				+ "\" name=\"" + XmlTools.escapeAttributeValue(libId.getName()) + "\"");
		LibraryVersion version = libId.getVersion();
		if (version != null) {
			result.append(" version=\"" + XmlTools.escapeAttributeValue(version.toString()) + "\"");
		}
		result.append(">\n");
		if (dependencies != null && dependencies.size() > 0) {
			result.append("<dependencies>\n");
			for (Entry<String, byte[]> dep : dependencies.entrySet()) {
				result.append("\t<dependency name=\"" + XmlTools.escapeAttributeValue(dep.getKey())
						+ "\" encoding=\"Base64\">" + Base64.getEncoder().encodeToString(dep.getValue())
						+ "</dependency>\n");
			}
			result.append("</dependencies>\n");
		}
		result.append("</las2peer:networklibrary>\n");
		return result.toString();
	}

	public static LoadedNetworkLibrary createFromXml(PastryNodeImpl node, String xmlStr) throws MalformedXMLException {
		Element root = XmlTools.getRootElement(xmlStr, "las2peer:networklibrary");
		LibraryIdentifier libId;
		String identifier = root.getAttribute("identifier");
		if (identifier != null) {
			libId = new LibraryIdentifier(identifier);
		} else {
			String name = root.getAttribute("name");
			if (name == null) {
				throw new MalformedXMLException("No identifier and no name specified for network library");
			}
			String version = root.getAttribute("version");
			libId = new LibraryIdentifier(name, version);
		}
		HashMap<String, byte[]> libDeps = null;
		Element elDependencies = XmlTools.getOptionalElement(root, "dependencies");
		if (elDependencies != null) {
			libDeps = new HashMap<>();
			NodeList nodeList = elDependencies.getElementsByTagName("dependency");
			for (int c = 0; c < nodeList.getLength(); c++) {
				org.w3c.dom.Node currentNode = nodeList.item(c);
				short nodeType = currentNode.getNodeType();
				if (nodeType != org.w3c.dom.Node.ELEMENT_NODE) {
					throw new MalformedXMLException(
							"Node type (" + nodeType + ") is not type element (" + org.w3c.dom.Node.ELEMENT_NODE + ")");
				}
				Element currentElement = (Element) currentNode;
				String name = currentElement.getAttribute("name");
				if (name == null) {
					throw new MalformedXMLException("Dependency name is null");
				}
				String encoding = currentElement.getAttribute("encoding");
				if (!encoding.equalsIgnoreCase("base64")) {
					throw new MalformedXMLException("Base64 encoding expected, got '" + encoding + "'");
				}
				String base64Hash = currentElement.getTextContent();
				if (base64Hash == null) {
					throw new MalformedXMLException("Dependency hash is null");
				}
				try {
					byte[] hash = Base64.getDecoder().decode(base64Hash);
					libDeps.put(name, hash);
				} catch (IllegalArgumentException e) {
					throw new MalformedXMLException("Could not decode base64 hash", e);
				}
			}
		}
		return new LoadedNetworkLibrary(node, libId, libDeps);
	}

}
