package i5.las2peer.p2p.pastry;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

/**
 * A message as answer to a {@link SearchAgentContent} indicating that the requested agent is running at the sending
 * node of this response message
 * 
 */
public class SearchAnswerMessage implements Message {

	private static final long serialVersionUID = 2852577083676648181L;

	private final NodeHandle sender;
	private final NodeHandle recipient;
	private final long answerToMessageId;

	/**
	 * create a new answer
	 * 
	 * @param to
	 * @param from
	 * @param answerToId id of the {@link SearchAgentContent} message this one is a response to
	 */
	public SearchAnswerMessage(NodeHandle to, NodeHandle from, long answerToId) {
		this.sender = from;
		this.recipient = to;
		this.answerToMessageId = answerToId;
	}

	/**
	 * create a new answer for the given search request
	 * 
	 * @param from
	 * @param search
	 */
	public SearchAnswerMessage(NodeHandle from, SearchAgentContent search) {
		this(search.getOrigin(), from, search.getRandomId());
	}

	@Override
	public int getPriority() {
		// TODO message prioritization
		return 0;
	}

	/**
	 * get the origin node of this answer
	 * 
	 * @return a node handle
	 */
	public NodeHandle getSendingNode() {
		return sender;
	}

	/**
	 * get the (designated) receiver of this answer
	 * 
	 * @return a node handle
	 */
	public NodeHandle getRecievingNode() {
		return recipient;
	}

	/**
	 * get the id of the original search request
	 * 
	 * @return id of the original search request
	 */
	public long getRequestMessageId() {
		return answerToMessageId;
	}

}
