package i5.las2peer.execution;

/**
 * a Service InvocationException is thrown when the invocation of a service method leads to an internal exception
 * 
 * 
 *
 */
public class ServiceInvocationException extends L2pServiceException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -908408442672540604L;

	/**
	 * create a new exception with the original service exception as cause
	 * 
	 * @param message
	 * @param cause
	 */
	public ServiceInvocationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * create a new exception indicating result interpretation problems (no cause)
	 * 
	 * @param message
	 */
	public ServiceInvocationException(String message) {
		super(message);
	}

}
