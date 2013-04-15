package i5.las2peer.httpConnector.client;

/**
 * Thrown by the (@see Client) if a requested service or service method is not
 * found at the remote server
 *
 * @author Holger Jan�en
 * @version 	$Revision: 1.1 $, $Date: 2013/01/23 00:27:21 $
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

