package i5.las2peer.registry.exceptions;

/**
 * Thrown when a registry lookup (of users, services, etc.) fails because the
 * entry does not exist.
 */
public class NotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public NotFoundException() {
	}

	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException(Throwable cause) {
		super(cause);
	}

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
