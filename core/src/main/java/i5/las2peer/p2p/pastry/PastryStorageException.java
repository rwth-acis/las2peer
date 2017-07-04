package i5.las2peer.p2p.pastry;

import i5.las2peer.api.persistency.EnvelopeException;

/**
 * Exception thrown on failures in the underlying pastry storage
 * 
 */
public class PastryStorageException extends EnvelopeException {

	private static final long serialVersionUID = 3043090090067718209L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public PastryStorageException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public PastryStorageException(String message, Throwable cause) {
		super(message, cause);
	}

}
