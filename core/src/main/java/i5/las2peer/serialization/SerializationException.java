package i5.las2peer.serialization;

/**
 * Base class for exceptions indicating serialization problems.
 */
public class SerializationException extends Exception {

	private static final long serialVersionUID = 1L;

	public SerializationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public SerializationException(String msg) {
		super(msg);
	}

	public SerializationException(Throwable cause) {
		super(cause);
	}

}
