package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client}, if the remote server
 * complains about the access rights of the current user during an invocation.
 *
 * @author Holger Jan&szlig;en
 */

public class AccessDeniedException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -116711676976993518L;

	public AccessDeniedException () {
		super();
	}
	
}

