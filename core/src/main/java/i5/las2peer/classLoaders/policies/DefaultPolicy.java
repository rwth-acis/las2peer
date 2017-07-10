package i5.las2peer.classLoaders.policies;

/**
 * Default policies with no restrictions.
 *
 */
public class DefaultPolicy extends ClassLoaderPolicy {

	public DefaultPolicy() {
		super();
		
		// allow all packages
		allow("");
	}
	
}
