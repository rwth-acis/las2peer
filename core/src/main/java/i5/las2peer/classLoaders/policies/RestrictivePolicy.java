package i5.las2peer.classLoaders.policies;

/**
 * Policy aimed for production use: Restricts access from the service to the node and some JDK methods.
 *
 */
public class RestrictivePolicy extends ClassLoaderPolicy {
	public RestrictivePolicy() {
		super();
		
		allow("i5.las2peer.api");
		allow("java.lang");
		allow("java.util");
		deny("java.lang.System");
		deny("java.lang.Runtime");
		deny("java.lang.Thread");
		deny("java.lang.Process");
		deny("java.lang.ProcessBuilder");
		deny("java.lang.SecurityManager");
		deny("java.lang.instrument");
		deny("java.lang.management");
		deny("java.util.concurrent.ThreadPoolExecutor");
	}
}
