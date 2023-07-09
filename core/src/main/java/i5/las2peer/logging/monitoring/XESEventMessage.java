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
public class XESEventMessage extends MonitoringMessage {

	private static final long serialVersionUID = -1481584785721621545L;

	private String caseId;
	private String activityName;
	private String resourceId;
	private String resourceType;

	/**
	 * 
	 * Constructor of a XESEventMessage. This is a subclass of
	 * {@link i5.las2peer.logging.monitoring.MonitoringMessage} with additional
	 * fields for XES event log.
	 * 
	 * @param timestamp          A timestamp for this message
	 * @param event              A monitoring event identifier
	 * @param sourceNode         A source node
	 * @param sourceAgentId      A source agent id
	 * @param destinationNode    A destination node
	 * @param destinationAgentId A destination agent id
	 * @param remarks            Any additional information about this message
	 * @param caseId             caseId of the event
	 * @param activityName       activityName of the event
	 * @param resourceId         resourceId of the event
	 * @param resourceType       resourceType of the event (e.g. user, service, bot,
	 *                           etc.)
	 */
	public XESEventMessage(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
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
