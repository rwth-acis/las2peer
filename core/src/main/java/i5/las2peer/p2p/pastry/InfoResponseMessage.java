package i5.las2peer.p2p.pastry;

import i5.las2peer.p2p.NodeInformation;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * this message is sent from node to node as response to an {@link GetInfoMessage}
 * 
 */
public class InfoResponseMessage implements Message {

	private static final long serialVersionUID = 6095970420776244161L;

	private final long responseTo;
	private final NodeHandle sender;
	private final String xmlContent;

	/**
	 * create a new message
	 * 
	 * @param responseTo A message reference id
	 * @param sendingNode A sending node handle
	 * @param xmlContent An XML data content string
	 */
	public InfoResponseMessage(long responseTo, NodeHandle sendingNode, String xmlContent) {
		this.responseTo = responseTo;
		this.sender = sendingNode;
		this.xmlContent = xmlContent;
	}

	/**
	 * create a new Message
	 * 
	 * @param responseTo A message reference id
	 * @param sendingNode A sending node handle
	 * @param content A node information content
	 */
	public InfoResponseMessage(long responseTo, NodeHandle sendingNode, NodeInformation content) {
		this(responseTo, sendingNode, content.toXmlString());
	}

	/**
	 * get the id of the message, this is a response to
	 * 
	 * @return message id
	 */
	public long getResponseToId() {
		return responseTo;
	}

	/**
	 * get the handle of the sending node
	 * 
	 * @return message sender
	 */
	public NodeHandle getSender() {
		return sender;
	}

	/**
	 * get the delivered node information
	 * 
	 * @return node information
	 */
	public NodeInformation getInfoContent() {
		try {
			return NodeInformation.createFromXml(xmlContent);
		} catch (Exception e) {
			System.out.println("trying to read NodeInformation from: " + xmlContent);
			e.printStackTrace();
			// should not occur -- faulty message!
			return null;
		}
	}

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

}
