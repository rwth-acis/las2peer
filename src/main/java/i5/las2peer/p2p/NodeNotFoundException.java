package i5.las2peer.p2p;


/**
 * exception thrown on access to a node which is not knwon to the network
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2012/12/11 12:33:32 $
 *
 */
public class NodeNotFoundException extends Exception {
	/**
	 * serialization id
	 */
	private static final long serialVersionUID = -4914514127957678468L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public NodeNotFoundException ( String message ) {
		super ( message );
	}
	
	
	/**
	 * create a new exception
	 * @param id	id of the node, to which no access is possible
	 */
	public NodeNotFoundException ( long id ) {
		this ( "a node with the id " + id + " is not known!");
	}
}
