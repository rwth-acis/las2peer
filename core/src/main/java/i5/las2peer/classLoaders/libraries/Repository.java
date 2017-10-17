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
	 * @param name A library name
	 * @return a loaded library for the given library name
	 * @throws LibraryNotFoundException If the library was not found in this repository
	 */
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException;

	/**
	 * get a library matching name and version of the given identifier
	 * 
	 * @param lib A library identifier
	 * @return a loaded library for the given identifier
	 * @throws LibraryNotFoundException If the library was not found in this repository
	 */
	public LoadedLibrary findLibrary(LibraryIdentifier lib) throws LibraryNotFoundException;

}
