package i5.las2peer.communication;

import java.io.Serializable;

/**
 * a simple message content for an {@link Message} indicating an exception thrown via an remote method invocation
 * 
 * 
 *
 */
public class RMIExceptionContent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3811848605195392152L;
	private Throwable exception;

	public RMIExceptionContent(Throwable content) {
		exception = content;
	}

	public Throwable getException() {
		return exception;
	}

}
