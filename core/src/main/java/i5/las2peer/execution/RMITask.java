package i5.las2peer.execution;

import java.io.Serializable;

import i5.las2peer.api.p2p.ServiceNameVersion;

/**
 * a simple invocation task
 * 
 * 
 *
 */
public class RMITask implements Serializable {

	private static final long serialVersionUID = 6654217287828959042L;

	private Serializable[] parameters;

	private String methodName;

	private ServiceNameVersion service;

	/**
	 * create a new invocation task
	 * 
	 * @param service
	 * @param methodName
	 * @param parameters
	 */
	public RMITask(ServiceNameVersion service, String methodName, Serializable[] parameters) {
		this.service = service;
		this.methodName = methodName;
		this.parameters = parameters.clone();
	}

	public ServiceNameVersion getServiceNameVersion() {
		return service;
	}

	public String getMethodName() {
		return methodName;
	}

	public Serializable[] getParameters() {
		return parameters;
	}

}
