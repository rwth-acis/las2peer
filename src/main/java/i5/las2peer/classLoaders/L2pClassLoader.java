package i5.las2peer.classLoaders;

import java.util.Enumeration;
import java.util.Hashtable;

import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.NotFoundException;
import i5.las2peer.classLoaders.libraries.Repository;

/**
 * The main class for loading classes in the las2peer environment. This ClassLoader handles library registering and
 * resolving (i.e. loading) dependencies as well as hotplugging.
 * 
 * The main idea is to keep each service separate but load each needed library only once.
 * 
 * 
 *
 */
public class L2pClassLoader extends ClassLoader {

	/**
	 * repositories to load libraries from
	 */
	private Repository[] repositories;

	/**
	 * all registered main bundles (i.e. services) (name, version) => BundleClassLoader
	 */
	private Hashtable<String, Hashtable<LibraryVersion, BundleClassLoader>> registeredLoaders = new Hashtable<String, Hashtable<LibraryVersion, BundleClassLoader>>();

	/**
	 * all registered libraries name, version => LibraryClassLoader
	 */
	private Hashtable<String, Hashtable<LibraryVersion, LibraryClassLoader>> registeredLibraryClassLoaders = new Hashtable<String, Hashtable<LibraryVersion, LibraryClassLoader>>();

	/**
	 * create a new L2pClassLoader, which uses the given repository
	 * 
	 * @param repository
	 * @param parentLoader
	 */
	public L2pClassLoader(Repository repository, ClassLoader parentLoader) {
		this(new Repository[] { repository }, parentLoader);
	}

	/**
	 * create a new L2PClassLoader, which uses the given repositories
	 * 
	 * @param repositories
	 * @param parentLoader
	 */
	public L2pClassLoader(Repository[] repositories, ClassLoader parentLoader) {
		super(parentLoader);

		this.repositories = repositories;
	}

	/**
	 * Register a service and get all needed libraries from the used repository/ies. The package name of the service
	 * will be used as library name.
	 * 
	 * @param serviceClassName
	 * @throws ClassLoaderException
	 */
	public void registerService(String serviceClassName) throws ClassLoaderException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassLoader> htBcls = registeredLoaders.get(sPackage);
		if (htBcls != null && htBcls.size() > 0)
			return; // at least one version is already registered

		LoadedLibrary lib = findLoadedLibrary(sPackage);
		if (!lib.getIdentifier().getName().equals(sPackage))
			throw new ClassLoaderException("Name mismatch: package library '" + sPackage + "' provides package '"
					+ lib.getIdentifier().getName() + "'!");

		registerLibrary(lib);
	}

	/**
	 * register a library from the repository as a BundleClassLoader
	 * 
	 * @param lib
	 * @throws UnresolvedDependenciesException
	 */
	private void registerLibrary(LoadedLibrary lib) throws UnresolvedDependenciesException {
		LibraryClassLoader[] resolvedDeps = resolveDependencies(lib);

		BundleClassLoader bcl = new BundleClassLoader(resolvedDeps);

		Hashtable<LibraryVersion, BundleClassLoader> htBcls = registeredLoaders.get(lib.getIdentifier().getName());
		if (htBcls == null) {
			htBcls = new Hashtable<LibraryVersion, BundleClassLoader>();
			registeredLoaders.put(lib.getIdentifier().getName(), htBcls);
		}

		htBcls.put(lib.getLibraryIdentifier().getVersion(), bcl);
	}

	/**
	 * resolve all library dependencies stated in the libraries manifest
	 * 
	 * the resulting array contains all class loaders necessary to handle the given library (including one for the
	 * library itself)
	 * 
	 * @param library
	 * @return array with library class loaders
	 * @throws UnresolvedDependenciesException
	 */
	private LibraryClassLoader[] resolveDependencies(LoadedLibrary library) throws UnresolvedDependenciesException {
		LibraryDependency[] deps = library.getDependencies();

		// TODO: merge dependencies recursively? - Probably not, since this would
		// require prefetching of possibly not needed jars!
		// which is not a good idea for the intended p2p environment

		LoadedLibrary[] libraries = new LoadedLibrary[deps.length + 1];
		libraries[0] = library;
		for (int i = 0; i < deps.length; i++)
			try {
				libraries[i + 1] = findLoadedLibrary(deps[i]);
			} catch (LibraryNotFoundException e) {
				throw new UnresolvedDependenciesException(library.getIdentifier().toString(), deps[i]);
			}

		// ok, now i'm sure that all dependencies can be resolved!
		LibraryClassLoader[] result = new LibraryClassLoader[libraries.length];
		for (int i = 0; i < libraries.length; i++) {
			result[i] = getLibraryClassLoader(libraries[i]);
			if (result[i] == null)
				result[i] = registerLoadedLibrary(libraries[i]);
		}

		return result;
	}

	/**
	 * check, if the given library is already registered
	 * 
	 * @param library
	 * @return true, if the given library is loaded an registered here
	 */
	/*private boolean isRegistered ( LoadedLibrary library ) {
		return getLibraryClassLoader( library ) != null;
	}
	
	/**
	 * get a library class loader for the given loaded library
	 * @param lib
	 * @return the library class loader corresponding to the given library
	 */
	private LibraryClassLoader getLibraryClassLoader(LoadedLibrary lib) {
		return getLibraryClassLoader(lib.getIdentifier());
	}

	/**
	 * get a library class loader for the library given by the stated identifier
	 * 
	 * @param iden
	 * @return the library class loader corresponding to the given library
	 */
	private LibraryClassLoader getLibraryClassLoader(LibraryIdentifier iden) {
		Hashtable<LibraryVersion, LibraryClassLoader> htLoaders = registeredLibraryClassLoaders.get(iden.getName());
		if (htLoaders == null)
			return null;
		else
			return htLoaders.get(iden.getVersion());
	}

	/**
	 * register a loaded library as a LibraryClassLoader
	 * 
	 * @param lib
	 * @return a class loader for the given library
	 */
	private LibraryClassLoader registerLoadedLibrary(LoadedLibrary lib) {
		LibraryClassLoader loader = new LibraryClassLoader(lib);
		Hashtable<LibraryVersion, LibraryClassLoader> htLoaders = registeredLibraryClassLoaders
				.get(lib.getIdentifier().getName());
		if (htLoaders == null) {
			htLoaders = new Hashtable<LibraryVersion, LibraryClassLoader>();
			registeredLibraryClassLoaders.put(lib.getIdentifier().getName(), htLoaders);
		}
		htLoaders.put(lib.getIdentifier().getVersion(), loader);

		return loader;
	}

	/**
	 * get the newest loaded library for the given (package) name
	 * 
	 * @param name
	 * 
	 * @return a library representation for the given package name
	 * 
	 * @throws LibraryNotFoundException library could not be found in the repositories
	 * @throws UnresolvedDependenciesException problems resolving the dependency information stated in the libraries
	 *             manifest
	 */
	private LoadedLibrary findLoadedLibrary(String name)
			throws LibraryNotFoundException, UnresolvedDependenciesException {
		// TODO: tidy up - allow missing version info in dependency!

		// System.err.println( "looking for " + name + " in " + repositories.length + " repos");

		LoadedLibrary result = null;

		UnresolvedDependenciesException ude = null;

		for (int i = 0; i < repositories.length; i++) {
			try {
				LoadedLibrary temp = repositories[i].findLibrary(name);

				if (result == null
						|| temp.getLibraryIdentifier().getVersion().isLargerThan(result.getIdentifier().getVersion()))
					result = temp;
			} catch (NotFoundException e) {
			} catch (UnresolvedDependenciesException e) {
				if (ude == null)
					ude = e;
			}
		}

		if (result == null) {
			if (ude != null)
				throw ude;
			else
				throw new LibraryNotFoundException(name);
		}

		return result;
	}

	/**
	 * get a loaded library for the given name and version
	 * 
	 * @param name
	 * @param version
	 * 
	 * @return a library representation for the given library name (and version)
	 * 
	 * @throws LibraryNotFoundException
	 */
	/*
	private LoadedLibrary findLoadedLibrary ( String name, LibraryVersion version ) throws LibraryNotFoundException {
		return findLoadedLibrary ( new LibraryDependency ( name, version ));
	}
	*/

	/**
	 * find a loaded library matching the given dependency
	 * 
	 * @param dep
	 * 
	 * @return a library representation fitting the given dependency
	 * 
	 * @throws LibraryNotFoundException
	 */
	private LoadedLibrary findLoadedLibrary(LibraryDependency dep) throws LibraryNotFoundException {
		LoadedLibrary result = null;
		for (int i = 0; i < repositories.length; i++) {
			try {
				LoadedLibrary temp = repositories[i].findMatchingLibrary(dep);
				if (result == null
						|| temp.getLibraryIdentifier().getVersion().isLargerThan(result.getIdentifier().getVersion()))
					result = temp;
			} catch (NotFoundException e) {
			}
		}

		if (result == null)
			throw new LibraryNotFoundException(dep.getName());

		return result;
	}

	/**
	 * register a service using the given library version the package name of the service class is used a package name
	 * 
	 * @param serviceClassName
	 * @param version
	 * @throws ClassLoaderException
	 */
	public void registerService(String serviceClassName, LibraryVersion version) throws ClassLoaderException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassLoader> htBcls = registeredLoaders.get(sPackage);
		if (htBcls != null && htBcls.size() > 0)
			return; // at least one version is already registered

		LoadedLibrary lib = null;
		for (int i = 0; i < repositories.length && lib == null; i++) {
			try {
				lib = repositories[i].findLibrary(new LibraryIdentifier(sPackage, version));
			} catch (NotFoundException e) {
			}
		}

		if (lib == null)
			throw new LibraryNotFoundException(sPackage);
		if (!lib.getIdentifier().getName().equals(sPackage))
			throw new ClassLoaderException("Name mismatch: package library '" + sPackage + "' provides package '"
					+ lib.getIdentifier().getName() + "'!");

		registerLibrary(lib);
	}

	/**
	 * remove a service with all versions from this class loader
	 * 
	 * @param serviceName
	 */
	// public void unregisterService ( String serviceClassName ) {
	// }

	/**
	 * remove a service (bundle) from this class loader and remove all orphaned LibraryClassLoaders as well
	 * 
	 * @param serviceClassName
	 * @param version
	 * @throws NotRegisteredException
	 */
	public void unregisterService(String serviceClassName, String version) throws NotRegisteredException {
		String pkg = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassLoader> htVersions = registeredLoaders.get(pkg);
		if (htVersions == null)
			throw new NotRegisteredException(pkg, version);

		BundleClassLoader bcl = htVersions.get(new LibraryVersion(version));
		if (bcl == null)
			throw new NotRegisteredException(pkg, version);

		htVersions.remove(version);
		if (htVersions.size() == 0)
			registeredLoaders.remove(pkg);

		LibraryClassLoader[] subloaders = bcl.getLibraryLoaders();
		for (int i = 0; i < subloaders.length; i++) {
			subloaders[i].unregisterParentLopader(bcl);
			if (!subloaders[i].hasParentLoaders())
				removeLibraryLoader(subloaders[i]);
		}
	}

	/**
	 * remove a LibraryClassLoader from the registry
	 * 
	 * @param l
	 * @throws NotRegisteredException
	 */
	private void removeLibraryLoader(LibraryClassLoader l) throws NotRegisteredException {
		Hashtable<LibraryVersion, LibraryClassLoader> htVersions = registeredLibraryClassLoaders
				.get(l.getLibrary().getIdentifier().getName());
		if (htVersions == null)
			throw new NotRegisteredException(l.getLibrary().getIdentifier());

		htVersions.remove(l.getLibrary().getIdentifier().getVersion());
		if (htVersions.size() == 0)
			registeredLibraryClassLoaders.remove(l.getLibrary().getIdentifier().getName());
	}

	/**
	 * get the newest available version of the given service
	 * 
	 * a service has to be provided in a bundle of the package containing the service
	 * 
	 * @param serviceClassName
	 * 
	 * @return class definition of the requested service
	 * 
	 * @throws ClassLoaderException
	 * @throws ClassNotFoundException
	 */
	public Class<?> getServiceClass(String serviceClassName) throws ClassLoaderException, ClassNotFoundException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassLoader> htVersions = registeredLoaders.get(sPackage);
		if (htVersions == null || htVersions.size() == 0) {
			registerService(serviceClassName);
			htVersions = registeredLoaders.get(sPackage);
		}

		LibraryVersion version = null;
		for (Enumeration<LibraryVersion> en = htVersions.keys(); en.hasMoreElements();) {
			LibraryVersion v = en.nextElement();
			if (version == null || v.isLargerThan(version))
				version = v;
		}

		BundleClassLoader bcl = htVersions.get(version);

		try {
			return bcl.loadClass(serviceClassName);
		} catch (ClassNotFoundException e) {
			throw new LibraryNotFoundException(
					"The library for " + serviceClassName + " could be loaded, but the class is not available!", e);
		}
	}

	/**
	 * get a service class in the specified version
	 * 
	 * a service has to be provided in a bundle of the package containing the service
	 * 
	 * @param serviceClassName
	 * @param version
	 * 
	 * @return a class definition of the requested service
	 * 
	 * @throws ClassLoaderException
	 */
	public Class<?> getServiceClass(String serviceClassName, LibraryVersion version) throws ClassLoaderException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassLoader> htVersions = registeredLoaders.get(sPackage);
		if (htVersions == null || htVersions.get(version) == null) {
			registerService(serviceClassName, version);
			htVersions = registeredLoaders.get(sPackage);
		}

		BundleClassLoader bcl = htVersions.get(version);

		try {
			return bcl.loadClass(serviceClassName);
		} catch (ClassNotFoundException e) {
			throw new LibraryNotFoundException(
					"The library for " + serviceClassName + " could be loaded, but the class is not available!", e);
		}
	}

	/**
	 * get a service class
	 * 
	 * a service has to be provided in a bundle of the package containing the service
	 * 
	 * @param serviceClassName name of the requested service
	 * @param version string specifying the requested version of the service, resp. its library
	 * 
	 * @return class definition of the requested service
	 * 
	 * @throws IllegalArgumentException
	 * @throws ClassLoaderException
	 */
	public Class<?> getServiceClass(String serviceClassName, String version)
			throws IllegalArgumentException, ClassLoaderException {
		return getServiceClass(serviceClassName, new LibraryVersion(version));
	}

	/**
	 * statistics: get the number of loaded (registered) jar libraries
	 * 
	 * @return the number of the registered libraries
	 */
	int numberOfRegisteredLibraries() {
		int result = 0;
		for (Enumeration<String> names = registeredLibraryClassLoaders.keys(); names.hasMoreElements();) {
			String name = names.nextElement();
			result += registeredLibraryClassLoaders.get(name).keySet().size();
		}

		return result;
	}

	/**
	 * statistics: get the number of loaded (registered) (service) bundles
	 * 
	 * @return the number of registered bundles
	 */
	int numberOfRegisteredBundles() {
		int result = 0;

		for (Enumeration<String> names = registeredLoaders.keys(); names.hasMoreElements();) {
			String name = names.nextElement();
			result += registeredLoaders.get(name).keySet().size();
		}

		return result;
	}

	/**
	 * extract the package name from a class name
	 * 
	 * @param className
	 * @return the package name of a complete class name
	 */
	public static final String getPackageName(String className) {
		if (className.indexOf('.') < 0)
			throw new IllegalArgumentException("this class is not contained in a package!");

		return className.substring(0, className.lastIndexOf('.'));
	}

}
