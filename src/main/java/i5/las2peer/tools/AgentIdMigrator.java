package i5.las2peer.tools;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.AgentImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;

import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public class AgentIdMigrator {

	public static void main(String[] args) throws MalformedXMLException, DOMException, SerializationException,
			IOException {
		if (args.length < 1) {
			System.out.println("Usage: agent-filename.xml [agent-filename.xml...]");
			System.exit(1);
		} else {
			// change agent id to derived one from public key
			for (String agentFilename : args) {
				migrateAgentXMLFile(agentFilename);
			}
		}
	}

	public static void migrateAgentXMLFile(String xmlFilename) throws MalformedXMLException, DOMException,
			SerializationException, IOException {
		System.out.println("Migrating XML file: " + xmlFilename);
		Element root = XmlTools.getRootElement(new File(xmlFilename), "las2peer:agent");
		replaceAgentId(root);
		AgentImpl migratedAgent = AgentImpl.createFromXml(root);
		String migratedXMLStr = migratedAgent.toXmlString();
		FileWriter fw = new FileWriter(xmlFilename);
		fw.write(migratedXMLStr);
		fw.close();
	}

	public static void replaceAgentId(Element root) throws MalformedXMLException, DOMException, SerializationException {
		Element elId = XmlTools.getSingularElement(root, "id");
		Element pubKey = XmlTools.getSingularElement(root, "publickey");
		if (!pubKey.getAttribute("encoding").equals("base64")) {
			throw new MalformedXMLException("base64 encoding expected");
		}
		PublicKey publicKey = (PublicKey) SerializeTools.deserializeBase64(pubKey.getTextContent());
		String safeId = CryptoTools.publicKeyToSHA512(publicKey);
		System.out.println("Replace agent id '" + elId.getTextContent() + "' with safe agent id '" + safeId + "'");
		elId.setTextContent(safeId);
	}

}
