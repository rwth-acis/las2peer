package i5.las2peer.persistency;

import i5.las2peer.security.L2pSecurityException;


/**
 * A simple exception thrown on a forbidden attempt to overwrite an envelope.
 * @author Holger Jan&szlig;en
 *
 */
public class OverwriteException extends L2pSecurityException {
	
	private static final long serialVersionUID = -1478686384798433233L;

	public OverwriteException(String message) {
		super(message);
	}

}
