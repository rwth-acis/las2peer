package i5.las2peer.classLoaders;

import i5.las2peer.api.p2p.ServiceNameVersion;

/**
 * thrown by the {@link ClassManager} on access to a resource that is not registered
 * 
 * 
 *
 */
public class NotRegisteredException extends ClassLoaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2113971422558755065L;

	public NotRegisteredException(ServiceNameVersion identifier) {
		super("The service " + identifier.toString() + " is not registered!");
	}

}
