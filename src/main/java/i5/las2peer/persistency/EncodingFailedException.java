package i5.las2peer.persistency;


/**
 * Exception thrown if an encoding has failed for some reason.
 * 
 * 
 *
 */
public class EncodingFailedException extends EnvelopeException {

	/**
	 * serialization id
	 */
	private static final long serialVersionUID = 759243476581752536L;
	
	/**
	 * create a new exception
	 * @param message
	 */
	public EncodingFailedException ( String message ) {
		super ( message );
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public EncodingFailedException ( String message, Throwable cause ) {
		super ( message, cause );
	}

}
