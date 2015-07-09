package i5.las2peer.restMapper.data;

import java.io.Serializable;

/**
 * Stores all information needed to invoke a service method from the Web Connector
 *
 */
public class InvocationData {

	private String serviceName;
	private String serviceVersion;
	private String methodName;
	private Serializable[] parameters;
	private Class<?> returnType;
	private Class<?>[] parameterTypes;
	private int matchLevel;
	private String[] mime;

	/**
	 * constructor creating data object 
	 * @param serviceName full class name of the service containing the method
	 * @param serviceVersion version of the service
	 * @param methodName name of the method to invoke
	 * @param returnType return type of the method
	 * @param mime MIME type the method produces
	 * @param matchLevel how good the MIME type matches the accept header (lower = better)
	 * @param parameters list of parameter values for invocation
	 * @param parameterTypes array of the used parameter types
	 */
	public InvocationData(String serviceName, String serviceVersion, String methodName, Class<?> returnType,
			String[] mime, int matchLevel, Serializable[] parameters, Class<?>[] parameterTypes)
	{
		this.serviceName = serviceName;
		this.serviceVersion = serviceVersion;
		this.methodName = methodName;
		this.parameters = parameters;
		this.parameterTypes = parameterTypes;
		this.returnType = returnType;
		this.matchLevel = matchLevel;
		this.mime = mime;
	}

	/**
	 *
	 * @return the full class name of the service
	 */
	public String getServiceName()
	{
		return serviceName;
	}

	/**
	 * 
	 * @return the version of the service
	 */
	public String getServiceVersion()
	{
		return serviceVersion;
	}

	/**
	 * 
	 * @return the name of the method to invoke
	 */
	public String getMethodName()
	{
		return methodName;
	}

	/**
	 * 
	 * @return the return type of the method
	 */
	public Class<?> getReturnType()
	{
		return returnType;
	}

	/**
	 * 
	 * @return array of parameter values to use for invocation
	 */
	public Serializable[] getParameters()
	{
		return parameters;
	}

	/**
	 * 
	 * @return array of the used parameter types
	 */
	public Class<?>[] getParameterTypes()
	{
		return parameterTypes;
	}

	/**
	 *
	 * @return positve value, lower = better match for accept header
	 */
	public int getMatchLevel()
	{
		return matchLevel;
	}

	public String[] getMIME()
	{
		return mime;
	}
}
