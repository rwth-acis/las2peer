package i5.las2peer.logging;

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
		NODE_CREATED (100),
		NODE_SHUTDOWN (200),
		NODE_STATUS_CHANGE (300),
		
		NEW_NODE_NOTICE (110),
		
		NODE_ERROR (-100),
		
		AGENT_SEARCH_STARTED (1000),
		AGENT_SEARCH_RECEIVED (1100),
		AGENT_SEARCH_ANSWER_SENT (1150),
		AGENT_SEARCH_ANSWER_RECEIVED (1160),
		AGENT_SEARCH_FINISHED (1200),
		AGENT_SEARCH_FAILED(-1500),
		
		ARTIFACT_ADDED (2000),
		ARTIFACT_UPDATED (2050),
		ARTIFACT_FETCH_STARTED (2060),
		ARTIFACT_RECEIVED (2065),
		ARTIFACT_FETCH_FAILED (-2065),
		ARTIFACT_NOTIFICATION (2100),
		ARTIFACT_UPLOAD_FAILED (-2200),
		ARTIFACT_OVERWRITE_FAILED (-2201),
		
		AGENT_REGISTERED (3000), //Done at the node class (not its implementation)
		AGENT_UNLOCKED (3010),
		AGENT_UNLOCK_FAILED (-3020),
		AGENT_CREATED (3050),
		AGENT_REMOVED (3060),
		AGENT_UPLOAD_STARTED (3100),
		AGENT_UPLOAD_SUCCESS (3101),
		AGENT_UPLOAD_FAILED (-3101),
		AGENT_GET_STARTED (3200),
		AGENT_GET_SUCCESS (3201),
		AGENT_GET_FAILED (-3201),
		AGENT_LOAD_FAILED (-3000),
		
		RMI_SENT (4000),
		RMI_RECEIVED(4100),
		RMI_ANSWERED(4150),
		RMI_ANSWER_RECEIVED(4200),
		
		MESSAGE_RECEIVED (5000),
		MESSAGE_RECEIVED_ANSWER (5001),
		MESSAGE_SENDING(5100),
		MESSAGE_FORWARDING (5200),
		MESSAGE_FAILED (-5300),
		MESSAGE_RECEIVED_UNKNOWN(-5300),
		
		RESPONSE_SENDING (5500),
		RESPONSE_FAILED  (5600),
		
		PASTRY_NEW_TOPIC_CHILD (6000),
		PASTRY_REMOVED_TOPIC_CHILD (6010),
		PASTRY_TOPIC_SUBSCRIPTION_FAILED (-6100),
		PASTRY_TOPIC_SUBSCRIPTION_SUCCESS (6100),
		
		//Start service messages
		//When adding new Events, please mind that the area from |7000| - |7999| is reserved for service messages
		//(and thus will only be monitored by the monitoring observer if the service monitoring flag was set)
		SERVICE_STARTUP (7000),
		SERVICE_SHUTDOWN (7100),
		SERVICE_INVOCATION (7200),
		SERVICE_INVOCATION_FINISHED (7210),
		SERVICE_INVOCATION_FAILED (-7210),
		
		SERVICE_ADD_TO_MONITORING(7300), //Used by the LoggingObserver itself
		
		//To be used by the service developer
		SERVICE_MESSAGE (7500),
		SERVICE_CUSTOM_MESSAGE_1 (7501),
		SERVICE_CUSTOM_MESSAGE_2 (7502),
		SERVICE_CUSTOM_MESSAGE_3 (7503),
		SERVICE_CUSTOM_MESSAGE_4 (7504),
		SERVICE_CUSTOM_MESSAGE_5 (7505),
		SERVICE_CUSTOM_MESSAGE_6 (7506),
		SERVICE_CUSTOM_MESSAGE_7 (7507),
		SERVICE_CUSTOM_MESSAGE_8 (7508),
		SERVICE_CUSTOM_MESSAGE_9 (7509),
		SERVICE_CUSTOM_MESSAGE_10 (7510),
		SERVICE_CUSTOM_MESSAGE_11 (7511),
		SERVICE_CUSTOM_MESSAGE_12 (7512),
		SERVICE_CUSTOM_MESSAGE_13 (7513),
		SERVICE_CUSTOM_MESSAGE_14 (7514),
		SERVICE_CUSTOM_MESSAGE_15 (7515),
		SERVICE_CUSTOM_MESSAGE_16 (7516),
		SERVICE_CUSTOM_MESSAGE_17 (7517),
		SERVICE_CUSTOM_MESSAGE_18 (7518),
		SERVICE_CUSTOM_MESSAGE_19 (7519),
		SERVICE_CUSTOM_MESSAGE_20 (7520),
		SERVICE_CUSTOM_MESSAGE_21 (7521),
		SERVICE_CUSTOM_MESSAGE_22 (7522),
		SERVICE_CUSTOM_MESSAGE_23 (7523),
		SERVICE_CUSTOM_MESSAGE_24 (7524),
		SERVICE_CUSTOM_MESSAGE_25 (7525),
		SERVICE_CUSTOM_MESSAGE_26 (7526),
		SERVICE_CUSTOM_MESSAGE_27 (7527),
		SERVICE_CUSTOM_MESSAGE_28 (7528),
		SERVICE_CUSTOM_MESSAGE_29 (7529),
		SERVICE_CUSTOM_MESSAGE_30 (7530),
		SERVICE_CUSTOM_MESSAGE_31 (7531),
		SERVICE_CUSTOM_MESSAGE_32 (7532),
		SERVICE_CUSTOM_MESSAGE_33 (7533),
		SERVICE_CUSTOM_MESSAGE_34 (7534),
		SERVICE_CUSTOM_MESSAGE_35 (7535),
		SERVICE_CUSTOM_MESSAGE_36 (7536),
		SERVICE_CUSTOM_MESSAGE_37 (7537),
		SERVICE_CUSTOM_MESSAGE_38 (7538),
		SERVICE_CUSTOM_MESSAGE_39 (7539),
		SERVICE_CUSTOM_MESSAGE_40 (7540),
		SERVICE_CUSTOM_MESSAGE_41 (7541),
		SERVICE_CUSTOM_MESSAGE_42 (7542),
		SERVICE_CUSTOM_MESSAGE_43 (7543),
		SERVICE_CUSTOM_MESSAGE_44 (7544),
		SERVICE_CUSTOM_MESSAGE_45 (7545),
		SERVICE_CUSTOM_MESSAGE_46 (7546),
		SERVICE_CUSTOM_MESSAGE_47 (7547),
		SERVICE_CUSTOM_MESSAGE_48 (7548),
		SERVICE_CUSTOM_MESSAGE_49 (7549),
		SERVICE_CUSTOM_MESSAGE_50 (7550),
		SERVICE_CUSTOM_MESSAGE_51 (7551),
		SERVICE_CUSTOM_MESSAGE_52 (7552),
		SERVICE_CUSTOM_MESSAGE_53 (7553),
		SERVICE_CUSTOM_MESSAGE_54 (7554),
		SERVICE_CUSTOM_MESSAGE_55 (7555),
		SERVICE_CUSTOM_MESSAGE_56 (7556),
		SERVICE_CUSTOM_MESSAGE_57 (7557),
		SERVICE_CUSTOM_MESSAGE_58 (7558),
		SERVICE_CUSTOM_MESSAGE_59 (7559),
		SERVICE_CUSTOM_MESSAGE_60 (7560),
		SERVICE_CUSTOM_MESSAGE_61 (7561),
		SERVICE_CUSTOM_MESSAGE_62 (7562),
		SERVICE_CUSTOM_MESSAGE_63 (7563),
		SERVICE_CUSTOM_MESSAGE_64 (7564),
		SERVICE_CUSTOM_MESSAGE_65 (7565),
		SERVICE_CUSTOM_MESSAGE_66 (7566),
		SERVICE_CUSTOM_MESSAGE_67 (7567),
		SERVICE_CUSTOM_MESSAGE_68 (7568),
		SERVICE_CUSTOM_MESSAGE_69 (7569),
		SERVICE_CUSTOM_MESSAGE_70 (7570),
		SERVICE_CUSTOM_MESSAGE_71 (7571),
		SERVICE_CUSTOM_MESSAGE_72 (7572),
		SERVICE_CUSTOM_MESSAGE_73 (7573),
		SERVICE_CUSTOM_MESSAGE_74 (7574),
		SERVICE_CUSTOM_MESSAGE_75 (7575),
		SERVICE_CUSTOM_MESSAGE_76 (7576),
		SERVICE_CUSTOM_MESSAGE_77 (7577),
		SERVICE_CUSTOM_MESSAGE_78 (7578),
		SERVICE_CUSTOM_MESSAGE_79 (7579),
		SERVICE_CUSTOM_MESSAGE_80 (7580),
		SERVICE_CUSTOM_MESSAGE_81 (7581),
		SERVICE_CUSTOM_MESSAGE_82 (7582),
		SERVICE_CUSTOM_MESSAGE_83 (7583),
		SERVICE_CUSTOM_MESSAGE_84 (7584),
		SERVICE_CUSTOM_MESSAGE_85 (7585),
		SERVICE_CUSTOM_MESSAGE_86 (7586),
		SERVICE_CUSTOM_MESSAGE_87 (7587),
		SERVICE_CUSTOM_MESSAGE_88 (7588),
		SERVICE_CUSTOM_MESSAGE_89 (7589),
		SERVICE_CUSTOM_MESSAGE_90 (7590),
		SERVICE_CUSTOM_MESSAGE_91 (7591),
		SERVICE_CUSTOM_MESSAGE_92 (7592),
		SERVICE_CUSTOM_MESSAGE_93 (7593),
		SERVICE_CUSTOM_MESSAGE_94 (7594),
		SERVICE_CUSTOM_MESSAGE_95 (7595),
		SERVICE_CUSTOM_MESSAGE_96 (7596),
		SERVICE_CUSTOM_MESSAGE_97 (7597),
		SERVICE_CUSTOM_MESSAGE_98 (7598),
		SERVICE_CUSTOM_MESSAGE_99 (7599),
		
		
		
		SERVICE_ERROR (-7500),
		//To be used by the service developer
		SERVICE_CUSTOM_ERROR_1 (-7501),
		SERVICE_CUSTOM_ERROR_2 (-7502),
		SERVICE_CUSTOM_ERROR_3 (-7503),
		SERVICE_CUSTOM_ERROR_4 (-7504),
		SERVICE_CUSTOM_ERROR_5 (-7505),
		SERVICE_CUSTOM_ERROR_6 (-7506),
		SERVICE_CUSTOM_ERROR_7 (-7507),
		SERVICE_CUSTOM_ERROR_8 (-7508),
		SERVICE_CUSTOM_ERROR_9 (-7509),
		SERVICE_CUSTOM_ERROR_10 (-7510),
		SERVICE_CUSTOM_ERROR_11 (-7511),
		SERVICE_CUSTOM_ERROR_12 (-7512),
		SERVICE_CUSTOM_ERROR_13 (-7513),
		SERVICE_CUSTOM_ERROR_14 (-7514),
		SERVICE_CUSTOM_ERROR_15 (-7515),
		SERVICE_CUSTOM_ERROR_16 (-7516),
		SERVICE_CUSTOM_ERROR_17 (-7517),
		SERVICE_CUSTOM_ERROR_18 (-7518),
		SERVICE_CUSTOM_ERROR_19 (-7519),
		SERVICE_CUSTOM_ERROR_20 (-7520),
		SERVICE_CUSTOM_ERROR_21 (-7521),
		SERVICE_CUSTOM_ERROR_22 (-7522),
		SERVICE_CUSTOM_ERROR_23 (-7523),
		SERVICE_CUSTOM_ERROR_24 (-7524),
		SERVICE_CUSTOM_ERROR_25 (-7525),
		SERVICE_CUSTOM_ERROR_26 (-7526),
		SERVICE_CUSTOM_ERROR_27 (-7527),
		SERVICE_CUSTOM_ERROR_28 (-7528),
		SERVICE_CUSTOM_ERROR_29 (-7529),
		SERVICE_CUSTOM_ERROR_30 (-7530),
		SERVICE_CUSTOM_ERROR_31 (-7531),
		SERVICE_CUSTOM_ERROR_32 (-7532),
		SERVICE_CUSTOM_ERROR_33 (-7533),
		SERVICE_CUSTOM_ERROR_34 (-7534),
		SERVICE_CUSTOM_ERROR_35 (-7535),
		SERVICE_CUSTOM_ERROR_36 (-7536),
		SERVICE_CUSTOM_ERROR_37 (-7537),
		SERVICE_CUSTOM_ERROR_38 (-7538),
		SERVICE_CUSTOM_ERROR_39 (-7539),
		SERVICE_CUSTOM_ERROR_40 (-7540),
		SERVICE_CUSTOM_ERROR_41 (-7541),
		SERVICE_CUSTOM_ERROR_42 (-7542),
		SERVICE_CUSTOM_ERROR_43 (-7543),
		SERVICE_CUSTOM_ERROR_44 (-7544),
		SERVICE_CUSTOM_ERROR_45 (-7545),
		SERVICE_CUSTOM_ERROR_46 (-7546),
		SERVICE_CUSTOM_ERROR_47 (-7547),
		SERVICE_CUSTOM_ERROR_48 (-7548),
		SERVICE_CUSTOM_ERROR_49 (-7549),
		SERVICE_CUSTOM_ERROR_50 (-7550),
		SERVICE_CUSTOM_ERROR_51 (-7551),
		SERVICE_CUSTOM_ERROR_52 (-7552),
		SERVICE_CUSTOM_ERROR_53 (-7553),
		SERVICE_CUSTOM_ERROR_54 (-7554),
		SERVICE_CUSTOM_ERROR_55 (-7555),
		SERVICE_CUSTOM_ERROR_56 (-7556),
		SERVICE_CUSTOM_ERROR_57 (-7557),
		SERVICE_CUSTOM_ERROR_58 (-7558),
		SERVICE_CUSTOM_ERROR_59 (-7559),
		SERVICE_CUSTOM_ERROR_60 (-7560),
		SERVICE_CUSTOM_ERROR_61 (-7561),
		SERVICE_CUSTOM_ERROR_62 (-7562),
		SERVICE_CUSTOM_ERROR_63 (-7563),
		SERVICE_CUSTOM_ERROR_64 (-7564),
		SERVICE_CUSTOM_ERROR_65 (-7565),
		SERVICE_CUSTOM_ERROR_66 (-7566),
		SERVICE_CUSTOM_ERROR_67 (-7567),
		SERVICE_CUSTOM_ERROR_68 (-7568),
		SERVICE_CUSTOM_ERROR_69 (-7569),
		SERVICE_CUSTOM_ERROR_70 (-7570),
		SERVICE_CUSTOM_ERROR_71 (-7571),
		SERVICE_CUSTOM_ERROR_72 (-7572),
		SERVICE_CUSTOM_ERROR_73 (-7573),
		SERVICE_CUSTOM_ERROR_74 (-7574),
		SERVICE_CUSTOM_ERROR_75 (-7575),
		SERVICE_CUSTOM_ERROR_76 (-7576),
		SERVICE_CUSTOM_ERROR_77 (-7577),
		SERVICE_CUSTOM_ERROR_78 (-7578),
		SERVICE_CUSTOM_ERROR_79 (-7579),
		SERVICE_CUSTOM_ERROR_80 (-7580),
		SERVICE_CUSTOM_ERROR_81 (-7581),
		SERVICE_CUSTOM_ERROR_82 (-7582),
		SERVICE_CUSTOM_ERROR_83 (-7583),
		SERVICE_CUSTOM_ERROR_84 (-7584),
		SERVICE_CUSTOM_ERROR_85 (-7585),
		SERVICE_CUSTOM_ERROR_86 (-7586),
		SERVICE_CUSTOM_ERROR_87 (-7587),
		SERVICE_CUSTOM_ERROR_88 (-7588),
		SERVICE_CUSTOM_ERROR_89 (-7589),
		SERVICE_CUSTOM_ERROR_90 (-7590),
		SERVICE_CUSTOM_ERROR_91 (-7591),
		SERVICE_CUSTOM_ERROR_92 (-7592),
		SERVICE_CUSTOM_ERROR_93 (-7593),
		SERVICE_CUSTOM_ERROR_94 (-7594),
		SERVICE_CUSTOM_ERROR_95 (-7595),
		SERVICE_CUSTOM_ERROR_96 (-7596),
		SERVICE_CUSTOM_ERROR_97 (-7597),
		SERVICE_CUSTOM_ERROR_98 (-7598),
		SERVICE_CUSTOM_ERROR_99 (-7599),
		//End service messages
		
		
		HTTP_CONNECTOR_MESSAGE (8000),
		HTTP_CONNECTOR_REQUEST (8001),
		HTTP_CONNECTOR_ERROR (-8100);

		
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
	 */
	public void logEvent (Event event) {
		log (event, null, null, null, null, null);
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param remarks
	 */
	public void logEvent (Event event, String remarks) {
		log (event, null, null, null, null, remarks);		
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param remarks
	 */
	public void logEvent (Event event, Object sourceNode, String remarks) {
		log (event, sourceNode, null, null, null, remarks);		
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param remarks
	 */
	public void logEvent (Event event, Object sourceNode, Long sourceAgentId, String remarks) {
		log (event, sourceNode, sourceAgentId, null, null, remarks);
	}
	
	
	/**
	 * Logs a node event.
	 * 
	 * @param event
	 * @param sourceNode
	 * @param sourceAgentId
	 * @param destinationNode
	 * @param destinationAgentId
	 * @param remarks
	 */
	public void logEvent (Event event, Object sourceNode, Long sourceAgentId, Object destinationNode, Long destinationAgentId, String remarks) {
		log (event, sourceNode, sourceAgentId, destinationNode, destinationAgentId, remarks);
	}
	
	/**
	 * Derive a String representation for a node from the given identifier
	 * object. The type of the object depends on the setting of the current node.
	 * 
	 * Tries to specify an ip address and a port for an actual p2p node
	 * ({@link i5.las2peer.p2p.PastryNodeImpl} or {@link rice.pastry.NodeHandle}).
	 * 
	 * @param node
	 * @return string representation for the given node object
	 */
	protected String getNodeRepresentation ( Object node ) {
		if ( node == null)
			return null;
		else if ( node instanceof SocketNodeHandle ) {
			SocketNodeHandle nh = (SocketNodeHandle) node;
			return nh.getId() + "/" + nh.getIdentifier();
		}
		else if ( node instanceof PastryNode ) {
			PastryNode pNode = (PastryNode) node;
			return getNodeRepresentation ( pNode.getLocalNodeHandle());
		}
		else
			return "" + node + " (" + node.getClass().getName() + ")";
	}
	
	
	/**
	 * Writes a log entry.
	 * 
	 * @param event necessary
	 * @param sourceNode an object representing some kind of node
	 * 		will be transferred to an node representation if possible (see {@link #getNodeRepresentation}) (can be null)
	 * @param sourceAgentId the id of the source agent (can be null)
	 * @param destinationNode (can be null)
	 * @param destinationAgentId (can be null)
	 * @param remarks (can be null)
	 */
	protected void log (Event event, Object sourceNode, Long sourceAgentId, Object destinationNode , Long destinationAgentId, String remarks ) {
		long timestamp =  new Date().getTime();
		String sourceNodeRepresentation = getNodeRepresentation (sourceNode);
		String destinationNodeRepresentation = getNodeRepresentation (destinationNode);
		
		writeLog (timestamp, event, sourceNodeRepresentation, sourceAgentId, destinationNodeRepresentation, destinationAgentId, remarks);
	}
	
	
	/**
	 * This method has to be implemented by any (non abstract) deriving class.
	 * Each call represents one event to log by this observer. All parameters except the time stamp 
	 * and the event may be null.
	 * 
	 * @param timestamp		UNIX time stamp of the event
	 * @param event			the event to log
	 * @param sourceNode	a source (p2p) node of the event (e.g. message sender)
	 * @param sourceAgentId	a source (las2peer) agent of the event (e.g. message sender)
	 * @param destinationNode	a destination (p2p) node for the event (e.g. message receiver)
	 * @param destinationAgentId a destination (las2peer) agent of the event (e.g. message receiver)
	 * @param remarks		(optional) additional remarks
	 */
	protected abstract void writeLog (Long timestamp, Event event, String sourceNode, Long sourceAgentId, String destinationNode, Long destinationAgentId, String remarks);
	
	
}