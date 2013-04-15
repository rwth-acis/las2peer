package i5.las2peer.api;


/**
 * basic exception for connectors
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/01/22 14:06:46 $
 *
 */
public class ConnectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4867021553376111059L;

	/**
	 * create a new exception
	 * @param message
	 */
	public ConnectorException ( String message ) {
		super (message );
	}
	
	
	
	/**
	 * create a new exception
	 * @param message
	 * @param cause
	 */
	public ConnectorException ( String message, Throwable cause ) {
		super ( message, cause);
	}
}
