package i5.las2peer.logging.monitoring;

import i5.las2peer.api.logging.MonitoringEvent;

/**
 * 
 * This class stores a message that was logged by the monitoring module of
 * las2peer. An Array of instances of this class
 * is send via a {@link i5.las2peer.communication.Message} to the central
 * monitoring node.
 * 
 * 
 *
 */
public class EventMessage extends MonitoringMessage {

	private static final long serialVersionUID = -1481584785721621545L;

	private String caseId;
	private String activityName;
	private String resourceId;
	private String resourceType;

	/**
	 * 
	 * Constructor of a EventMessage.
	 * 
	 * @param timestamp          A timestamp for this message
	 * @param event              A monitoring event identifier
	 * @param sourceNode         A source node
	 * @param sourceAgentId      A source agent id
	 * @param destinationNode    A destination node
	 * @param destinationAgentId A destination agent id
	 * @param remarks            Any additional information about this message
	 */
	public EventMessage(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks, String caseId, String activityName,
			String resourceId, String resourceType) {
		super(timestamp, event, sourceNode, sourceAgentId, destinationNode, destinationAgentId, remarks);

		this.caseId = caseId;
		this.activityName = activityName;
		this.resourceId = resourceId;
		this.resourceType = resourceType;
	}

	public String getCaseId() {
		return caseId;
	}

	public String getActivityName() {
		return activityName;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getResourceType() {
		return resourceType;
	}

}
