package i5.las2peer.logging.monitoring;

import i5.las2peer.logging.NodeObserver;


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
	
	
	public MonitoringObserver () {
		//TODO
	}
	
	
	@Override
	protected void writeLog(long timestamp, long timespan, Event e,
			String sourceNode, Long sourceAgentId, String originNode,
			Long originAgentId, String remarks) {
		//TODO
	}
	
	
}
