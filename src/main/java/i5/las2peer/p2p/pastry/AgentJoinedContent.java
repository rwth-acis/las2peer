package i5.las2peer.p2p.pastry;

import rice.p2p.commonapi.NodeHandle;

/**
 * Scribe content for a message, the an agent joined the net
 * 
 */
public class AgentJoinedContent extends L2pScribeContent {

	private static final long serialVersionUID = 311073208550954373L;

	private final long agentId;

	/**
	 * create a new instance stating that the agent of the given id has been loaded at the given node
	 * 
	 * @param from
	 * @param agentId
	 */
	public AgentJoinedContent(NodeHandle from, long agentId) {
		super(from);
		this.agentId = agentId;
	}

	/**
	 * get the id of the agent, which has joined the net
	 * 
	 * @return an agent id
	 */
	public long getAgentId() {
		return agentId;
	}

}
