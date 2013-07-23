package i5.las2peer.httpConnector.client;

/**
 * Thrown by the (@see Client) if a requested service or service method is not
 * found at the remote server
 *
 * @author Holger Jan&szlig;en
 */


public class NotFoundException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1619083329523860232L;

	public NotFoundException () {
		super ();
	}
	
}

