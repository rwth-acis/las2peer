package i5.las2peer.p2p.pastry;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.MalformedXMLException;
import rice.p2p.commonapi.NodeHandle;


/**
 * content class for messages to be broadcastd via the pastry ring
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/02/12 18:10:24 $
 *
 */
public class BroadcastMessageContent extends L2pScribeContent {

	private String xmlMessageContent = "";
	
	private transient Message message = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5111771506510680578L;

	public BroadcastMessageContent(NodeHandle fromNode, Message l2pMessage) {
		
		super(fromNode);
		
		xmlMessageContent = l2pMessage.toXmlString();
		message = l2pMessage;
	}
	
	/**
	 * get the l2p message inside
	 * 
	 * @return the contained Las2peer message
	 * 
	 * @throws MalformedXMLException 
	 */
	public Message getMessage () throws MalformedXMLException {
		if ( message == null)
			message = Message.createFromXml(xmlMessageContent);
		
		return message;
	}

}
