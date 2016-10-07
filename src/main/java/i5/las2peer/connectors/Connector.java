package i5.las2peer.connectors;

import i5.las2peer.helper.Configurable;
import i5.las2peer.p2p.Node;

/**
 * Base class for connectors enabling the las2peer network to be accessed from the outside. Basically, a connector only
 * has to implement a start and a stop method that will be called by the used launcher.
 *
 */
public abstract class Connector extends Configurable {

	/**
	 * 
	 * A connector has to have a mandatory reference to the node it is running at.
	 * 
	 */
	Node runningAt;

	/**
	 * Start a connector at the given node.
	 * 
	 * @param runningAt A parent node to start this connector instance at.
	 * @throws ConnectorException If an error occurs.
	 */
	public abstract void start(Node runningAt) throws ConnectorException;

	/**
	 * Stops the connector.
	 * 
	 * @throws ConnectorException If an error occurs.
	 */
	public abstract void stop() throws ConnectorException;

}
