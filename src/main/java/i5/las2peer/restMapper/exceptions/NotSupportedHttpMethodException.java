package i5.las2peer.restMapper.exceptions;

/**
 * Exception is thrown, if there is a not supported HTTP method
 * 
 */
public class NotSupportedHttpMethodException extends Exception {

	private static final long serialVersionUID = -6454667548614471201L;

	private final String method;

	public NotSupportedHttpMethodException(String method) {
		this.method = method;
	}

	public String getErrorCode() {
		return method;
	}

	public String getMessage() {
		return "HTTP Method: " + method;
	}

}