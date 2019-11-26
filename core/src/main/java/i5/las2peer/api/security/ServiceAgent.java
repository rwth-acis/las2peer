package i5.las2peer.api.security;

import i5.las2peer.api.p2p.ServiceNameVersion;

/**
 * Represents an agent responsible for executing a service on a node.
 *
 */
public interface ServiceAgent extends PassphraseAgent {

	/**
	 * Get the service name and version of the service executed by this agent.
	 * 
	 * @return service name and sersion
	 */
	public ServiceNameVersion getServiceNameVersion();
}
