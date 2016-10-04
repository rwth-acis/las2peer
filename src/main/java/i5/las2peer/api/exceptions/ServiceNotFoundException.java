package i5.las2peer.api.exceptions;

/**
 * Thrown if the service has not been found.
 */
public class ServiceNotFoundException extends ServiceInvocationException {
	private static final long serialVersionUID = 1L;

	public ServiceNotFoundException() {

	}

	public ServiceNotFoundException(String message) {
		super(message);
	}

	public ServiceNotFoundException(Throwable cause) {
		super(cause);
	}

	public ServiceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
