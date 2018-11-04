package i5.las2peer.registryGateway;

public class EthereumException extends Exception {
	public EthereumException() {
	}

	public EthereumException(String message) {
		super(message);
	}

	public EthereumException(Throwable cause) {
		super(cause);
	}

	public EthereumException(String message, Throwable cause) {
		super(message, cause);
	}
}
