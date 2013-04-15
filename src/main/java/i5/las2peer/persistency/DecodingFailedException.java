package i5.las2peer.persistency;


/**
 * exception throw, if the decoding has failed for some reason
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/01/31 11:31:49 $
 *
 */
public class DecodingFailedException extends EnvelopeException {

	/**
	 *	serialization id 
	 */
	private static final long serialVersionUID = 1740406171454807879L;

	
	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public DecodingFailedException ( String message ) {
		super ( message );
	}
	
	
	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public DecodingFailedException (String message, Throwable cause ) {
		super ( message, cause );
	}
	
}
