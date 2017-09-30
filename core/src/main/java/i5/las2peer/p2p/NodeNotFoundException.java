package i5.las2peer.p2p;

/**
 * Exception thrown on access to a node which is not known to the network.
 * 
 */
public class NodeNotFoundException extends Exception {

	private static final long serialVersionUID = -4914514127957678468L;

	public NodeNotFoundException(String message) {
		super(message);
	}

	public NodeNotFoundException(long id) {
		this("a node with the id " + id + " is not known!");
	}

}
