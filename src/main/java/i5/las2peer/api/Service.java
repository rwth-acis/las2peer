package i5.las2peer.api;

import i5.las2peer.api.security.ServiceAgent;
import i5.las2peer.helper.Configurable;

/**
 * Base class for services to be hosted within the las2peer network.
 * 
 * All public methods of this service are available for RMI.
 * 
 * The service is instantiated once per node. Thus, no user related data should be stored in class members.
 * 
 * Access to las2peers service api is given via the {@link Context} interface.
 * 
 */
public abstract class Service extends Configurable {

	/**
	 * Used to determine, if this service should be monitored. Can be overwritten by service configuration file.
	 * Deactivated per default.
	 */
	private boolean monitor = false;

	/**
	 * The service agent responsible for this service.
	 */
	private ServiceAgent agent = null;

	/**
	 * Notifies the service, that it has been launched at the given node.
	 * 
	 * Simple startup hook that may be overwritten in subclasses.
	 * 
	 * @param agent The agent responsible for executing this service.
	 * @throws ServiceException Can be thrown if an error occurs. The service will not be advertised on running.
	 */
	public void onStart(ServiceAgent agent) throws ServiceException {
		this.agent = agent;
	}

	/**
	 * Notifies the service, that it has been stopped at this node.
	 * 
	 * Simple shutdown hook to be overwritten in subclasses.
	 */
	public void onStop() {

	}

	/**
	 * Should return the service alias, which is registered on service start.
	 * 
	 * @return the alias, or null if no alias should be registered
	 */
	public String getAlias() {
		return null;
	}

	/**
	 * Indicates whether monitoring is enabled for this service.
	 * 
	 * @return true if this service should be monitored
	 */
	public final boolean isMonitor() {
		return monitor;
	}

	/**
	 * Gets the agent corresponding to this service.
	 * 
	 * @return the agent responsible for this service
	 * @throws ServiceException If the service is not started yet.
	 */
	public final ServiceAgent getAgent() throws ServiceException {
		if (this.agent == null) {
			throw new ServiceException("This Service has not been started yet!");
		}
		return this.agent;
	}

}
