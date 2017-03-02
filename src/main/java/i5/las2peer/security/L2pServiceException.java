package i5.las2peer.security;

import i5.las2peer.api.security.AgentException;

// TODO API remove

/**
 * basic exception thrown on service (invocation) problems
 * 
 *
 */
public class L2pServiceException extends AgentException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2266707294292209691L;

	public L2pServiceException(String message) {
		super(message);
	}

	public L2pServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
