package i5.las2peer.logging.monitoring;

import i5.las2peer.communication.Message;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.execution.UnlockNeededException;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.p2p.AgentNotKnownException;
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
	private boolean readyForInitializing = false; //Is set to false, as long as the Agents are not initialized.
	private MonitoringMessage[] messages; //The size is determined by the constructor, will be send at once.
	int messagesCount; //Counter to determine how many messages are currently inside the messages Array.
	MonitoringAgent sendingAgent; //The Agent responsible for this observer.
	MonitoringAgent receivingAgent; //The Agent registered at the Processing Service.
	MessageResultListener messageResultListener; //The ResultListener that will be used for message-sending.
	Node registeredAt; //If we want to send messages, we need a node.
	
	boolean initializedDone = false;
	
	public MonitoringObserver (int messageCache, Node registeredAt) {
		this.registeredAt = registeredAt;
		if(messageCache < 50)
			messageCache = 50; //Minimum cache to give the observer enough time to initialize before first sending
		this.messages = new MonitoringMessage[messageCache];
		this.messagesCount = 0;
		try {
			sendingAgent = MonitoringAgent.createMonitoringAgent("sendingAgentPass");
		} catch (CryptoException e) {
			e.printStackTrace();
		} catch (L2pSecurityException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private boolean initializeAgents(){
		try {
			System.out.println("Monitoring: initializing..");
			sendingAgent.unlockPrivateKey("sendingAgentPass");
			System.out.println("Monitoring: unlocked..");
			System.out.println("Monitoring: Storing Agent " +  sendingAgent.getId());
			registeredAt.storeAgent(sendingAgent);
			registeredAt.registerReceiver(sendingAgent);
			System.out.println("Monitoring: Registered Receiver: " +  sendingAgent.getId());

		} catch (AgentException e) {
			System.out.println("Monitoring: Problem Storing Agent!" + e);
			e.printStackTrace();
		} catch(L2pSecurityException e) {
			System.out.println("Monitoring: Problem Storing Agent!" + e);
			e.printStackTrace();
		}
		
		try {
			System.out.println("Monitoring: Trying to invoke Processing Service..");
			String[] testParameters = {"Node " + registeredAt.getNodeId() + " registered observer!"};
			long receivingAgentId =  (Long) registeredAt.invokeGlobally(sendingAgent,
					"i5.las2peer.services.monitoring.processing.MonitoringDataProcessingService", "getReceivingAgentId", testParameters);
			
			System.out.println("Monitoring: Invoke success, received id: " + receivingAgentId);
			try {
				receivingAgent = (MonitoringAgent) registeredAt.getAgent(receivingAgentId);
				System.out.println("Monitoring: Fetched receiving agent: " + receivingAgent.getId());
			} catch (AgentNotKnownException e) {
				e.printStackTrace();
			}
		} catch (UnlockNeededException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (L2pSecurityException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (TimeoutException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (ServiceInvocationException e) {
			System.out.println("Monitoring: Processing service does not seem available! " + e);
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	protected void writeLog(long timestamp, long timespan, Event event,
			String sourceNode, Long sourceAgentId, String originNode,
			Long originAgentId, String remarks) {
		//Now this is a bit tricky..
		//We get a "Node is Running" event, but we have to wait until the next event to be sure that
		//the method that called the "Running" event has terminated, otherwise our request will crash this startup method
		if(readyForInitializing){
			readyForInitializing = false; //"Nearly" atomic, enough in this case;-)
			initializedDone = initializeAgents();
		}
		if(event == Event.NODE_STATUS_CHANGE && remarks.equals("RUNNING")){
			readyForInitializing = true;
		}
		if(messagesCount >= messages.length){
			
			if(initializedDone){
				messagesCount = 0;
				sendMessages();
			}
			else{
				messagesCount = 0;
				System.out.println("Monitoring: Problems with identifying Agents..");
			}
		}
		messages[messagesCount] = new MonitoringMessage(timestamp, timespan, event, sourceNode,
				sourceAgentId, originNode, originAgentId, remarks);
		messagesCount++;
	}
	
	
	private void sendMessages() {
		System.out.println("Sending..");
		try {
			Message message = new Message(sendingAgent,receivingAgent,messages);
			System.out.println("Monitoring: Message created!");
			if(messageResultListener == null || messageResultListener.isFinished()){
				messageResultListener = new MessageResultListener(2000);
				registeredAt.sendMessage(message, messageResultListener);
				try {
					messageResultListener.waitForOneAnswer(2000);
					System.out.println("Monitoring: message " + message.getId() + " send!");
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
	
}
