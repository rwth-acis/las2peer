package i5.las2peer.logging.monitoring;

import i5.las2peer.logging.NodeObserver.Event;

import java.io.Serializable;

/**
 * 
 * MonitoringMessage.java
 * <br>
 * This class stores a message that was logged by the monitoring module of LAS2peer.
 * An Array of instances of this class is send via a {@link i5.las2peer.communication.Message}
 * to the central monitoring node.
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
	
	
	/**
	 * 
	 * Constructor of a MonitoringMessage.
	 * 
	 * @param timestamp
	 * @param timespan
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param destinationNode
	 * @param destinationAgentId
	 * @param remarks
	 * 
	 */
	public MonitoringMessage(long timestamp, long timespan, Event event, String sourceNode,
			Long sourceAgentId, String destinationNode, Long destinationAgentId, String remarks){
		this.timestamp = timestamp;
		this.timespan = timespan;
		this.event = event;
		this.sourceNode = sourceNode;
		this.sourceAgentId = sourceAgentId;
		this.originNode = destinationNode;
		this.originAgentId = destinationAgentId;
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
	
	public String getDestinationNode() {
		return originNode;
	}
	
	
	public Long getDestinationAgentId() {
		return originAgentId;
	}
	
	
	public String getRemarks() {
		return remarks;
	}
	
	
}
