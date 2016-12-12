package i5.las2peer.classLoaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
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
public class L2pClassManager {

	/**
	 * classloader to load the platform's classes (Java + las2peer core + Connectors)
	 */
	private ClassLoader platformLoader;

	/**
	 * repositories to load libraries from
	 */
	private ArrayList<Repository> repositories;

	/**
	 * all registered main bundles (i.e. services) (name, version) => BundleClassLoader
	 */
	private Hashtable<String, Hashtable<LibraryVersion, BundleClassManager>> registeredLoaders = new Hashtable<String, Hashtable<LibraryVersion, BundleClassManager>>();

	/**
	 * all registered libraries name, version => LoadedLibraryCache
	 */
	private Hashtable<String, Hashtable<LibraryVersion, LoadedLibraryCache>> registeredLibraries = new Hashtable<String, Hashtable<LibraryVersion, LoadedLibraryCache>>();

	/**
	 * create a new L2pClassLoader, which uses the given repository
	 * 
	 * @param repository
	 * @param platformLoader
	 */
	public L2pClassManager(Repository repository, ClassLoader platformLoader) {
		this(new Repository[] { repository }, platformLoader);
	}

	/**
	 * create a new L2PClassLoader, which uses the given repositories
	 * 
	 * @param repositories
	 * @param platformLoader
	 */
	public L2pClassManager(Repository[] repositories, ClassLoader platformLoader) {
		this.platformLoader = platformLoader;

		this.repositories = new ArrayList<>(Arrays.asList(repositories));
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

		Hashtable<LibraryVersion, BundleClassManager> htBcls = registeredLoaders.get(sPackage);
		if (htBcls != null && htBcls.size() > 0) {
			return; // at least one version is already registered
		}

		LoadedLibrary lib = findLoadedLibrary(sPackage);
		if (!lib.getIdentifier().getName().equals(sPackage)) {
			throw new ClassLoaderException("Name mismatch: package library '" + sPackage + "' provides package '"
					+ lib.getIdentifier().getName() + "'!");
		}

		registerBundle(lib);
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

		Hashtable<LibraryVersion, BundleClassManager> htBcls = registeredLoaders.get(sPackage);
		if (htBcls != null && htBcls.get(version) != null) {
			return;
		}

		LoadedLibrary lib = null;
		for (int i = 0; i < repositories.size() && lib == null; i++) {
			try {
				lib = repositories.get(i).findLibrary(new LibraryIdentifier(sPackage, version));
			} catch (LibraryNotFoundException e) {
			}
		}

		if (lib == null) {
			throw new LibraryNotFoundException(sPackage);
		}
		if (!lib.getIdentifier().getName().equals(sPackage)) {
			throw new ClassLoaderException("Name mismatch: package library '" + sPackage + "' provides package '"
					+ lib.getIdentifier().getName() + "'!");
		}

		registerBundle(lib);
	}

	/**
	 * remove a service (bundle) from this class loader and remove all orphaned LibraryClassLoaders as well
	 * 
	 * @param serviceClassName
	 * @param version
	 * @throws NotRegisteredException
	 */
	public void unregisterService(String serviceClassName, String version) throws NotRegisteredException {
		unregisterBundle(serviceClassName, version);
	}

	/**
	 * create a bundle for the given library
	 * 
	 * @param lib
	 * @throws UnresolvedDependenciesException
	 */
	private void registerBundle(LoadedLibrary lib) throws UnresolvedDependenciesException {
		LoadedLibraryCache[] resolvedDeps = resolveDependencies(lib);

		// create bundle
		BundleClassManager bcl = new BundleClassManager(platformLoader);

		// create LibraryClassLoaders
		LibraryClassLoader[] classLoaderDeps = new LibraryClassLoader[resolvedDeps.length];
		for (int i = 0; i < resolvedDeps.length; i++) {
			resolvedDeps[i].registerBundle(bcl);
			classLoaderDeps[i] = new LibraryClassLoader(resolvedDeps[i], bcl);
		}

		// init bundle
		bcl.initLibraryLoaders(classLoaderDeps);

		// register bundle
		Hashtable<LibraryVersion, BundleClassManager> htBcls = registeredLoaders.get(lib.getIdentifier().getName());
		if (htBcls == null) {
			htBcls = new Hashtable<LibraryVersion, BundleClassManager>();
			registeredLoaders.put(lib.getIdentifier().getName(), htBcls);
		}
		htBcls.put(lib.getLibraryIdentifier().getVersion(), bcl);
	}

	/**
	 * unregister a bundle
	 * 
	 * @param libraryName
	 * @param libraryVersion
	 * @throws NotRegisteredException
	 */
	private void unregisterBundle(String libraryName, String libraryVersion) throws NotRegisteredException {
		String pkg = getPackageName(libraryName);

		Hashtable<LibraryVersion, BundleClassManager> htVersions = registeredLoaders.get(pkg);
		if (htVersions == null) {
			throw new NotRegisteredException(pkg, libraryVersion);
		}

		BundleClassManager bcl = htVersions.get(new LibraryVersion(libraryVersion));
		if (bcl == null) {
			throw new NotRegisteredException(pkg, libraryVersion);
		}

		htVersions.remove(libraryVersion);
		if (htVersions.size() == 0) {
			registeredLoaders.remove(pkg);
		}

		LibraryClassLoader[] subloaders = bcl.getLibraryLoaders();
		for (LibraryClassLoader subloader : subloaders) {
			LoadedLibraryCache cache = getRegisteredLoadedLibrary(subloader.getLibrary());
			cache.unregisterBundle(bcl);
			if (!cache.isUsed()) {
				removeLoadedLibrary(cache.getLoadedLibrary());
			}
		}
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
	private LoadedLibraryCache[] resolveDependencies(LoadedLibrary library) throws UnresolvedDependenciesException {
		LibraryDependency[] deps = library.getDependencies();

		// TODO merge dependencies recursively OR implement lazy loading

		LoadedLibrary[] libraries = new LoadedLibrary[deps.length + 1];
		libraries[0] = library;
		for (int i = 0; i < deps.length; i++) {
			try {
				libraries[i + 1] = findLoadedLibrary(deps[i]);
			} catch (LibraryNotFoundException e) {
				throw new UnresolvedDependenciesException(library.getIdentifier().toString(), deps[i]);
			}
		}

		// ok, now i'm sure that all dependencies can be resolved!
		LoadedLibraryCache[] result = new LoadedLibraryCache[libraries.length];
		for (int i = 0; i < libraries.length; i++) {
			result[i] = getRegisteredLoadedLibrary(libraries[i]);
			if (result[i] == null) {
				result[i] = registerLoadedLibrary(libraries[i]);
			}
		}

		return result;
	}

	/**
	 * get the newest loaded library for the given (package) name
	 * 
	 * @param name
	 * @return a library representation for the given package name
	 * @throws LibraryNotFoundException library could not be found in the repositories
	 * @throws UnresolvedDependenciesException problems resolving the dependency information stated in the libraries
	 *             manifest
	 */
	private LoadedLibrary findLoadedLibrary(String name)
			throws LibraryNotFoundException, UnresolvedDependenciesException {
		// TODO versions of services - allow missing version info in dependency!
		LoadedLibrary result = null;
		StringBuilder sb = new StringBuilder();
		for (Repository repository : repositories) {
			try {
				LoadedLibrary temp = repository.findLibrary(name);
				if (result == null
						|| temp.getLibraryIdentifier().getVersion().isLargerThan(result.getIdentifier().getVersion())) {
					result = temp;
				}
			} catch (LibraryNotFoundException e) {
				// library not found in this repository, no problem we (may) have others
			} catch (UnresolvedDependenciesException e) {
				if (sb.length() != 0) {
					sb.append(", ");
				}
				sb.append(e.getMessage());
			}
		}
		if (result == null) {
			if (sb.length() != 0) {
				// the library was found at least in one repository, but dependencies could not be resolved
				throw new UnresolvedDependenciesException("Dependency for library '" + name
						+ "' could not be resolved in any repository! Details: " + sb.toString());
			} else {
				throw new LibraryNotFoundException(name);
			}
		}
		return result;
	}

	/**
	 * find a loaded library matching the given dependency
	 * 
	 * @param dep
	 * @return a library representation fitting the given dependency
	 * @throws LibraryNotFoundException
	 */
	private LoadedLibrary findLoadedLibrary(LibraryDependency dep) throws LibraryNotFoundException {
		LoadedLibrary result = null;
		for (int i = 0; i < repositories.size(); i++) {
			try {
				LoadedLibrary temp = repositories.get(i).findMatchingLibrary(dep);
				if (result == null
						|| temp.getLibraryIdentifier().getVersion().isLargerThan(result.getIdentifier().getVersion())) {
					result = temp;
				}
			} catch (LibraryNotFoundException e) {
			}
		}

		if (result == null) {
			throw new LibraryNotFoundException(dep.getName());
		}

		return result;
	}

	/**
	 * get a library class loader for the given loaded library
	 * 
	 * @param lib
	 * @return the library class loader corresponding to the given library
	 */
	private LoadedLibraryCache getRegisteredLoadedLibrary(LoadedLibrary lib) {
		return getRegisteredLoadedLibrary(lib.getIdentifier());
	}

	/**
	 * get a library class loader for the library given by the stated identifier
	 * 
	 * @param iden
	 * @return the library class loader corresponding to the given library
	 */
	private LoadedLibraryCache getRegisteredLoadedLibrary(LibraryIdentifier iden) {
		Hashtable<LibraryVersion, LoadedLibraryCache> htLoaders = registeredLibraries.get(iden.getName());
		if (htLoaders == null) {
			return null;
		} else {
			return htLoaders.get(iden.getVersion());
		}
	}

	/**
	 * register a loaded library as a LibraryClassLoader
	 * 
	 * @param lib
	 * @return a class loader for the given library
	 */
	private LoadedLibraryCache registerLoadedLibrary(LoadedLibrary lib) {
		LoadedLibraryCache loader = new LoadedLibraryCache(lib);
		Hashtable<LibraryVersion, LoadedLibraryCache> htLoaders = registeredLibraries
				.get(lib.getIdentifier().getName());
		if (htLoaders == null) {
			htLoaders = new Hashtable<LibraryVersion, LoadedLibraryCache>();
			registeredLibraries.put(lib.getIdentifier().getName(), htLoaders);
		}
		htLoaders.put(lib.getIdentifier().getVersion(), loader);

		return loader;
	}

	/**
	 * remove a LoadedLibrary from the registry
	 * 
	 * @param l
	 * @throws NotRegisteredException
	 */
	private void removeLoadedLibrary(LoadedLibrary l) throws NotRegisteredException {
		Hashtable<LibraryVersion, LoadedLibraryCache> htVersions = registeredLibraries.get(l.getIdentifier().getName());
		if (htVersions == null) {
			throw new NotRegisteredException(l.getIdentifier());
		}

		htVersions.remove(l.getIdentifier().getVersion());
		if (htVersions.size() == 0) {
			registeredLibraries.remove(l.getIdentifier().getName());
		}
	}

	/**
	 * get the newest available version of the given service
	 * 
	 * a service has to be provided in a bundle of the package containing the service
	 * 
	 * @param serviceClassName
	 * @return class definition of the requested service
	 * @throws ClassLoaderException
	 */
	public Class<?> getServiceClass(String serviceClassName) throws ClassLoaderException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassManager> htVersions = registeredLoaders.get(sPackage);
		try {
			if (htVersions == null || htVersions.size() == 0) {
				registerService(serviceClassName);
				htVersions = registeredLoaders.get(sPackage);
			}
		} catch (LibraryNotFoundException e) {
			// class could not be found in any registered library, try default classpath
			// this is usually the case when executed within an IDE
			System.err.println("Warning! No library (jar) found for " + serviceClassName
					+ "! Trying default classpath. This should not happen in a productive environment!");
			try {
				return this.getClass().getClassLoader().loadClass(serviceClassName);
			} catch (ClassNotFoundException e2) {
				throw new LibraryNotFoundException(e2);
			}
		}

		LibraryVersion version = null;
		for (Enumeration<LibraryVersion> en = htVersions.keys(); en.hasMoreElements();) {
			LibraryVersion v = en.nextElement();
			if (version == null || v.isLargerThan(version)) {
				version = v;
			}
		}

		BundleClassManager bcl = htVersions.get(version);

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
	 * @return a class definition of the requested service
	 * @throws ClassLoaderException
	 */
	public Class<?> getServiceClass(String serviceClassName, LibraryVersion version) throws ClassLoaderException {
		String sPackage = getPackageName(serviceClassName);

		Hashtable<LibraryVersion, BundleClassManager> htVersions = registeredLoaders.get(sPackage);
		try {
			if (htVersions == null || htVersions.get(version) == null) {
				registerService(serviceClassName, version);
				htVersions = registeredLoaders.get(sPackage);
			}
		} catch (LibraryNotFoundException e) {
			// class could not be found in any registered library, try default classpath
			// this is usually the case when executed within an IDE
			System.err.println("No library found for " + serviceClassName
					+ "! Trying default classpath. This should not happen in a productive environment!");
			try {
				return this.getClass().getClassLoader().loadClass(serviceClassName);
			} catch (ClassNotFoundException e2) {
				throw new LibraryNotFoundException(e2);
			}
		}

		BundleClassManager bcl = htVersions.get(version);

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
	 * @return class definition of the requested service
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
		for (Enumeration<String> names = registeredLibraries.keys(); names.hasMoreElements();) {
			String name = names.nextElement();
			result += registeredLibraries.get(name).keySet().size();
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
		if (className.indexOf('.') < 0) {
			throw new IllegalArgumentException("this class is not contained in a package!");
		}

		return className.substring(0, className.lastIndexOf('.'));
	}

	public void addRepository(Repository repository) {
		repositories.add(repository);
	}

	// TODO add method to remove repository
	// What about already loaded libraries and classes from that repository?

}
