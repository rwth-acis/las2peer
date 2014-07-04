package i5.las2peer.classLoaders;

import i5.las2peer.classLoaders.helpers.LibraryIdentifier;


/**
 * thrown by the {@link L2pClassLoader} on access to a resource that is not registered
 * 
 * 
 *
 */
public class NotRegisteredException extends ClassLoaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2113971422558755065L;

	public NotRegisteredException(String packageName, String version) {
		super ( "the service/package " + packageName + " is not registered with version " + version );
	}

	public NotRegisteredException(LibraryIdentifier identifier) {
		super ( "the library " + identifier.toString() + " is not registered!");
	}

	
	
}
