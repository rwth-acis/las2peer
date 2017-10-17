package i5.las2peer.persistency;

/**
 * Exception thrown on failed cryptografic signature verifications.
 * 
 */
public class VerificationFailedException extends Exception {

	private static final long serialVersionUID = 8790215226557335143L;

	public VerificationFailedException(String message) {
		super(message);
	}

	public VerificationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
