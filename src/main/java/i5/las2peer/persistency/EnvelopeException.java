package i5.las2peer.persistency;


/**
 * base class for all exceptions concerning {@link Envelope}s
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
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
