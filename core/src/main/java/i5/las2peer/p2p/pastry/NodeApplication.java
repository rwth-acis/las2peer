package i5.las2peer.p2p.pastry;

import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentNotRegisteredException;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.WaiterThread;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;

/**
 * The NodeApplication implements the interface for message based interaction with the pastry p2p network.
 * 
 * In particular, message sending and retrieval, artifact storage via past and publish/subscribe via scribe are used to
 * provide the las2peer node (and agent) communication.
 * 
 * 
 *
 */
public class NodeApplication implements Application, ScribeMultiClient {

	public static final String FREEPASTRY_APPLICATION_CODE = "i5.las2peer-node-application";
	public static final String SCRIBE_APPLICATION_CODE = "i5.las2peer-agent-notification";

	public static final int SEARCH_SLEEP_TIME = 2500; // 2,5s
	public static final long SEARCH_TIMEOUT = 10000; // 10 seconds
	private static final int RESPONSE_WAIT_TIMEOUT = 10000; // 10 seconds

	private static final L2pLogger logger = L2pLogger.getInstance(NodeApplication.class.getName());

	protected Endpoint endpoint;

	private PastryNodeImpl l2pNode;

	private Scribe scribeClient;

	private Hashtable<String, Topic> htAgentTopics = new Hashtable<>();

	private Hashtable<Long, Topic> htTopics = new Hashtable<>();

	private Hashtable<Long, HashSet<NodeHandle>> htPendingAgentSearches = new Hashtable<>();

	private Hashtable<Long, WaiterThread<Message>> appMessageWaiters = new Hashtable<>();

	/**
	 * create a pastry application for the given node
	 * 
	 * @param node
	 */
	public NodeApplication(PastryNodeImpl node) {
		l2pNode = node;
		endpoint = l2pNode.getPastryNode().buildEndpoint(this, FREEPASTRY_APPLICATION_CODE);

		scribeClient = new ScribeImpl(l2pNode.getPastryNode(), SCRIBE_APPLICATION_CODE);

		endpoint.register();
	}

	/**
	 * register this node to the topic related to the given message receiver
	 * 
	 * @param receiver
	 */
	public void registerAgentTopic(MessageReceiver receiver) {
		synchronized (htAgentTopics) {
			if (htAgentTopics.get(receiver.getResponsibleForAgentSafeId()) != null) {
				return;
			}

			Topic agentTopic = getAgentTopic(receiver);

			htAgentTopics.put(receiver.getResponsibleForAgentSafeId(), agentTopic);

			logger.info("\t--> registering agent topic for " + receiver.getResponsibleForAgentSafeId() + " ("
					+ agentTopic.getId() + ")");

			// always subscribe to the root:
			NodeHandle root = scribeClient.getRoot(agentTopic);

			scribeClient.subscribe(agentTopic, this,
					new AgentJoinedContent(getLocalHandle(), receiver.getResponsibleForAgentSafeId()), root);
			l2pNode.observerNotice(MonitoringEvent.PASTRY_TOPIC_SUBSCRIPTION_SUCCESS, this.l2pNode.getNodeId(),
					receiver, "" + agentTopic.getId());
			/*
			System.out.println( "children of agent topic: " + scribeClient.numChildren(getAgentTopic(receiver)) );
			for ( NodeHandle nh: scribeClient.getChildrenOfTopic(getAgentTopic ( receiver ))) 
					System.out.println( "Child: " + nh);
			*/
		}
	}

	/**
	 * unregister an agent from its subscription
	 * 
	 * @param id
	 * @throws AgentNotRegisteredException
	 */
	public void unregisterAgentTopic(String id) throws AgentNotRegisteredException {
		synchronized (htAgentTopics) {

			Topic agentTopic = htAgentTopics.get(id);

			if (agentTopic == null) {
				throw new AgentNotRegisteredException("an agent with id " + id + " is not registered at this node");
			}

			scribeClient.unsubscribe(agentTopic, this);
			htAgentTopics.remove(id);

		}

	}

	public void registerTopic(long id) {
		synchronized (htTopics) {
			if (htTopics.get(id) != null) {
				return;
			}

			Topic topic = getTopic(id);

			htTopics.put(id, topic);

			logger.info("\t--> registering topic " + topic.getId() + ")");

			scribeClient.subscribe(topic, this, null, null);
			l2pNode.observerNotice(MonitoringEvent.PASTRY_TOPIC_SUBSCRIPTION_SUCCESS, this.l2pNode.getNodeId(),
					(String) null, "" + topic.getId());
		}
	}

	public void unregisterTopic(long id) throws NodeException {
		synchronized (htTopics) {

			Topic topic = htTopics.get(id);

			if (topic == null) {
				throw new NodeException("topic not found");
			}

			scribeClient.unsubscribe(topic, this);
			htTopics.remove(id);

		}

	}

	/**
	 * get information about a foreign node
	 * 
	 * @param nodeHandle
	 * @return node information
	 * @throws NodeNotFoundException
	 */
	public NodeInformation getNodeInformation(NodeHandle nodeHandle) throws NodeNotFoundException {
		GetInfoMessage gim = new GetInfoMessage(getLocalHandle());
		long messageId = gim.getId();

		sendMessageDirectly(gim, nodeHandle);

		try {
			WaiterThread<Message> waiter = new WaiterThread<>(RESPONSE_WAIT_TIMEOUT);
			appMessageWaiters.put(messageId, waiter);
			waiter.start();
			waiter.join();

			if (waiter.hasResult()) {
				InfoResponseMessage irm = (InfoResponseMessage) waiter.getResult();
				return irm.getInfoContent();
			} else {
				throw new NodeNotFoundException("Timeout waiting for information answer");
			}
		} catch (InterruptedException e) {
			throw new NodeNotFoundException("Interrupted while waiting for answer");
		} finally {
			// remove the waiter to prevent memory holes
			appMessageWaiters.remove(messageId);
		}
	}

	@Override
	public void deliver(Id id, Message pastMessage) {
		logger.info("\t<-- received message:" + pastMessage);

		if (pastMessage instanceof MessageEnvelope) {
			try {
				final i5.las2peer.communication.Message m = ((MessageEnvelope) pastMessage).getContainedMessage();

				// Is already done in Node-Classes
//				l2pNode.observerNotice( Event.MESSAGE_RECEIVED, ((MessageEnvelope) pastMessage).getSendingNode(),
//						m.getSender(), l2pNode.getPastryNode(), m.getRecipient(), "Got an envelope for a las2peer message!" );

				// hmmmm, is the problem here??
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							l2pNode.receiveMessage(m);
						} catch (Exception e) {
							System.out.println("Exception while delivering message to the node: " + e);
							e.printStackTrace();
						}
					}
				}).start();

			} catch (Exception e) {
				System.out.println("Exception while opening message!: " + e);
				e.printStackTrace();
			}
		} else if (pastMessage instanceof SearchAnswerMessage) {
			// k, got an answer for my own search
			l2pNode.observerNotice(MonitoringEvent.AGENT_SEARCH_ANSWER_RECEIVED,
					((SearchAnswerMessage) pastMessage).getSendingNode(), (String) null, l2pNode.getPastryNode(),
					(String) null, "");

			// just store the sending node handle
			HashSet<NodeHandle> pendingCollection = htPendingAgentSearches.get(((SearchAnswerMessage) pastMessage)
					.getRequestMessageId());

			if (pendingCollection != null) {
				pendingCollection.add(((SearchAnswerMessage) pastMessage).getSendingNode());
			} else {
				logger.warning("got a timed out response or response to a message not sent by me!");
			}
		} else if (pastMessage instanceof GetInfoMessage) {
			// just send a response
			GetInfoMessage gim = (GetInfoMessage) pastMessage;
			try {
				InfoResponseMessage answer = new InfoResponseMessage(gim.getId(), getLocalHandle(),
						l2pNode.getNodeInformation());

				sendMessageDirectly(answer, gim.getSender());
			} catch (CryptoException e) {
				throw new RuntimeException("Critical - I'm not able to create my own Node Information!");
			}
		} else if (pastMessage instanceof InfoResponseMessage) {
			InfoResponseMessage irm = (InfoResponseMessage) pastMessage;

			WaiterThread<Message> waiter = appMessageWaiters.get(irm.getResponseToId());
			if (waiter == null) {
				l2pNode.observerNotice(MonitoringEvent.MESSAGE_FAILED, l2pNode.getNodeId(), (MessageReceiver) null,
						"Got an answer to an information request I do not know from " + irm.getSender());
			} else {
				l2pNode.observerNotice(MonitoringEvent.MESSAGE_RECEIVED, l2pNode.getNodeId(), (MessageReceiver) null,
						"Got an answer for Information request " + irm.getResponseToId() + "from " + irm.getSender());
				waiter.collectResult(irm);
			}
		} else {
			l2pNode.observerNotice(MonitoringEvent.MESSAGE_RECEIVED, l2pNode.getNodeId(), (String) null,
					"unkown message: " + pastMessage);
			logger.warning("\t<-- received unknown message: " + pastMessage);
		}
	}

	@Override
	public boolean forward(RouteMessage pastMessage) {
		l2pNode.observerNotice(MonitoringEvent.MESSAGE_FORWARDING, l2pNode.getNodeId(), (String) null,
				pastMessage.getDestinationId(), (String) null, "" + pastMessage);
		return true;
	}

	@Override
	public void update(NodeHandle nh, boolean arg1) {
		// called when a new neighbor joined the net
		l2pNode.observerNotice(MonitoringEvent.NEW_NODE_NOTICE, l2pNode.getNodeId(), "" + nh);
	}

	/**
	 * Called to route a message to the id
	 * 
	 * @param id
	 */
	public void routeMyMsg(Id id) {
		logger.info("\t --> " + this + " sending to " + id);
		Message msg = new PastryTestMessage(endpoint.getId(), id);
		endpoint.route(id, msg, null);
	}

	/**
	 * Called to directly send a message to the nh
	 * 
	 * @param nh
	 */
	public void routeMyMsgDirect(NodeHandle nh) {
		logger.info("\t --> " + this + " sending direct to " + nh);

		Message msg = new PastryTestMessage(endpoint.getId(), nh.getId());
		endpoint.route(null, msg, nh);
	}

	/**
	 * send a message to the given node handle
	 * 
	 * @param m
	 * @param to
	 * @throws MalformedXMLException
	 * @throws MessageException
	 */
	public void sendMessage(MessageEnvelope m, NodeHandle to) throws MalformedXMLException, MessageException {
		l2pNode.observerNotice(MonitoringEvent.MESSAGE_SENDING, l2pNode.getPastryNode(), m.getContainedMessage()
				.getSender(), to, m.getContainedMessage().getRecipient(), "message: " + m);

		logger.info("\t --> " + this + " sending (encapsulated) message directly to " + to);
		endpoint.route(null, m, to);
	}

	/**
	 * send a pastry message to the given node
	 * 
	 * @param m
	 * @param to
	 */
	public void sendMessageDirectly(Message m, NodeHandle to) {
		endpoint.route(null, m, to);
	}

	/**
	 * broadcast a {@link i5.las2peer.communication.Message} to all running instances of its recipient agent
	 * 
	 * @param l2pMessage
	 */
	public void sendMessage(i5.las2peer.communication.Message l2pMessage) {
		BroadcastMessageContent content = new BroadcastMessageContent(getLocalHandle(), l2pMessage);

		System.out.println(" --> sending Message " + l2pMessage.getId());

		if (!l2pMessage.isTopic()) {
			scribeClient.publish(getAgentTopic(l2pMessage.getRecipientId()), content);
			// scribeClient.anycast( getAgentTopic ( l2pMessage.getRecipientId() ), content );
		} else {
			scribeClient.publish(getTopic(l2pMessage.getTopicId()), content);
		}

	}

	@Override
	public void childAdded(Topic topic, NodeHandle nh) {
		l2pNode.observerNotice(MonitoringEvent.PASTRY_NEW_TOPIC_CHILD, nh, topic.toString());

		// System.out.println(ColoredOutput.colorize("child added to topic at this node", ForegroundColor.Yellow));
	}

	@Override
	public void childRemoved(Topic topic, NodeHandle nh) {
		l2pNode.observerNotice(MonitoringEvent.PASTRY_REMOVED_TOPIC_CHILD, nh, topic.toString());

		// System.out.println(ColoredOutput.colorize("child removed to topic at this node", ForegroundColor.Yellow));
	}

	/**
	 * look for an agent in the p2p net
	 * 
	 * This method broadcasts a search message for the given agent and collects all answers. It tries to wait for
	 * <i>expectedAnswers</i> nodes to respond to the search.
	 * 
	 * However the search will be aborted after <i>SEARCH_TIMEOUT</i> milliseconds.
	 * 
	 * @param agentId
	 * @param expectedAnswers
	 * @return a collections of node handles where the requested agent is registered to
	 */
	public Collection<NodeHandle> searchAgent(String agentId, int expectedAnswers) {

		Topic agentTopic = getAgentTopic(agentId);

		System.out.println("Topic info:");
		System.out.println("root: " + scribeClient.isRoot(agentTopic));
		System.out.println("parent: " + scribeClient.getParent(agentTopic));
		// System.out.println( "children: " + scribeClient.getChildren(agentTopic).length );

		for (NodeHandle nh : scribeClient.getChildrenOfTopic(getAgentTopic(agentId))) {
			System.out.println("Child in search: " + nh);
		}

		l2pNode.observerNotice(MonitoringEvent.AGENT_SEARCH_STARTED, this.l2pNode.getNodeId(), agentId, null,
				(String) null, "(" + expectedAnswers + ") - topic: " + getAgentTopic(agentId));

		SearchAgentContent search = new SearchAgentContent(getLocalHandle(), agentId);
		HashSet<NodeHandle> resultSet = new HashSet<>();
		htPendingAgentSearches.put(search.getRandomId(), resultSet);

		// publish a message to search the agent registers
		scribeClient.publish(agentTopic, search);
		// scribeClient.anycast( agentTopic,search );

		long timeout = new Date().getTime() + SEARCH_TIMEOUT;

		// todo: use a waiterThread here
		while (new Date().getTime() <= timeout && resultSet.size() < expectedAnswers) {
			try {
				System.out.println("\t\t waiting for agent-search (" + agentId + ") - "
						+ (-new Date().getTime() + timeout) / 1000 + "s left");
				Thread.sleep(SEARCH_SLEEP_TIME);
			} catch (InterruptedException e) {
			}
		}

		htPendingAgentSearches.remove(search.getRandomId());

		l2pNode.observerNotice(MonitoringEvent.AGENT_SEARCH_FINISHED, this.l2pNode.getNodeId(), agentId, null,
				(String) null, "" + resultSet.size());

		return resultSet;
	}

	/**
	 * get the handle of the local pastry node
	 * 
	 * @return pastry node handle
	 */
	private NodeHandle getLocalHandle() {
		return l2pNode.getPastryNode().getLocalNodeHandle();
	}

	/**
	 * look for a running/registered version of an agent in the p2p net
	 * 
	 * @param agentId
	 * @return a collection of node handles running the requested agent
	 */
	public Collection<NodeHandle> searchAgent(String agentId) {
		return searchAgent(agentId, 1);
	}

	@Override
	public void deliver(Topic topic, ScribeContent content) {
		if (content instanceof SearchAgentContent) {
			logger.info("\t\t<---got request for agent");

			for (String regId : htAgentTopics.keySet()) {
				if (htAgentTopics.get(regId).equals(topic)) {
					// found the agent
					// send message to searching node

					endpoint.route(
							null,
							new SearchAnswerMessage(((SearchAgentContent) content).getOrigin(), this.l2pNode
									.getPastryNode().getLocalNodeHandle(), ((SearchAgentContent) content).getRandomId()),
							((SearchAgentContent) content).getOrigin());
					// send return message

					logger.info("\t\t---> found " + regId + " > response sent");
					return;
				}
			}

			logger.severe("\t\t<--- subscribed but agent not found!!!!");
		} else if (content instanceof AgentJoinedContent) {
			logger.info("\t\t<--- got notification about agent joining: " + ((AgentJoinedContent) content).getAgentId());
		} else if (content instanceof BroadcastMessageContent) {
			final BroadcastMessageContent c = (BroadcastMessageContent) content;

			// or is the storage problem here?
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						l2pNode.receiveMessage(c.getMessage());
					} catch (MalformedXMLException e) {
						logger.severe("unable to open BroadcastMessageContent!");
					} catch (InternalSecurityException e) {
						logger.severe("L2pSecurityException while handling received message!");
					} catch (MessageException e) {
						logger.log(Level.SEVERE, "MessageException while handling received message!", e);
					} catch (AgentNotRegisteredException e) {
						logger.severe("AgentNotKnown!?! - I shouldn't have gotten this message!");
					} catch (AgentException e) {
						logger.log(Level.SEVERE, "Got a message for an agent, but he failed!", e);
					}
				}

			}).start();
		} else {
			l2pNode.observerNotice(MonitoringEvent.MESSAGE_RECEIVED_UNKNOWN, this.l2pNode.getNodeId(),
					"got an unknown message of type " + content.getClass().getName());
			logger.info("got unknown Scribe content of type " + content.getClass().getName());
		}
	}

	@Override
	public void subscribeFailed(Topic topic) {
		logger.warning("topic '" + topic.getId() + "' subscription failed");
		l2pNode.observerNotice(MonitoringEvent.PASTRY_TOPIC_SUBSCRIPTION_FAILED, this.l2pNode.getNodeId(),
				"" + topic.toString());
	}

	@Override
	public void subscribeFailed(Collection<Topic> topics) {
		// System.out.println(ColoredOutput.colorize( "topic subscription failed for collection of topics!",
		// ForegroundColor.Yellow));
		for (Topic t : topics) {
			subscribeFailed(t);
		}
	}

	@Override
	public void subscribeSuccess(Collection<Topic> topics) {
		for (Topic t : topics) {
			l2pNode.observerNotice(MonitoringEvent.PASTRY_TOPIC_SUBSCRIPTION_SUCCESS,
					this.l2pNode.getNodeId() + "" + t.toString());
			logger.info("Successfully subscribed to topic '" + t.getId() + "'");
		}
	}

	/**
	 * generate an id string for topic corresponding to a l2p agent
	 * 
	 * @param agent
	 * @return a topic identifier for the agent to subscribe to
	 */
	public static String getAgentTopicId(AgentImpl agent) {
		return getAgentTopicId(agent.getIdentifier());
	}

	/**
	 * generate an id string for topic corresponding to a l2p agent
	 * 
	 * @param id
	 * @return a topic identifier for the agent to subscribe to
	 */
	public static String getAgentTopicId(String id) {
		return "agent-" + id;
	}

	/**
	 * create a Scribe topic for the given message receiver (the underlying agent resp.)
	 * 
	 * @param receiver
	 * @return the topic corresponding to the given message receiver
	 */
	private Topic getAgentTopic(MessageReceiver receiver) {
		return getAgentTopic(receiver.getResponsibleForAgentSafeId());
	}

	/**
	 * create a Scribe topic for the given agent
	 * 
	 * @param agentId
	 * @return the topic corresponding to the given agent
	 */
	private Topic getAgentTopic(String agentId) {
		return new Topic(new PastryIdFactory(l2pNode.getPastryNode().getEnvironment()), getAgentTopicId(agentId));
	}

	/**
	 * create a Scribe topic for the given topic
	 * 
	 * @param topicId
	 * @return
	 */
	private Topic getTopic(long topicId) {
		return new Topic(new PastryIdFactory(l2pNode.getPastryNode().getEnvironment()), topicId + "");
	}

	@Override
	public boolean anycast(Topic topic, ScribeContent content) {
		try {
			deliver(topic, content);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
