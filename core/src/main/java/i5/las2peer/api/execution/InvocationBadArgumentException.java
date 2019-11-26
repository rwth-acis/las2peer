package i5.las2peer.api.execution;

/**
 * Thrown if the service argument validation failed.
 */
public class InvocationBadArgumentException extends InternalServiceException {

	private static final long serialVersionUID = 1L;

	public InvocationBadArgumentException() {
	}

	public InvocationBadArgumentException(String message) {
		super(message);
	}

	public InvocationBadArgumentException(Throwable cause) {
		super(cause);
	}

	public InvocationBadArgumentException(String message, Throwable cause) {
		super(message, cause);
	}

}
