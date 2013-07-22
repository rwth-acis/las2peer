package i5.las2peer.httpConnector.client;

/**
 * Exception thrown if the {@link Client} is not able to interpret the answer
 * of the server.
 *
 * @author Holger Jan&szlig;en
 */

public class InvalidServerAnswerException extends ConnectorClientException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4695080845712779101L;

	public InvalidServerAnswerException ( String message ) {
		super ( message );
	}
	
}

