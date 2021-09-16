package i5.las2peer.api.security;

/**
 * Thrown if an agent cannot be found.
 *
 */
public class AgentNotFoundInRegistryException extends AgentException {

	private static final long serialVersionUID = 1L;

	public AgentNotFoundInRegistryException(String message) {
		super(message);
	}

	public AgentNotFoundInRegistryException(Throwable cause) {
		super(cause);
	}

	public AgentNotFoundInRegistryException(String message, Throwable cause) {
		super(message, cause);
	}

}
