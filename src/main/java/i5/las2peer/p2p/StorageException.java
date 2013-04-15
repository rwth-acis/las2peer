package i5.las2peer.p2p;


/**
 * exception thrown on any problems with the p2p storage of {@link i5.las2peer.persistency.Envelope}s
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
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
