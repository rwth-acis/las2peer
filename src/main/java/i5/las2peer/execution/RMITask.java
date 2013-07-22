package i5.las2peer.execution;

import java.io.Serializable;


/**
 * a simple invocation task
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class RMITask implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = 6654217287828959042L;




	private Serializable [] parameters;
	
	
	private String methodName;
	
	private String serviceName;
	
	/**
	 * create a new invocation task
	 * 
	 * @param serviceName
	 * @param methodName
	 * @param parameters
	 */
	public RMITask ( String serviceName, String methodName, Serializable[] parameters) {
		this.serviceName = serviceName;
		this.methodName = methodName;
		this.parameters = parameters.clone();
	}

	
	public String getServiceName () { return serviceName; }
	
	public String getMethodName () { return methodName; }
	
	public Serializable[] getParameters () { return parameters; }
	

	
}
