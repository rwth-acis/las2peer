package i5.las2peer.p2p.pastry;

import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.tools.ColoredOutput;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.WaiterThread;

import java.security.PublicKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;

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
 * The NodeApplication implements the interface for message based interaction with the pastry p2p
 * network.
 * 
 * In particular, message sending and retrieval, artifact storage via past and publish/subscribe via
 * scribe are used to provide the LAS2peer node (and agent) communication.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class NodeApplication implements Application, ScribeMultiClient {
	
	public final static String FREEPASTRY_APPLICATION_CODE = "i5.las2peer-node-application";
	public final static String SCRIBE_APPLICATION_CODE = "i5.las2peer-agent-notification";
	
	
	public final static int SEARCH_SLEEP_TIME = 2500; // 2,5s
	public final static long SEARCH_TIMEOUT = 10000; // 10 seconds
	private static final int RESPONSE_WAIT_TIMEOUT = 10000; // 10 seconds
	
	protected Endpoint endpoint;
	
	private PastryNodeImpl l2pNode;
		
	private Scribe scribeClient;
	
	private Hashtable<Long, Topic> htAgentTopics = new Hashtable<Long, Topic> (); 
	
	
	private Hashtable<Long, HashSet<NodeHandle>> htPendingAgentSearches = new Hashtable<Long, HashSet<NodeHandle>> ();
		
	private Hashtable<Long, WaiterThread<Message>> appMessageWaiters = 
			new Hashtable<Long, WaiterThread<Message>> ();
	
	
	
	/**
	 * create a pastry application for the given node
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
	 * @param receiver
	 */
	public void registerAgentTopic ( MessageReceiver receiver ) {
		synchronized ( htAgentTopics ) {
			if ( htAgentTopics.get( receiver.getResponsibleForAgentId()) != null )
				return;
			
			Topic agentTopic = getAgentTopic ( receiver ); 
					
			htAgentTopics.put( receiver.getResponsibleForAgentId(), agentTopic);
			
			ColoredOutput.printlnYellow( "\t--> registering agent topic for " + receiver.getResponsibleForAgentId() + " (" + agentTopic.getId() + ")");
			
			
			// always subscribe to the root:
			NodeHandle root = scribeClient.getRoot(agentTopic);
			
			scribeClient.subscribe(
					agentTopic, 
					this, 
					new AgentJoinedContent ( getLocalHandle(), receiver.getResponsibleForAgentId()),
					root
			);
			l2pNode.observerNotice(Event.PASTRY_TOPIC_SUBSCRIPTION_SUCCESS, l2pNode.getNodeId(), receiver, "" +agentTopic.getId());
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
	 * @throws AgentNotKnownException
	 */
	public void unregisterAgentTopic ( long id ) throws AgentNotKnownException  {
		synchronized ( htAgentTopics ) {
			
			Topic agentTopic = htAgentTopics.get( id );
			
			if ( agentTopic == null )
				throw new AgentNotKnownException("an agent with id " + id + " is not registered at this node");		
			
			scribeClient.unsubscribe(agentTopic, this);
			htAgentTopics.remove( id );
					
		}
				
	}
	
	
	/**
	 * get information about a foreign node
	 * 
	 * @param nodeHandle
	 * 
	 * @return	node information
	 * @throws NodeNotFoundException 
	 */
	public NodeInformation getNodeInformation ( NodeHandle nodeHandle ) throws NodeNotFoundException {		
		GetInfoMessage gim = new GetInfoMessage ( getLocalHandle() );
		long messageId = gim.getId();
		
		sendMessageDirectly(gim, nodeHandle);    

		try {
			WaiterThread<Message> waiter = new WaiterThread<Message> ( RESPONSE_WAIT_TIMEOUT );
			appMessageWaiters.put ( messageId, waiter);
			waiter.start();
			waiter.join();
			
			if ( waiter.hasResult() ) {
				InfoResponseMessage irm = (InfoResponseMessage) waiter.getResult();				
				return irm.getInfoContent();
			} else
				throw new NodeNotFoundException ( "Timeout waiting for information answer" );				
		} catch ( InterruptedException e ) {
			throw new NodeNotFoundException ( "Interrupted while waiting for answer");
		} finally {
			// remove the waiter to prevent memory holes
			appMessageWaiters.remove( messageId);
		}		
	}
	
	
	@Override
	public void deliver(Id id, Message pastMessage) {
		ColoredOutput.printlnYellow( "\t<-- received message:" + pastMessage );
		
		if ( pastMessage instanceof MessageEnvelope  ) {
			try {
				final i5.las2peer.communication.Message m = ((MessageEnvelope)pastMessage).getContainedMessage();
				
				//Is already done in Node-Classes
//				l2pNode.observerNotice( Event.MESSAGE_RECEIVED, ((MessageEnvelope) pastMessage).getSendingNode(),
//						m.getSender(), l2pNode.getPastryNode(), m.getRecipient(), "Got an envelope for a LAS2peer message!" );
				
				// hmmmm, is the problem here??
				new Thread ( new Runnable () {
					@Override
					public void run() {
						try {
							l2pNode.receiveMessage(m);
						} catch ( Exception e ) {
							System.out.println( "Exception while delivering message to the node: " + e );
							e.printStackTrace();							
						}
					}
				} ).start();
					
				
			} catch (Exception e) {
				System.out.println( "Exception while opening message!: " + e );
				e.printStackTrace();
			}
		} else if ( pastMessage instanceof SearchAnswerMessage ) {
			// k, got an answer for my own search
			l2pNode.observerNotice(Event.AGENT_SEARCH_ANSWER_RECEIVED, ((SearchAnswerMessage) pastMessage).getSendingNode(), null, l2pNode.getPastryNode(), null, "" );
			
			// just store the sending node handle
			HashSet<NodeHandle> pendingCollection = htPendingAgentSearches
				.get( ((SearchAnswerMessage) pastMessage).getRequestMessageId() );
			
			if ( pendingCollection != null)
				pendingCollection.add ( ((SearchAnswerMessage) pastMessage).getSendingNode());
			else
				ColoredOutput.printlnRed ( "got a timed out response or response to a message not sent by me!");
		} else if ( pastMessage instanceof GetInfoMessage ) {
			// just send a response
			GetInfoMessage gim = (GetInfoMessage) pastMessage;
			try {
				InfoResponseMessage answer = new InfoResponseMessage(
						gim.getId(),
						getLocalHandle(),
						l2pNode.getNodeInformation ()
				);
				
				sendMessageDirectly( answer, gim.getSender() );
			} catch (CryptoException e) {
				throw new RuntimeException ( "Critical - I'm not able to create my own Node Information!");
			}
		} else if ( pastMessage instanceof UnlockAgentMessage ) {
			UnlockAgentMessage uam = (UnlockAgentMessage) pastMessage;
			
			UnlockAgentResponse response;
			try {
				l2pNode.unlockRemoteAgent(uam.getAgentId(), uam.getEncryptedPass());
				response = new UnlockAgentResponse(uam.getMessageId());
			} catch ( Exception e ) {
				response = new UnlockAgentResponse(uam.getMessageId(), e);
			}
			sendMessageDirectly(response, uam.getSendingNode());
		} else if ( pastMessage instanceof InfoResponseMessage ) {
			InfoResponseMessage irm = (InfoResponseMessage) pastMessage;
			
			WaiterThread<Message> waiter = appMessageWaiters.get( irm.getResponseToId());
			if ( waiter == null )
				l2pNode.observerNotice(Event.MESSAGE_FAILED, irm.getSender(), (MessageReceiver) null, "got an answer to an information request I do not know!");
			else {
				l2pNode.observerNotice(Event.MESSAGE_RECEIVED, irm.getSender(), (MessageReceiver) null, "got an answer for Information Request " + irm.getResponseToId());
				waiter.collectResult(irm);
			}
		} else if ( pastMessage instanceof UnlockAgentResponse ) {
			UnlockAgentResponse uar = (UnlockAgentResponse) pastMessage;
			
			WaiterThread<Message> waiter = appMessageWaiters.get( uar.getOriginalMessageId());
			waiter.collectResult(uar);
		} else { 
			l2pNode.observerNotice( Event.MESSAGE_RECEIVED, l2pNode.getPastryNode(), null, "unkown message: "+pastMessage );
			ColoredOutput.printlnYellow( "\t<-- received unknown message: " + pastMessage);
		}
	}

	@Override
	public boolean forward(RouteMessage pastMessage) {
		l2pNode.observerNotice(Event.MESSAGE_FORWARDING, l2pNode.getNodeId(), "" + pastMessage);
		
		return true;
	}

	@Override
	public void update(NodeHandle nh, boolean arg1) {
		// called when a new neighbor joined the net
		l2pNode.observerNotice( Event.NEW_NODE_NOTICE, null, ""+nh);
	}

	
	/**
	 * Called to route a message to the id
	 */
	public void routeMyMsg(Id id) {
		ColoredOutput.printlnYellow("\t --> " + this + " sending to " + id);
		Message msg = new PastryTestMessage(endpoint.getId(), id);
		endpoint.route(id, msg, null);
	}

	/**
	 * Called to directly send a message to the nh
	 */
	public void routeMyMsgDirect(NodeHandle nh) {
		ColoredOutput.printlnYellow("\t --> " + this + " sending direct to " + nh);
		
		Message msg = new PastryTestMessage(endpoint.getId(), nh.getId());
		endpoint.route(null, msg, nh);
	}
	
	
	/**
	 * send a message to the given node handle
	 * @param m
	 * @param to
	 * 
	 * @throws MalformedXMLException 
	 * @throws L2pSecurityException 
	 * @throws AgentNotKnownException 
	 * @throws MessageException 
	 */
	public void sendMessage ( MessageEnvelope m , NodeHandle to ) throws MalformedXMLException, L2pSecurityException, AgentNotKnownException, MessageException {
		l2pNode.observerNotice(Event.MESSAGE_SENDING,l2pNode.getPastryNode(), m.getContainedMessage().getSender(), to, m.getContainedMessage().getRecipient(), "message: " + m );
		
		ColoredOutput.printlnYellow("\t --> " + this + " sending (encapsulated) message directly to " + to);
		endpoint.route( null,  m, to );
	}
	
	
	/**
	 * send a pastry message to the given node
	 * 
	 * @param m
	 * @param to
	 */
	public void sendMessageDirectly ( Message m, NodeHandle to ) {
		endpoint.route( null, m, to);
	}
	

	/**
	 * broadcast a {@link i5.las2peer.communication.Message} to all
	 * running instances of its recipient agent
	 *  
	 * @param l2pMessage
	 */
	public void sendMessage ( i5.las2peer.communication.Message l2pMessage ) {
		BroadcastMessageContent content = new BroadcastMessageContent(getLocalHandle(), l2pMessage);
				
		System.out.println ( " --> sending Message " + l2pMessage.getId() );
		
		scribeClient.publish( getAgentTopic ( l2pMessage.getRecipientId() ), content );
		//scribeClient.anycast( getAgentTopic ( l2pMessage.getRecipientId() ), content );
	}

	

	@Override
	public void childAdded(Topic topic, NodeHandle nh) {
		l2pNode.observerNotice (Event.PASTRY_NEW_TOPIC_CHILD, nh, topic.toString());
		
		// ColoredOutput.printlnYellow("child added to topic at this node");
	}


	@Override
	public void childRemoved(Topic topic, NodeHandle nh) {
		l2pNode.observerNotice (Event.PASTRY_REMOVED_TOPIC_CHILD, nh, topic.toString());
		
		//ColoredOutput.printlnYellow("child removed to topic at this node");		
	}
	
	
	/**
	 * look for an agent in the p2p net
	 * 
	 * This method broadcasts a search message for the given agent and collects all answers.
	 * It tries to wait for <i>expectedAnswers</i> nodes to respond to the search.
	 * 
	 * However the search will be aborted after <i>SEARCH_TIMEOUT</i> milliseconds. 
	 * 
	 * @param agentId
	 * @param expectedAnswers
	 * 
	 * @return	a collections of node handles where the requested agent is registered to
	 */
	public Collection<NodeHandle> searchAgent ( long agentId, int expectedAnswers ) {
		
		Topic agentTopic = getAgentTopic ( agentId );
		
		System.out.println ( "Topic info:");
		System.out.println( "root: " + scribeClient.isRoot ( agentTopic ));
		System.out.println( "parent: " + scribeClient.getParent(agentTopic));
		//System.out.println( "children: " + scribeClient.getChildren(agentTopic).length );
		
		for ( NodeHandle nh: scribeClient.getChildrenOfTopic(getAgentTopic ( agentId ))) 
			System.out.println( "Child in search: " + nh);
		
		
		l2pNode.observerNotice (Event.AGENT_SEARCH_STARTED, "" + agentId + " (" + expectedAnswers + ") - topic: " + getAgentTopic( agentId));		
		
		SearchAgentContent search = new SearchAgentContent(getLocalHandle(), agentId);
		HashSet<NodeHandle> resultSet = new HashSet<NodeHandle> ();
		htPendingAgentSearches.put( search.getRandomId(), resultSet);
	
			
		// publish a message to search the agent registers
		scribeClient.publish( agentTopic,search );
		//scribeClient.anycast( agentTopic,search );
		
				
		long timeout = new Date().getTime() + SEARCH_TIMEOUT;
		
		
		// todo: use a waiterThread here
		while ( new Date().getTime() <= timeout && resultSet.size() < expectedAnswers )
			try {
				System.out.println( "\t\t waiting for agent-search (" + agentId + ") - " + (-new Date().getTime() + timeout)/1000 + "s left");
				Thread.sleep (SEARCH_SLEEP_TIME);
			} catch (InterruptedException e) {
			}

		htPendingAgentSearches.remove(search.getRandomId());
		
		l2pNode.observerNotice (Event.AGENT_SEARCH_FINISHED, "" + agentId+ " (" + resultSet.size() + ")");
		
		return resultSet;
	}


	/**
	 * get the handle of the local pastry node
	 * 
	 * @return	pastry node handle
	 */
	private NodeHandle getLocalHandle() {
		return l2pNode.getPastryNode().getLocalNodeHandle();
	};
	
	
	/**
	 * look for a running/registered version of an agent in the p2p net
	 * 
	 * @param agentId
	 * 
	 * @return a collection of node handles running the requested agent
	 */
	public Collection<NodeHandle> searchAgent ( long agentId ) {
		return searchAgent ( agentId, 1);
	}
	
	


	@Override
	public void deliver(Topic topic, ScribeContent content) {
		if ( content instanceof SearchAgentContent ) {
			ColoredOutput.printlnYellow( "\t\t<---got request for agent");
			
			for ( Long regId : htAgentTopics.keySet() ){
				if ( htAgentTopics.get(regId).equals( topic )) {
					// found the agent
					// send message to searching node
					
					endpoint.route( 
							null,
							new SearchAnswerMessage (
								((SearchAgentContent) content).getOrigin(),
								this.l2pNode.getPastryNode().getLocalNodeHandle(),
								((SearchAgentContent) content).getRandomId()
							), 
							((SearchAgentContent) content).getOrigin());
					// send return message
					
					ColoredOutput.printlnYellow( "\t\t---> found " + regId + " > response sent");
					return;
				}
			}
			
			ColoredOutput.printlnYellow( "\t\t<--- subscribed but agent not found!!!!");
		} else if ( content instanceof AgentJoinedContent ) {
			ColoredOutput.printlnYellow( "\t\t<--- got notification about agent joining: " + ((AgentJoinedContent)content).getAgentId());
		} else if ( content instanceof BroadcastMessageContent ) {
			final BroadcastMessageContent c = (BroadcastMessageContent) content;
			
			// or is the storage problem here? 
			new Thread ( new Runnable () {
				@Override
				public void run() {
					
					try {
						l2pNode.receiveMessage(c.getMessage());				
					} catch (MalformedXMLException e) {
						ColoredOutput.printlnRed( "unable to open BroadcastMessageContent!");
					} catch ( L2pSecurityException e ) {
						ColoredOutput.printlnRed( "L2pSecurityException while handling received message!");
					} catch ( MessageException e ) {
						ColoredOutput.printlnRed( "MessageException while handling received message!");
						e.printStackTrace();
					} catch ( AgentNotKnownException e ) {
						ColoredOutput.printlnRed ( "AgentNotKnown!?! - I shouldn't have gotten this message!");
					}
					
				}

			}).start();
		} else {
			l2pNode.observerNotice(Event.MESSAGE_RECEIVED_UNKNOWN, l2pNode, "got an unknown message of type " + content.getClass().getName());
			ColoredOutput.printlnYellow( "got unknown Scribe content of type " + content.getClass().getName());
		}		
	}


	@Override
	public void subscribeFailed(Topic topic) {
		//ColoredOutput.printlnYellow( "topic subscription failed!");		
		l2pNode.observerNotice(Event.PASTRY_TOPIC_SUBSCRIPTION_FAILED, "" + topic.toString());
	}


	@Override
	public void subscribeFailed(Collection<Topic> topics) {
		//ColoredOutput.printlnYellow( "topic subscription failed for collection of topics!");
		for ( Topic t : topics )
			subscribeFailed ( t );
	}


	@Override
	public void subscribeSuccess(Collection<Topic> topics) {
		for ( Topic t : topics ) {
			System.out.println ( "Subscribe success! Topic info:");
			System.out.println( "\troot: " + scribeClient.getRoot(t)   + " / " +scribeClient.isRoot ( t ));
			System.out.println( "\tparent: " + scribeClient.getParent(t));
			//System.out.println( "\tchildren: " + scribeClient.getChildren(t).length );			
		}
			
		//ColoredOutput.printlnYellow( "\t\t<--sucessfully subscribed to topic collection");
		//for (Topic t: topics) {
		//	ColoredOutput.printlnYellow( "\t\t\t" + t.getId());
		//}
	}

	/**
	 * generate an id string for topic corresponding to a l2p agent
	 * 
	 * @param agent
	 * 
	 * @return a topic identifier for the agent to subscribe to
	 */
	public static String getAgentTopicId ( Agent agent ) {
		return getAgentTopicId ( agent.getId () );
	}
	
	/**
	 * generate an id string for topic corresponding to a l2p agent
	 * 
	 * @param id
	 * 
	 * @return a topic identifier for the agent to subscribe to
	 */
	public static String getAgentTopicId ( long id ) {
		return "agent-" + id ;
	}
	
	/**
	 * create a Scribe topic for the given message receiver (the underlying agent resp.) 
	 * 
	 * @param receiver
	 * 
	 * @return	the topic corresponding to the given message receiver
	 */
	private Topic getAgentTopic ( MessageReceiver receiver ) {
		return getAgentTopic ( receiver.getResponsibleForAgentId() );
	}
	
	
	/**
	 * create a Scribe topic for the given agent
	 * 
	 * @param agentId
	 * @return the topic corresponding to the given agent
	 */
	private Topic getAgentTopic ( long agentId ) {
		return new Topic ( 
				new PastryIdFactory (l2pNode.getPastryNode().getEnvironment()), 
				getAgentTopicId ( agentId )
			);		
	}
	

	@Override
	public boolean anycast(Topic topic, ScribeContent content) {
		//deprecated?!?!
		
		try {
			deliver ( topic, content );
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}


	
	/**
	 * send a request to a foreign node to unlock the private key of an agent
	 * 
	 * @param agentId
	 * @param passphrase
	 * @param targetNode
	 * @param nodeEncryptionKey
	 * @throws L2pSecurityException 
	 */
	public void unlockRemoteAgent(long agentId, String passphrase,
			NodeHandle targetNode, PublicKey nodeEncryptionKey) throws L2pSecurityException {
		if ( targetNode == null || nodeEncryptionKey == null )
			throw new NullPointerException ( "Whom should I send auth to?!");
		
		byte[] encPass;
		try {
			 encPass = CryptoTools.encryptAsymmetric(passphrase,  nodeEncryptionKey);			
		} catch ( Exception e ) {
			throw new L2pSecurityException("unable to encrypt passphrase!", e );
		}
		
		UnlockAgentMessage uam = new UnlockAgentMessage ( getLocalHandle(), agentId, encPass );
		long messageId = uam.getMessageId();
		
		sendMessageDirectly(uam, targetNode);    

		try {
			WaiterThread<Message> waiter = new WaiterThread<Message> ( RESPONSE_WAIT_TIMEOUT );
			appMessageWaiters.put ( messageId, waiter);
			waiter.start();
			waiter.join();
			
			if ( waiter.hasResult() ) {
				UnlockAgentResponse irm = (UnlockAgentResponse) waiter.getResult();
				if ( irm.hasException() )
					throw new L2pSecurityException ( "got an exception from remote node!", irm.getException () );
			} else
				throw new L2pSecurityException ( "timeout");				
		} catch ( InterruptedException e ) {
			throw new L2pSecurityException ( "Interrupted while waiting for answer");
		} finally {
			// remove the waiter to prevent memory holes
			appMessageWaiters.remove( messageId);
		}		
		
	}
	
}
