package i5.las2peer.p2p;

import i5.las2peer.communication.Message;
import i5.las2peer.mobsos.NodeObserver.Event;
import i5.las2peer.p2p.pastry.PastryStorageException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.TimerThread;

import java.security.PublicKey;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;


/**
 * Implementation of the abstract {@link Node} class mostly for testing purposes.
 * All data and agents will be stored in the same JVM, which may be used in JUnit test cases
 * or to launch a <i>local only</i> server for example.
 * 
 * 
 * TODO: uses loggers / observers 
 * 
 * @author Holger Janssen
 * @version $Revision: 1.16 $, $Date: 2013/04/10 10:09:54 $
 *
 */
public class LocalNode extends Node {
	
	private BasicAgentStorage locallyKnownAgents;

	
	/**
	 * an id for this node
	 */
	private long nodeId;

	/**
	 * create a LocalNode
	 */
	private LocalNode () {
		super();
		
		Random r = new Random();
		nodeId = r.nextLong();
		
		//htStoredArtifacts = new Hashtable<Long, Envelope>();
		locallyKnownAgents = new BasicAgentStorage(this);
		
		setStatus ( NodeStatus.CONFIGURED );
	}
	
	/**
	 * get the id of this node
	 * @return	id of this node
	 */
	public Long getNodeId() {
		return nodeId;
	}
	
	
	
	@Override
	public void launch() {
		setStatus ( NodeStatus.RUNNING );
		
		registerNode ( this );
	}

	@Override
	public void shutDown() {
		super.shutDown();
		unregisterNode ( this );
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException, L2pSecurityException, AgentException {
		super.registerReceiver( receiver );

		deliverPendingMessages(receiver.getResponsibleForAgentId(), getNodeId());		
	}

	
	@Override
	public void sendMessage(Message message, MessageResultListener listener, SendMode mode) {
		message.setSendingNodeId ( this.getNodeId());
		registerAnswerListener(message.getId(), listener);
		
		try {
			switch ( mode ) {
			case ANYCAST: 
				localSendMessage (findFirstNodeWithAgent(message.getRecipientId()), message );
				
				break;
			case BROADCAST:
				Long[] ids = findAllNodesWithAgent(message.getRecipientId());
								
				if ( ids.length == 0 ) {
					listener.collectException ( new AgentNotKnownException (message.getRecipientId()));
				} else {
					listener.addRecipients( ids.length-1);
					for ( long id: ids ) {
						localSendMessage ( id, message );
					}
				}
			}
		} catch (AgentNotKnownException e) {
			storeMessage ( message, listener );
		}
	}

	@Override
	public void sendMessage(Message message, Object atNodeId, MessageResultListener listener ) throws AgentNotKnownException, NodeNotFoundException {
		message.setSendingNodeId ( this.getNodeId());
		
		if ( ! ( atNodeId instanceof Long ))
			throw new IllegalArgumentException ( "a node id for a LocalNode has to be a Long!");
		
		if ( ! hasNode ( (Long) atNodeId )) {
			listener.collectException( new NodeNotFoundException ( (Long) atNodeId) );
		} else {
			registerAnswerListener(message.getId(), listener);
			localSendMessage((Long) atNodeId, message);
		}
	}
	
			

	@Override
	public Envelope fetchArtifact(long id) throws ArtifactNotFoundException {
		if ( htStoredArtifacts.get( id ) == null)
			throw new ArtifactNotFoundException(id);
		return htStoredArtifacts.get( id );
	}

	@Override
	public void storeArtifact(Envelope envelope) throws L2pSecurityException {
		
		try {
			Envelope stored = fetchArtifact(envelope.getId());
			stored.checkOverwrite ( envelope );
		} catch (ArtifactNotFoundException e) {
			// ok, new artifact
		}
		
		htStoredArtifacts.put( envelope.getId(), envelope);
	}

	@Override
	public void removeArtifact(long id, byte[] signature) throws ArtifactNotFoundException {
		if ( htStoredArtifacts.get ( id) == null )
			throw new ArtifactNotFoundException ( id );
		
		htStoredArtifacts.remove( id );
	}

	@Override
	public Object[] findRegisteredAgent(long agentId, int hintOfExpectedCount) throws AgentNotKnownException {
		return findAllNodesWithAgent( agentId);		
	}
	
	@Override
	public Agent getAgent(long id) throws AgentNotKnownException {
		if ( locallyKnownAgents.hasAgent ( id ))
			return locallyKnownAgents.getAgent( id );
		else {
			synchronized (htKnownAgents) {
				String xml = htKnownAgents.get ( id );
				if (xml == null )
					throw new AgentNotKnownException(id);
				
				try {
					Agent result = Agent.createFromXml(xml);
					locallyKnownAgents.registerAgent( result);
					return result;
				} catch ( MalformedXMLException e) {
					throw new AgentNotKnownException("XML problems with storage!", e);
				}
			}
		}	
	}
	

	@Override
	public void storeAgent(Agent agent) throws L2pSecurityException, AgentException {
		synchronized ( htKnownAgents ) {
			// only accept unlocked agents at startup
			if ( agent.isLocked() && getStatus() == NodeStatus.RUNNING )
				throw new L2pSecurityException ( "Only unlocked agents may be updated during runtime!" );
			
			if ( htKnownAgents.get( agent.getId()) != null  )
				throw new AgentAlreadyRegisteredException("Agent " + agent.getId() + " already in storage");
			
			locallyKnownAgents.registerAgent(agent);
			
			htKnownAgents.put( agent.getId(), agent.toXmlString());

			if ( agent instanceof UserAgent )
				updateUserAgentList((UserAgent) agent);
		}
	}

	@Override
	public void updateAgent(Agent agent) throws AgentException, L2pSecurityException {
		if ( agent.isLocked() )
			throw new L2pSecurityException ( "Only unlocked agents may be updated!" );
		
		synchronized (htKnownAgents) {
			if ( htKnownAgents.get(agent.getId()) == null)
				throw new AgentNotKnownException(agent.getId());
			
			// TODO: verify, that it is the same agent!!! (e.g. the same private key)
			// idea: encrypt to stored agent
			// decrypt with new agent (which is unlocked)
			// then update is ok
			
			// other idea:
			// get rid of agent id
			// use hash value of private key instead
			
			// this is verifyable on each local node
			
			locallyKnownAgents.registerAgent( agent );
			htKnownAgents.put( agent.getId(), agent.toXmlString());
			
			if ( agent instanceof UserAgent )
				updateUserAgentList((UserAgent) agent);			
		}
	}	
	
	
	/************************** factories ***************************************/
	
	/**
	 * 
	 * @return a new configured but not running node
	 */
	public static LocalNode newNode () {
		return new LocalNode();
	}
	
	/**
	 * factory: launch a node
	 * 
	 * @return	a freshly started node
	 */
	public static LocalNode launchNode () {
		LocalNode result = newNode();
		result.launch();
		return result;
	}
	
	/**
	 * factory: launch a node an register the given agent
	 * 
	 * @param a
	 * @return	a freshly started node hosting the given agent
	 * @throws L2pSecurityException 
	 * @throws AgentException 
	 * @throws PastryStorageException 
	 */
	public static LocalNode launchAgent ( Agent a ) throws L2pSecurityException, AgentException {
		LocalNode result = launchNode();
		try {
			result.registerReceiver(a);
		} catch (AgentAlreadyRegisteredException e) {
			// should not occur with a freshly generated node
		}
		return result;
	}
	
	
	
	
	
	

	
	
	/****************************** static *****************************************/
	
	
	private static Hashtable <Long, LocalNode> htLocalNodes = new Hashtable<Long, LocalNode>();
	
	private static Hashtable <Long,Envelope> htStoredArtifacts = new Hashtable<Long, Envelope> ();

	private static Hashtable <Long, Hashtable<Message, MessageResultListener>> htPendingMessages = new Hashtable <Long, Hashtable<Message, MessageResultListener>> ();
	
	/**
	 * Hashtable with string representations of all known agents 
	 */
	private static Hashtable <Long, String> htKnownAgents = new Hashtable<Long, String> ();
	
	
	
	/**
	 * register a node for later use
	 * 
	 * @param node
	 */
	private static void registerNode(LocalNode node) {
		synchronized ( htLocalNodes ) {
			htLocalNodes.put ( node.getNodeId(), node );
		}
	}
	
	/**
	 * remove a node from the central storage
	 * @param node
	 */
	private static void unregisterNode ( LocalNode node ) {
		synchronized ( htLocalNodes ) {
			htLocalNodes.remove ( node.getNodeId());
		}
	}

	/**
	 * get a node from the central storage
	 * @param id
	 * @return	the node with the given id 
	 */
	public static LocalNode getNode ( long id ) {
		synchronized ( htLocalNodes ) {
			return htLocalNodes.get ( id );
		}
	}
	
	/**
	 * does the given node exist in the central storage?
	 * @param id
	 * @return	true, if a node of the given it is known to the registry
	 */
	public static boolean hasNode ( long id ) {
		synchronized ( htLocalNodes ) {
			return getNode ( id ) != null;
		}
	}
	
	/**
	 * do a complete restart of all nodes, artifacts and messages
	 */
	public static void reset () {
		htPendingMessages = new Hashtable<Long, Hashtable<Message,MessageResultListener>>();
		htStoredArtifacts = new Hashtable<Long, Envelope>();
		htKnownAgents = new Hashtable<Long, String>();
		htLocalNodes = new Hashtable<Long, LocalNode>();
		
		stopCleaner();
		
		startPendingTimer();
	}
	
	/**
	 * stop the timeout cleaner thread
	 */
	public static void stopCleaner () {
		if (pendingTimer != null) {
			pendingTimer.stopTimer();
			pendingTimer.interrupt();
			try {
				pendingTimer.join();
			} catch (InterruptedException e) {
			}
		}
		
	}
	
	
	/**
	 * find the first node, where the given agent is registered to
	 * 
	 * @param agentId
	 * @return	id of a node hosting the given agent
	 * @throws AgentNotKnownException
	 */
	public static long findFirstNodeWithAgent ( long agentId) throws AgentNotKnownException {
		synchronized ( htLocalNodes ) {
	
			for ( long nodeId : htLocalNodes.keySet()) {
				if ( htLocalNodes.get( nodeId).hasLocalAgent ( agentId ) )
					return nodeId;
			}
			
			throw new AgentNotKnownException(agentId);
		}
	}
	
	/**
	 * get the ids of all nodes where the given agent is running
	 * 
	 * @param agentId
	 * @return	array with all ids of nodes hosting the given agent
	 */
	public static Long[] findAllNodesWithAgent ( long agentId ) {
		synchronized ( htLocalNodes ) {
			HashSet<Long> hsResult = new HashSet<Long>();
		
			for ( long nodeId : htLocalNodes.keySet()) {
				if ( htLocalNodes.get( nodeId).hasLocalAgent ( agentId ) )
					hsResult.add ( nodeId );
			}		
			
			return hsResult.toArray( new Long[0] );
		}
	}
	
	
	

	
	
	/**
	 * store messages for agents not known to this "network" of nodes
	 * @param message
	 * @param listener
	 */
	protected static void storeMessage ( Message message, MessageResultListener listener ) {
		synchronized ( htPendingMessages ) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get( message.getRecipientId());
			if ( pending == null) {
				pending = new Hashtable<Message, MessageResultListener> ();
				htPendingMessages.put( message.getRecipientId(), pending);
			}
				
			pending.put(message, listener);
		}
	}
	
	/**
	 * fetch all pending messages for the given agent
	 * 
	 * @param recipientId
	 */
	protected static void deliverPendingMessages ( long recipientId, long nodeId ) {
		
		synchronized ( htPendingMessages ) {
			Hashtable<Message, MessageResultListener> pending = htPendingMessages.get(recipientId);
			
			if ( pending != null) {
				for ( Message m: pending.keySet()) {
					System.out.println ( "send pending message..." + m.getId());
					localSendMessage(nodeId, m );
				}
				
				htPendingMessages.remove( recipientId );
			}
		}
		
	}

	
	/**
	 * get all expired messages and notify their senders
	 */
	protected static void notifExpiredMessages () {
		synchronized (htPendingMessages) {
			
			System.out.println( "checking for expired messages");
			System.out.println( "waiting for " +  htPendingMessages.size() + " agents ");
			
			for ( long agentId: htPendingMessages.keySet()) {
				Hashtable<Message, MessageResultListener> agentMessages = htPendingMessages.get(agentId);
				
				for ( Message m : agentMessages.keySet() ) {
					MessageResultListener mrl = agentMessages.get( m);
					if ( mrl.checkTimeOut() ) {
						System.out.println( "message " + m.getId() + " is timed out!");
						agentMessages.remove(  m);
					}
				}
				
				// remove agent entry, if empty
				if ( agentMessages.size() == 0)
					htPendingMessages.remove( agentId);
			}			
		}
	}
	
	private static final long DEFAULT_PENDING_TIMEOUT = 20000; // 20 seconds
	private static final int DEFAULT_MESSAGE_MIN_WAIT = 500;
	private static final int DEFAULT_MESSAGE_MAX_WAIT = 3000;
	
	private static final int iMessageMinWait = DEFAULT_MESSAGE_MIN_WAIT;
	private static final int iMessageMaxWait = DEFAULT_MESSAGE_MAX_WAIT;
	private static long lPendingTimeout = DEFAULT_PENDING_TIMEOUT;
	
	private static TimerThread pendingTimer = null;
	
	public static void setPendingTimeOut ( int newtimeout ) { lPendingTimeout = newtimeout; };
	
	public static int getMaxMessageWait () { return iMessageMaxWait; }
	
	static {
		startPendingTimer();
	}
	
	/**
	 * start a thread clearing up expired messages from time to time
	 */
	private static void startPendingTimer() {
		pendingTimer = new TimerThread(lPendingTimeout) {

			@Override
			public void tick() {
				notifExpiredMessages();
			}
			
		};
		
		pendingTimer.start();			
	}


	/**
	 * does the actual <i>sending</i> of a message in a separate thread with a configurable
	 * delay
	 * 
	 * @param nodeId
	 * @param message
	 * @param listener
	 */
	private static void localSendMessage ( final long nodeId, final Message message ) {
		
		// it  is important to close the message her,
		// since the recipient knows other versions of the involved agents 
		message.close();
		
		new Thread (
				new Runnable() {
					public void run() {
						Random r = new Random();
						
						int wait = iMessageMinWait + r.nextInt ( iMessageMaxWait - iMessageMinWait  );
						try {
							Thread.sleep ( wait);
						} catch (InterruptedException e1) {
						}
						
						try {
							getNode(nodeId).receiveMessage(message);
						} catch (Exception e ){
							System.out.println ( "problems at node " + nodeId);
							throw new RuntimeException ( e );
						}
					}
				}
		).start();
		
	}

	@Override
	public boolean knowsAgentLocally(long agentId) {
		return locallyKnownAgents.hasAgent( agentId);
	}

	@Override
	public Object[] getOtherKnownNodes() {
		return htLocalNodes.values().toArray();
	}

	@Override
	public NodeInformation getNodeInformation(Object nodeId) throws NodeNotFoundException {
		try {
			LocalNode node = getNode( (Long) nodeId );			
			return node.getNodeInformation();
		} catch ( Exception e) {
			throw new NodeNotFoundException ( "Node with id " + nodeId + " not found");
		}
	}

	@Override
	public void sendUnlockRequest(long agentId, String passphrase, Object targetNode,
			PublicKey nodeEncryptionKey) throws L2pSecurityException  {

		if ( ! ( targetNode instanceof Long))
			throw new IllegalArgumentException ( "node id is not a Long value but a " + targetNode.getClass().getName() + "(" + targetNode + ")");
	
		try {
			byte[] encPass = CryptoTools.encryptAsymmetric(passphrase, nodeEncryptionKey);
			getNode((Long) targetNode).unlockRemoteAgent(agentId, encPass);
			
			observerNotice(Event.AGENT_UNLOCKED, targetNode, "agent " + agentId + " unlocked at target node" );
		} catch (Exception e) {
			observerNotice(Event.AGENT_UNLOCK_FAILED, "unlocking of agent " + agentId + " failed: " + e );
			throw new L2pSecurityException ( "unable to unlock agent at node " + targetNode );
		}
		
		
	}



	
}
