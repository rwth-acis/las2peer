package i5.las2peer.logging.monitoring;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.communication.Message;
import i5.las2peer.logging.NodeObserver;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.MonitoringAgent;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

import java.util.concurrent.*;

/**
 * 
 * This is the base class of the logging module of las2peer. It sends the collected data to the "Monitoring Data
 * Processing Service" via the las2peer message concept.
 *
 * 
 *
 */
public class MonitoringObserver implements NodeObserver {

	public static final String DATA_PROCESSING_SERVICE = "i5.las2peer.services.mobsos.dataProcessing.MonitoringDataProcessingService";
	private static final int RMI_TIMEOUT = 5;
	private boolean readyForInitializing = true; // Is set to false as long as the node is not ready to initialize the
													// monitoring agents.
	private boolean initializedDone = false; // Used to determine, if the initialization process has finished.
	private MonitoringMessage[] monitoringMessages; // The size is determined by the constructor. Will be send at once.
	private int messagesCount; // Counter to determine how many messages are currently stored at the messages array.
	private MonitoringAgent sendingAgent; // The agent responsible for this observer.
	private MonitoringAgent receivingAgent; // The agent registered at the Processing Service.
	private MessageResultListener messageResultListener; // The ResultListener that will be used for message-sending
															// (currently unused though).
	private Node registeredAt; // If we want to send messages, we need a sending node.
	private long waitUntilSend;

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
		waitUntilSend = 1000 * 10; // 10 s
		Thread sendingThread = new Thread() {
			public void run() {
				try {
					while (true) {
						Thread.sleep(waitUntilSend);
						if (messagesCount > 0) {
							checkInit();
							// Send messages after waitUntilSend ms
							if (initializedDone) {
								// Do not send old messages...
								int counter = messagesCount;
								while (counter < monitoringMessages.length) {
									monitoringMessages[counter] = null;
									counter++;
								}
								// reset messageCount before sending messages
								// otherwise StackOverflow...
								messagesCount = 0;
								sendMessages();
							} else {
								messagesCount = 0;
								System.out.println("Monitoring: Problems with initializing Agents..");
							}
						}
					}
				} catch (InterruptedException v) {
					System.out.println(v);
				}
			}
		};

		sendingThread.start();
		if (messageCache < 50) {
			messageCache = 50; // Minimum cache to give the observer enough time to initialize before first sending
		}
		this.monitoringMessages = new MonitoringMessage[messageCache];
		this.messagesCount = 0;
		try {
			sendingAgent = MonitoringAgent.createMonitoringAgent("sendingAgentPass");
		} catch (CryptoException | AgentOperationFailedException e) {
			e.printStackTrace();
		}
	}


	private void checkInit() {
		if (readyForInitializing) {
			readyForInitializing = false; // "Nearly" atomic, enough in this case;-)
			initializedDone = initializeAgents();
			if (!initializedDone) {
				readyForInitializing = true;
			}
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
			if (!registeredAt.hasAgent(sendingAgent.getIdentifier())) {
				sendingAgent.unlock("sendingAgentPass");
				registeredAt.storeAgent(sendingAgent);
				registeredAt.registerReceiver(sendingAgent);
				System.out.println("Monitoring: Registered MonitoringAgent: " + sendingAgent.getIdentifier());
			}
		} catch (AgentException e) {
			System.out.println("Monitoring: Problems registering MonitoringAgent!" + e);
		}

		try {
			System.out.println("Monitoring: Trying to invoke Processing Service..");
			String receivingAgentId = getReceivingAgentID();
			try {
				receivingAgent = (MonitoringAgent) registeredAt.getAgent(receivingAgentId);
				System.out.println("Monitoring: Fetched receiving MonitoringAgent: " + receivingAgent.getIdentifier());
			} catch (AgentNotFoundException e) {
				e.printStackTrace();
			}
		} catch (AgentException | ServiceInvocationException e) {
			System.out.println("Monitoring: Processing Service does not seem available! " + e);
			return false;
		}
		return true;
	}

	/**
	 * Try to fetch the agent ID of the processing service.
	 * <p>
	 * In rare cases this method invocation might cause a deadlock,
	 * because the processing service is not fully booted up.
	 * To avoid this we set a timeout on the method invocation. This is what the service executor is for.
	 *
	 * @return The agent ID of the processing service.
	 * @throws ServiceInvocationException Any exception is converted to a ServiceInvocationException in order to behave
	 *                                    like the invoke method.
	 */
	private String getReceivingAgentID() throws ServiceInvocationException {
		ExecutorService executor = Executors.newCachedThreadPool();
		Callable<String> task = () -> {
			String[] testParameters = {"Node " + registeredAt.getNodeId() + " registered observer!"};
			return (String) registeredAt.invoke(sendingAgent, DATA_PROCESSING_SERVICE,
					"getReceivingAgentId", testParameters);
		};
		Future<String> future = executor.submit(task);
		try {
			return future.get(RMI_TIMEOUT, TimeUnit.SECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException ex) {
			future.cancel(true);
			throw new ServiceInvocationException("Monitoring: Could not fetch monitoring service agent. " +
					"The monitoring service is either offline or still booting.", ex);
		}
	}

	/**
	 *
	 * Processes the incoming data by generating a {@link MonitoringMessage} of it. This {@link MonitoringMessage} will
	 * be stored in an array of {@link MonitoringMessage}s, which will be send via a
	 * {@link i5.las2peer.communication.Message} to the Processing Service.
	 *
	 */
	@Override
	public void log(Long timestamp, MonitoringEvent event, String sourceNode, String sourceAgentId,
					String destinationNode, String destinationAgentId, String remarks) {
		if (sourceNode == null) {
			return; // We do not log events without a source node into a database with different sources;-)
		}

		/*
		 * Temporary fix to exclude the very frequent messages from monitoring
		 * in order to not spam too much into the monitoring db.
		 */
		if (event == MonitoringEvent.ARTIFACT_FETCH_STARTED || event == MonitoringEvent.ARTIFACT_RECEIVED
				|| event == MonitoringEvent.AGENT_GET_STARTED || event == MonitoringEvent.AGENT_GET_SUCCESS
				|| event == MonitoringEvent.MESSAGE_SENDING || event == MonitoringEvent.ARTIFACT_FETCH_FAILED
				|| event == MonitoringEvent.MESSAGE_RECEIVED_ANSWER || event == MonitoringEvent.MESSAGE_FORWARDING
				|| event == MonitoringEvent.MESSAGE_RECEIVED) {
			return;
		}

		monitoringMessages[messagesCount++] = new MonitoringMessage(timestamp, event, sourceNode, sourceAgentId,
				destinationNode, destinationAgentId, remarks);

		if (readyToSend()) {
			checkInit();
			if (initializedDone) {
				messagesCount = 0;
				sendMessages();
			} else {
				messagesCount = 0;
				System.out.println("Monitoring: Problems with initializing Agents..");
			}
		}

		// We can only send our last message if the node is closing, so we will have to assume that all services are
		// shutdown
		// when a node is closed (seems to be a fair bet)
		if (event == MonitoringEvent.NODE_SHUTDOWN) {
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
	 * Checks whether the queue is ready to be flushed, either due to reaching the limit or because enough time has
	 * passed
	 * 
	 * @return true is ready to flush
	 */
	private boolean readyToSend() {
		if (messagesCount >= monitoringMessages.length) {
			return true;
		}

		if (messagesCount > 1) {
			long previous = monitoringMessages[messagesCount - 2].getTimestamp();
			long sendDeadline = System.currentTimeMillis() - waitUntilSend;
			return previous < sendDeadline;
		}

		return false;
	}

	private void resetReceivingAgent() {
		if (receivingAgent != null) {
			System.out.println("Monitoring: Reinitialize on next log message...");
			initializedDone = false;
			readyForInitializing = true;
		}
	}

	/**
	 * 
	 * Helper method that actually sends the {@link MonitoringMessage}s to the Processing Service's agent.
	 *
	 */
	private void sendMessages() {
		try {
			Message las2peerMessage = new Message(sendingAgent, receivingAgent, monitoringMessages);
			// if something goes wrong after sending a message the receiving agent is marked for reinitialization
			messageResultListener = new MessageResultListener(2000) {
				@Override
				public void notifyException(Exception exception) {
					resetReceivingAgent();
					System.out.println("Monitoring: message " + las2peerMessage.getId() +
							" encountered an exception: " + exception.getMessage());
				}

				@Override
				public void notifyTimeout() {
					resetReceivingAgent();
					System.out.println("Monitoring: message " + las2peerMessage.getId() + " timed out. ");
				}
			};
			registeredAt.sendMessage(las2peerMessage, messageResultListener);
			System.out.println("Monitoring: message " + las2peerMessage.getId() + " send!");
		} catch (InternalSecurityException | EncodingFailedException | SerializationException e) {
			e.printStackTrace();
		}
	}

}