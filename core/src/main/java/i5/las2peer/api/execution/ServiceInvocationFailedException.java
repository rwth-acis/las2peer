package i5.las2peer.api.execution;

/**
 * Thrown if the service invocation failed for some reason.
 *
 */
public class ServiceInvocationFailedException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;

	public ServiceInvocationFailedException() {

	}

	public ServiceInvocationFailedException(String message) {
		super(message);
	}

	public ServiceInvocationFailedException(Throwable cause) {
		super(cause);
	}

	public ServiceInvocationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}

