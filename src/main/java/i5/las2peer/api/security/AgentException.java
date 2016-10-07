package i5.las2peer.api.security;

/**
 * Exceptions related to agents.
 *
 */
public class AgentException extends Exception {

	private static final long serialVersionUID = 1L;

	public AgentException(String message) {
		this(message, null);
	}

	public AgentException(Throwable cause) {
		this("", cause);
	}

	public AgentException(String message, Throwable cause) {
		super(message);
		this.initCause(cause);
	}

}
