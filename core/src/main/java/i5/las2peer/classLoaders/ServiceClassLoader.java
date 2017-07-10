package i5.las2peer.classLoaders;

import java.io.IOException;
import java.net.URL;

import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;
import i5.las2peer.classLoaders.policies.ClassLoaderPolicy;

/**
 * A service class loader is responsible for loading classes from the service bundle, probably loaded via a
 * {@link i5.las2peer.classLoaders.libraries.Repository}
 * 
 * 
 *
 */
public class ServiceClassLoader extends ClassLoader {

	/**
	 * the library this class loader is responsible for
	 */
	private LoadedLibrary library = null;

	/**
	 * parent class manager
	 */
	private ClassLoader parent;
	
	/**
	 * The policy
	 */
	private ClassLoaderPolicy policy;

	/**
	 * create a new class loader for a given library.
	 *
	 * @param lib
	 * @param parent
	 * @param policy 
	 */
	public ServiceClassLoader(LoadedLibrary lib, ClassLoader parent, ClassLoaderPolicy policy) {
		this.library = lib;

		this.parent = parent;
		
		this.policy = policy;
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
		return library.getResourceAsBinary(resourceName);
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		byte[] binaryDefinition;
		Logger.logFinding(this, className, null);

		try {
			binaryDefinition = getResourceContent(LoadedLibrary.classToResourceName(className));

			Logger.logFinding(this, className, true);

			// define package
			// Don't know why this is neccessary, Java's docs are not sufficient.
			// Implementing custom class loaders is a mess...
			String packageName = className.substring(0, className.lastIndexOf('.'));
			if (getPackage(packageName) == null) {
				definePackage(packageName, null, null, null, null, null, null, null);
			}

			return defineClass(className, binaryDefinition, 0, binaryDefinition.length);
		} catch (Exception e) {
			Logger.logFinding(this, className, false);
			throw new ClassNotFoundException("The class " + className + " could not be loaded by this classloader!", e);
		}
	}

	/**
	 * loading classes in a Library classloader follows the following steps: check if the class has already been loaded
	 * look into all bundles class loaders this library class loader has been registered to look into the bootstrap
	 * classloader alert that the class cannot be found
	 * 
	 * @param name
	 * @param resolve
	 * @return class definition of the requested class
	 * @throws ClassNotFoundException
	 */
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Logger.logLoading(this, name, null);

		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);

		// ask parent loader
		if (this.policy.canLoad(name)) {
			if (c == null && parent != null) {
				try {
					c = parent.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			} else if (c == null && parent == null) { // for test cases
				try {
					c = getSystemClassLoader().loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		

		// then look in this library
		if (c == null) {
			try {
				c = findClass(name);
			} catch (ClassNotFoundException e) {
				// class not found in this Library
				// XXX logging
			}
		}

		// resolve
		// note that all classes need to be resolved here (even the ones loaded by another loader),
		// because of the order classes are found (Platform, Bundle, Library)
		if (resolve && c != null) {
			resolveClass(c);
		}

		if (c == null) {
			Logger.logLoading(this, name, false);
			throw new ClassNotFoundException();
		}

		Logger.logLoading(this, name, true);
		return c;
	}

	/**
	 * get the library this loader is responsible for
	 * 
	 * @return library linked to this classloader
	 */
	public LoadedLibrary getLibrary() {
		return library;
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
	 * @return Returns the resource
	 */
	URL getResource(String resourceName, boolean lookUp) {
		Logger.logGetResource(this, resourceName, null, lookUp);

		URL res;

		try {
			res = library.getResourceAsUrl(resourceName);
		} catch (ResourceNotFoundException e) {
			if (lookUp && parent != null) {
				URL result = parent.getResource(resourceName);
				if (result != null) {
					return result;
				} else {
					Logger.logGetResource(this, resourceName, false, null);
					return null;
				}

			} else {
				Logger.logGetResource(this, resourceName, false, null);
				return null;
			}
		}

		if (res != null) {
			Logger.logGetResource(this, resourceName, true, null);
			return res;
		} else {
			return null;
		}
	}

	/**
	 * get the URL for a resource
	 * 
	 * @param resourceName
	 * @return Returns the URL for the resource or {@code null}, if the resource was not found
	 */
	@Override
	public URL getResource(String resourceName) {
		return getResource(resourceName, true);
	}

}
