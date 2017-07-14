package i5.las2peer.serialization;

/**
 * Exception for problems with XML-deserialization.
 * 
 * 
 *
 */
public class MalformedXMLException extends Exception {

	/**
	 * serialization id
	 */
	private static final long serialVersionUID = -5805964819905172422L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public MalformedXMLException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public MalformedXMLException(String message, Throwable cause) {
		super(message, cause);
	}

}
