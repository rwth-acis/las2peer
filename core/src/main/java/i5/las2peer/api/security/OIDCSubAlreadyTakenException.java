package i5.las2peer.api.security;

/**
 * Thrown if the OIDC sub has already been taken.
 *
 */
public class OIDCSubAlreadyTakenException extends AgentAlreadyExistsException {

	private static final long serialVersionUID = 1L;

	public OIDCSubAlreadyTakenException() {
		super("OIDC sub already taken.");
	}

}
