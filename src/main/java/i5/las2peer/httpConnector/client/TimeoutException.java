package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client} on timeouts of the used session.
 *
 * @author Holger Jan&szlig;en
 */

public class TimeoutException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7754364836838905532L;

	public TimeoutException () {
		super ();
	}
	
}

