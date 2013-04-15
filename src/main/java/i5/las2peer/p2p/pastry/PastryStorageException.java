package i5.las2peer.p2p.pastry;

import i5.las2peer.p2p.StorageException;


/**
 * exception thrown on failures in the underlying pastry storage
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/01/15 18:18:52 $
 *
 */
public class PastryStorageException extends StorageException {

	/**
	 * serialization id 
	 */
	private static final long serialVersionUID = 3043090090067718209L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public PastryStorageException ( String message ) {
		super ( message );
	}
	
	
	/** 
	 * create a new exception 
	 * @param message
	 * @param cause
	 */
	public PastryStorageException ( String message, Throwable cause) {
		super ( message, cause);
	}
	
}
