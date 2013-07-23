package i5.las2peer.communication;


/**
 * exception thrown on problems to handle messages inside an agent
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class MessageException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -269381775118048818L;

	public MessageException ( String message ) {
		super (message);
	}
	
	public MessageException ( String message, Throwable cause ) {
		super ( message, cause );
	}
}
