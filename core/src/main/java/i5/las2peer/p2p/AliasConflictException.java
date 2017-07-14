package i5.las2peer.p2p;

public class AliasConflictException extends Exception {

	private static final long serialVersionUID = 1L;

	public AliasConflictException(String message) {
		super(message);
	}

	public AliasConflictException(String message, Throwable reason) {
		super(message, reason);
	}

}
