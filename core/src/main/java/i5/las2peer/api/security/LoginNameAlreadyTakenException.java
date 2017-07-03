package i5.las2peer.api.security;

/**
 * Thrown if the login name has already been taken.
 *
 */
public class LoginNameAlreadyTakenException extends AgentAlreadyExistsException {

	public LoginNameAlreadyTakenException() {
		super("Login name already taken.");
	}

	private static final long serialVersionUID = 1368931190224300150L;

}
