package i5.las2peer.api.security;

/**
 * Thrown if an agent is locked but an unlocked agent is needed to call a method.
 * 
 * Intentionally not a subclass of {@link AgentAccessDeniedException} because it represents an invalid usage of an agent
 * (caused by the code) and not a user exception.
 *
 */
public class AgentLockedException extends AgentException {

	private static final long serialVersionUID = 1L;
	
	public AgentLockedException() {
		super("An unlocked agent is required to perform this action.");
	}

	public AgentLockedException(String message) {
		super(message);
	}

	public AgentLockedException(Throwable cause) {
		super(cause);
	}

	public AgentLockedException(String message, Throwable cause) {
		super(message, cause);
	}

}
