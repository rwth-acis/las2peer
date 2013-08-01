package i5.las2peer.logging;

import i5.las2peer.security.Agent;

import java.util.Date;

import rice.pastry.PastryNode;
import rice.pastry.socket.SocketNodeHandle;

/**
 * 
 * The NodeObserver is an abstract class providing all necessary methods
 * to log all interesting node events for a {@link i5.las2peer.p2p.Node}
 * (mainly {@link i5.las2peer.p2p.PastryNodeImpl}).
 * 
 * @author Holger Jan&szlig;en
 *
 */
public abstract class NodeObserver {
	
	/**
	 * An enumeration element with all types of events.
	 * 
	 * @author Holger Jan&szlig;en
	 * 
	 */
	public enum Event {
		NODE_CREATED(100),
		NEW_NODE_NOTICE (110),
		NEW_AGENT (200),
		NEW_AGENT_NOTICE (210),
		AGENT_REMOVED (500),
		
		AGENT_UNLOCKED ( 300 ),
		AGENT_UNLOCK_FAILED ( 310 ),
		
		NODE_STATUS_CHANGE (700),
		
		AGENT_SEARCH_STARTED (1000),
		AGENT_SEARCH_RECEIVED (1100),
		AGENT_SEARCH_ANSWER_SENT (1150),
		AGENT_SEARCH_ANSWER_RECEIVED (1160),
		AGENT_SEARCH_FINISHED (1200),
		AGENT_SEARCH_FAILED(1500),
		
		ARTIFACT_ADDED (2000),
		ARTIFACT_UPDATED (2050),
		ARTIFACT_FETCH_STARTED (2060),
		ARTIFACT_RECEIVED (2065),
		ARTIFACT_FETCH_FAILED (2067),
		ARTIFACT_NOTIFICATION (2100),
		ARTIFACT_UPLOAD_FAILED (2200),
		ARTIFACT_OVERWRITE_FAILED (2201),
		
		AGENT_LOADED (3000),
		AGENT_CREATED (3050),
		AGENT_UPLOAD_STARTED (3100),
		AGENT_UPLOAD_SUCCESS (3101),
		AGENT_UPLOAD_FAILED (-3102),
		AGENT_GET_STARTED(3200),
		AGENT_GET_SUCCESS(3201),
		AGENT_GET_FAILED(-3202),
		AGENT_LOAD_FAILED (-3000),
		
		
		RMI_SENT (4000),
		RMI_RECEIVED(4100),
		RMI_ANSWERED(4150),
		RMI_ANSWER_RECEIVED(4200),
		
		MESSAGE_RECEIVED (5000),
		MESSAGE_RECEIVED_ANSWER (5001),
		MESSAGE_SENDING(5100),
		MESSAGE_FORWARDING (5200),
		MESSAGE_FAILED (5300),
		MESSAGE_RECEIVED_UNKNOWN(5310),
		
		RESPONSE_SENDING (5500),
		RESPONSE_FAILED  (5600),
		
		PASTRY_NEW_TOPIC_CHILD (6000),
		PASTRY_REMOVED_TOPIC_CHILD (6010),
		PASTRY_TOPIC_SUBSCRIPTION_FAILED (6100),
		PASTRY_TOPIC_SUBSCRIPTION_SUCCESS (6110),
		
		SERVICE_STARTUP (6400),
		SERVICE_SHUTDOWN (6410),
		SERVICE_INVOKATION (6440),
		SERVICE_INVOKATION_FINISHED (6450),
		SERVICE_INVOKATION_FAILED (6460),
		
		SERVICE_MESSAGE (6500),
		SERVICE_ERROR (6505),
		
		HTTP_CONNECTOR_MESSAGE (8000),
		HTTP_CONNECTOR_REQUEST (8001),
		HTTP_CONNECTOR_ERROR (8100),
		
		NODE_SHUTDOWN (10000),
		
		NODE_ERROR (-1000);
		
		/**
		 * a numeric event code
		 */
		private int value = -1;
		
		/**
		 * simple constructor for the code assignment
		 * @param value
		 */
		private Event ( int value ) { this.value = value; }
		
		/**
		 * @return code of the event
		 */
		public int getCode() { return value; }
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param nodeId
	 * @param agent
	 * @param remarks
	 */
	public void logEvent ( Event event, Object nodeId, Agent agent, String remarks ) {
		writeLog ( -1, event, nodeId, agent, null, null, remarks  );
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param fromNode
	 * @param fromAgent
	 * @param toNode
	 * @param toAgent
	 * @param remarks
	 */
	public void logEvent ( Event event, Object fromNode, Agent fromAgent, Object toNode, Agent toAgent, String remarks ) {
		writeLog ( -1, event, fromNode, fromAgent, toNode, toAgent, remarks  );
	}
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param timespan
	 * @param fromNode
	 * @param fromAgent
	 * @param toNode
	 * @param toAgent
	 * @param remarks
	 */
	public void logEvent ( Event event, long timespan, Object fromNode, Agent fromAgent, Object toNode, Agent toAgent, String remarks ) {
		writeLog ( timespan, event, fromNode, fromAgent, toNode, toAgent, remarks  );
	}

	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param timespan
	 * @param fromNode
	 * @param fromAgentId
	 * @param toNode
	 * @param toAgentId
	 * @param remarks
	 */
	public void logEvent ( Event event, long timespan, Object fromNode, Long fromAgentId, Object toNode, Long toAgentId, String remarks ) {
		writeLog ( timespan, event, fromNode, fromAgentId, toNode, toAgentId, remarks  );
	}
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 */
	public void logEvent ( Event event ) {
		writeLog ( -1, event, null, (Long)null, null, null, "");
	}
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param remarks
	 */
	public void logEvent ( Event event, String remarks ) {
		writeLog ( -1, event, null, (Long) null, null, null, remarks);		
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param timespan
	 * @param remarks
	 */
	public void logEvent ( Event event, long timespan, String remarks ) {
		writeLog ( timespan, event, null, (Long) null, null, null, remarks);		
	}
	

	/**
	 * Derive a String representation for a node from the given identifier
	 * object. The type of the object depends on the setting of the current node.
	 * 
	 * Tries to specify an ip address and a port for an actual p2p node
	 * ({@link i5.las2peer.p2p.PastryNodeImpl} or {@link rice.pastry.NodeHandle}).
	 * 
	 * @param nodeId
	 * @return string representation for the given node object
	 */
	private String getNodeDesc ( Object nodeId ) {
		if ( nodeId == null)
			return null;
		else if ( nodeId instanceof SocketNodeHandle ) {
			SocketNodeHandle nh = (SocketNodeHandle) nodeId;
			return nh.getId() + "/" + nh.getIdentifier();
		}
		else if ( nodeId instanceof PastryNode ) {
			PastryNode node = (PastryNode) nodeId;
			return getNodeDesc ( node.getLocalNodeHandle());
		}
		else
			return "" + nodeId + " (" + nodeId.getClass().getName() + ")";
	}
	
	
	/**
	 * Write a log entry.
	 * 
	 * @param timespan
	 * @param event
	 * @param sourceNode
	 * @param sourceAgent
	 * @param destinationNode
	 * @param destinationAgent
	 * @param remarks
	 */
	protected void writeLog (long timespan, Event event, Object sourceNode, Agent sourceAgent, Object destinationNode , Agent destinationAgent, String remarks ) {
		writeLog ( new Date().getTime(), timespan, event, sourceNode, sourceAgent, destinationNode, destinationAgent, remarks);
	}

	/**
	 * Write a log entry.
	 * 
	 * @param timespan
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param destinationNode
	 * @param destinationAgentId
	 * @param remarks
	 */
	protected void writeLog (long timespan, Event event, Object sourceNode, Long sourceAgentId, Object destinationNode , Long destinationAgentId, String remarks ) {
		writeLog ( new Date().getTime(), timespan, event, getNodeDesc(sourceNode), sourceAgentId, getNodeDesc(destinationNode), destinationAgentId, remarks);
	}

	
	/**
	 * Write a log entry.
	 * 
	 * @param timestamp
	 * @param timespan
	 * @param event
	 * @param sourceNode
	 * @param sourceAgent
	 * @param destinationNode
	 * @param destinationAgent
	 * @param remarks
	 */
	protected void writeLog ( long timestamp, long timespan, Event event, Object sourceNode, Agent sourceAgent, Object destinationNode , Agent destinationAgent, String remarks ) {
		writeLog ( timestamp, timespan, event, getNodeDesc( sourceNode), sourceAgent, getNodeDesc ( destinationNode), destinationAgent, remarks);
	}
	
	
	/**
	 * Write a log entry.
	 * 
	 * @param timestamp
	 * @param timespan
	 * @param event
	 * @param sourceNode
	 * @param sourceAgent
	 * @param destinationNode
	 * @param destinationAgent
	 * @param remarks
	 */
	protected void writeLog ( long timestamp, long timespan, Event event, String sourceNode, Agent sourceAgent, String destinationNode , Agent destinationAgent, String remarks ) {
		Long sourceAgentId = null;
		if ( sourceAgent != null ) sourceAgentId = sourceAgent.getId();
		Long destinationAgentId = null;
		if ( destinationAgent != null) destinationAgentId = destinationAgent.getId();
		writeLog ( timestamp, timespan, event, sourceNode, sourceAgentId, destinationNode, destinationAgentId, remarks  );
	}

	
	/**
	 * This method has to be implemented by any (non abstract) deriving class.
	 * Each call represents one event to log by this observer. All parameters except the time stamp 
	 * and the event may be null.
	 * 
	 * @param timestamp		UNIX time stamp of the event
	 * @param timespan		a corresponding time span (e.g. for an answer retrieval, the time span between sending and answer
	 * @param event			the event to log
	 * @param sourceNode	a source (p2p) node of the event (e.g. message sender)
	 * @param sourceAgentId	a source (las2peer) agent of the event (e.g. message sender)
	 * @param destinationNode	an origin (p2p) node for the event (e.g. message receiver)
	 * @param destinationAgentId an origin (las2peer) agent of the event (e.g. message receiver)
	 * @param remarks		(optional) additional remarks
	 */
	protected abstract void writeLog ( long timestamp, long timespan, Event event, String sourceNode, Long sourceAgentId, String destinationNode , Long destinationAgentId, String remarks );
	
	
	
	
}
