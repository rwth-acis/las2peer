
package i5.las2peer.httpConnector.client;

/**
 * Exception thrown by the {@link Client} on an invokation if the invokation
 * itself has been successfull but returned an object which could not be shipped
 * via the coders used in the {@link Client}.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 00:27:21 $
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

