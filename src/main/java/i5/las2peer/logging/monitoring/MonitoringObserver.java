package i5.las2peer.logging.monitoring;

import i5.las2peer.communication.Message;
import i5.las2peer.execution.L2pThread;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.execution.UnlockNeededException;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MonitoringAgent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;


/**
 * 
 * This is the base class of the logging module of LAS2peer.
 * It sends the collected data to the Monitoring Data Processing Service
 * via the LAS2peer message concept.
 * 
 * UNDER CONSTRUCTION!!
 * 
 * 
 * @author Peter de Lange
 *
 */
public class MonitoringObserver extends NodeObserver {
	private boolean monitor = true; //Extra check to stop sending if the Processing Service does not seem available.
	private MonitoringMessage[] messages; //The size is determined by the constructor, will be send at once.
	int messagesCount; //Counter to determine how many messages are currently inside the messages Array.
	MonitoringAgent sendingAgent; //The Agent responsible for this observer.
	MonitoringAgent receivingAgent; //The Agent registered at the Processing Service.
	MessageResultListener messageResultListener; //The ResultListener that will be used for message-sending.
	
	
	public MonitoringObserver (int messageCache) {
		this.messages = new MonitoringMessage[messageCache];
		this.messagesCount = 0;
		try {
			sendingAgent = MonitoringAgent.createMonitoringAgent("SendingAgent");
			sendingAgent.unlockPrivateKey("SendingAgent");
			try {
				getActiveNode().storeAgent(sendingAgent);
			} catch (AgentException e) {
				e.printStackTrace();
			}
		} catch (CryptoException e){
			e.printStackTrace();	
		} catch(L2pSecurityException e) {
			e.printStackTrace();
		}
		
		try {
			receivingAgent = (MonitoringAgent) getActiveNode().invokeGlobally(sendingAgent,
					"i5.las2peer.services.monitoring.processing.MonitoringDataProcessingService", "getReceivingAgent", null);
		} catch (UnlockNeededException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			monitor = false;
			e.printStackTrace();
		} catch (L2pSecurityException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			monitor = false;
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			monitor = false;
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			monitor = false;
			e.printStackTrace();
		} catch (ServiceInvocationException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			monitor = false;
			e.printStackTrace();
		}
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
		if(!monitor)
			return; //TODO: Throw Exception
			try {
				Message message = new Message(sendingAgent,receivingAgent,messages);
				if(messageResultListener == null || messageResultListener.isFinished()){
					messageResultListener = new MessageResultListener(2000);
					getActiveNode().sendMessage(message, messageResultListener);
					try {
						messageResultListener.waitForOneAnswer(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else{
					System.out.println("Monitoring: busy.."); //TODO
				}
		} catch (L2pSecurityException e) {
			e.printStackTrace();
		} catch (EncodingFailedException e) {
			e.printStackTrace();
		} catch (SerializationException e){
			e.printStackTrace();
		}
 		
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
