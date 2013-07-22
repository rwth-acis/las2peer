package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client}, if the authentication failed.
 * (i.e. the stated user does not exist or the password has been incorrect.)
 *
 * @author Holger Jan&szlig;en
 */


public class AuthenticationFailedException extends ConnectorClientException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3395962880754457253L;

	public AuthenticationFailedException () {
		super ();
	}
}

