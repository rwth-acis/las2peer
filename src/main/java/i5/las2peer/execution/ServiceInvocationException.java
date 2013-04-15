package i5.las2peer.execution;

import i5.las2peer.execution.L2pServiceException;


/**
 * a Service InvocationException is thrown when the invocation of a service
 * method leads to an internal exception
 * 
 * @author Holger Janßen
 * @version $Revision: 1.1 $, $Date: 2013/01/25 14:19:18 $
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
	public ServiceInvocationException ( String message, Throwable cause ) {
		super ( message, cause );
	}
	
	/**
	 * create a new exception indicating result interpretation problems
	 * (no cause) 
	 * @param message
	 */
	public ServiceInvocationException ( String message ) {
		super ( message );
	}

	
	
}
