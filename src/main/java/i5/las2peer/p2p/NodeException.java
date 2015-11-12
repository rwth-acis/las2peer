package i5.las2peer.p2p;

/**
 * base class for any exception occurring in a {@link Node}
 * 
 * 
 *
 */
public class NodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3790557488934068251L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public NodeException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public NodeException(String message, Throwable cause) {
		super(message, cause);
	}

}
