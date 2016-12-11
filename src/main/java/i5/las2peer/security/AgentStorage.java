package i5.las2peer.security;

import i5.las2peer.p2p.AgentNotKnownException;

/**
 * Interface for agent storages.
 */
public interface AgentStorage {

	/**
	 * get an agent from this storage
	 * 
	 * @param id the agent id
	 * @return a new locked Agent instance
	 * @throws AgentNotKnownException If the agent is not found in this storage
	 * @throws AgentException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public Agent getAgent(String id) throws AgentNotKnownException, AgentException;

	/**
	 * does this storage know the requested agent?
	 * 
	 * Does not refer to the backup storage if applicable
	 * 
	 * @param id
	 * @return true, if this storage knows an agent of the given id
	 * @throws AgentException AgentException If any issue with the agent occurs, e. g. XML not readable
	 */
	public boolean hasAgent(String id) throws AgentException;

}
