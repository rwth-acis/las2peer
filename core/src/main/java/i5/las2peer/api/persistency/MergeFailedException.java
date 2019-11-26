package i5.las2peer.api.persistency;

/**
 * Indicates that merging envelopes failed. To be thrown by {@link EnvelopeCollisionHandler}.
 *
 */
public class MergeFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	public MergeFailedException(String message) {
		this(message, null);
	}

	public MergeFailedException(Throwable cause) {
		this("", cause);
	}

	public MergeFailedException(String message, Throwable cause) {
		super(message);
		this.initCause(cause);
	}

}
