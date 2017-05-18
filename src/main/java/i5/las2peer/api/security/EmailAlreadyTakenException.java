package i5.las2peer.api.security;

/**
 * Thrown if the email address has already been taken.
 *
 */
public class EmailAlreadyTakenException extends AgentAlreadyExistsException {

	public EmailAlreadyTakenException() {
		super("Email address already taken.");
	}

	private static final long serialVersionUID = 7457109148079493665L;

}
