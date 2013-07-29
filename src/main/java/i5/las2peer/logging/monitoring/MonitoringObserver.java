package i5.las2peer.logging.monitoring;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.ServiceAgent;


/**
 * This will be the base class of the logging module of LAS2peer.
 * It will send the collected data to the Monitoring Data Processing Service
 * via the LAS2peer message concept.
 * 
 * UNDER CONSTRUCTION!!
 * 
 * 
 * @author Peter de Lange
 *
 */
public class MonitoringObserver extends NodeObserver {
	
	private MonitoringMessage[] messages;
	int messagesCount;
	
	public MonitoringObserver (int messageCache) {
		this.messages = new MonitoringMessage[messageCache];
		this.messagesCount = 0;
	}
	
	
	@Override
	protected void writeLog(long timestamp, long timespan, Event event,
			String sourceNode, Long sourceAgentId, String originNode,
			Long originAgentId, String remarks) {
		if(messagesCount >= messages.length){
			sendMessages();
			messagesCount = 0;
		}
		messages[messagesCount] = new MonitoringMessage(timestamp, timespan, event, sourceNode,
				sourceAgentId, originNode, originAgentId, remarks);
	}
	
	
	private void sendMessages() {
		try {
			ServiceAgent processingService = 
					getActiveNode().getServiceAgent("i5.las2peer.services.monitoring.processing.MonitoringDataProcessingService");
		} catch (AgentNotKnownException e) {
			e.printStackTrace();
		}
		//TODO Great, now we got everything we need...except for a Sending Agent (TODO), a receiving Agent (TODO)..and basically everything else
		//Alternatively we could invoke globally..but that seems a bit "not-scaling"
	}
	
	//Helper methods to get the current node
	private final L2pThread getL2pThread () {
		Thread t = Thread.currentThread();
		if (! ( t instanceof L2pThread ))
			throw new IllegalStateException ( "Not executed in a L2pThread environment!");
		return (L2pThread) t;
	}
		
	
	private Node getActiveNode() {
		return getL2pThread().getContext().getLocalNode();
	}
	
}
