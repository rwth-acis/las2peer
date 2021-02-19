package i5.las2peer.api.security;

/**
 * Thrown if the group name has already been taken.
 *
 */
public class NameAlreadyTakenException extends AgentAlreadyExistsException {

	public NameAlreadyTakenException() {
		super("Group name already taken.");
	}

	private static final long serialVersionUID = 1368931190224300150L;

}
