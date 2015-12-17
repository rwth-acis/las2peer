package i5.las2peer.restMapper.exceptions;

/**
 * Exception is thrown, if a service method could not be found
 *
 */
public class NoMethodFoundException extends Exception {

	private static final long serialVersionUID = 5388974002513762310L;

	private final String errorCode;

	public NoMethodFoundException(String code) {
		errorCode = code;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return "Path: " + errorCode;
	}

}