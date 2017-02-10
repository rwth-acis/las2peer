package i5.las2peer.classLoaders.libraries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.NodeStorageInterface;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.XmlTools;

/**
 * This class is stored as meta information in the network and represents a network library. All getter refer to
 * internal meta information.
 *
 */
public class LoadedNetworkLibrary extends LoadedLibrary implements XmlAble {

	private final NodeStorageInterface node;

	public LoadedNetworkLibrary(NodeStorageInterface node, LibraryIdentifier lib,
			LibraryDependency[] initialDependencies) {
		super(lib, initialDependencies);
		this.node = node;
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
		String clsEnvId = SharedStorageRepository.getFileEnvelopeIdentifier(this.getIdentifier(), resourceName);
		try {
			Envelope clsEnv = node.fetchEnvelope(clsEnvId);
			return (byte[]) clsEnv.getContent();
		} catch (ArtifactNotFoundException e) {
			throw new ResourceNotFoundException(resourceName, getIdentifier().toString(), e);
		} catch (StorageException | SerializationException | L2pSecurityException | CryptoException e) {
			throw new IOException("Could not read class from envelope", e);
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
		LibraryDependency[] deps = getDependencies();
		if (deps != null && deps.length > 0) {
			result.append("<dependencies>\n");
			for (LibraryDependency dep : getDependencies()) {
				result.append("\t<dependency>" + XmlTools.escapeString(dep.toString()) + "</dependency>\n");
			}
			result.append("</dependencies>\n");
		}
		result.append("</las2peer:networklibrary>\n");
		return result.toString();
	}

	public static LoadedNetworkLibrary createFromXml(NodeStorageInterface node, String xmlStr)
			throws MalformedXMLException {
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
		LibraryDependency[] libDeps = null;
		Element elDependencies = XmlTools.getOptionalElement(root, "dependencies");
		if (elDependencies != null) {
			ArrayList<LibraryDependency> depList = new ArrayList<>();
			NodeList deps = elDependencies.getElementsByTagName("dependency");
			for (int c = 0; c < deps.getLength(); c++) {
				depList.add(new LibraryDependency(deps.item(c).getTextContent()));
			}
			libDeps = depList.toArray(new LibraryDependency[depList.size()]);
		}
		return new LoadedNetworkLibrary(node, libId, libDeps);
	}

}
