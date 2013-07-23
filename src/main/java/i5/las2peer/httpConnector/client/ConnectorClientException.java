package i5.las2peer.httpConnector.client;

/**
 * Base class for all exceptions thrown by the {@link Client}.
 *
 * @author Holger Jan&szlig;en
 */

public class ConnectorClientException extends Exception
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8822984983692866764L;

	public ConnectorClientException () {
		super ();
	}
	
	public ConnectorClientException ( String message ) {
		super ( message );
	}
	
	public ConnectorClientException ( String message, Exception cause ){
		super ( message, cause );
	}
	
	public ConnectorClientException ( Exception cause ) {
		super ( cause );
	}
	
}

