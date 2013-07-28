package i5.las2peer.p2p.pastry;

import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;

import java.util.Date;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdFactory;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;
import rice.pastry.commonapi.PastryIdFactory;

/**
 * A simple envelope for LAS2peer data to be stored in the p2p network.
 * 
 * On knowledge of the class of the contained data the user may use the <i>getContained...</i>
 * methods to get typed access. 
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class ContentEnvelope extends ContentHashPastContent {
	
	// TODO: Environment / settings of pastry!
	private static	IdFactory idFactory = new PastryIdFactory(new Environment());

	/**
	public enum TYPE {
		AGENT, ENVELOPE
	};
	**/
	
	public final static byte TYPE_AGENT = 50;
	public final static byte TYPE_ENVELOPE = 100; 
	public final static byte TYPE_UNKNOWN= -1; 
	
	
	private static final long serialVersionUID = -1949920691543117540L;

	private String content;
	
	private long timestamp;
	
	private byte type = TYPE_UNKNOWN;

	
	/**
	 * create a pastry envelope for the given agent
	 * 
	 * @param agent
	 */
	public ContentEnvelope( Agent agent ) {
		super( getPastId ( agent )  );
		
		content = agent.toXmlString();
		type = TYPE_AGENT;
		
		timestamp = new Date().getTime();
	}
	
	
	/**
	 * get the type constant of this envelope.
	 * 
	 * @see #TYPE_AGENT
	 * @see #TYPE_ENVELOPE
	 * @return the type
	 */
	public byte getType () {
		return type;
	}
	
	/**
	 * does this enveloped enwrap an agent? 
	 * @return true, if this envelope contains an agent.
	 */
	public boolean isAgent () {
		return getType() == TYPE_AGENT;
	}
	
	/**
	 * is this envelope a content envelope?
	 * @return false for unknown or agent envelopes
	 */
	public boolean isEnvelope () {
		return getType() == TYPE_ENVELOPE;
	}
	
	/**
	 * create a pastry envelope for the given las2peer envelope
	 * @param e
	 */
	public ContentEnvelope ( Envelope e ) {
		super( getPastId ( e )  );
		
		type = TYPE_ENVELOPE;
		content = e.toXmlString();
		
		timestamp = new Date().getTime();
	}

	/**
	 * get the agent stored within this pastry envelope 
	 * 
	 * @return	an agent stored in this envelope
	 * @throws PastryStorageException
	 */
	public Agent getContainedAgent () throws PastryStorageException {
		if ( type != TYPE_AGENT )
			throw new PastryStorageException ( "This is not an Agent!");
		
		try {
			return Agent.createFromXml(content);
		} catch ( MalformedXMLException e ) {
			throw new PastryStorageException("Xml problems with content xml representation", e);
		}
	}
	
	/**
	 * get the las2peer envelope stored within this pastry envelope 
	 * 
	 * @return	an las2peer envelope stored within this pastry envelope
	 * 
	 * @throws PastryStorageException
	 */
	public Envelope getContainedEnvelope () throws PastryStorageException {
		if ( type != TYPE_ENVELOPE )
			throw new PastryStorageException ( "This is not an envelope!");
		
		try {
			return Envelope.createFromXml(content);
		} catch ( MalformedXMLException e ) {
			throw new PastryStorageException("Xml problems with content xml representation", e);
		}
	}
	
	/**
	 * get the contained object
	 * 
	 * @return the contained data
	 * @throws PastryStorageException
	 */
	public Object getContainedObject () throws PastryStorageException {
		switch ( type ) {
		case TYPE_AGENT: return getContainedAgent();
		case TYPE_ENVELOPE: return getContainedEnvelope();
		default: throw new PastryStorageException("Unkown content type!" ); 
		}			
	}
	
	
	/**
	 * Create a pastry id for the given agent.
	 * 
	 * The id is determined by the agent's id and unique in the complete network.
	 * 
	 * @param agent
	 * @return a pastry envelope id 
	 */
	public static Id getPastId ( Agent agent ) {
		return getPastAgentId ( agent.getId());
	}
	
	/**
	 * create a pastry id for the given las2peer agent id
	 * 
	 * The id is unique in the complete network.
	 * 
	 * @param id
	 * @return a pastry envelope id
	 */
	public static Id getPastAgentId ( long id ) {
		return idFactory.buildId("agent-" + id);
	}
	
	/**
	 * create a pastry id for the given envelope
	 * 
	 * The id is determined by the envelope's own id and unique in the complete network.
	 * 
	 * @param e
	 * @return a pastry envelope id
	 */
	public static Id getPastId ( Envelope e ) {
		return getPastEnvelopeId ( e.getId() );
	}
	
	/**
	 * create a past id to the given las2peer envelope id
	 * 
	 * The id is unique in the complete network.
	 * 
	 * @param id
	 * 
	 * @return a pastry envelope id
	 */
	public static Id getPastEnvelopeId ( long id ) {
		return idFactory.buildId( "envelope-" + id);
	}
	
	/**
	 * create a unique id for the given class and identifier string
	 * 
	 * useful e.g. to defer an envelope by its content parts
	 * 
	 * @param cls
	 * @param identifier
	 * 
	 * @return a (hash) if for the given class using the given identifier
	 */
	public static Id getUniqueClassId ( String cls, String identifier ) {
		return idFactory.buildId( "cls-" + cls + "-" + identifier);
	}

	
	
	
	@Override
	public PastContent checkInsert(Id id, PastContent existingContent) throws PastException {
		/** check, if an update operation is ok to proceed **/
		// always try to prefer the newer one? 
		// the parameter is the existing content, this the new replacement
				
		if ( this == existingContent) {
			System.out.println ( "replace with myself?!?!");
			return this;
		}
		
		if ( existingContent instanceof ContentEnvelope ) {
			//System.out.println( "overwrite? - preferring newer / " + this.timestamp + "/" + ((ContentEnvelope) existingContent).timestamp );
			//System.out.println( "sizes: me:" + content.length() + " other: " + ((ContentEnvelope) existingContent).content.length() );
			
			if ( ((ContentEnvelope) existingContent).timestamp == this.timestamp)
				System.out.println ( "update with same timestamp?!?!");
			
			if (((ContentEnvelope) existingContent).timestamp > this.timestamp ) {
				try {
					if ( overwriteThis ( existingContent ))
						return this;
					else
						return existingContent;
				} catch (PastryStorageException e) {
					// on problems prefer existing one
					return existingContent;
				}
			} else {
				return this;
			}
		}

		try {
			PastContent result = super.checkInsert( id,  existingContent );
			//System.out.println( " past result: " + (result == this));
			return result;
		} catch( PastException e ) {
			//System.out.println ( "Exception in update: " + e );
			throw e;
		}
	}
	
	/**
	 * overwrite the this envelope with the given new content?
	 * 
	 * @param existingContent
	 * @return true, if this instance is to be used, false, if the existing parameter envelops should stay
	 * @throws PastryStorageException 
	 */
	private boolean overwriteThis(PastContent existingContent) throws PastryStorageException {
		// prefer content envelopes, since we are a las2peer application 
		if ( ! ( existingContent instanceof ContentEnvelope) )
			return true;
		
		ContentEnvelope existing = (ContentEnvelope) existingContent;
		
		// prefer existing on type change!?
		if ( this.getType() != existing.getType())
			return false;
		
		if ( this.isAgent() ) {
			// check signature
			// for overwriting own signature is needed
			Agent thisAgent =  this.getContainedAgent();
			Agent existingAgent = existing.getContainedAgent();
			
			// allow overwrite only with the same key!
			if (thisAgent.getPublicKey().equals( existingAgent.getPublicKey()))
				return true;
			else 
				return false;
		} else if ( this.isEnvelope() ) {
			// let the envelope decide			
			Envelope thisEnv = getContainedEnvelope();
			Envelope existingEnv = existing.getContainedEnvelope();
			
			try {
				existingEnv.checkOverwrite(thisEnv);
				return true;
			} catch (L2pSecurityException e) {
				return false;
			}
		} else
			// unknown what to do
			return false;
	}

	@Override
	public boolean isMutable () {
		return true;
	}
	
}
