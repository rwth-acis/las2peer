package i5.las2peer.classLoaders;

import i5.las2peer.classLoaders.helpers.LibraryIdentifier;


/**
 * thrown by the {@link L2pClassLoader} on access to a resource that is not registered
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2012/09/26 10:38:45 $
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
