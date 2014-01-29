package i5.las2peer.api;

import i5.las2peer.p2p.Node;


/**
 * Base class for connectors enabling the LAS2peer network to be accessed from the outside via non p2p protocols.  
 * 
 * @author Holger Jan&szlig;en
 *
 */
public abstract class Connector extends Configurable {

	
	/**
	 * Initialize the connector.
	 */
	public void init () {
	}
	
	/**
	 * Sets the port of a connector.
	 * 
	 * @param port
	 */
	public void setPort ( int port ) {
		
	}
	
	/**
	 * Start a connector at the given node.
	 * 
	 * @param node
	 */
	public abstract void start ( Node node ) throws ConnectorException ;
	
	
	/**
	 * Stops the connector.
	 * 
	 * @throws ConnectorException
	 */
	public abstract void stop () throws ConnectorException;
	

	
}
