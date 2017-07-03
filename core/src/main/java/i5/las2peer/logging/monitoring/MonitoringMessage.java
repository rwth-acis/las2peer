package i5.las2peer.logging.monitoring;

import i5.las2peer.api.logging.MonitoringEvent;

import java.io.Serializable;

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
	private MonitoringEvent event;
	private String sourceNode;
	private String sourceAgentId;
	private String destinationNode;
	private String destinationAgentId;
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
	public MonitoringMessage(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks) {
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

	public MonitoringEvent getEvent() {
		return event;
	}

	public String getSourceNode() {
		return sourceNode;
	}

	public String getSourceAgentId() {
		return sourceAgentId;
	}

	public String getDestinationNode() {
		return destinationNode;
	}

	public String getDestinationAgentId() {
		return destinationAgentId;
	}

	public String getRemarks() {
		return remarks;
	}

}
