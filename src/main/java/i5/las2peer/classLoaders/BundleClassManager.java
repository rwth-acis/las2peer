package i5.las2peer.classLoaders;

import java.net.URL;

import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

/**
 * A BundleClassLoader is responsible of providing all classes for a main class / service via separate libraries.
 * 
 * 
 *
 */
public class BundleClassManager {

	/**
	 * A class loader where to find classes if no libraryLoader has the class
	 */
	private ClassLoader parent;

	/**
	 * a list of libraries
	 */
	private LibraryClassLoader[] libraryLoaders = new LibraryClassLoader[0];

	/**
	 * create a Bundle
	 * 
	 * @param parent the fallback class loader
	 */
	public BundleClassManager(ClassLoader parent) {
		this.parent = parent;
	}

	/**
	 * init library loader with a list
	 * 
	 * Attention: overwrites existing loaders, only for initialization!
	 * 
	 * @param loaders
	 */
	public void initLibraryLoaders(LibraryClassLoader[] loaders) {
		this.libraryLoaders = loaders;
	}

	/**
	 * 
	 * @return identifier of the main library of this bundle class loader
	 */
	public LibraryIdentifier getMainLibraryIdentifier() {
		return libraryLoaders[0].getLibrary().getLibraryIdentifier();
	}

	/**
	 * get an array with all identifiers for the libraries registered to this BundleClassLoader
	 * 
	 * @return array with the identifiers of all registered libraries
	 */
	public LibraryIdentifier[] getLibraryIdentifiers() {
		LibraryIdentifier[] result = new LibraryIdentifier[libraryLoaders.length];

		for (int i = 0; i < result.length; i++)
			result[i] = libraryLoaders[i].getLibrary().getLibraryIdentifier();

		return result;
	}

	public URL getResource(String resourceName) {
		// just delegate the resource loading to the first registered LibraryClassLoader
		// via {@link findResource} all other child loaders will be asked as well
		return libraryLoaders[0].getResource(resourceName);
	}

	/**
	 * Method to actually find a resource. Currently, a class using this class loader may only get resources from its
	 * own jar.
	 *
	 * @param resourceName
	 * @param calledFromChild
	 * 
	 * @return an URL
	 */
	public URL findResource(String resourceName, LibraryClassLoader calledFromChild) {
		Logger.logGetResource(this, resourceName, null, null);

		for (int i = 0; i < libraryLoaders.length; i++) {
			if (libraryLoaders[i] != calledFromChild) {
				try {
					return libraryLoaders[i].getResource(resourceName, false);
				} catch (Exception e) {
					System.err.println("some other than " + ResourceNotFoundException.class.getName() + ": " + e);
				}
			}
		}

		return parent.getResource(resourceName);
	}

	/**
	 * Load a bundle class from the outside. The class will be resolved.
	 * 
	 * Used to load the main service class.
	 * 
	 * @param className
	 * @return
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		Logger.logLoading(this, className, null, null);

		try {
			Class<?> result = libraryLoaders[0].loadClass(className, false);

			Logger.logLoading(this, className, true, null);
			return result;
		} catch (ClassNotFoundException e) {
			Logger.logLoading(this, className, false, null);
			throw e;
		}
	}

	/**
	 * Will be called by a child LibraryClassLoader if a class cannot be found there.
	 * 
	 * The parameter for the child library is necessary to prevent loops of loadClass calls.
	 * 
	 * @param className
	 * @param child
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 */
	protected Class<?> loadClass(String className, LibraryClassLoader child) throws ClassNotFoundException {
		Logger.logLoading(this, className, null,
				"by child " + child.getLibrary().getIdentifier() + " - try " + libraryLoaders.length + " children");

		// ask platform loader first
		if (parent != null) {
			try {
				return parent.loadClass(className);
			} catch (ClassNotFoundException e) {
			}
		}

		// then ask child bundles
		Class<?> result = null;
		for (int i = 0; i < libraryLoaders.length; i++) {
			if (libraryLoaders[i] != child) {
				Logger.logLoading(this, className, null, "by child " + child.getLibrary().getIdentifier()
						+ " - try child " + i + libraryLoaders[i].getLibrary().getIdentifier());

				try {
					result = libraryLoaders[i].loadClass(className, false, false);
					if (result != null) {
						Logger.logLoading(this, className, true, "by child" + child.getLibrary().getIdentifier());
						return result;
					}
				} catch (ClassNotFoundException e) {
					// just try the next one!
				}
			}
		}

		Logger.logLoading(this, className, false, "by child " + child.getLibrary().getIdentifier());
		throw new ClassNotFoundException();
	}

	/**
	 * get an array with all registered LibraryLoadres
	 * 
	 * @return an array with all registered library loaders
	 */
	public LibraryClassLoader[] getLibraryLoaders() {
		return libraryLoaders.clone();
	}

}
