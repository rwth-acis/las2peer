package i5.las2peer.p2p;

import i5.las2peer.security.AgentException;


/**
 * exception thrown on an attempt to register an agent, which is already 
 * registered
 *  
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/04/10 10:09:54 $
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
