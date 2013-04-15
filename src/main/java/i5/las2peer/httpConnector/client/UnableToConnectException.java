package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client} on connection problems.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 00:27:21 $
 */

public class UnableToConnectException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8067761887954458238L;


	public UnableToConnectException () {
		super ();
	}
	
	public UnableToConnectException ( Exception cause ) {
		super ( cause );
	}
	
	public UnableToConnectException ( String message ) {
		super ( message );
	}
	
	
	public UnableToConnectException ( String message, Exception cause ) {
		super ( message, cause );
	}
}

