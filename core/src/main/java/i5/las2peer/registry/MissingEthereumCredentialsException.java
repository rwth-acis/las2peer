package i5.las2peer.registry;

public class MissingEthereumCredentialsException extends Exception {
	public MissingEthereumCredentialsException() {
	}

	public MissingEthereumCredentialsException(String message) {
		super(message);
	}

	public MissingEthereumCredentialsException(Throwable cause) {
		super(cause);
	}

	public MissingEthereumCredentialsException(String message, Throwable cause) {
		super(message, cause);
	}
}
