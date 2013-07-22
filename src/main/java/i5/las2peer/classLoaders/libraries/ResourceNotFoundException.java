package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.ClassLoaderException;


/**
 * exception thrown on not found resources within a LoadedLibrary
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class ResourceNotFoundException extends ClassLoaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3067427615471129816L;

	/**
	 * create a new exception
	 * 
	 * @param resource
	 * @param library
	 */
	protected ResourceNotFoundException(String resource, String library) {
		super("the resource '" + resource + "' could not be loaded from '" + library + "'");
	}
	
	
	/**
	 * create a new exception
	 * 
	 * @param resource
	 * @param library
	 * @param cause
	 */
	protected ResourceNotFoundException(String resource, String library, Throwable cause) {
		super("the resource '" + resource + "' could not be loaded from '" + library + "'", cause);
	}

}
