package i5.las2peer.p2p;

import i5.las2peer.api.security.AgentException;

/**
 * Exception if the an agent is not registered at a node or anywhere in the network.
 * 
 * 
 *
 */
public class AgentNotRegisteredException extends AgentException {
	
	private static final long serialVersionUID = 1L;

	public AgentNotRegisteredException(String message) {
		super(message);
	}

	public AgentNotRegisteredException(Throwable cause) {
		super(cause);
	}

	public AgentNotRegisteredException(String message, Throwable cause) {
		super(message, cause);
	}
		
}
