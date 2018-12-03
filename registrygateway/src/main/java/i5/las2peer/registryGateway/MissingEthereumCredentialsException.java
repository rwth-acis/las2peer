package i5.las2peer.registryGateway;

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
