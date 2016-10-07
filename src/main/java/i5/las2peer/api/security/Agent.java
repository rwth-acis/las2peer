package i5.las2peer.api.security;

/**
 * Represents an acting entitiy in the network.
 * 
 * It can have two states: Locked agents cannot used to perform any actions while unlocked agents are able to do so.
 *
 */
public interface Agent {
	/**
	 * The agent id
	 * 
	 * @return The agent id.
	 */
	public String getId();

	/**
	 * Returns whether the agent is locked or not.
	 * 
	 * @return true if the agent is unlocked.
	 */
	public boolean isLocked();
}
