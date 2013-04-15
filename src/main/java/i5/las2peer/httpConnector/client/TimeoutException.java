package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client} on timeouts of the usesd session.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 00:27:21 $
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

