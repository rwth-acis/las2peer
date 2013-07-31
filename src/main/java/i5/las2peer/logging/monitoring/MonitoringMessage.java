package i5.las2peer.logging.monitoring;

import i5.las2peer.logging.NodeObserver.Event;

import java.io.Serializable;

/**
 * MonitoringMessage.java
 * 
 * This is the data class that will be send over the network to the
 * Monitoring Data Processing Service.
 * 
 * @author Peter de Lange
 *
 */
public class MonitoringMessage implements Serializable{

	private static final long serialVersionUID = -1481582785721621545L;
	
	private long timestamp;
	private long timespan;
	private Event event;
	private String sourceNode;
	private Long sourceAgentId;
	private String originNode;
	private Long originAgentId;
	private String remarks;
	
	public MonitoringMessage(long timestamp, long timespan, Event event, String sourceNode,
			Long sourceAgentId, String originNode, Long originAgentId, String remarks){
		this.timestamp = timestamp;
		this.timespan = timespan;
		this.event = event;
		this.sourceNode = sourceNode;
		this.sourceAgentId = sourceAgentId;
		this.originNode = originNode;
		this.originAgentId = originAgentId;
		this.remarks = remarks;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getTimespan() {
		return timespan;
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

	public String getOriginNode() {
		return originNode;
	}

	public Long getOriginAgentId() {
		return originAgentId;
	}

	public String getRemarks() {
		return remarks;
	}
}
