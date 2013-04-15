package i5.las2peer.classLoaders;

public class LibraryNotFoundException extends ClassLoaderException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4258813635392321654L;

	LibraryNotFoundException(String library) {
		super("library '" + library + "' package could not be found in the repositories!");
	}

	LibraryNotFoundException(String library, Throwable cause) {
		super("library '" + library + "' package could not be found in the repositories!", cause);
	}

}
