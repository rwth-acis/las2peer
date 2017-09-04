package i5.las2peer.api.execution;

/**
 * Thrown if access to the service (method) has been denied.
 * 
 * May also be thrown by a service to indicate that the main agent does have access to the method or accessed content.
 *
 */
public class ServiceAccessDeniedException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;

	public ServiceAccessDeniedException() {

	}

	public ServiceAccessDeniedException(String message) {
		super(message);
	}

	public ServiceAccessDeniedException(Throwable cause) {
		super(cause);
	}

	public ServiceAccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
}
