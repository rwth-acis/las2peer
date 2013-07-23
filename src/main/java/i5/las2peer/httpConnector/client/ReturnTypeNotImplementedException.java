
package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client} on an invocation if the invocation
 * itself has been successful but returned an object which could not be shipped
 * via the coders used in the {@link Client}.
 *
 * @author Holger Jan&szlig;en
 */

public class ReturnTypeNotImplementedException extends ConnectorClientException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4012999031007106466L;

	public ReturnTypeNotImplementedException () {
		super ();
	}
	
	public ReturnTypeNotImplementedException ( String mess ) {
		super ();
	}
	
}

