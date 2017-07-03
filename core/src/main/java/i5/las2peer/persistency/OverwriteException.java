package i5.las2peer.persistency;

import i5.las2peer.api.persistency.EnvelopeException;

/**
 * A simple exception thrown on a forbidden attempt to overwrite an envelope.
 * 
 *
 */
public class OverwriteException extends EnvelopeException {

	private static final long serialVersionUID = -1478686384798433233L;

	public OverwriteException(String message) {
		super(message);
	}

}
