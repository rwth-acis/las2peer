package i5.las2peer.security;

import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.PassphraseAgent;
import i5.las2peer.execution.ServiceThread;
import i5.las2peer.p2p.Node;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.util.Date;
import java.util.Hashtable;

/**
 * A context that can be created for an agent to perform operations in the network. Cached in the node for remembering
 * unlocked agents. Used for messaging and service calls.
 */
public class AgentContext implements AgentStorage {

	private AgentImpl agent;

	private Hashtable<String, GroupAgentImpl> groupAgents = new Hashtable<>();

	private Node localNode;

	private long lastUsageTime = -1;

	/**
	 * Creates a new (local) context.
	 * 
	 * @param mainAgent
	 * @param localNode
	 */
	public AgentContext(Node localNode, AgentImpl mainAgent) {
		this.agent = mainAgent;

		this.localNode = localNode;

		touch();
	}

	/**
	 * unlocks the main agent of this context
	 * 
	 * @param passphrase Passphrase of the agent
	 * @throws AgentAccessDeniedException If the passphrase is invalid.
	 */
	public void unlockMainAgent(String passphrase) throws AgentAccessDeniedException {
		if (this.agent.isLocked() && this.agent instanceof PassphraseAgent) {
			((PassphraseAgent) this.agent).unlock(passphrase);
		}
	}

	/**
	 * Gets the main agent of this context.
	 * 
	 * @return the agent of this context
	 */
	public AgentImpl getMainAgent() {
		return agent;
	}

	/**
	 * Tries to open the given id for this context.
	 * 
	 * @param groupId The group id to fetch and unlock
	 * @return the unlocked GroupAgent of the given id
	 * @throws AgentAccessDeniedException
	 * @throws AgentNotFoundException
	 * @throws AgentOperationFailedException
	 */
	public GroupAgentImpl requestGroupAgent(String groupId) throws AgentAccessDeniedException, AgentNotFoundException,
			AgentOperationFailedException {
		if (groupAgents.containsKey(groupId)) {
			return groupAgents.get(groupId);
		}

		AgentImpl agent;
		try {
			agent = localNode.getAgent(groupId);
		} catch(AgentNotFoundException e) {
			throw e;
		} catch (AgentException e1) {
			throw new AgentOperationFailedException(e1);
		}

		if (!(agent instanceof GroupAgentImpl)) {
			throw new AgentNotFoundException("Agent " + groupId + " is not a group agent!");
		}

		GroupAgentImpl group = (GroupAgentImpl) agent;

		if (group.isMember(this.getMainAgent())) {
			try {
				group.unlockPrivateKey(this.getMainAgent());
			} catch (L2pSecurityException | CryptoException e) {
				throw new AgentAccessDeniedException("Unable to open group!", e);
			} catch (SerializationException e) {
				throw new AgentOperationFailedException(e);
			}
		} else {
			for (String memberId : group.getMemberList()) {
				try {
					GroupAgentImpl member = requestGroupAgent(memberId);
					group.unlockPrivateKey(member);
					break;
				} catch (Exception e) {
					// do nothing
				}
			}
		}

		if (group.isLocked()) {
			throw new AgentAccessDeniedException("Unable to open group!");
		}

		groupAgents.put(groupId, group);

		return group;
	}

	/**
	 * returns an unlocked instance of the requested Agent
	 * 
	 * @param agentId the requested agent
	 * @return an unlocked agent instance
	 * @throws AgentOperationFailedException
	 * @throws AgentNotFoundException
	 * @throws AgentAccessDeniedException
	 */
	public Agent requestAgent(String agentId) throws AgentAccessDeniedException, AgentNotFoundException,
			AgentOperationFailedException {
		if (agentId.equalsIgnoreCase(getMainAgent().getIdentifier())) {
			return getMainAgent();
		} else {
			return requestGroupAgent(agentId);
		}
	}

	/**
	 * Checks if the agent can be accessed (e.g. unlocked) from this context.
	 * 
	 * @param agentId The agent to be accessed
	 * @return true if the given agent can be unlocked.
	 * @throws AgentNotFoundException If the agent does not exist
	 */
	public boolean hasAccess(String agentId) throws AgentNotFoundException {
		if (agent.getIdentifier().equals(agentId)) {
			return true;
		}

		AgentImpl a;
		try {
			a = getAgent(agentId);
		} catch (AgentException e) {
			throw new AgentNotFoundException("Agent could not be found!", e);
		}

		if (a instanceof GroupAgentImpl) {
			return isMemberRecursive((GroupAgentImpl) a, agent.getIdentifier());
		}

		return false;
	}

	/**
	 * Checks if the given agent is transitive member of the given group.
	 * 
	 * @param groupAgent The group
	 * @param agentId the agent to check if its member of the group
	 * @return true if it is a transitive member of the group.
	 */
	public boolean isMemberRecursive(GroupAgentImpl groupAgent, String agentId) {
		if (groupAgent.isMember(agentId) == true) {
			return true;
		}
		for (String memberId : groupAgent.getMemberList()) {
			try {
				AgentImpl agent = getAgent(memberId);
				if (agent instanceof GroupAgentImpl) {
					GroupAgentImpl group = (GroupAgentImpl) agent;
					if (isMemberRecursive(group, agentId)) {
						return true;
					}
				}
			} catch (AgentException e) {
			}
		}
		return false;
	}

	/**
	 * Gives access to the local node.
	 * 
	 * @return the local P2P node
	 */
	public Node getLocalNode() {
		return localNode;
	}

	/**
	 * Mark the current time as the last usage.
	 */
	public void touch() {
		lastUsageTime = new Date().getTime();
	}

	/**
	 * Returns the time of the last usage of this context.
	 * 
	 * @return the timestamp of the last usage
	 */
	public long getLastUsageTimestamp() {
		return lastUsageTime;
	}

	/**
	 * Uses this context as {@link AgentStorage}. Returns agents that are unlocked in this context first. E.g. necessary
	 * for opening a received {@link i5.las2peer.communication.Message}.
	 * 
	 * @param id
	 * @return get the agent of the given id
	 * @throws AgentException
	 */
	@Override
	public AgentImpl getAgent(String id) throws AgentException {
		if (id.equalsIgnoreCase(agent.getIdentifier())) {
			return agent;
		}

		AgentImpl result;
		if ((result = groupAgents.get(id)) != null) {
			return result;
		}

		return localNode.getAgent(id);
	}

	@Override
	public boolean hasAgent(String id) {
		return id.equalsIgnoreCase(agent.getIdentifier()) || groupAgents.containsKey(id);
	}

	/**
	 * Gets the current las2peer context.
	 * 
	 * @throws IllegalStateException called not in a las2peer execution thread
	 * @return the current context
	 */
	public static AgentContext getCurrent() {
		return ServiceThread.getCurrentContext().getCallerContext();
	}

}