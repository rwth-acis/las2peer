package i5.las2peer.tools;


/**
 * exception for cryptografical problems 
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2012/12/11 16:17:44 $
 *
 */
public class CryptoException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1307395129777106542L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public CryptoException ( String message ) {
		super ( message );
	}
	
	
	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public CryptoException ( String message, Throwable cause) {
		super ( message, cause );
	}
	
}
