package i5.las2peer.logging;

import i5.las2peer.api.logging.MonitoringEvent;

/**
 * 
 * The NodeObserver is an interface providing all necessary methods to monitor all interesting node events for a
 * {@link i5.las2peer.p2p.Node} (mainly {@link i5.las2peer.p2p.PastryNodeImpl}).
 * 
 * 
 *
 */
public interface NodeObserver {

	/**
	 * Each call represents one event to log by this observer. All parameters except the time stamp and the event may be
	 * null.
	 * 
	 * @param timestamp UNIX time stamp of the event
	 * @param event the event to log
	 * @param sourceNode a source (p2p) node of the event (e.g. message sender)
	 * @param sourceAgentId a source (las2peer) agent of the event (e.g. message sender)
	 * @param destinationNode a destination (p2p) node for the event (e.g. message receiver)
	 * @param destinationAgentId a destination (las2peer) agent of the event (e.g. message receiver)
	 * @param remarks (optional) additional remarks
	 */
	public void log(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks);

	/**
	 * Each call represents one event to log by this observer.
	 * All parameters except the time stamp and the event may be null.
	 * Additionally, this method logs the caseId, activityName, resourceId and
	 * resourceType of the event.
	 * 
	 * 
	 * @param timestamp          UNIX time stamp of the event
	 * @param event              the event to log
	 * @param sourceNode         a source (p2p) node of the event (e.g. message
	 *                           sender)
	 * @param sourceAgentId      a source (las2peer) agent of the event (e.g.
	 *                           message sender)
	 * @param destinationNode    a destination (p2p) node for the event (e.g.
	 *                           message receiver)
	 * @param destinationAgentId a destination (las2peer) agent of the event (e.g.
	 *                           message receiver)
	 * @param remarks            (optional) additional remarks
	 * @param caseId             caseId of the event
	 * @param activityName       activityName of the event
	 * @param resourceId         resourceId of the event
	 * @param resourceType       resourceType of the event
	 */
	public void logXESEvent(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
			String destinationNode, String destinationAgentId, String remarks, String caseId, String activityName,
			String resourceId, String resourceType, String lifecyclePhase);

}