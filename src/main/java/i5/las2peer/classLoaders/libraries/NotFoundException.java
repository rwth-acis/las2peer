package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.ClassLoaderException;

/**
 * exception thrown if a requested library cannot be found in a repository 
 * 
 * 
 *
 */
public class NotFoundException extends ClassLoaderException {

	private static final long serialVersionUID = 7494402597261214332L;

	/**
	 * create a new exception
	 * @param message
	 */
	NotFoundException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	NotFoundException ( String message, Throwable cause ) {
		super ( message, cause );
	}
	
}
