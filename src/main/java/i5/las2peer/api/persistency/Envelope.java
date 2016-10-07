package i5.las2peer.api.persistency;

import i5.las2peer.api.Context;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.GroupAgent;

import java.io.Serializable;

/**
 * Represents a (mutable) piece of data that is stored in the network.
 * 
 * It contains a Java object and is always signed by an agent which is the only one allowed to update the envelope.
 * 
 * Also, a list of readers can be set up.
 *
 */
public interface Envelope {

	/**
	 * Get the identifier of the envelope.
	 * 
	 * @return The identifier.
	 */
	public String getIdentifier();

	/**
	 * Get the content.
	 * 
	 * @return The decrypted content.
	 */
	public Serializable getContent();

	/**
	 * Set the content.
	 * 
	 * After modifying the envelope, it should be stored using the {@link Context#storeEnvelope}.
	 * 
	 * @param content
	 */
	public void setContent(Serializable content);

	/**
	 * Adds a reader to the envelope.
	 * 
	 * @param agent The new reader.
	 */
	public void addReader(Agent agent);

	/**
	 * Removes a reader from the envelope.
	 * 
	 * Notice that the removed reader still has access to older versions of this envelope in the network due to
	 * consequences of a p2p network. However, the agent cannot read future changes.
	 * 
	 * @param agent The reader to remove.
	 */
	public void revokeReader(Agent agent);

	/**
	 * Checks if the given agent is direct reader of this envelope. This method does not check for transitive access
	 * (e.g. via GroupAgents).
	 * 
	 * @param agent The agent to check.
	 * @return True, if the given agent is a reader of this envelope.
	 */
	public boolean hasReader(Agent agent);

	/**
	 * Removes all readers from this envelope and makes the contents available for everyone (the content is not
	 * encrypted in thsi case).
	 */
	public void setPublic();

	/**
	 * Checks if the content is private (encrypted).
	 * 
	 * @return True, if the content is private.
	 */
	public boolean isPrivate();

}
