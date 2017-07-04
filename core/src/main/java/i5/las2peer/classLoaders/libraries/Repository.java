package i5.las2peer.classLoaders.libraries;

import i5.las2peer.classLoaders.LibraryNotFoundException;

/**
 * a repository provides the {@link i5.las2peer.classLoaders.ClassManager} with needed libraries (jars) in form of
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
	 * @return a loaded library for the given library name
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException;

	/**
	 * get a library matching name and version of the given identifier
	 * 
	 * @param lib
	 * @return a loaded library for the given identifier
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findLibrary(LibraryIdentifier lib) throws LibraryNotFoundException;

}
