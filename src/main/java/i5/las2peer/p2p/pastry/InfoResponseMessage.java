package i5.las2peer.p2p.pastry;

import i5.las2peer.p2p.NodeInformation;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;


/**
 * this message is sent from node to node as response to an {@link GetInfoMessage}
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/18 02:00:17 $
 *
 */
public class InfoResponseMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6095970420776244161L;
	
	
	private long responseTo;
	
	private NodeHandle sender;

	private String xmlContent = "hallo";
	

	/**
	 * create a new message
	 * 
	 * @param responseTo
	 * @param sendingNode
	 * @param xmlContent
	 */
	public InfoResponseMessage ( long responseTo, NodeHandle sendingNode, String xmlContent ) {
		this.responseTo = responseTo;
		this.sender = sendingNode;
		
		this.xmlContent = xmlContent;
	}
	
	/**
	 * create a new Message 
	 * 
	 * @param resonseTo
	 * @param sendingNode
	 * @param content
	 */
	public InfoResponseMessage ( long resonseTo, NodeHandle sendingNode, NodeInformation content ) {
		this ( resonseTo, sendingNode, content.toXmlString () );
	}

	/**
	 * get the id of the message, this is a response to
	 * @return message id
	 */
	public long getResponseToId () { 
		return responseTo;
	}
	
	/**
	 * get the handle of the sending node
	 * 
	 * @return message sender
	 */
	public NodeHandle getSender () {
		return sender;
	}
	
	/**
	 * get the delivered node information 
	 * 
	 * @return node information
	 */
	public NodeInformation getInfoContent () {
		try {
			return NodeInformation.createFromXml(xmlContent);
		} catch (Exception e) {
			e.printStackTrace();
			// should not occur -- faulty message!
			return null;
		}
	}
	
	
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

}
