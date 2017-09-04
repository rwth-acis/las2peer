package i5.las2peer.api.execution;

/**
 * Thrown if access to the service requires a logged in user.
 * 
 * May also be thrown by a service to indicate that the main agent is anonymous agent.
 *
 */
public class ServiceNotAuthorizedException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;

	public ServiceNotAuthorizedException() {

	}

	public ServiceNotAuthorizedException(String message) {
		super(message);
	}

	public ServiceNotAuthorizedException(Throwable cause) {
		super(cause);
	}

	public ServiceNotAuthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}
