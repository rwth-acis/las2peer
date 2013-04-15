package i5.las2peer.persistency;

/**
 * exception thrown on failed cryptografic signature verifications 
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2012/12/11 12:33:32 $
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
