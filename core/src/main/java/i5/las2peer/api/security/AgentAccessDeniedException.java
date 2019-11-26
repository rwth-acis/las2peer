package i5.las2peer.api.security;

/**
 * Thrown if an agent cannot be unlocked.
 *
 */
public class AgentAccessDeniedException extends AgentException {

	private static final long serialVersionUID = 1L;

	public AgentAccessDeniedException(String message) {
		super(message);
	}

	public AgentAccessDeniedException(Throwable cause) {
		super(cause);
	}

	public AgentAccessDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

}
