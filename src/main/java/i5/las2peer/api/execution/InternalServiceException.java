package i5.las2peer.api.execution;

/**
 * Thrown if the service itself has thrown an exception.
 */
public class InternalServiceException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;

	public InternalServiceException() {

	}

	public InternalServiceException(String message) {
		super(message);
	}

	public InternalServiceException(Throwable cause) {
		super(cause);
	}

	public InternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
