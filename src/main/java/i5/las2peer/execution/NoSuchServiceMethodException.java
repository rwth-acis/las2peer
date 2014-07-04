package i5.las2peer.execution;


/**
 * exception thrown on a service method invocation, if the requested
 * method is not implemented at the given service
 * 
 * 
 *
 */
public class NoSuchServiceMethodException extends ServiceInvocationException {


	/**
	 * 
	 */
	private static final long serialVersionUID = 200278355961032834L;

	private String method;
	
	private String service;
	
	private String parameterString;
	
	/**
	 * create a new exception instance
	 * 
	 * @param serviceClass	the class of the service causing the problems
	 * @param method		name of not existing method
	 */
	public NoSuchServiceMethodException ( String serviceClass, String method ) {
		super ( "The method '" + method + "' of service '" + serviceClass + "' does not exist");
		
		this.service = serviceClass;
		this.method = method;
	}
	
	
	/**
	 * create a new exception instance
	 * 
	 * @param serviceClass	the class of the service causing the problems
	 * @param method		name of not existing method
	 * @param parameterString	description of the requested parameters
	 */
	public NoSuchServiceMethodException ( String serviceClass, String method, String parameterString ) {
		super ( "The method '" + method + " " + parameterString +"' of service '" + serviceClass + "' does not exist");
		
		this.service = serviceClass;
		this.method = method;
		this.parameterString = parameterString;
		
	}
	
	
	public String getServiceClass () { return service; }
	
	public String getMethod () { return method; }
	
	public String getParateterString () { return parameterString; }
	
}
