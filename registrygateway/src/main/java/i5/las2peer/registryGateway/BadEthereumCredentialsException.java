package i5.las2peer.registryGateway;

public class BadEthereumCredentialsException extends Exception {
	public BadEthereumCredentialsException() {
	}

	public BadEthereumCredentialsException(String message) {
		super(message);
	}

	public BadEthereumCredentialsException(Throwable cause) {
		super(cause);
	}

	public BadEthereumCredentialsException(String message, Throwable cause) {
		super(message, cause);
	}
}
