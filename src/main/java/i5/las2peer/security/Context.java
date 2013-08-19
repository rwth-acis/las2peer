package i5.las2peer.security;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

import java.util.Date;
import java.util.Hashtable;


/**
 * Each {@link i5.las2peer.execution.L2pThread} is bound to a context,
 * which is mainly determined by the executing agent.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class Context implements AgentStorage {

	private Agent agent;
	private Object remoteNodeReference = null;
	
	private Hashtable<Long, GroupAgent> groupAgents = new Hashtable <Long, GroupAgent> ();
	
	private Node localNode;
	
	private long lastUsageTime = -1;
	
	
	/**
	 * Creates a new (local) context.
	 * 
	 * @param mainAgent
	 * @param localNode
	 * 
	 * @throws L2pSecurityException
	 */
	public Context ( Node localNode, Agent mainAgent) throws L2pSecurityException {
		this.agent = mainAgent;
		
		//if ( agent.isLocked() )
		//	throw new L2pSecurityException ("Agent needs to be unlocked!");
		
		this.localNode = localNode;
		
		touch();
	}
	
	
	/**
	 * Creates a new (remote) context.
	 * 
	 * @param localNode
	 * @param mainAgent
	 * @param remoteNodeReference
	 * 
	 * @throws L2pSecurityException
	 */
	public Context ( Node localNode, Agent mainAgent, Object remoteNodeReference ) throws L2pSecurityException {
		this ( localNode, mainAgent );
		this.remoteNodeReference = remoteNodeReference;
	}
	
	
	/**
	 * Gets the main agent of this context.
	 * 
	 * @return the agent of this context
	 */
	public Agent getMainAgent () {
		return agent;
	}
	
	
	/**
	 * Gets all group agents, which have been unlocked in this context.
	 * 
	 * @return all (unlocked) group agents of this context
	 */
	public GroupAgent[] getGroupAgents () {
		return groupAgents.entrySet().toArray(new GroupAgent[0]);
	}
	
	
	/**
	 * Tries to open the given id for this context.
	 * 
	 * @param groupId
	 * 
	 * @return the unlocked GroupAgent of the given id
	 * 
	 * @throws AgentNotKnownException 
	 * @throws L2pSecurityException 
	 */
	public GroupAgent requestGroupAgent ( long groupId ) throws AgentNotKnownException, L2pSecurityException {
		if ( groupAgents.containsKey(groupId))
			return groupAgents.get( groupId );
		
		// TODO: hierarchical groups
		// insert here?
		
		
		Agent agent = localNode.getAgent(groupId);
		
		if ( ! (agent instanceof GroupAgent))
			throw new AgentNotKnownException("Agent " + groupId + " is not a group agent!");
	
		GroupAgent group = (GroupAgent) agent; 
		
		try {
			group.unlockPrivateKey(this.getMainAgent());
		} catch (SerializationException e) {
			throw new L2pSecurityException ( "Unable to open group! - serialization problems", e);
		} catch (CryptoException e) {
			throw new L2pSecurityException ( "Unable to open group! - cryptographic problems", e);
		}
		
		groupAgents.put( groupId, group );
		
		return group;
	}
	
	
	/**
	 * Gets a stored envelope from the p2p network.
	 * 
	 * @param id
	 * 
	 * @return envelope containing the requested data
	 * 
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public Envelope getStoredObject ( long id ) throws ArtifactNotFoundException, StorageException{
		return localNode.fetchArtifact( id );
	}
	
	
	/**
	 * Gets a stored envelope from the p2p network.
	 * The envelope will be identified by the stored class and an arbitrary identifier selected by
	 * the using service(s).
	 * 
	 * @param cls
	 * @param identifier
	 * 
	 * @return envelope containing the requested data
	 * 
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public Envelope getStoredObject ( Class<?> cls, String identifier ) throws ArtifactNotFoundException, StorageException {
		long id = Envelope.getClassEnvelopeId(cls, identifier);
		return getStoredObject ( id );
	}
	
	
	/**
	 * Gets a stored envelope from the p2p network.
	 * The envelope will be identified by the stored class and an arbitrary identifier selected by
	 * the using service(s).
	 * 
	 * @param className
	 * @param identifier
	 * 
	 * @return envelope containing the requested data
	 * 
	 * @throws ArtifactNotFoundException
	 * @throws StorageException
	 */
	public Envelope getStoredObject ( String className, String identifier) throws ArtifactNotFoundException, StorageException {
		return getStoredObject ( Envelope.getClassEnvelopeId(className, identifier));
	}
	
	
	/**
	 * Returns the reference object to the executing node.
	 * 
	 * @return the (possibly remote) node trying to execute something at this (local) node
	 */
	public Object getNodeReference () {
		return remoteNodeReference;
	}
	
	
	/**
	 * Refers this context to a local executing agent or a remote one? 
	 * 
	 * @return true, if the request is started locally and not via the P2P network
	 */
	public boolean isLocal () {
		return remoteNodeReference == null;
	}
	
	
	/**
	 * Gives access to the local node.
	 * 
	 * @return	the local P2P node
	 */
	public Node getLocalNode() {
		return localNode;
	}
	
	
	/**
	 * Mark the current time as the last usage.
	 */
	public void touch() {
		lastUsageTime = new Date().getTime();
	}
	
	
	/**
	 * Returns the time of the last usage of this context.
	 * 
	 * @return the timestamp of the last usage
	 */
	public long getLastUsageTimestamp () {
		return lastUsageTime;
	}
	
	
	/**
	 * Uses this context as {@link AgentStorage}.
	 * Returns agents that are unlocked in this context first.
	 * E.g. necessary for opening a received {@link i5.las2peer.communication.Message}. 
	 *  
	 * @param id
	 * 
	 * @return get the agent of the given id 
	 * 
	 * @throws AgentNotKnownException
	 */
	public Agent getAgent(long id) throws AgentNotKnownException {
		if( id == agent.getId())
			return agent;
		
		Agent result;
		if ( (result = groupAgents.get(id)) != null)
			return result;
		
		return localNode.getAgent(id);
	}
	
	
	@Override
	public boolean hasAgent(long id) {
		return id == agent.getId() || groupAgents.containsKey(id);
	}
	
	
	/**
	 * Gets the current LAS2peer context.
	 * 
	 * @throws IllegalStateException called not in a LAS2peer execution thread
	 * 
	 * @return the current context
	 */
	public static Context getCurrent () {
		Thread t = Thread.currentThread();
		
		if (! ( t instanceof L2pThread ))
			throw new IllegalStateException ( "Not executed in a L2pThread environment!");
		
		return ((L2pThread) t).getContext();
	}
	
	
	/**
	 * Logs a message to the l2p system using the observers.
	 * 
	 * Since this method will/should only be used in an L2pThread, the message will come from a
	 * service or a helper, so a SERVICE_MESSAGE is assumed.
	 * Then this message will not be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param message
	 */
	public static void logMessage ( Object from, String message) {
		getCurrent().getLocalNode().observerNotice(Event.SERVICE_MESSAGE, getCurrent().getLocalNode().getNodeId(),
			from.getClass().getSimpleName() + ": " + message);
	}
	
	
	/**
	 * Writes a log message.
	 * The given index (1-50) can be used to differentiate between different log messages.
	 * The serviceAgent and actingUser can be set to null if not known.
	 * Then this message will not be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param index an index between 1 and 50
	 * @param message
	 * @param serviceAgent
	 * @param actingUser
	 */
	public static void logMessage (Object from, int index, String message, Agent serviceAgent, Agent actingUser) {
		Event event = Event.SERVICE_MESSAGE; //Default
		switch(index){
        case 1:
            event = Event.SERVICE_CUSTOM_MESSAGE_1;
        case 2:
            event = Event.SERVICE_CUSTOM_MESSAGE_2;
        case 3:
            event = Event.SERVICE_CUSTOM_MESSAGE_3;
        case 4:
            event = Event.SERVICE_CUSTOM_MESSAGE_4;
        case 5:
            event = Event.SERVICE_CUSTOM_MESSAGE_5;
        case 6:
            event = Event.SERVICE_CUSTOM_MESSAGE_6;
        case 7:
            event = Event.SERVICE_CUSTOM_MESSAGE_7;
        case 8:
            event = Event.SERVICE_CUSTOM_MESSAGE_8;
        case 9:
            event = Event.SERVICE_CUSTOM_MESSAGE_9;
        case 10:
            event = Event.SERVICE_CUSTOM_MESSAGE_10;
        case 11:
            event = Event.SERVICE_CUSTOM_MESSAGE_11;
        case 12:
            event = Event.SERVICE_CUSTOM_MESSAGE_12;
        case 13:
            event = Event.SERVICE_CUSTOM_MESSAGE_13;
        case 14:
            event = Event.SERVICE_CUSTOM_MESSAGE_14;
        case 15:
            event = Event.SERVICE_CUSTOM_MESSAGE_15;
        case 16:
            event = Event.SERVICE_CUSTOM_MESSAGE_16;
        case 17:
            event = Event.SERVICE_CUSTOM_MESSAGE_17;
        case 18:
            event = Event.SERVICE_CUSTOM_MESSAGE_18;
        case 19:
            event = Event.SERVICE_CUSTOM_MESSAGE_19;
        case 20:
            event = Event.SERVICE_CUSTOM_MESSAGE_20;
        case 21:
            event = Event.SERVICE_CUSTOM_MESSAGE_21;
        case 22:
            event = Event.SERVICE_CUSTOM_MESSAGE_22;
        case 23:
            event = Event.SERVICE_CUSTOM_MESSAGE_23;
        case 24:
            event = Event.SERVICE_CUSTOM_MESSAGE_24;
        case 25:
            event = Event.SERVICE_CUSTOM_MESSAGE_25;
        case 26:
            event = Event.SERVICE_CUSTOM_MESSAGE_26;
        case 27:
            event = Event.SERVICE_CUSTOM_MESSAGE_27;
        case 28:
            event = Event.SERVICE_CUSTOM_MESSAGE_28;
        case 29:
            event = Event.SERVICE_CUSTOM_MESSAGE_29;
        case 30:
            event = Event.SERVICE_CUSTOM_MESSAGE_30;
        case 31:
            event = Event.SERVICE_CUSTOM_MESSAGE_31;
        case 32:
            event = Event.SERVICE_CUSTOM_MESSAGE_32;
        case 33:
            event = Event.SERVICE_CUSTOM_MESSAGE_33;
        case 34:
            event = Event.SERVICE_CUSTOM_MESSAGE_34;
        case 35:
            event = Event.SERVICE_CUSTOM_MESSAGE_35;
        case 36:
            event = Event.SERVICE_CUSTOM_MESSAGE_36;
        case 37:
            event = Event.SERVICE_CUSTOM_MESSAGE_37;
        case 38:
            event = Event.SERVICE_CUSTOM_MESSAGE_38;
        case 39:
            event = Event.SERVICE_CUSTOM_MESSAGE_39;
        case 40:
            event = Event.SERVICE_CUSTOM_MESSAGE_40;
        case 41:
            event = Event.SERVICE_CUSTOM_MESSAGE_41;
        case 42:
            event = Event.SERVICE_CUSTOM_MESSAGE_42;
        case 43:
            event = Event.SERVICE_CUSTOM_MESSAGE_43;
        case 44:
            event = Event.SERVICE_CUSTOM_MESSAGE_44;
        case 45:
            event = Event.SERVICE_CUSTOM_MESSAGE_45;
        case 46:
            event = Event.SERVICE_CUSTOM_MESSAGE_46;
        case 47:
            event = Event.SERVICE_CUSTOM_MESSAGE_47;
        case 48:
            event = Event.SERVICE_CUSTOM_MESSAGE_48;
        case 49:
            event = Event.SERVICE_CUSTOM_MESSAGE_49;
        case 50:
            event = Event.SERVICE_CUSTOM_MESSAGE_50;
		}
		getCurrent().getLocalNode().observerNotice(event, getCurrent().getLocalNode().getNodeId(),
				serviceAgent, null, actingUser, from.getClass().getSimpleName() + ": " + message);
	}
	
	
	/**
	 * Logs an error message to the l2p system using the observers.
	 * 
	 * Since this method will/should only be used in an L2pThread, the message will come from a
	 * service or a helper, so a SERVICE_MESSAGE is assumed.
	 * Then this message will not be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param message
	 */
	public static void logError (Object from, String message) {
		getCurrent().getLocalNode().observerNotice(Event.SERVICE_ERROR, getCurrent().getLocalNode().getNodeId(),
			from.getClass().getSimpleName() + ": " + message);
	}
	
	
	/**
	 * Writes an error message.
	 * The given index (1-50) can be used to differentiate between different log messages.
	 * The serviceAgent and userAgent can be set to null if not known.
	 * Then this message will not be monitored by the monitoring observer.
	 * 
	 * @param from the calling class
	 * @param index an index between 1 and 50
	 * @param message
	 * @param serviceAgent
	 * @param actingUser
	 */
	public static void logError (Object from, int index, String message, Agent serviceAgent, Agent actingUser) {
		Event event = Event.SERVICE_ERROR; //Default
		switch(index){
		  case 1:
	            event = Event.SERVICE_CUSTOM_ERROR_1;
	        case 2:
	            event = Event.SERVICE_CUSTOM_ERROR_2;
	        case 3:
	            event = Event.SERVICE_CUSTOM_ERROR_3;
	        case 4:
	            event = Event.SERVICE_CUSTOM_ERROR_4;
	        case 5:
	            event = Event.SERVICE_CUSTOM_ERROR_5;
	        case 6:
	            event = Event.SERVICE_CUSTOM_ERROR_6;
	        case 7:
	            event = Event.SERVICE_CUSTOM_ERROR_7;
	        case 8:
	            event = Event.SERVICE_CUSTOM_ERROR_8;
	        case 9:
	            event = Event.SERVICE_CUSTOM_ERROR_9;
	        case 10:
	            event = Event.SERVICE_CUSTOM_ERROR_10;
	        case 11:
	            event = Event.SERVICE_CUSTOM_ERROR_11;
	        case 12:
	            event = Event.SERVICE_CUSTOM_ERROR_12;
	        case 13:
	            event = Event.SERVICE_CUSTOM_ERROR_13;
	        case 14:
	            event = Event.SERVICE_CUSTOM_ERROR_14;
	        case 15:
	            event = Event.SERVICE_CUSTOM_ERROR_15;
	        case 16:
	            event = Event.SERVICE_CUSTOM_ERROR_16;
	        case 17:
	            event = Event.SERVICE_CUSTOM_ERROR_17;
	        case 18:
	            event = Event.SERVICE_CUSTOM_ERROR_18;
	        case 19:
	            event = Event.SERVICE_CUSTOM_ERROR_19;
	        case 20:
	            event = Event.SERVICE_CUSTOM_ERROR_20;
	        case 21:
	            event = Event.SERVICE_CUSTOM_ERROR_21;
	        case 22:
	            event = Event.SERVICE_CUSTOM_ERROR_22;
	        case 23:
	            event = Event.SERVICE_CUSTOM_ERROR_23;
	        case 24:
	            event = Event.SERVICE_CUSTOM_ERROR_24;
	        case 25:
	            event = Event.SERVICE_CUSTOM_ERROR_25;
	        case 26:
	            event = Event.SERVICE_CUSTOM_ERROR_26;
	        case 27:
	            event = Event.SERVICE_CUSTOM_ERROR_27;
	        case 28:
	            event = Event.SERVICE_CUSTOM_ERROR_28;
	        case 29:
	            event = Event.SERVICE_CUSTOM_ERROR_29;
	        case 30:
	            event = Event.SERVICE_CUSTOM_ERROR_30;
	        case 31:
	            event = Event.SERVICE_CUSTOM_ERROR_31;
	        case 32:
	            event = Event.SERVICE_CUSTOM_ERROR_32;
	        case 33:
	            event = Event.SERVICE_CUSTOM_ERROR_33;
	        case 34:
	            event = Event.SERVICE_CUSTOM_ERROR_34;
	        case 35:
	            event = Event.SERVICE_CUSTOM_ERROR_35;
	        case 36:
	            event = Event.SERVICE_CUSTOM_ERROR_36;
	        case 37:
	            event = Event.SERVICE_CUSTOM_ERROR_37;
	        case 38:
	            event = Event.SERVICE_CUSTOM_ERROR_38;
	        case 39:
	            event = Event.SERVICE_CUSTOM_ERROR_39;
	        case 40:
	            event = Event.SERVICE_CUSTOM_ERROR_40;
	        case 41:
	            event = Event.SERVICE_CUSTOM_ERROR_41;
	        case 42:
	            event = Event.SERVICE_CUSTOM_ERROR_42;
	        case 43:
	            event = Event.SERVICE_CUSTOM_ERROR_43;
	        case 44:
	            event = Event.SERVICE_CUSTOM_ERROR_44;
	        case 45:
	            event = Event.SERVICE_CUSTOM_ERROR_45;
	        case 46:
	            event = Event.SERVICE_CUSTOM_ERROR_46;
	        case 47:
	            event = Event.SERVICE_CUSTOM_ERROR_47;
	        case 48:
	            event = Event.SERVICE_CUSTOM_ERROR_48;
	        case 49:
	            event = Event.SERVICE_CUSTOM_ERROR_49;
	        case 50:
	            event = Event.SERVICE_CUSTOM_ERROR_50;
			}
		getCurrent().getLocalNode().observerNotice(event, getCurrent().getLocalNode().getNodeId(),
				serviceAgent, null, actingUser, from.getClass().getSimpleName() + ": " + message);
	}
	
	
	/**
	 * Opens the given envelope using the agents of this context.
	 * 
	 * @param envelope
	 * @throws DecodingFailedException
	 * @throws L2pSecurityException 
	 */
	public void openEnvelope(Envelope envelope) throws DecodingFailedException, L2pSecurityException {
		if ( agent.isLocked())
			throw new AgentLockedException ();
		
		try {
			envelope.open ( agent );
		} catch ( L2pSecurityException e ) {
			for ( long groupId : envelope.getReaderGroups() ) {
				try {
					GroupAgent group = groupAgents.get( groupId );
					
					if ( group == null ) {
						group = (GroupAgent) getLocalNode().getAgent(groupId);
						if ( group.isMember(getMainAgent() )) {
							group.unlockPrivateKey(getMainAgent());
							groupAgents.put( groupId, group);
						}
					}
					
					if ( group != null ) {
						envelope.open ( group );
						return;
					}
				} catch (Exception e1) {
					System.out.println( "strange, no exception should occur here! - " + e1);
					e1.printStackTrace();
				}
			}
			throw e;
		}
	}
	
	
	/**
	 * Tries to unlock the private key of the main agent.
	 * 
	 * @param passphrase
	 * 
	 * @throws L2pSecurityException
	 */
	public void unlockMainAgent(String passphrase) throws L2pSecurityException {
		if(  agent instanceof PassphraseAgent)
			((PassphraseAgent) agent).unlockPrivateKey(passphrase);
		else
			throw new L2pSecurityException ( "this is not passphrase protected agent!");
	}
	
	
}