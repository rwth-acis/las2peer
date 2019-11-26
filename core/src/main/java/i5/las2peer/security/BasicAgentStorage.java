package i5.las2peer.security;

import java.util.Hashtable;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;

/**
 * A simple Hashtable based storage for agents.
 */
public class BasicAgentStorage implements AgentStorage {

	private Hashtable<String, AgentImpl> htRegistered = new Hashtable<>();

	private AgentStorage backupStorage;

	/**
	 * create a basic agent storage with a backupStorage to use, if a requested agent is not stored here (i.e. a
	 * {@link i5.las2peer.p2p.Node} to look for the requested agent in the whole network
	 *
	 * @param backupStorage
	 */
	public BasicAgentStorage(AgentStorage backupStorage) {
		this.backupStorage = backupStorage;

		// TODO maybe initialize tidy up thread
	}

	/**
	 * create a new basic agent storage
	 */
	public BasicAgentStorage() {
		this(null);
	}

	/**
	 * register an agent for later use
	 *
	 * use a locked copy to store
	 *
	 * @param agent
	 */
	public void registerAgent(AgentImpl agent) {
		try {
			AgentImpl register = agent.cloneLocked();

			htRegistered.put(register.getIdentifier(), register);
		} catch (CloneNotSupportedException e) {
			// should not occur, since agent is cloneable
			throw new RuntimeException("Clone problems", e);
		}
	}

	/**
	 * register multiple agents to this storage
	 *
	 * @param agents
	 */
	public void registerAgents(AgentImpl... agents) {
		for (AgentImpl a : agents) {
			registerAgent(a);
		}
	}

	/**
	 * remove an agent from this storage
	 *
	 * @param agent
	 */
	public void unregisterAgent(AgentImpl agent) {
		htRegistered.remove(agent.getIdentifier());
	}

	/**
	 * remove an agent from this storage
	 *
	 * @param id
	 */
	public void unregisterAgent(String id) {
		htRegistered.remove(id);
	}

	@Override
	public AgentImpl getAgent(String id) throws AgentNotFoundException, AgentException {
		AgentImpl result = htRegistered.get(id);

		if (result != null) {
			try {
				return result.cloneLocked();
			} catch (CloneNotSupportedException e) {
				throw new AgentNotFoundException(id, e);
			}
		}

		if (backupStorage != null) {
			return backupStorage.getAgent(id);
		} else {
			throw new AgentNotFoundException(id);
		}
	}

	@Override
	public boolean hasAgent(String id) {
		return htRegistered.get(id) != null;
	}

}
