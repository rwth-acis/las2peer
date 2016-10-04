package i5.las2peer.api.exceptions;

/**
 * Represents an exception during a service invocation.
 */
public class ServiceInvocationException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServiceInvocationException() {

	}

	public ServiceInvocationException(String message) {
		super(message);
	}

	public ServiceInvocationException(Throwable cause) {
		super(cause);
	}

	public ServiceInvocationException(String message, Throwable cause) {
		super(message, cause);
	}
}
