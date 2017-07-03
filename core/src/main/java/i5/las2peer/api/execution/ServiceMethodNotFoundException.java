package i5.las2peer.api.execution;

/**
 * Thrown if a service method does not exist.
 *
 */
public class ServiceMethodNotFoundException extends ServiceInvocationException {
	private static final long serialVersionUID = 1L;

	public ServiceMethodNotFoundException(String className, String methodName, String parameters) {
		super("Service method not found: " + className + "/" + methodName + " with parameters " + parameters);
	}
}
