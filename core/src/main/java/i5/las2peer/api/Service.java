package i5.las2peer.api;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

import i5.las2peer.api.security.ServiceAgent;

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
	 * The service agent responsible for this service.
	 */
	private ServiceAgent agent = null;
	
	/**
	 * The logger for the service class.
	 */
	private Logger logger = null;

	/**
	 * Notifies the service that it has been launched at the given node.
	 * 
	 * Simple startup hook that may be overwritten in subclasses.
	 * 
	 * @throws ServiceException Can be thrown if an error occurs. The service will not be advertised to be running.
	 */
	public void onStart() throws ServiceException {
	}
	
	/**
	 * Notifies the service that it has been launched at the given node.
	 * 
	 * @param agent The agent responsible for executing this service.
	 * @param logger The logger for the service class.
	 * @throws ServiceException Can be thrown if an error occurs. The service will not be advertised to be running.
	 */
	public final void onStart(ServiceAgent agent, Logger logger) throws ServiceException {
		this.agent = agent;
		this.logger = logger;
		onStart();
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
	 * @return The alias, or null if no alias should be registered.
	 */
	public String getAlias() {
		return null;
	}

	/**
	 * Indicates whether monitoring is enabled for this service.
	 * 
	 * For self-deploying services, this will be always true since monitoring is set up on node level.
	 * 
	 * @return True if this service should be monitored.
	 */
	public final boolean isMonitor() {
		for (Annotation classAnnotation : this.getClass().getAnnotations()) {
			if (classAnnotation instanceof DoNotMonitor) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Indicates whether the service is self deployable (restricting some features) or not (unlimited access).
	 * 
	 * @return True if {@link ManualDeployment} is not set.
	 */
	public final boolean isSelfDeployable() {
		for (Annotation classAnnotation : this.getClass().getAnnotations()) {
			if (classAnnotation instanceof ManualDeployment) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets the fields of the service according to the configuration file.
	 * 
	 * Note that this feature is only available for services with self-deployment disabled.
	 * 
	 * @throws IllegalStateException If the service is not set up for manual deployment by annotating the service class
	 *             with {@link ManualDeployment}.
	 * 
	 */
	@Override
	protected final void setFieldValues() {
		if (isSelfDeployable()) {
			throw new IllegalStateException("Configuration files are not available for self-deploying services. "
					+ "To switch to manual deployment, add the @ManualDeployment to your class.");
		} else {
			super.setFieldValues();
		}
	}

	/**
	 * Gets the agent corresponding to this service.
	 * 
	 * @return The agent responsible for this service.
	 * @throws ServiceException If the service is not started yet.
	 */
	public final ServiceAgent getAgent() throws ServiceException {
		if (this.agent == null) {
			throw new ServiceException("This service has not been started yet!");
		}
		return this.agent;
	}
	
	/**
	 * Gets the logger for the service class.
	 * 
	 * @return The agent responsible for this service.
	 * @throws ServiceException If the service is not started yet.
	 */
	public final Logger getLogger() throws ServiceException {
		if (this.logger == null) {
			throw new ServiceException("This service has not been started yet!");
		}
		return this.logger;
	}

}
