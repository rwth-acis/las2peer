package i5.las2peer.security;


/**
 * Base class for all security related exceptions in the LAS2peer setting.
 * 
 * 
 *
 */
public class L2pSecurityException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1673893263860540906L;
	
	/**
	 * create a new exception
	 * @param message
	 */
	public L2pSecurityException ( String message ) {
		super ( message );
	}

	/**
	 * create a new exception
	 * @param message
	 * @param cause
	 */
	public L2pSecurityException ( String message, Throwable cause ) {
		super ( message, cause );
	}

}
