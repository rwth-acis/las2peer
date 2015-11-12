package i5.las2peer.p2p;

/**
 * A simple exception thrown on timeouts.
 * 
 *
 */
public class TimeoutException extends Exception {

	private static final long serialVersionUID = -8509715321020571787L;

	public TimeoutException(String message) {
		super(message);
	}

	public TimeoutException() {
		super();
	}

}
