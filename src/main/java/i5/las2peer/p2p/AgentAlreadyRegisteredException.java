package i5.las2peer.p2p;

import i5.las2peer.security.AgentException;


/**
 * Exception thrown on an attempt to register an agent, which is already registered.
 *  
 * @author Holger Jan&szlig;en
 *
 */
public class AgentAlreadyRegisteredException extends AgentException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -692705354414022880L;

	
	public AgentAlreadyRegisteredException ( String message ) {
		super (message );
	}
}
