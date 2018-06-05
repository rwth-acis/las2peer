package i5.las2peer.logging.bot;

import java.io.Serializable;

import i5.las2peer.api.logging.MonitoringEvent;

public class BotMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long timestamp;
	private MonitoringEvent event;
	private String sourceNode;
	private String sourceAgentId;
	private String destinationNode;
	private String destinationAgentId;
	private String remarks;

	public BotMessage(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
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

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public MonitoringEvent getEvent() {
		return event;
	}

	public void setEvent(MonitoringEvent event) {
		this.event = event;
	}

	public String getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(String sourceNode) {
		this.sourceNode = sourceNode;
	}

	public String getSourceAgentId() {
		return sourceAgentId;
	}

	public void setSourceAgentId(String sourceAgentId) {
		this.sourceAgentId = sourceAgentId;
	}

	public String getDestinationNode() {
		return destinationNode;
	}

	public void setDestinationNode(String destinationNode) {
		this.destinationNode = destinationNode;
	}

	public String getDestinationAgentId() {
		return destinationAgentId;
	}

	public void setDestinationAgentId(String destinationAgentId) {
		this.destinationAgentId = destinationAgentId;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

}
