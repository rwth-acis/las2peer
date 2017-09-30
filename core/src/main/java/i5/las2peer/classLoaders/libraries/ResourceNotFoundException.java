package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.ClassLoaderException;

/**
 * Exception thrown on not found resources within a LoadedLibrary
 */
public class ResourceNotFoundException extends ClassLoaderException {

	private static final long serialVersionUID = -3067427615471129816L;

	/**
	 * create a new exception
	 * 
	 * @param resource A resource name
	 * @param library A library name
	 */
	protected ResourceNotFoundException(String resource, String library) {
		super("the resource '" + resource + "' could not be loaded from '" + library + "'");
	}

	/**
	 * create a new exception
	 * 
	 * @param resource A resource name
	 * @param library A library name
	 * @param cause A reason for this exception
	 */
	protected ResourceNotFoundException(String resource, String library, Throwable cause) {
		super("the resource '" + resource + "' could not be loaded from '" + library + "'", cause);
	}

}
