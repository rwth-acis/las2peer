package i5.las2peer.p2p.pastry;

import java.util.Random;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;

public class GetInfoMessage implements Message {

	private static final long serialVersionUID = 6642232699532550877L;

	private final NodeHandle sendingNode;
	private final long randomId;

	/**
	 * create a new
	 * 
	 * @param sender A sending node handle
	 */
	public GetInfoMessage(NodeHandle sender) {
		this.sendingNode = sender;
		randomId = new Random().nextLong();
	}

	/**
	 * get the sender of the information request
	 * 
	 * @return sending node of the request
	 */
	public NodeHandle getSender() {
		return sendingNode;
	}

	/**
	 * a random id for answer collection
	 * 
	 * @return id of this message
	 */
	public long getId() {
		return randomId;
	}

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

}
