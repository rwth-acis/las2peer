package i5.las2peer.api.security;

/**
 * Represents an error caused by the network or the node.
 *
 */
public class AgentOperationFailedException extends AgentException {

	private static final long serialVersionUID = 1L;

	public AgentOperationFailedException(String message) {
		super(message);
	}

	public AgentOperationFailedException(Throwable cause) {
		super(cause);
	}

	public AgentOperationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
