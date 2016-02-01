package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.LibraryNotFoundException;
import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;

/**
 * a repository provides the {@link i5.las2peer.classLoaders.L2pClassManager} with needed libraries (jars) in form of
 * {@link LoadedLibrary} implementations
 * 
 * 
 *
 */
public interface Repository {

	/**
	 * get the newest library for the given name
	 * 
	 * @param name
	 * 
	 * @return a loaded library for the given library name
	 * 
	 * @throws LibraryNotFoundException
	 * @throws UnresolvedDependenciesException
	 */
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException, UnresolvedDependenciesException;

	/**
	 * get a library matching name and version of the given identifier
	 * 
	 * @param lib
	 * 
	 * @return a loaded library for the given identifier
	 * 
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findLibrary(LibraryIdentifier lib) throws LibraryNotFoundException;

	/**
	 * get the newest library matching the given library dependency (name and version range)
	 * 
	 * @param dep
	 * 
	 * @return a loaded library matching the given dependency
	 * 
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findMatchingLibrary(LibraryDependency dep) throws LibraryNotFoundException;

}
