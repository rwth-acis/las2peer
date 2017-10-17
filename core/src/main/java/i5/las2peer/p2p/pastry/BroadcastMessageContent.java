package i5.las2peer.p2p.pastry;

import i5.las2peer.communication.Message;
import i5.las2peer.serialization.MalformedXMLException;
import rice.p2p.commonapi.NodeHandle;

/**
 * content class for messages to be broadcasted via the pastry ring
 * 
 */
public class BroadcastMessageContent extends L2pScribeContent {

	private static final long serialVersionUID = 5111771506510680578L;

	private final String xmlMessageContent;
	private final transient Message message;

	public BroadcastMessageContent(NodeHandle fromNode, Message l2pMessage) {
		super(fromNode);
		xmlMessageContent = l2pMessage.toXmlString();
		message = l2pMessage;
	}

	/**
	 * get the l2p message inside
	 * 
	 * @return the contained Las2peer message
	 * @throws MalformedXMLException If the XML data string is malformed
	 */
	public Message getMessage() throws MalformedXMLException {
		if (message == null) {
			return Message.createFromXml(xmlMessageContent);
		} else {
			return message;
		}
	}

}
