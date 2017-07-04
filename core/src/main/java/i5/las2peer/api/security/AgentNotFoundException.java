package i5.las2peer.api.security;

/**
 * Thrown if an agent cannot be found.
 *
 */
public class AgentNotFoundException extends AgentException {

	private static final long serialVersionUID = 1L;

	public AgentNotFoundException(String message) {
		super(message);
	}

	public AgentNotFoundException(Throwable cause) {
		super(cause);
	}

	public AgentNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
