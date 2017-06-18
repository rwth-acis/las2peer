package i5.las2peer.connectors.nodeAdminConnector.handler;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.PastryNodeImpl;

public abstract class AbstractHandler {

	protected final L2pLogger logger = L2pLogger.getInstance(getClass());
	protected final NodeAdminConnector connector;
	protected final PastryNodeImpl node;

	protected AbstractHandler(NodeAdminConnector connector) {
		this.connector = connector;
		this.node = connector.getNode();
	}

	// FIXME test cross origin header
	// FIXME test OPTIONS requests

}
