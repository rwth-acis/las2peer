package i5.las2peer.registry.exceptions;

/**
 * Thrown for errors related to Ethereum, including execution of the
 * smart contracts themselves, errors in their wrapper code, or
 * communication with the Ethereum client.
 */
public class EthereumException extends Exception {
	private static final long serialVersionUID = 1L;

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
