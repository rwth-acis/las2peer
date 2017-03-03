package i5.las2peer.serialization;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Simple <i>static</i> class collecting useful methods for XML (de-)serialization.
 * 
 * 
 *
 */
public class XmlTools {

	/**
	 * Gets the root element from the given XML String and throws an exception if the name does not match with the given
	 * name.
	 * 
	 * @param xml The XML String that should be parsed.
	 * @param rootElementName The tag name of the root element. CASE SENSITIVE
	 * @return Returns the root element with the given tag name.
	 * @throws MalformedXMLException If the root element does not have the given name or multiple root elements exist.
	 */
	public static Element getRootElement(String xml, String rootElementName) throws MalformedXMLException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
			return getRootElement(doc, rootElementName);
		} catch (ParserConfigurationException | IOException | SAXException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}

	/**
	 * Gets the root element from the given file containing only ONE XML representation and throws an exception if the
	 * name does not match with the given name.
	 * 
	 * @param xmlFile The file containing one XML representation that should be parsed.
	 * @param rootElementName The tag name of the root element. CASE SENSITIVE
	 * @return Returns the root element with the given tag name.
	 * @throws MalformedXMLException If the root element does not have the given name or multiple root elements exist.
	 */
	public static Element getRootElement(File xmlFile, String rootElementName) throws MalformedXMLException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			return getRootElement(doc, rootElementName);
		} catch (ParserConfigurationException | IOException | SAXException e) {
			throw new MalformedXMLException("Error parsing xml string", e);
		}
	}

	/**
	 * Gets the root element from the given document type containing only ONE XML representation and throws an exception
	 * if the name does not match with the given name.
	 * 
	 * @param document The document type containing one XML representation that should be parsed.
	 * @param rootElementName The tag name of the root element. CASE SENSITIVE
	 * @return Returns the root element with the given tag name.
	 * @throws MalformedXMLException If the root element does not have the given name or multiple root elements exist.
	 */
	public static Element getRootElement(Document document, String rootElementName) throws MalformedXMLException {
		document.getDocumentElement().normalize();
		NodeList nList = document.getElementsByTagName(rootElementName);
		int len = nList.getLength();
		if (len != 1) {
			throw new MalformedXMLException("Exactly one element '" + rootElementName
					+ "' per XML document expected! Found: " + len);
		}
		org.w3c.dom.Node rootNode = nList.item(0);
		String rootNodeName = rootNode.getNodeName();
		if (!rootNodeName.equalsIgnoreCase(rootElementName)) {
			throw new MalformedXMLException("This is not an " + rootElementName + " but a " + rootNodeName);
		}
		short rootNodeType = rootNode.getNodeType();
		if (rootNodeType != org.w3c.dom.Node.ELEMENT_NODE) {
			throw new MalformedXMLException("Root node type (" + rootNodeType + ") is not type element ("
					+ org.w3c.dom.Node.ELEMENT_NODE + ")");
		}
		return (Element) rootNode;
	}

	/**
	 * Gets exactly one child tag from given parent XML element. Otherwise throws an exception.
	 * 
	 * @param parent The parent XML element.
	 * @param tagName The tag name of the singular child element. CASE SENSITIVE
	 * @return Returns the child element with the given tag name.
	 * @throws MalformedXMLException If not exactly one child has the specified tag name or it's not a node itself.
	 */
	public static Element getSingularElement(Element parent, String tagName) throws MalformedXMLException {
		NodeList nodeList = parent.getElementsByTagName(tagName);
		int len = nodeList.getLength();
		if (len != 1) {
			throw new MalformedXMLException("Exactly one '" + tagName + "' element expected! Found: " + len);
		}
		org.w3c.dom.Node firstNode = nodeList.item(0);
		short nodeType = firstNode.getNodeType();
		if (nodeType != org.w3c.dom.Node.ELEMENT_NODE) {
			throw new MalformedXMLException("Node type (" + nodeType + ") is not type element ("
					+ org.w3c.dom.Node.ELEMENT_NODE + ")");
		}
		return (Element) firstNode;
	}

	/**
	 * Gets one optional child tag from given parent XML element. If more than one child matches an exception is thrown.
	 * 
	 * @param parent The parent XML element.
	 * @param tagName The tag name of the optional child element. CASE SENSITIVE
	 * @return Returns the child element with the given tag name or null if no child matches the given tag name.
	 * @throws MalformedXMLException If more than one child has the specified tag name or it's not a node itself.
	 */
	public static Element getOptionalElement(Element parent, String tagName) throws MalformedXMLException {
		NodeList nodeList = parent.getElementsByTagName(tagName);
		int len = nodeList.getLength();
		if (len > 1) {
			throw new MalformedXMLException("Only one '" + tagName + "' element expected!");
		} else if (len == 1) {
			org.w3c.dom.Node firstNode = nodeList.item(0);
			short nodeType = firstNode.getNodeType();
			if (nodeType != org.w3c.dom.Node.ELEMENT_NODE) {
				throw new MalformedXMLException("Node type (" + nodeType + ") is not type element ("
						+ org.w3c.dom.Node.ELEMENT_NODE + ")");
			}
			return (Element) firstNode;
		} else {
			// no child element with tag name found
			return null;
		}
	}

	public static String escapeString(String str) {
		return str.replace("&", "&amp;").replace("'", "&apos;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static String escapeAttributeValue(String attribute) {
		return escapeString(attribute).replace("\"", "&quot;");
	}

}
