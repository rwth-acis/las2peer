package i5.las2peer.p2p;

public class AliasNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public AliasNotFoundException(String message) {
		super(message);
	}

	public AliasNotFoundException(String message, Throwable reason) {
		super(message, reason);
	}

}
