package i5.las2peer.security;

import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.EmailAlreadyTakenException;
import i5.las2peer.api.security.LoginNameAlreadyTakenException;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

/**
 * Maps usernames and emails to {@link UserAgentImpl}s.
 *
 */
public class UserAgentManager {

	private static final String PREFIX_USER_NAME = "USER_NAME-";
	private static final String PREFIX_USER_MAIL = "USER_MAIL-";

	private Node node;

	public UserAgentManager(Node node) {
		this.node = node;
	}

	/**
	 * Stores login name and email of an user agent to the network
	 * 
	 * @param agent an unlocked UserAgent
	 * @throws EmailAlreadyTakenException
	 * @throws LoginNameAlreadyTakenException
	 * @throws AgentLockedException
	 */
	public void registerUserAgent(UserAgentImpl agent) throws EmailAlreadyTakenException,
			LoginNameAlreadyTakenException, AgentLockedException {
		if (agent.isLocked()) {
			throw new AgentLockedException("Only unlocked Agents can be registered.");
		}
		String agentId = agent.getIdentifier();
		if (agent.hasLoginName()) {
			try {
				String identifier = PREFIX_USER_NAME + agent.getLoginName().toLowerCase();
				EnvelopeVersion envName = null;
				try {
					EnvelopeVersion stored = node.fetchEnvelope(identifier);
					envName = node.createUnencryptedEnvelope(stored, agentId);
				} catch (EnvelopeNotFoundException e) {
					envName = node.createUnencryptedEnvelope(identifier, agent.getPublicKey(), agentId);
				}
				node.storeEnvelope(envName, agent);
			} catch (EnvelopeAlreadyExistsException e) {
				throw new LoginNameAlreadyTakenException();
			} catch (SerializationException | CryptoException | EnvelopeException e) {
				node.observerNotice(MonitoringEvent.NODE_ERROR, "Envelope error while updating user list: " + e);
			}
		}

		if (agent.hasEmail()) {
			try {
				String identifier = PREFIX_USER_MAIL + agent.getEmail().toLowerCase();
				EnvelopeVersion envMail = null;
				try {
					envMail = node.fetchEnvelope(identifier);
				} catch (EnvelopeNotFoundException e) {
					envMail = node.createUnencryptedEnvelope(identifier, agent.getPublicKey(), agentId);
				}
				node.storeEnvelope(envMail, agent);
			} catch (EnvelopeAlreadyExistsException e) {
				throw new EmailAlreadyTakenException();
			} catch (SerializationException | CryptoException | EnvelopeException e) {
				node.observerNotice(MonitoringEvent.NODE_ERROR, "Envelope error while updating user list: " + e);
			}
		}
	}

	/**
	 * get an {@link UserAgentImpl}'s id by login name
	 * 
	 * @param name
	 * @return the id of the agent
	 * @throws AgentNotFoundException If no agent for the given login is found
	 * @throws AgentOperationFailedException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public String getAgentIdByLogin(String name) throws AgentNotFoundException, AgentOperationFailedException {
		if (name.equalsIgnoreCase("anonymous")) {
			return node.getAnonymous().getIdentifier();
		}
		try {
			EnvelopeVersion env = node.fetchEnvelope(PREFIX_USER_NAME + name.toLowerCase());
			return (String) env.getContent();
		} catch (EnvelopeNotFoundException e) {
			throw new AgentNotFoundException("Username not found!", e);
		} catch (EnvelopeException | SerializationException | L2pSecurityException | CryptoException e) {
			throw new AgentOperationFailedException("Could not read agent id from storage");
		}
	}

	/**
	 * get an {@link UserAgentImpl}'s id by email address
	 * 
	 * @param email
	 * @return the id of the agent
	 * @throws AgentNotFoundException If no agent for the given email is found
	 * @throws AgentOperationFailedException If any other issue with the agent occurs, e. g. XML not readable
	 */
	public String getAgentIdByEmail(String email) throws AgentNotFoundException, AgentOperationFailedException {
		try {
			EnvelopeVersion env = node.fetchEnvelope(PREFIX_USER_MAIL + email.toLowerCase());
			return (String) env.getContent();
		} catch (EnvelopeNotFoundException e) {
			throw new AgentNotFoundException("Email not found!", e);
		} catch (EnvelopeException | SerializationException | L2pSecurityException | CryptoException e) {
			throw new AgentOperationFailedException("Could not read email from storage");
		}
	}
}
