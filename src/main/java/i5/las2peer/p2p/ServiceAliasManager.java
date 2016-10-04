package i5.las2peer.p2p;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

/**
 * Responsible for mapping service aliases to service names
 *
 */
public class ServiceAliasManager {

	private static final String PREFIX = "SERVICE_ALIAS-";

	private Node node;

	public ServiceAliasManager(Node node) {
		this.node = node;
	}

	/**
	 * Stores service alias of a service
	 * 
	 * @param agent an unlocked UserAgent
	 * @param alias
	 * @throws AgentLockedException
	 * @throws DuplicateServiceAliasException
	 */
	public void registerServiceAlias(ServiceAgent agent, String alias)
			throws AgentLockedException, DuplicateServiceAliasException {
		if (agent.isLocked()) {
			throw new AgentLockedException("Only unlocked Agents can be registered!");
		}

		String content = agent.getServiceNameVersion().getName();

		try { // check if alias already exists
			String currentContent = getServiceNameByAlias(alias);
			if (!currentContent.equals(content)) { // if service name is not the same, it's an error
				throw new DuplicateServiceAliasException();
			} else {
				return; // otherwise we're done
			}
		} catch (AliasNotFoundException e) {
			// if no mapping exists, ignore and continue with creating a new mapping
			try {
				Envelope envName = node.createEnvelope(PREFIX + alias.toLowerCase(), content, agent,
						node.getAnonymous());
				node.storeEnvelope(envName, agent);
			} catch (SerializationException | StorageException | IllegalArgumentException | CryptoException e2) {
				node.observerNotice(Event.NODE_ERROR, "Envelope error while updating user list: " + e2);
			}
		}
	}

	/**
	 * get a service name id by alias
	 * 
	 * @param alias
	 * @return
	 * @throws AliasNotFoundException
	 */
	public String getServiceNameByAlias(String alias) throws AliasNotFoundException {
		try {
			Envelope env = node.fetchEnvelope(PREFIX + alias.toLowerCase());
			String content = (String) env.getContent(node.getAnonymous());
			return content;
		} catch (StorageException | CryptoException | L2pSecurityException | SerializationException e) {
			throw new AliasNotFoundException("Alias not found!", e);
		}
	}
}
