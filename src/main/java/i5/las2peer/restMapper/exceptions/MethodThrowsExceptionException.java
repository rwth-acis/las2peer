package i5.las2peer.restMapper.exceptions;

/**
 * Exception is thrown, if a service method throws an exception
 *
 */
public class MethodThrowsExceptionException extends Exception {

	private static final long serialVersionUID = 8705410691079747674L;

	private final String method;

	public MethodThrowsExceptionException(String method) {
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	public String getMessage() {
		return "Method: " + method + " throws unhandled exception(s). Please handle exceptions inside the method!";
	}

}