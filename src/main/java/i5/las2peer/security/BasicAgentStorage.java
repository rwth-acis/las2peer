package i5.las2peer.security;

import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;

import java.util.Hashtable;


/**
 * A simple Hashtable based storage for agents.
 * 
 * 
 *
 */
public class BasicAgentStorage implements AgentStorage {

	private Hashtable<Long, Agent> htRegistered = new Hashtable<Long, Agent>(); 
	
	private AgentStorage backupStorage;
	
	
	/**
	 * create a basic agent storage with a backupStorage to use, if a 
	 * requested agent is not stored here
	 * (i.e. a {@link Node} to look for the requested agent in the
	 * whole network
	 * 
	 * @param backupStorage
	 */
	public BasicAgentStorage ( AgentStorage backupStorage) {
		this.backupStorage = backupStorage;
		
		// TODO
		// maybe initialize tidy up thread
	}
	
	
	/**
	 * create a new basic agent storage
	 */
	public BasicAgentStorage () {
		this ( null );
	}
	
	
	/**
	 * register an agent for later use
	 * 
	 * use a locked copy to store
	 * 
	 * @param agent
	 */
	public void registerAgent ( Agent agent ) {
		try {
			Agent register = agent.cloneLocked ();
			
			htRegistered.put( register.getId(), register );
		} catch (CloneNotSupportedException e) {
			// should not occur, since agent is cloneable
			throw new RuntimeException ( "Clone problems", e);
		}
	}
	
	/**
	 * register multiple agents to this storage
	 * @param agents
	 */
	public void registerAgents ( Agent... agents) {
		for ( Agent a: agents)
			registerAgent ( a );
	}
	
	/**
	 * remove an agent from this storage
	 * @param agent
	 */
	public void unregisterAgent ( Agent agent ) {
		htRegistered.remove ( agent.getId());
	}
	
	/**
	 * remove an agent from this storage
	 * 
	 * @param id
	 */
	public void unregisterAgent ( long id ) {
		htRegistered.remove ( id );
	}
	
	/**
	 * get the agent for the given id
	 * @param id
	 * @return	an agent
	 */
	public Agent getAgent ( long id ) throws AgentNotKnownException {
		Agent result = htRegistered.get(id);
		
		if ( result != null )
			try {
				return result.cloneLocked();
			} catch (CloneNotSupportedException e) {
				throw new AgentNotKnownException(id, e);
			}
		
		if ( backupStorage != null ) {
			return backupStorage.getAgent(id);
		} else
			throw new AgentNotKnownException(id);
	}


	@Override
	public boolean hasAgent(long id) {
		return htRegistered.get( id )  != null;
	}
	
}
