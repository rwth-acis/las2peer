package i5.las2peer.tools;

/**
 * This exception is thrown if an issues occurs with a service package (jar) or while handling its dependencies. Usually
 * during the upload process.
 *
 */
public class ServicePackageException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServicePackageException(String message) {
		super(message);
	}

	public ServicePackageException(Throwable cause) {
		super(cause);
	}

	public ServicePackageException(String message, Throwable cause) {
		super(message, cause);
	}

}
