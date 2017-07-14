package i5.las2peer.api.security;

/**
 * Thrown if an agent already exists, e.g. when storing another agent with the same id.
 *
 */
public class AgentAlreadyExistsException extends AgentException {

	private static final long serialVersionUID = 1L;

	public AgentAlreadyExistsException(String message) {
		super(message);
	}

	public AgentAlreadyExistsException(Throwable cause) {
		super(cause);
	}

	public AgentAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

}
