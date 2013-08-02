package i5.las2peer.p2p;

import i5.las2peer.communication.Message;
import i5.las2peer.communication.MessageException;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.pastry.ContentEnvelope;
import i5.las2peer.p2p.pastry.MessageEnvelope;
import i5.las2peer.p2p.pastry.NodeApplication;
import i5.las2peer.p2p.pastry.PastGetContinuation;
import i5.las2peer.p2p.pastry.PastPutContinuation;
import i5.las2peer.p2p.pastry.PastryStorageException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.BasicAgentStorage;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.MessageReceiver;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.ColoredOutput;
import i5.las2peer.tools.SerializationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastImpl;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.PersistentStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;


/**
 * A <a href="http://freepastry.org">FreePastry</a> implementation of a LAS2peer {@link Node}.
 * 
 * This class is the actual heart of the p2p based network of interacting nodes an agents in
 * the las2peer setting.
 * 
 * The package {@link i5.las2peer.p2p.pastry} provides all necessary helper classes
 * for the integration (and encapsulation) of the freepastry library.
 *  
 * @author Holger Jan&szlig;en
 *
 */
public class PastryNodeImpl extends Node {

	public static final int DEFAULT_PAST_STORAGE_DISCSIZE = 50*1024*1024;
	private int pastStorageDiscSize = DEFAULT_PAST_STORAGE_DISCSIZE;
	
	public static final int DEFAULT_MEMORY_CACHESIZE = 512 * 1024;
	private int pastMemoryCacheSize = DEFAULT_MEMORY_CACHESIZE;
	
	
	public static final int DEFAULT_PAST_REPLICATS = 3;
	private int pastReplicas = DEFAULT_PAST_REPLICATS;

	/**
	 * Storage mode for the pastry node &ndash; either use only memory or the filesystem
	 * for stored artifacts.
	 *  
	 * @author Holger Jan&szlig;en
	 *
	 */
	public enum STORAGE_MODE {
		filesystem,
		memory
	}
		
	public static final int STANDARD_PORT = 9901;
	public static final String STANDARD_BOOTSTRAP = "localhost:9900,localhost:9999";
	private static final int AGENT_GET_TIMEOUT = 10000;
	private static final int ARTIFACT_GET_TIMEOUT = 10000; 
	
	private int pastryPort = STANDARD_PORT;
	private String bootStrap = STANDARD_BOOTSTRAP;
	
	private Environment pastryEnvironment;	
	private PastryNode pastryNode = null;
	
	private NodeApplication application;
	
	private Past pastStorage;
	
	
	private STORAGE_MODE mode = STORAGE_MODE.filesystem;
	
	private String storagePath = ".las2peer";
		
	private BasicAgentStorage locallyKnownAgents;
	
	
	
	/**
	 * create a node listening to the given port an 
	 * trying to connect to the hosts given in the bootstrap string
	 * The bootstrap string may be a comma separated lists of host possibly including
	 * port information separated be a colon.
	 * 
	 * Leave the bootstrap empty or null to start a new ring
	 * 
	 * @param baseClassLoader
	 * @param port
	 * @param bootstrap
	 */
	public PastryNodeImpl ( ClassLoader baseClassLoader, int port, String bootstrap ) {
		super ( baseClassLoader );
		initialize ( port, bootstrap );
	}
		
	
	/**
	 * create a standard node on localhost trying to connect
	 * to the standard bootstrap
	 * 
	 * @throws UnknownHostException
	 */
	public PastryNodeImpl () throws UnknownHostException {
		this( STORAGE_MODE.filesystem );
	}
	
	/**
	 * create a standard node on localhost trying to connect to the standard bootstrap 
	 * @param mode
	 */
	public PastryNodeImpl ( STORAGE_MODE mode ) {
		super ();
		
		initialize ( mode );
	}
	
	
	/**
	 * create a node listening to the given port an 
	 * trying to connect to the hosts given in the bootstrap string
	 * The bootstrap string may be a comma separated lists of host possibly including
	 * port information separated be a colon. Leave empty or null to start a new ring.
	 * 
	 * @param port
	 * @param bootstrap
	 */
	public PastryNodeImpl ( int port, String bootstrap) {
		this ( port, bootstrap, STORAGE_MODE.filesystem);
	}
	
	
	/**
	 * create a node listening to the given port an 
	 * trying to connect to the hosts given in the bootstrap string
	 * The bootstrap string may be a comma separated lists of host possibly including
	 * port information separated be a colon. Leave empty or null to start a new ring.
	 * 
	 * @param port
	 * @param bootstrap
	 * @param mode
	 */
	public PastryNodeImpl ( int port, String bootstrap, STORAGE_MODE mode ) {
		super();
		initialize(port, bootstrap, mode);
	}

	/**
	 * create a node listening to the given port an 
	 * trying to connect to the hosts given in the bootstrap string
	 * The bootstrap string may be a comma separated lists of host possibly including
	 * port information separated be a colon. Leave empty or null to start a new ring.
	 * 
	 * The observer-flag determines, if the node will be available for monitoring.
	 *  
	 * @param port
	 * @param bootstrap
	 * @param mode
	 */
	public PastryNodeImpl ( int port, String bootstrap, STORAGE_MODE mode, boolean monitoringObserver) {
		super(null, true, monitoringObserver);
		initialize(port, bootstrap, mode);
	}
	
	
	/**
	 * local initialization for constructors
	 * 
	 * @param port
	 * @param bootstrap
	 * @param mode
	 */
	private void initialize(int port, String bootstrap, STORAGE_MODE mode) {
		pastryPort = port;
		this.bootStrap = bootstrap;
		this.mode = mode;
		
		locallyKnownAgents = new BasicAgentStorage ( this );
		
		setupPastryEnvironment();
		
		this.setStatus( NodeStatus.CONFIGURED );
	}
	
	/**
	 * local initialization for constructors
	 * 
	 * @param port
	 * @param bootstrap
	 */
	private void initialize ( int port, String bootstrap ) {
		initialize ( port, bootstrap, STORAGE_MODE.filesystem );
	}
	
	/**
	 * local initialization for constructors
	 * 
	 * @param mode
	 */
	private void initialize ( STORAGE_MODE mode ) {
		initialize( STANDARD_PORT, STANDARD_BOOTSTRAP, mode );		
	}
	
	
	

	/**
	 * access to the underlying pastry node
	 * 
	 * @return	the pastry node representing this las2peer node
	 */
	public PastryNode getPastryNode() { return pastryNode; }
	
	
	/**
	 * generate a collection of InetSocketAddresses from the given bootstrap string
	 * @return	collection of InetSocketAddresses from the given bootstrap string
	 */
	private Collection<InetSocketAddress> getBootstrapAddresses () {
		Vector<InetSocketAddress> result = new Vector<InetSocketAddress>();
		
		if ( bootStrap == null || bootStrap.equals( ""))
			return result;
		
		String[] addresses = bootStrap.split(",");
		for ( String address: addresses ) {
			String[] hostAndPort = address.split(":");
			int port = STANDARD_PORT;
			
			if ( hostAndPort.length == 2 )
				port = Integer.parseInt(hostAndPort[1]);
			
			try {
				result.add( new InetSocketAddress( InetAddress.getByName(hostAndPort[0]), port ));
			} catch (UnknownHostException e) {
				System.err.println ( "cannot resolve address for: " + address);
			}
		}
		
		return result;
	}
	
	/**
	 * setup pastry environment settings
	 */
	private void setupPastryEnvironment () {
		pastryEnvironment = new Environment();
		
		String[] configFiles = new String[] { "pastry.properties", "config/pastry.properties", "properties/pastry.properties" };
		String found = null;
		for ( int i=0; i<configFiles.length && found == null; i++) {
			if ( new File ( configFiles[i] ) .exists ())
				found = configFiles[i];		
		}

		Hashtable<String, String> properties = new Hashtable<String, String>();
		if ( found != null ) {
			System.out.println ( "Using pastry property file " + found);
			try {
				Properties props = new Properties();
				props.load(new FileInputStream ( found ));
							
				for ( Object propname: props.keySet() )
					properties.put( (String) propname, (String) props.get(propname)); 
			} catch (FileNotFoundException e) {
				System.err.println( "Unable to open property file " + found);
			} catch (IOException e) {
				System.err.println( "Error opening property file " + found + ": " + e.getMessage());
			}
		} else  
			System.out.println ( "No pastry property file found - using default values");
	
		
		if ( !properties.containsKey("nat_search_policy"))
			properties.put( "nat_search_policy", "never");
		if ( !properties.containsKey("firewall_test_policy"))
			properties.put( "firewall_test_policy", "never");
		if ( !properties.containsKey("nat_network_prefixes"))
			properties.put( "nat_network_prefixes", "127.0.0.1;10.;192.168.;");
		
		if ( !properties.containsKey("pastry_socket_known_network_address"))
			//properties.put( "pastry_socket_known_network_address", "127.0.0.1");
		if ( !properties.containsKey("pastry_socket_known_network_address_port"))
			properties.put( "pastry_socket_known_network_address_port", "80");
		
		// remarks: you need an network accessible host/port combination, even, if you want to start a new ring!!
		// for offline testing, you need to run some kind of port reachable server!
		
		if ( !properties.containsKey("nat_search_policy"))
			properties.put( "nat_search_policy", "never");
		
		
		// check for building a new ring
		/* if ( "NEW".equals ( bootStrap) ) {
			properties.put( "pastry_socket_known_network_address", "");
			properties.put( "pastry_socket_known_network_address_port", ""+pastryPort);
		} */
		

		for ( String prop : properties.keySet() ) {
			pastryEnvironment.getParameters().setString( prop, properties.get ( prop ) );
			
			System.out.println ( "setting: " + prop + ": '" + properties.get( prop) + "'");
		}
	}
	
	
	
	/**
	 * setup all pastry applications to run on this node
	 * 
	 * this will be
	 * <ul><li>the application for message passing {@link NodeApplication}</li>
	 * <li>a Past DHT storage from freepastry</li></ul>
	 * 
	 * For the past DHT either a memory mode or a disk persistence mode are selected
	 * based on {@link storageMode}
	 * 
	 * @throws IOException
	 */
	private void setupPastryApplications () throws IOException {
		application = new NodeApplication ( this );
		
		PastryIdFactory idf = new rice.pastry.commonapi.PastryIdFactory(pastryEnvironment);

		Storage storage;
		if (mode == STORAGE_MODE.filesystem) {
			String storageDirectory =  storagePath + File.separator + "node_" + pastryNode.getId().hashCode();
			storage = new PersistentStorage(
					idf, 
					storageDirectory,
					pastStorageDiscSize, 
					pastryNode.getEnvironment()
			);
		} else
			storage = new MemoryStorage ( idf );
							
		LRUCache cache = new LRUCache(
				new MemoryStorage(idf), 
				pastMemoryCacheSize,
				pastryEnvironment);
		
		pastStorage = new PastImpl(
				pastryNode, 
				new StorageManagerImpl(
						idf,
						storage, 
						cache), 
				pastReplicas, 
				""
		);
		
	}
	
	
	/**
	 * start this node
	 */
	@Override
	public void launch() throws NodeException {
		
		try {
			setStatus ( NodeStatus.STARTING );
			
			NodeIdFactory nidFactory = new RandomNodeIdFactory(pastryEnvironment);
			PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, pastryPort, pastryEnvironment);
			pastryNode = factory.newNode();
			
		    setupPastryApplications ();
			
			pastryNode.boot(getBootstrapAddresses());

			synchronized (pastryNode) {
				while (!pastryNode.isReady() && !pastryNode.joinFailed()) {
					// delay so we don't busy-wait
					pastryNode.wait(500);

					// abort if can't join
					if (pastryNode.joinFailed()) {
						throw new NodeException(
								"Could not join the FreePastry ring.  Reason:"
										+ pastryNode.joinFailedReason());
					}
				}
			}

			setStatus ( NodeStatus.RUNNING );
			
			registerShutdownMethod();
		
		} catch (IOException e) {
			throw new NodeException("IOException while joining pastry ring", e);
		} catch (InterruptedException e) {
			throw new NodeException("Interrupted while joining pastry ring!", e);
		} catch ( IllegalStateException e ) {
			throw new NodeException("Unable to open Netwock socket - is the port already in use?", e );
		}
	}

	/**
	 * register node shutdown as JVM shutdown method
	 */
	private void registerShutdownMethod () {
		final PastryNodeImpl self = this;
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	self.setStatus ( NodeStatus.CLOSING);
		        self.shutDown();
		        self.setStatus ( NodeStatus.CLOSED);
		    }
		});		
	}
	
	@Override
	public void shutDown() {
		this.setStatus(NodeStatus.CLOSING);
		pastryNode.destroy();
		
		super.shutDown();		
		this.setStatus(NodeStatus.CLOSED);
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) throws AgentAlreadyRegisteredException,
			L2pSecurityException, AgentException {

		synchronized ( this ) {
			super.registerReceiver( receiver );
			
			application.registerAgentTopic(receiver);
			
			//Observer is called in superclass!
		}
	}

	@Override
	public void unregisterAgent(long id) throws AgentNotKnownException {
		synchronized (this) {
			application.unregisterAgentTopic (id);
			
			super.unregisterAgent(id);						
		}
	}

	
	@Override
	public void sendMessage(Message message, MessageResultListener listener,
			SendMode mode) {
		// TODO: use mode?!?!
		observerNotice(Event.MESSAGE_SENDING, pastryNode.getLocalHandle(), message.getSenderId(), null, message.getRecipientId(), "broadcasting");
		
		registerAnswerListener(message.getId(), listener);
		
		application.sendMessage(message);
	}

	@Override
	public void sendMessage(Message message, Object atNodeId,
			MessageResultListener listener) throws AgentNotKnownException,
			NodeNotFoundException, L2pSecurityException {
		
		if ( ! ( atNodeId instanceof NodeHandle )) {
			String addition = "(null)";
			if ( atNodeId != null)
				addition = ""+atNodeId.getClass();
			IllegalArgumentException e = new IllegalArgumentException ( "node id in pastry nets has to be a NodeHandle instance but is " + addition);
			e.printStackTrace();
			throw e;
		}
		
		observerNotice(Event.MESSAGE_SENDING, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "" );
		
		registerAnswerListener(message.getId(), listener);
		
		try {
			application.sendMessage(new MessageEnvelope( pastryNode.getLocalHandle(), message), (NodeHandle) atNodeId );
		} catch (MalformedXMLException e) {
			observerNotice(Event.MESSAGE_FAILED, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "XML exception!" );			
		} catch ( MessageException e ) {
			observerNotice(Event.MESSAGE_FAILED, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "Message exception!" );
		}
	}
	
	

	/*
	public void sendResponse(Message message, Object atNodeId)
			throws AgentNotKnownException, L2pSecurityException {
		
		if ( ! ( atNodeId instanceof NodeHandle )) {
			String addition = "(null)";
			if ( atNodeId != null) addition = ""+atNodeId.getClass();
			IllegalArgumentException e = new IllegalArgumentException ( "node id in pastry nets has to be a NodeHandle instance but is " + addition);
			e.printStackTrace();
			throw e;
		}
		
		observerNotice(Event.RESPONSE_SENDING, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "" );
		
		try {
			application.sendMessage(new MessageEnvelope( pastryNode.getLocalHandle(), message), (NodeHandle) atNodeId );
		} catch (MalformedXMLException e) {
			observerNotice(Event.RESPONSE_FAILED, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "XML exception!" );			
		} catch ( MessageException e ) {
			observerNotice(Event.RESPONSE_FAILED, this, message.getSenderId(), (NodeHandle) atNodeId, message.getRecipientId(), "Message exception!" );
		}		
	}	*/


	@Override
	public Envelope fetchArtifact(long id) throws ArtifactNotFoundException, StorageException {

		observerNotice(Event.ARTIFACT_FETCH_STARTED, pastryNode, ""+id);
		
		Id pastryId = ContentEnvelope.getPastEnvelopeId(id);
		
		PastGetContinuation<Envelope> continuation = new PastGetContinuation<Envelope>( Envelope.class, ARTIFACT_GET_TIMEOUT, "getting artefact " + id );
		
		pastStorage.lookup( pastryId, continuation);
		
		try {
			Envelope contentFromNet = continuation.getResultWaiting();

			observerNotice(Event.ARTIFACT_RECEIVED, pastryNode, ""+id);
			
			return contentFromNet;
		} catch (Exception e) {
			observerNotice(Event.ARTIFACT_FETCH_FAILED, pastryNode, ""+id);
			throw new StorageException ( "Unable to retrieve Artifact from past storage", e);
		}			
	}

	@Override
	public void storeArtifact(Envelope envelope) throws StorageException, L2pSecurityException {
		
		// check for overwriting
		try {
			Envelope stored = fetchArtifact(envelope.getId());
			
			stored.checkOverwrite ( envelope );
		} catch (ArtifactNotFoundException e) {
			// ok, new artifact
		} catch ( StorageException e ) {
			// new artifact?!?
			observerNotice(Event.NODE_ERROR, pastryNode, "Got a StorageException while checking for Artifact " + envelope.getId() + " overwrite!");
		} catch ( L2pSecurityException e ){
			observerNotice ( Event.ARTIFACT_OVERWRITE_FAILED, pastryNode, envelope.getId()+"" );
			throw e;
		}
		
		PastPutContinuation conti = new PastPutContinuation();
		pastStorage.insert ( new ContentEnvelope ( envelope ), conti );

		//System.out.println ( "back from insert call");
		try {
			//System.out.println ( "   now waiting for feedback...");
			conti.waitForResult();
			if ( !conti.isSuccess() ) {
				observerNotice(Event.ARTIFACT_UPLOAD_FAILED, pastryNode, "Storage error for Artifact " + envelope.getId());
				if ( conti.hasException())
					throw new PastryStorageException ( "Error storing update", conti.getExceptions()[0] );
				else
					throw new PastryStorageException ( "Error storing update" );
			}
			

		} catch (InterruptedException e) {
			throw new PastryStorageException ( "Storage has been interrupted", e );
		}

		observerNotice(Event.ARTIFACT_ADDED, pastryNode, envelope.getId()+"");
		
	}

	@Override
	public void removeArtifact(long id, byte[] signature) throws StorageException, ArtifactNotFoundException {
		throw new PastryStorageException ( "delete not implemented in pastry!");
	}

	@Override
	public Object[] findRegisteredAgent(long agentId, int hintOfExpectedCount) throws AgentNotKnownException {
		observerNotice(Event.AGENT_SEARCH_STARTED, pastryNode, "" + agentId);
		
		return application.searchAgent ( agentId, hintOfExpectedCount ).toArray();
	}


	
	@Override
	public boolean knowsAgentLocally(long agentId) {
		return locallyKnownAgents.hasAgent( agentId);
	}
	

	/**
	 * provides access to the underlying pastry application
	 * mostly for testing purposes
	 * 
	 * @return	the pastry node application of this pastry node
	 */
	public NodeApplication getApplication () { return application; }
	
	
	/**
	 * simple testing method, will be removed later
	 * 
	 * @param from
	 * @param to
	 * @throws InterruptedException
	 * @throws EncodingFailedException
	 * @throws L2pSecurityException
	 * @throws SerializationException
	 * @throws MalformedXMLException
	 * @throws IOException
	 * @throws AgentNotKnownException 
	 * @throws MessageException 
	 */
	public void sendTestMessages( Agent from, Agent to) throws InterruptedException, EncodingFailedException, L2pSecurityException, SerializationException, MalformedXMLException, IOException, AgentNotKnownException, MessageException {
		int counter = 0;
		
		while ( true ) {
			pastryEnvironment.getTimeSource().sleep(10000);
			ColoredOutput.printlnRed ( "---------------------------------------");
			ColoredOutput.printlnRed( "Sending new Messages");
			
			
		    LeafSet leafSet = pastryNode.getLeafSet();
		    ColoredOutput.printlnRed ( "LeafSet-Size: " + leafSet.cwSize());
		    
		    // this is a typical loop to cover your leafset.  Note that if the leafset
		    // overlaps, then duplicate nodes will be sent to twice
		    for (int i=-leafSet.ccwSize(); i<=leafSet.cwSize(); i++) {
		      if (i != 0) { // don't send to self
		        // select the item
		        NodeHandle nh = leafSet.get(i);
		        
		        // send the message directly to the node
		        ColoredOutput.printlnRed( "sending to " + i + " / " + nh );
		        
		        counter++;
		        // TODO!!!!!
		        application.sendMessage ( new MessageEnvelope ( pastryNode.getLocalNodeHandle(), new Message (from, to, "testnachricht: " + counter)  ), nh);
		        
		        // wait a sec
		        pastryEnvironment.getTimeSource().sleep(1000);
		      }
		    }
		    
		    ColoredOutput.printlnRed( "---------------------------------------------");
		}

	}

	@Override
	public Agent getAgent(long id) throws AgentNotKnownException {
		if ( ! locallyKnownAgents.hasAgent ( id )) {
			observerNotice(Event.AGENT_GET_STARTED, pastryNode, "" + id );
		
			Id pastryId = ContentEnvelope.getPastAgentId(id);
			
			PastGetContinuation<Agent> continuation = new PastGetContinuation<Agent>( Agent.class, AGENT_GET_TIMEOUT,  "fetching agent: " + id );
			
			pastStorage.lookup( pastryId, continuation );
			
			try {
				Agent agentFromNet = continuation.getResultWaiting();

				observerNotice(Event.AGENT_GET_SUCCESS, pastryNode, "" + id );
				
				locallyKnownAgents.registerAgent(agentFromNet);
			} catch (Exception e) {
				observerNotice(Event.AGENT_GET_FAILED, pastryNode,  "" + id );
				throw new AgentNotKnownException ( "Unable to retrieve Agent "+id+" from past storage", e);
			}			
		}
	
		return locallyKnownAgents.getAgent( id );
	}

	@Override
	public void storeAgent(Agent agent) throws L2pSecurityException, AgentException {
		if ( agent.isLocked())
			throw new L2pSecurityException ( "You have to unlock the agent before storage!");
		if ( locallyKnownAgents.hasAgent(agent.getId()))
			throw new AgentAlreadyRegisteredException("This agent is already known locally!");
		
		observerNotice(Event.AGENT_UPLOAD_STARTED, pastryNode, "" + agent.getId());
		
		try {
			Agent stored = getAgent ( agent.getId() );
			observerNotice(Event.AGENT_UPLOAD_FAILED, pastryNode, "Agent " + agent + " already known!" );
			throw new AgentAlreadyRegisteredException ( "I already know stored version: " + stored );
		} catch (AgentNotKnownException e) {
		}
		
		locallyKnownAgents.registerAgent(agent);
		
		PastPutContinuation conti = new PastPutContinuation();
		
		pastStorage.insert ( new ContentEnvelope ( agent ), conti );

		try {
			conti.waitForResult();
			if ( !conti.isSuccess() ) {
				observerNotice(Event.AGENT_UPLOAD_FAILED, pastryNode, "Storage error for Agent: " + agent.getId() );
				locallyKnownAgents.unregisterAgent(agent);
				throw new AgentException ( "Storage problems", new PastryStorageException ( "error storing update" ));
			}
	
			if ( agent instanceof UserAgent )
				updateUserAgentList((UserAgent) agent);			
			
			observerNotice(Event.AGENT_UPLOAD_SUCCESS, pastryNode, "" + agent.getId());
		} catch (InterruptedException e) {
			locallyKnownAgents.unregisterAgent(agent);
			observerNotice(Event.AGENT_UPLOAD_FAILED, pastryNode, "Got interrupted for Agent " + agent.getId());
			throw new AgentException ( "Storage has been interrupted", e );
		}
	}

	@Override
	public void updateAgent(Agent agent) throws AgentException, L2pSecurityException, PastryStorageException {
		if ( agent.isLocked())
			throw new L2pSecurityException ( "You have to unlock the agent before updating!");
		
		//Agent stored = 
				getAgent ( agent.getId());
		
		locallyKnownAgents.registerAgent(agent);
				
		//TODO: compare agents for security check!
		
		PastPutContinuation conti = new PastPutContinuation ();
		pastStorage.insert( new ContentEnvelope ( agent ), conti);
		
		try {
			conti.waitForResult();
			if ( !conti.isSuccess() )
				throw new PastryStorageException ( "error storing update" );
			
			if ( agent instanceof UserAgent )
				updateUserAgentList((UserAgent) agent);
		} catch (InterruptedException e) {
			throw new PastryStorageException ( "interrupted", e );
		}
	}
	
	/**
	 * get the identifier of this node (string representation of the pastry node)
	 * 
	 * @return complete identifier of this pastry node as String
	 */
	public Serializable getNodeId () {
		if ( pastryNode == null)
			return null;
		else
			return pastryNode.getLocalNodeHandle();
	}


	@Override
	public Object[] getOtherKnownNodes() {
		return pastryNode.getLeafSet().getUniqueSet().toArray();
	}


	@Override
	public NodeInformation getNodeInformation(Object nodeId)
			throws NodeNotFoundException {
		
		if ( ! ( nodeId instanceof NodeHandle ))
			throw new NodeNotFoundException ( "given node id is not a pastry Node Handle!");
		
		return application.getNodeInformation ( (NodeHandle) nodeId );
	}


	@Override
	public void sendUnlockRequest(long agentId, String passphrase,
			Object targetNode, PublicKey nodeEncryptionKey)
			throws L2pSecurityException {
		try {
			application.unlockRemoteAgent(agentId, passphrase, (NodeHandle) targetNode, nodeEncryptionKey);
			observerNotice(Event.AGENT_UNLOCKED, null, null, targetNode, null, "Agent " + agentId + " unlocked at target node" );
		} catch ( L2pSecurityException e ) {
			observerNotice(Event.AGENT_UNLOCK_FAILED, null, null, targetNode, null, "Unlocking of Agent " + agentId + " failed!");
			throw e;
		}
	}
	
	
}
