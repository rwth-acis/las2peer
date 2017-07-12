package i5.las2peer.classLoaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.classLoaders.libraries.LibraryIdentifier;
import i5.las2peer.classLoaders.libraries.LibraryVersion;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.Repository;
import i5.las2peer.classLoaders.policies.ClassLoaderPolicy;

/**
 * The main class for loading classes in the las2peer environment. This ClassManager handles library registering and
 * resolving (i.e. loading) dependencies as well as hotplugging.
 * 
 * The main idea is to keep each service separate but load each needed library only once.
 * 
 * 
 *
 */
public class ClassManager {

	/**
	 * classloader to load the platform's classes (Java + las2peer core + Connectors)
	 */
	private ClassLoader platformLoader;

	/**
	 * repositories to load libraries from
	 */
	private ArrayList<Repository> repositories;

	/**
	 * all registered main bundles (i.e. services) (name, version) => ServiceClassLoader
	 */
	private Hashtable<ServiceNameVersion, ServiceClassLoader> registeredLoaders = new Hashtable<>();
	
	/**
	 * The policy
	 */
	private ClassLoaderPolicy policy;

	/**
	 * create a new L2pClassLoader, which uses the given repository
	 * 
	 * @param repository
	 * @param platformLoader
	 * @param policy 
	 */
	public ClassManager(Repository repository, ClassLoader platformLoader, ClassLoaderPolicy policy) {
		this(new Repository[] { repository }, platformLoader, policy);
	}

	/**
	 * create a new L2PClassLoader, which uses the given repositories
	 * 
	 * @param repositories
	 * @param platformLoader
	 * @param policy 
	 */
	public ClassManager(Repository[] repositories, ClassLoader platformLoader, ClassLoaderPolicy policy) {
		this(Arrays.asList(repositories), platformLoader, policy);
	}

	/**
	 * create a new L2PClassLoader, which uses the given repositories
	 * 
	 * @param repositories
	 * @param platformLoader
	 * @param policy 
	 */
	public ClassManager(List<Repository> repositories, ClassLoader platformLoader, ClassLoaderPolicy policy) {
		this.repositories = new ArrayList<>(repositories);
		this.platformLoader = platformLoader;
		this.policy = policy;
	}

	/**
	 * Register a service with the given identifier.
	 * @param serviceIdentifier 
	 * 
	 * @throws ClassLoaderException
	 */
	public void registerService(ServiceNameVersion serviceIdentifier) throws ClassLoaderException {
		LibraryIdentifier libId = new LibraryIdentifier(getPackageName(serviceIdentifier.getName()),
				new LibraryVersion(serviceIdentifier.getVersion().toString()));

		// check if already registered
		if (registeredLoaders.containsKey(serviceIdentifier)) {
			return;
		}

		// get loaded libraray from repository
		LoadedLibrary lib = null;
		for (int i = 0; i < repositories.size() && lib == null; i++) {
			try {
				lib = repositories.get(i).findLibrary(libId);
				if (lib != null) {
					break;
				}
			} catch (LibraryNotFoundException e) {
			}
		}

		if (lib == null) {
			throw new LibraryNotFoundException(libId.toString());
		}
		
		// register
		ServiceClassLoader cl = new ServiceClassLoader(lib, platformLoader, policy);
		registeredLoaders.put(serviceIdentifier, cl);
	}

	/**
	 * Unregister a service class loader.
	 * @param service 
	 * 
	 * @throws NotRegisteredException
	 */
	public void unregisterService(ServiceNameVersion service) throws NotRegisteredException {
		if (!registeredLoaders.containsKey(service)) {
			throw new NotRegisteredException(service);
		}
		
		registeredLoaders.remove(service);
	}

	/**
	 * get a service class in the specified version
	 * 
	 * a service has to be provided in a bundle of the package containing the service
	 * 
	 * @param service
	 * @return a class definition of the requested service
	 * @throws ClassLoaderException
	 */
	public Class<?> getServiceClass(ServiceNameVersion service) throws ClassLoaderException {
		ServiceClassLoader cl = registeredLoaders.get(service);
		if (cl == null) {
			try {
				registerService(service);
				cl = registeredLoaders.get(service);
			} catch (LibraryNotFoundException e) {
				// class could not be found in any registered library, try default classpath
				// this is usually the case when executed within an IDE
				System.err.println("No library found for " + service
						+ "! Trying default classpath. This should not happen in a productive environment!");
				try {
					return this.platformLoader.loadClass(service.getName());
				} catch (ClassNotFoundException e2) {
					throw new LibraryNotFoundException(e2);
				}
			}
		}
		
		try {
			return cl.loadClass(service.getName());
		} catch (ClassNotFoundException e) {
			throw new LibraryNotFoundException("The library for " + service
					+ " could be loaded, but the class is not available!", e);
		}
	}

	/**
	 * statistics: get the number of loaded (registered) jar libraries
	 * 
	 * @return the number of the registered libraries
	 */
	int numberOfRegisteredServices() {
		return registeredLoaders.size();
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

	/**
	 * Adds a repository. Repositories cannot be removed.
	 * 
	 * @param repository a repository
	 */
	public void addRepository(Repository repository) {
		repositories.add(repository);
	}

}
