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
	 * method stub, may be overridden in implementing subclasses
	 */
	public void init () {
	}
	
	
	/**
	 * start a connector at the given node
	 * 
	 * @param node
	 */
	public abstract void start ( Node node ) throws ConnectorException ;
	
	
	/**
	 * stop the connector
	 * 
	 * @throws ConnectorException
	 */
	public abstract void stop  () throws ConnectorException;
	

	
}
