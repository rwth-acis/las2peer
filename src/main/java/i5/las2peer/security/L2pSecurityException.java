package i5.las2peer.security;


/**
 * base class for all security related exceptions in the las2peer setting
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2012/12/11 16:17:44 $
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
