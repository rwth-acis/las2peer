package i5.las2peer.persistency;

/**
 * Exception thrown on failed cryptografic signature verifications.
 * 
 * 
 *
 */
public class VerificationFailedException extends Exception {

	/**
	 * serialization id
	 */
	private static final long serialVersionUID = 8790215226557335143L;
	
	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public VerificationFailedException ( String message ) {
		super ( message );
	}
	
	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public VerificationFailedException ( String message, Throwable cause ) {
		super ( message, cause );
	}

}
