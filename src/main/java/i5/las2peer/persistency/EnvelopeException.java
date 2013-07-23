package i5.las2peer.persistency;


/**
 * Base class for all exceptions concerning {@link Envelope}s.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class EnvelopeException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8048161202276767878L;

	public EnvelopeException ( String message ,Throwable cause ) {
		super ( message, cause);
	}
	
	public EnvelopeException ( String message ) {
		super ( message ) ;
	}
}
