package i5.las2peer.api.execution;

/**
 * Thrown if the service could not find the requested resource.
 * 
 * related: javax.ws.rs.NotFoundException
 */
public class ResourceNotFoundException extends InternalServiceException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException() {
	}

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
