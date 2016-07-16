package i5.las2peer.logging.monitoring;

import java.io.Serializable;

import i5.las2peer.logging.NodeObserver.Event;

/**
 * 
 * This class stores a message that was logged by the monitoring module of las2peer. An Array of instances of this class
 * is send via a {@link i5.las2peer.communication.Message} to the central monitoring node.
 * 
 * 
 *
 */
public class MonitoringMessage implements Serializable {

	private static final long serialVersionUID = -1481582785721621545L;

	private Long timestamp;
	private Event event;
	private String sourceNode;
	private Long sourceAgentId;
	private String destinationNode;
	private Long destinationAgentId;
	private String remarks;

	/**
	 * 
	 * Constructor of a MonitoringMessage.
	 * 
	 * @param timestamp
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param destinationNode
	 * @param destinationAgentId
	 * @param remarks
	 * 
	 */
	public MonitoringMessage(Long timestamp, Event event, String sourceNode, Long sourceAgentId, String destinationNode,
			Long destinationAgentId, String remarks) {
		this.timestamp = timestamp;
		this.event = event;
		this.sourceNode = sourceNode;
		this.sourceAgentId = sourceAgentId;
		this.destinationNode = destinationNode;
		this.destinationAgentId = destinationAgentId;
		this.remarks = remarks;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public Event getEvent() {
		return event;
	}

	public String getSourceNode() {
		return sourceNode;
	}

	public Long getSourceAgentId() {
		return sourceAgentId;
	}

	public String getDestinationNode() {
		return destinationNode;
	}

	public Long getDestinationAgentId() {
		return destinationAgentId;
	}

	public String getRemarks() {
		return remarks;
	}

}
