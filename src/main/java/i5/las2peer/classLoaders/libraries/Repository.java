package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;


/**
 * a repository provides the {@link i5.las2peer.classLoaders.L2pClassLoader} with needed libraries (jars)
 * in form of {@link LoadedLibrary} implementations 
 * 
 * @author Holger Jan&szlig;en
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
	 * @throws NotFoundException
	 * @throws UnresolvedDependenciesException 
	 */
	public LoadedLibrary findLibrary ( String name ) throws NotFoundException, UnresolvedDependenciesException;
	
	/**
	 * get a library matching name and version of the given identifier
	 * 
	 * @param lib
	 * 
	 * @return a loaded library for the given identifier
	 * 
	 * @throws NotFoundException
	 */
	public LoadedLibrary findLibrary( LibraryIdentifier lib ) throws NotFoundException;
	
	/**
	 * get the newest library matching the given library dependency (name and version range)
	 * 
	 * @param dep
	 * 
	 * @return a loaded library matching the given dependency
	 * 
	 * @throws NotFoundException
	 */
	public LoadedLibrary findMatchingLibrary(LibraryDependency dep) throws NotFoundException;
		
}
