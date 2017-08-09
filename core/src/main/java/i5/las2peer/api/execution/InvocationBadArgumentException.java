package i5.las2peer.api.execution;

/**
 * Thrown if the service argument validation failed.
 */
public class InvocationBadArgumentException extends InternalServiceException {

	private static final long serialVersionUID = 1L;

	public InvocationBadArgumentException() {
		// TODO Auto-generated constructor stub
	}

	public InvocationBadArgumentException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public InvocationBadArgumentException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public InvocationBadArgumentException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
