package i5.las2peer.p2p.pastry;

import i5.las2peer.persistency.MalformedXMLException;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


/**
 *	a simple envelope for sending las2peer messages through the pastry network
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class MessageEnvelope implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1930876529152356245L;
	
	
	private String content;
	
	private NodeHandle sendingNode;

	
	/**
	 * create a message envelope with simple string content
	 * 
	 * @param sendingNode
	 * @param content
	 */
	public MessageEnvelope ( NodeHandle sendingNode, String content ) {
		this.content = content;
		this.sendingNode = sendingNode;
	}
	
	
	/**
	 * generate an Pastry message envelope from a las2peer message
	 * 
	 * @param sendingNode
	 * @param content
	 */
	public MessageEnvelope ( NodeHandle sendingNode, i5.las2peer.communication.Message content ) {
		this.content = content.toXmlString();
		this.sendingNode = sendingNode;
	}
	
	
	/**
	 * get the content string of this message
	 * 
	 * @return	the contained data as String
	 */
	public String getContent () {
		return content;
	}
	
	/**
	 * get a handle to the sending node
	 * 
	 * @return	handle to the sending (pastry) node
	 */
	public NodeHandle getSendingNode () {
		return sendingNode;
	}
	
	
	/**
	 * get the contained las2peer message
	 * 
	 * @return	the contained las2peer message
	 * @throws MalformedXMLException
	 */
	public i5.las2peer.communication.Message getContainedMessage () throws MalformedXMLException {
		return i5.las2peer.communication.Message.createFromXml(content);
	}
	
	
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
}
