package i5.las2peer.api;

/**
 * Thrown if the service detected an error during starting or stopping a service.
 * 
 * Must not be thrown during service invocations.
 *
 */
public class ServiceException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServiceException() {

	}

	public ServiceException(String message) {
		super(message);
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
