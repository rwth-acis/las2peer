package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the (@link Client#invoke) method on Exception that occurred
 * inside the requested method.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 00:27:21 $
 */

public class ServerErrorException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5861786162629395823L;

	public ServerErrorException () {
		super ();
	}
	
	public ServerErrorException ( String message ) {
		super ( message ) ;
	}
	
	public ServerErrorException ( Exception cause ) {
		super ( "Remote error in invoking the requested method. See cause for details", cause );
	}
	
}

