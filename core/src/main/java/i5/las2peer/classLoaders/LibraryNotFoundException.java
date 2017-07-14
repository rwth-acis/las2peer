package i5.las2peer.classLoaders;

/**
 * exception thrown if a requested library cannot be found in a repository
 * 
 */
public class LibraryNotFoundException extends ClassLoaderException {

	private static final long serialVersionUID = 4258813635392321654L;

	/**
	 * create a new exception
	 * 
	 * @param message
	 */
	public LibraryNotFoundException(String message) {
		super(message);
	}

	/**
	 * create a new exception
	 * 
	 * @param cause
	 */
	public LibraryNotFoundException(Throwable cause) {
		super(cause);
	}

	/**
	 * create a new exception
	 * 
	 * @param message
	 * @param cause
	 */
	public LibraryNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
