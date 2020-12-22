package i5.las2peer.connectors;

/**
 * Basic exception for connectors.
 * 
 * 
 *
 */
public class ConnectorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4867021553376111059L;

	/**
	 * create a new exception
	 * 
	 * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 */
	public ConnectorException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
	 *            method.
	 * @param cause cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
	 *            <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public ConnectorException(String message, Throwable cause) {
		super(message, cause);
	}
}
