package i5.las2peer.tools;


/**
 * Base class for exceptions indicating serialization problems.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class SerializationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3616506038095675604L;


	public SerializationException ( String message ) {
		super ( message );
	}

	
	public SerializationException ( String message, Throwable cause ) {
		super ( message, cause);
	}
}

