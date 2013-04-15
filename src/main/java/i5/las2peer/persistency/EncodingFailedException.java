package i5.las2peer.persistency;


/**
 * exception thrown if an encoding has failed for some reason
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/01/31 11:31:49 $
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
