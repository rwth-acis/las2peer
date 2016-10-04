package i5.las2peer.api.exceptions;

/**
 * Thrown if the service throwed an exception.
 */
public class RemoteServiceException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;

	public RemoteServiceException() {

	}

	public RemoteServiceException(String message) {
		super(message);
	}

	public RemoteServiceException(Throwable cause) {
		super(cause);
	}

	public RemoteServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
