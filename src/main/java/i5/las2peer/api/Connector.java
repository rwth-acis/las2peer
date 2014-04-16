package i5.las2peer.api;

import i5.las2peer.p2p.Node;


/**
 * Base class for connectors enabling the LAS2peer network to be accessed from the outside.
 * 
 * @author Holger Jan&szlig;en
 * @author Peter de Lange
 *
 */
public abstract class Connector extends Configurable {

	
	/**
	 * Initialize the connector.
	 */
	public void init () {
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
