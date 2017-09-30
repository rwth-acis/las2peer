package i5.las2peer.logging.monitoring;

import java.io.Serializable;

import i5.las2peer.api.logging.MonitoringEvent;

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
	 * @param timestamp A timestamp for this message
	 * @param event A monitoring event identifier
	 * @param sourceNode A source node
	 * @param sourceAgentId A source agent id
	 * @param destinationNode A destination node
	 * @param destinationAgentId A destination agent id
	 * @param remarks Any additional information about this message
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
