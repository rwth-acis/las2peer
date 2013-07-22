package i5.las2peer.httpConnector.client;

/**
 * ConnectorClientException wrapper for the ParameterTypeNotImplementedException
 * of the coder package.
 *
 * This exception will be thrown by the client, if a user tries to send an
 * invokation parameter which type is not sendable via this connector.
 *
 *
 * @author Holger Jan&szlig;en
 */


public class ParameterTypeNotImplementedException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7305227514839056712L;

	public ParameterTypeNotImplementedException ( String message, Exception cause ) {
		super (message, cause);
	}
	
}

