package i5.las2peer.p2p;

import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.security.AgentLockedException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
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
	public void registerServiceAlias(ServiceAgent agent, String alias) throws AgentLockedException,
			DuplicateServiceAliasException {
		if (agent.isLocked())
			throw new AgentLockedException("Only unlocked Agents can be registered!");

		String content = agent.getServiceNameVersion().getName();

		try { // check if alias already exists
			String currentContent = getServiceNameByAlias(alias);
			if (!currentContent.equals(content)) { // if service name is not the same, it's an error
				throw new DuplicateServiceAliasException();
			} else {
				return; // otherwise we're done
			}
		} catch (ServiceNotFoundException e) {
			// if no mapping exists, ignore and continue with creating a new mapping
		}

		// create mapping
		try {
			Envelope envName = Envelope.createClassIdEnvelope(content, PREFIX + alias.toLowerCase(), agent);
			envName.open(agent);
			envName.setOverWriteBlindly(true);
			envName.addReader(node.getAnonymous());
			envName.addSignature(agent);
			node.storeArtifact(envName);
			envName.close();
		} catch (L2pSecurityException | EncodingFailedException | SerializationException | StorageException
				| DecodingFailedException e) {
			node.observerNotice(Event.NODE_ERROR, "Envelope error while updating user list: " + e);
		}
	}

	/**
	 * get a service name id by alias
	 * 
	 * @param alias
	 * @return
	 * @throws ServiceNotFoundException
	 */
	public String getServiceNameByAlias(String alias) throws ServiceNotFoundException {
		try {
			Envelope env = node.fetchArtifact(Envelope.getClassEnvelopeId(String.class, PREFIX + alias.toLowerCase()));
			env.open(node.getAnonymous());
			String content = env.getContentAsString();
			env.close();

			return content;
		} catch (StorageException | ArtifactNotFoundException | EnvelopeException | L2pSecurityException e) {
			throw new ServiceNotFoundException("Alias not found!", e);
		}
	}
}
