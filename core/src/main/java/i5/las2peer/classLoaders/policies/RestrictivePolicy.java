package i5.las2peer.classLoaders.policies;

/**
 * Policy aimed for production use: Restricts access from the service to the node and some JDK methods.
 *
 */
public class RestrictivePolicy extends ClassLoaderPolicy {
	public RestrictivePolicy() {
		super();

		// Core
		allow("i5.las2peer.api"); // las2peer API
		
		// RESTMapper
		allow("i5.las2peer.restMapper"); // las2peer RESTMapper
		allow("javax.ws.rs"); // REST annotations
		allow("io.swagger.annotations"); // Swagger annotations
		// Jersey does not need to be added here, features in the las2peer bundle should be registered
		// in the RESTMapper class
		
		// JDK
		allow("java.lang");
		allow("java.util");
		allow("java.net");
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
