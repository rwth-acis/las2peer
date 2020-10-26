package i5.las2peer.api.security;

import i5.las2peer.api.Context;

/**
 * An agent representing a user.
 *
 */
public interface UserAgent extends PassphraseAgent {

	/**
	 * Getter for the login name.
	 *
	 * Login names are unique in the network.
	 *
	 * @return The login name.
	 */
	public String getLoginName();

	/**
	 * Getter for the email address of this user.
	 *
	 * Emails are unique in the network.
	 *
	 * @return The login name.
	 */
	public String getEmail();

	/**
	 * Getter for the authentication flow type of this user.
	 *
	 * Either "simple" or "advanced".
	 *
	 * @return The flow type.
	 */
	public String getAuthenticationFlowType();

	/**
	 * Check if the agent has a login name set.
	 *
	 * @return true If a login name is set up.
	 */
	public boolean hasLoginName();

	/**
	 * Check if the agent has an email address set.
	 *
	 * @return true If an email address is set up.
	 */
	public boolean hasEmail();

	/**
	 * Check if the agent has an authentication flow type
	 *
	 * @return if an authentication flow type is set up.
	 */
	public boolean hasAuthenticationFlowType();

	/**
	 * Set the login name for this user.
	 *
	 * Note that user names are unique in the network.
	 *
	 * The name will be stored and reserved in the network only on {@link Context#storeAgent}. If the name is already
	 * taken, the store operation will fail with a {@link LoginNameAlreadyTakenException}.
	 *
	 * @param name A user name matching [a-zA-Z].*.
	 *
	 * @throws AgentLockedException If the agent is locked.
	 * @throws IllegalArgumentException If the name is of invalid format.
	 */
	public void setLoginName(String name) throws AgentLockedException, IllegalArgumentException;

	/**
	 * Set the email address for this user.
	 *
	 * Note that user names are unique in the network.
	 *
	 * The address will be stored and reserved in the network only on {@link Context#storeAgent}. If the address is
	 * already taken, the store operation will fail with a {@link EmailAlreadyTakenException}.
	 *
	 * @param address A valid email address.
	 *
	 * @throws AgentLockedException If the agent is locked.
	 * @throws IllegalArgumentException  If the email address is of invalid format.
	 */
	public void setEmail(String address) throws AgentLockedException, IllegalArgumentException;

	/**
	 * Set the authentication flow type of this user.
	 *
	 * @param flowType "simple" or "advanced".
	 *
	 * @throws IllegalArgumentException If the flow type is invalid.
	 */
	public void setAuthenticationFlowType(String flowType);
}
