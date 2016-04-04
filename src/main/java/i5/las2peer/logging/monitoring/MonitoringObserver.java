package i5.las2peer.logging.monitoring;

import i5.las2peer.communication.Message;
import i5.las2peer.execution.L2pServiceException;
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
 * This is the base class of the logging module of LAS2peer. It sends the collected data to the
 * "Monitoring Data Processing Service" via the LAS2peer message concept.
 *
 * 
 *
 */
public class MonitoringObserver implements NodeObserver {

	private boolean readyForInitializing = false; // Is set to false as long as the node is not ready to initialize the
													// monitoring agents.
	private boolean initializedDone = false; // Used to determine, if the initialization process has finished.
	private MonitoringMessage[] monitoringMessages; // The size is determined by the constructor. Will be send at once.
	private int messagesCount; // Counter to determine how many messages are currently stored at the messages array.
	private MonitoringAgent sendingAgent; // The agent responsible for this observer.
	private MonitoringAgent receivingAgent; // The agent registered at the Processing Service.
	private MessageResultListener messageResultListener; // The ResultListener that will be used for message-sending
															// (currently unused though).
	private Node registeredAt; // If we want to send messages, we need a sending node.

	/**
	 * 
	 * Constructor for the MonitoringObserver. Can be added to a node by adding "startObserver" after the bootstrap
	 * parameter at the {@link i5.las2peer.tools.L2pNodeLauncher}. Will be instantiated at a
	 * {@link i5.las2peer.p2p.Node}.
	 *
	 * @param messageCache determines, how many messages will be stored locally before send to the central collection
	 *            unit (&gt; 50)
	 * @param registeredAt the node this observer is registered at
	 * 
	 */
	public MonitoringObserver(int messageCache, Node registeredAt) {
		this.registeredAt = registeredAt;
		if (messageCache < 50)
			messageCache = 50; // Minimum cache to give the observer enough time to initialize before first sending
		this.monitoringMessages = new MonitoringMessage[messageCache];
		this.messagesCount = 0;
		try {
			sendingAgent = MonitoringAgent.createMonitoringAgent("sendingAgentPass");
		} catch (CryptoException e) {
			e.printStackTrace();
		} catch (L2pSecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Helper method that is called after a node has been fully configured. It registers the monitoring agent
	 * responsible for this node and tries to find the agent of the processing node by invoking the
	 * i5.las2peer.services.monitoring.processing.MonitoringDataProcessingService#getReceivingAgentId method.
	 *
	 * @return true, if successfully initialized
	 */
	private boolean initializeAgents() {
		try {
			sendingAgent.unlockPrivateKey("sendingAgentPass");
			registeredAt.storeAgent(sendingAgent);
			registeredAt.registerReceiver(sendingAgent);
			System.out.println("Monitoring: Registered MonitoringAgent: " + sendingAgent.getId());

		} catch (AgentException e) {
			System.out.println("Monitoring: Problems registering MonitoringAgent!" + e);
			e.printStackTrace();
		} catch (L2pSecurityException e) {
			System.out.println("Monitoring: Problems registering MonitoringAgent!" + e);
			e.printStackTrace();
		}

		try {
			System.out.println("Monitoring: Trying to invoke Processing Service..");
			String[] testParameters = { "Node " + registeredAt.getNodeId() + " registered observer!" };
			long receivingAgentId = (Long) registeredAt.invoke(sendingAgent, // TODO workaround: remove specified version when ServiceInfoAgent is replaced
					"i5.las2peer.services.monitoring.processing.MonitoringDataProcessingService@0.1", "getReceivingAgentId",
					testParameters);
			try {
				receivingAgent = (MonitoringAgent) registeredAt.getAgent(receivingAgentId);
				System.out.println("Monitoring: Fetched receiving MonitoringAgent: " + receivingAgent.getId());
			} catch (AgentNotKnownException e) {
				e.printStackTrace();
			}
		} catch (UnlockNeededException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (L2pSecurityException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (TimeoutException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (ServiceInvocationException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (AgentNotKnownException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		} catch (L2pServiceException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 
	 * Processes the incoming data by generating a {@link MonitoringMessage} of it. This {@link MonitoringMessage} will
	 * be stored in an array of {@link MonitoringMessage}s, which will be send via a
	 * {@link i5.las2peer.communication.Message} to the Processing Service.
	 *
	 */
	@Override
	public void log(Long timestamp, Event event, String sourceNode, Long sourceAgentId, String destinationNode,
			Long destinationAgentId, String remarks) {
		// Now this is a bit tricky..
		// We get a "Node is Running" event, but we have to wait until the next event to be sure that
		// the method that called the "Running" event has terminated, otherwise our request will crash this startup
		// method
		if (readyForInitializing) {
			readyForInitializing = false; // "Nearly" atomic, enough in this case;-)
			initializedDone = initializeAgents();
		}
		if (event == Event.NODE_STATUS_CHANGE && remarks.equals("RUNNING")) {
			readyForInitializing = true;
		}
		if (sourceNode == null)
			return; // We do not log events without a source node into a database with different sources;-)
		if (messagesCount >= monitoringMessages.length) {
			if (initializedDone) {
				messagesCount = 0;
				sendMessages();
			} else {
				messagesCount = 0;
				System.out.println("Monitoring: Problems with initializing Agents..");
			}
		}
		monitoringMessages[messagesCount] = new MonitoringMessage(timestamp, event, sourceNode, sourceAgentId,
				destinationNode, destinationAgentId, remarks);
		messagesCount++;
		// We can only send our last message if the node is closing, so we will have to assume that all services are
		// shutdown
		// when a node is closed (seems to be a fair bet)
		if (event == Event.NODE_SHUTDOWN) {
			if (initializedDone) {
				// To remove "old" messages since they are not overwritten
				int counter = messagesCount;
				while (counter < monitoringMessages.length) {
					monitoringMessages[counter] = null;
					counter++;
				}
				sendMessages();
			} else {
				System.out.println("Monitoring: Problems with initializing Agents..");
			}
		}
	}

	/**
	 * 
	 * Helper method that actually sends the {@link MonitoringMessage}s to the Processing Service's agent.
	 *
	 */
	private void sendMessages() {
		try {
			Message LAS2peerMessage = new Message(sendingAgent, receivingAgent, monitoringMessages);
			messageResultListener = new MessageResultListener(2000); // unused
			registeredAt.sendMessage(LAS2peerMessage, messageResultListener);
			System.out.println("Monitoring: message " + LAS2peerMessage.getId() + " send!");
		} catch (L2pSecurityException e) {
			e.printStackTrace();
		} catch (EncodingFailedException e) {
			e.printStackTrace();
		} catch (SerializationException e) {
			e.printStackTrace();
		}
	}

}