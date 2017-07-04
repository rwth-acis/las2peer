package i5.las2peer.p2p.pastry;

import rice.p2p.commonapi.NodeHandle;

/**
 * A <i>content</i> to be published to an agent's scribe topic, if someone is looking for running versions of this
 * agent.
 * 
 */
public class SearchAgentContent extends L2pScribeContent {

	private static final long serialVersionUID = 8073537449959759751L;

	/**
	 * the id of the agent to look for
	 */
	private final String searchAgentId;

	/**
	 * create a new search content looking for the agent of the given id, created at the given node (handle)
	 * 
	 * @param from
	 * @param id
	 */
	public SearchAgentContent(NodeHandle from, String id) {
		super(from);
		searchAgentId = id;
	}

	/**
	 * get the id of the agent to search
	 * 
	 * @return id of the requested agent
	 */
	public String getAgentId() {
		return searchAgentId;
	}

}
