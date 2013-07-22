package i5.las2peer.httpConnector.coder;


/**
 * Exception thrown by a {@link XmlDecoder} subclass on problems during decoding
 * of a message.
 *
 * @author Holger Janßen
 */

public class InvalidCodingException extends CodingException
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3990070000012164435L;

	public InvalidCodingException () {
		super();
	}
	
	public InvalidCodingException ( String message ) {
		super ( message );
	}
	
	public InvalidCodingException ( String message, Exception cause ) {
		super ( message, cause );
	}
	
}


