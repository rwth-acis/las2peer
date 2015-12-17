package i5.las2peer.restMapper.exceptions;

/**
 * Exception is thrown, if there is a not supported URI path
 *
 */
public class NotSupportedUriPathException extends Exception {

	private static final long serialVersionUID = -8124228440016225003L;

	private final String path;

	public NotSupportedUriPathException(String path) {
		this.path = path;
	}

	public String getErrorCode() {
		return path;
	}

	public String getMessage() {
		return "Path: " + path;
	}

}