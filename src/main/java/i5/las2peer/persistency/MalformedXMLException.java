package i5.las2peer.persistency;


/**
 * exception for problems with xml deserialization
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2012/12/11 12:33:32 $
 *
 */
public class MalformedXMLException extends Exception {

	/**
	 * serialization id
	 */
	private static final long serialVersionUID = -5805964819905172422L;
	
	
	/**
	 * create a new exception
	 * @param message
	 */
	public MalformedXMLException ( String message ) {
		super ( message );
	}
	
	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public MalformedXMLException ( String message, Throwable cause ) {
		super ( message, cause );
	}

}
