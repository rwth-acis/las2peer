package i5.las2peer.execution;

import java.io.Serializable;

import i5.las2peer.api.p2p.ServiceNameVersion;

/**
 * a simple invocation task
 */
public class RMITask implements Serializable {

	private static final long serialVersionUID = 6654217287828959042L;

	private final Serializable[] parameters;

	private final String methodName;

	private final ServiceNameVersion service;

	/**
	 * create a new invocation task
	 * 
	 * @param service A service name and version
	 * @param methodName A method name to call
	 * @param parameters A bunch of parameters
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
