package i5.las2peer.tools;


/**
 * base class for exceptions indicating serialization problems
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
 *
 */
public class SerializationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3616506038095675604L;


	public SerializationException ( String message ) {
		super ( message );
	}

	
	public SerializationException ( String message, Throwable cause ) {
		super ( message, cause);
	}
}

