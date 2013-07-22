package i5.las2peer.p2p;


/**
 * Exception thrown on any problems with the p2p storage of {@link i5.las2peer.persistency.Envelope}s.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class StorageException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7247822441102127808L;

	public StorageException ( String message ) {
		super (message );
	}
	
	public StorageException ( String message, Throwable cause ) {
		super ( message, cause);
	}
	
}
