package i5.las2peer.classLoaders;

import java.io.IOException;
import java.net.URL;

import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

/**
 * a LibraryClassLoader is responsible for loading classes from one library bundle, probably loaded via a
 * {@link i5.las2peer.classLoaders.libraries.Repository}
 * 
 * 
 *
 */
public class LibraryClassLoader extends ClassLoader {

	/**
	 * the library, this class loader is responsible for
	 */
	private LoadedLibrary myLibrary = null;

	/**
	 * parent class loader
	 */
	private BundleClassManager parent;

	/**
	 * create a new class loader for a given library.
	 *
	 * @param lib
	 * @param parent
	 */
	public LibraryClassLoader(LoadedLibrary lib, BundleClassManager parent) {
		this.myLibrary = lib;

		this.parent = parent;
	}

	/**
	 * get the binary contents of the given resource
	 * 
	 * @param resourceName
	 * @return contents of the resource
	 * @throws ClassLoaderException
	 * @throws IOException
	 */
	private byte[] getResourceContent(String resourceName) throws ClassLoaderException, IOException {
		return myLibrary.getResourceAsBinary(resourceName);
	}

	protected Class<?> findClass(String className) throws ClassNotFoundException {
		byte[] binaryDefinition;
		Logger.logFinding(this, className, null);

		try {
			binaryDefinition = getResourceContent(LoadedLibrary.classToResourceName(className));

			Logger.logFinding(this, className, true);

			return defineClass(className, binaryDefinition, 0, binaryDefinition.length);
		} catch (Exception e) {
			Logger.logFinding(this, className, false);
			throw new ClassNotFoundException("The class " + className + " could not be loaded by this classloader!", e);
		}
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass(name, resolve, true);
	}

	/**
	 * loading classes in a Library classloader follows the following steps: check if the class has already been loaded
	 * look into all bundles class loaders this library class loader has been registered to look into the bootstrap
	 * classloader alert that the class cannot be found
	 * 
	 * @param name
	 * @param resolve
	 * @param lookUp
	 * 
	 * @return class definition of the requested class
	 * 
	 * @throws ClassNotFoundException
	 */
	protected synchronized Class<?> loadClass(String name, boolean resolve, boolean lookUp) throws ClassNotFoundException {
		Logger.logLoading(this, name, null, lookUp);
		
		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);

		// ask parent loader
		if (c == null && lookUp && parent != null) {
			try {
				c = parent.loadClass(name, this);
			}
			catch (ClassNotFoundException e) {
			}
		}
		else if (c == null && lookUp && parent == null){ // for test cases
			try {
				c = getSystemClassLoader().loadClass(name);
			}
			catch (ClassNotFoundException e) {
			}
		}
		
		// then look in this library
		if (c == null) {
			try {
				c = findClass(name);
			} catch (ClassNotFoundException e) {
				// class not found in this Library
			}
		}

		// resolve
		// note that all classes need to be resolved here (even the ones loaded by another loader),
		// because of the order classes are found (Platform, Bundle, Library)
		if (resolve && c != null) {
			resolveClass(c);
		}

		if (c == null) {
			Logger.logLoading(this, name, false, lookUp);
			throw new ClassNotFoundException();
		}

		Logger.logLoading(this, name, true, lookUp);
		return c;
	}

	/**
	 * get the library, this loader is responsible for
	 * 
	 * @return library linked to this classloader
	 */
	public LoadedLibrary getLibrary() {
		return myLibrary;
	}

	/**
	 * get a resource from this class loader in form of an URL
	 * 
	 * this method is used to implement the actual {@link #getResource(String)} method to look up into all libraries of
	 * this bundle. To prevent endless loops, the corresponding bundle class loader will set the lookUp flag to false so
	 * that the search will stay local in this class loader.
	 * 
	 * @param resourceName
	 * @param lookUp
	 * @return the resource
	 */
	URL getResource(String resourceName, boolean lookUp) {
		try {
			return myLibrary.getResourceAsUrl(resourceName);
		} catch (ResourceNotFoundException e) {
			if (lookUp && parent != null) {
				URL result = parent.findResource(resourceName, this);
				if (result != null)
					return result;
				else
					return null;
			} else
				return null;
		}
	}

	/**
	 * get the URL for a resource
	 * @param resourceName 
	 * @return 
	 */
	@Override
	public URL getResource(String resourceName) {
		return getResource(resourceName, true);
	}

}
