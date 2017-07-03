package i5.las2peer.security;

/**
 * Represents internal security exceptions.
 * 
 */
public class InternalSecurityException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1673893263860540906L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public InternalSecurityException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public InternalSecurityException(String message, Throwable cause) {
		super(message, cause);
	}

}
