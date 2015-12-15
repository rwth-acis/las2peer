package i5.las2peer.p2p.pastry;

import java.util.Date;
import java.util.Random;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

/**
 * base class for content published to the pub/sub-facilities of the pastry library (Scribe)
 * 
 * 
 *
 */
public abstract class L2pScribeContent implements ScribeContent {

	/**
	 * serialization id
	 */
	private static final long serialVersionUID = -6701164615785435436L;
	private NodeHandle from;

	private long randomId;

	private long timestamp;

	/**
	 * create a new scribe content created at the given node handle
	 * 
	 * @param from
	 */
	public L2pScribeContent(NodeHandle from) {
		this.from = from;

		randomId = new Random().nextLong();

		timestamp = new Date().getTime();
	}

	/**
	 * get the origin node of this content
	 * 
	 * @return a handle to the origin of this content
	 */
	public NodeHandle getOrigin() {
		return from;
	}

	/**
	 * the (random) id of this content
	 * 
	 * @return an id
	 */
	public long getRandomId() {
		return randomId;
	}

	/**
	 * get the (unix) timestamp of creation
	 * 
	 * @return (unix) timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

}
