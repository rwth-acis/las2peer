package i5.las2peer.execution;


/**
 * exception thrown on access to a service that could not be found or is not run by the
 * node which has been asked for it
 * 
 * 
 *
 */
public class NoSuchServiceException extends ServiceInvocationException {


	/**
	 * 
	 */
	private static final long serialVersionUID = 5677066794484053198L;
	private String service;
	
	
	/**
	 * create a new exception instance
	 * 
	 * @param service	name of the not existing service
	 * @param cause
	 */
	public NoSuchServiceException ( String service, Throwable cause ) {
		super ( "The requested service '" + service + "' is not known.", cause);
		
		this.service = service;
	}
	
	/**
	 * create a new exception instance
	 * 
	 * @param service	name of the not existing service
	 */
	public NoSuchServiceException ( String service  ) {
		super ( "The requested service '" + service + "' is not known.");
		
		this.service = service;
	}
	
	/**
	 * get the name of the not existing service
	 * @return name of the service which does not exist
	 */
	public String getService () { return service; }
	
}
