package i5.las2peer.api;


/**
 * Basic exception for connectors.
 * 
 * 
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
