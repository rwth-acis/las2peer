package i5.las2peer.security;

import i5.las2peer.p2p.AgentNotKnownException;

/**
 * Interface for agent storages.
 * 
 * 
 *
 */
public interface AgentStorage {

	/**
	 * get an agent from this storage
	 * 
	 * @param id
	 * @return the agent stored here
	 * @throws AgentNotKnownException
	 */
	public Agent getAgent(long id) throws AgentNotKnownException;

	/**
	 * does this storage know the requested agent?
	 * 
	 * Does not refer to the backup storage if applicable
	 * 
	 * @return true, if this storage knows an agent of the given id
	 */
	public boolean hasAgent(long id);
}
