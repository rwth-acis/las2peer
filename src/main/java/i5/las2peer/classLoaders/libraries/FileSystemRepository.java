package i5.las2peer.classLoaders.libraries;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import i5.las2peer.classLoaders.LibraryNotFoundException;
import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;
import i5.las2peer.tools.SimpleTools;

/**
 * implements a repository which loads all libraries from a given directory or from severeal ones. The search for
 * library files (jars) may be recursive.
 *
 */
public class FileSystemRepository implements Repository {

	private Iterable<String> directories;
	private boolean recursive = false;
	private Hashtable<String, Hashtable<LibraryVersion, String>> htFoundJars;

	/**
	 * create a repository for the given directory, non-recursive
	 * 
	 * @param directory
	 */
	public FileSystemRepository(String directory) {
		this(new String[] { directory }, false);
	}

	/**
	 * create a repository for the given directory
	 * 
	 * @param directory
	 * @param recursive
	 */
	public FileSystemRepository(String directory, boolean recursive) {
		this(new String[] { directory }, recursive);
	}

	/**
	 * create a repository for the given directories, non-recursive
	 * 
	 * @param directories
	 */
	public FileSystemRepository(String[] directories) {
		this(directories, false);
	}

	/**
	 * create a repository for the given directories
	 * 
	 * @param directories
	 * @param recursive
	 */
	public FileSystemRepository(String[] directories, boolean recursive) {
		this(Arrays.asList(directories), recursive);
	}

	/**
	 * create a repository for the given directories
	 * 
	 * @param directories
	 * @param recursive
	 */
	public FileSystemRepository(Iterable<String> directories, boolean recursive) {
		this.directories = directories;
		this.recursive = recursive;

		initJarList();
	}

	/**
	 * get the newest library for the given name
	 * 
	 * @param name
	 * @return a LoadedLibrary for the requested library name
	 * @throws LibraryNotFoundException
	 * @throws UnresolvedDependenciesException
	 */
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException, UnresolvedDependenciesException {
		Hashtable<LibraryVersion, String> htVersions = htFoundJars.get(name);
		if (htVersions == null) {
			System.err.println(this + " could not find " + name);
			throw new LibraryNotFoundException(
					"library '" + name + "' package could not be found in the repositories!");
		} else {
			System.err.println(this + " has " + htVersions.size() + " versions of " + name);
		}

		LibraryVersion version = null;
		for (Enumeration<LibraryVersion> en = htVersions.keys(); en.hasMoreElements();) {
			LibraryVersion v = en.nextElement();
			if (version == null || v.isLargerThan(version))
				version = v;
		}

		try {
			return LoadedJarLibrary.createFromJar(htVersions.get(version));
		} catch (IllegalArgumentException e) {
			// somthing's wrong with the manifest
			throw new UnresolvedDependenciesException(
					"Somethings seems wrong with the dependency information of " + name + ": " + e.getMessage(), e);
		} catch (Exception e) {
			throw new LibraryNotFoundException("Error opening library jar " + htVersions.get(version), e);
		}
	}

	/**
	 * get a library matching name and version of the given identifier
	 * 
	 * @param lib
	 * 
	 * @return a LoadedLibrary for the requested library identifier
	 * 
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findLibrary(LibraryIdentifier lib) throws LibraryNotFoundException {
		Hashtable<LibraryVersion, String> htVersions = htFoundJars.get(lib.getName());
		if (htVersions == null)
			throw new LibraryNotFoundException(
					"library '" + lib.toString() + "' package could not be found in the repositories!");

		String jar = htVersions.get(lib.getVersion());

		if (jar == null)
			throw new LibraryNotFoundException(
					"library '" + lib.toString() + "' package could not be found in the repositories!");

		try {
			return LoadedJarLibrary.createFromJar(jar);
		} catch (Exception e) {
			throw new LibraryNotFoundException(
					"library '" + lib.toString() + "' package could not be found in the repositories!", e);
		}
	}

	/**
	 * get the newest library matching the given library dependency (name and version range)
	 * 
	 * @param dep
	 * @return a LoadedLibray matching the given library dependency
	 * @throws LibraryNotFoundException
	 */
	public LoadedLibrary findMatchingLibrary(LibraryDependency dep) throws LibraryNotFoundException {
		// TODO: find better search solution: Search sorted and find always newest version
		Hashtable<LibraryVersion, String> htVersions = htFoundJars.get(dep.getName());
		if (htVersions == null)
			throw new LibraryNotFoundException(
					"library '" + dep.getName() + "' package could not be found in the repositories!");

		for (LibraryVersion version : htVersions.keySet()) {
			if (dep.fits(version)) {
				try {
					return LoadedJarLibrary.createFromJar(htVersions.get(version));
				} catch (Exception e) {
					System.out.println("Error loading jar: " + e);
					e.printStackTrace();
				}
			}
			// else System.out.println ( "--> does not fit");
		}

		throw new LibraryNotFoundException(
				"library '" + dep.toString() + "' package could not be found in the repositories!");
	}

	/**
	 * get an array with all versions found for the given library name
	 * 
	 * @param libraryName
	 * @return array with all available versions of the given library
	 */
	public String[] getAvailableVersions(String libraryName) {
		return getAvailableVersionSet(libraryName).toArray(new String[0]);
	}

	/**
	 * get a collection with all versions found for the given library name
	 * 
	 * @param libraryName
	 * @return a collections with all versions of the given library
	 */
	public Collection<LibraryVersion> getAvailableVersionSet(String libraryName) {
		Hashtable<LibraryVersion, String> htFound = htFoundJars.get(libraryName);
		if (htFound == null)
			return new HashSet<LibraryVersion>();

		return htFound.keySet();
	}

	/**
	 * get an array with found jar files within this repository
	 * 
	 * @return an array with all libraries in this repository
	 */
	public String[] getAllLibraries() {
		Collection<String> libs = getLibraryCollection();
		return libs.toArray(new String[0]);
	}

	/**
	 * get a collection with all found jar files within this repository
	 * 
	 * @return a collection with all libraries in this repository
	 */
	public Collection<String> getLibraryCollection() {
		HashSet<String> hsTemp = new HashSet<String>();

		Enumeration<String> eLibs = htFoundJars.keys();
		while (eLibs.hasMoreElements()) {
			String lib = eLibs.nextElement();
			Iterator<String> jars = htFoundJars.get(lib).values().iterator();
			while (jars.hasNext()) {
				String jar = jars.next();
				hsTemp.add(jar);
			}
		}

		return hsTemp;
	}

	/**
	 * initialize the list if jars
	 */
	private void initJarList() {
		htFoundJars = new Hashtable<String, Hashtable<LibraryVersion, String>>();

		for (String directory : directories) {
			searchJars(directory);
		}
	}

	/**
	 * look for jars in the given directory, search recursive, if flag is set
	 * 
	 * @param directory
	 */
	private void searchJars(String directory) {
		File f = new File(directory);

		if (!f.exists() || !f.isDirectory()) {
			// since this is a search function, stay friendly and don't throw an exception
			System.err.println("Given path is not a directory: " + f.toString());
			return;
		}

		File[] entries = f.listFiles();

		for (int i = 0; i < entries.length; i++) {

			if (entries[i].isDirectory()) {
				if (recursive)
					searchJars(entries[i].toString());
			} else if (entries[i].getPath().endsWith(".jar")) {
				if (entries[i].getName().contains("-")) {
					String[] split = entries[i].getName().substring(0, entries[i].getName().length() - 4).split("-", 2);
					try {
						LibraryVersion version = new LibraryVersion(split[1]);
						registerJar(entries[i].getPath(), split[0], version);
					} catch (IllegalArgumentException e) {
						// ok, version info not correct
						// TODO: print warning about missing version info?

						System.out.println("Error registering library " + entries[i] + ": " + e);

					}
				} else {
					// TODO: print warning about missing version info?
					// maybe depending on log level
					System.out.println("library " + entries[i] + " has no version info in it's name! - Won't be used!");
				}
			}
		}
	}

	/**
	 * register a found jar file to the hashtable of available jars in this repository
	 * 
	 * @param file
	 * @param name
	 * @param version
	 */
	private void registerJar(String file, String name, LibraryVersion version) {
		Hashtable<LibraryVersion, String> htNameEntries = htFoundJars.get(name);
		if (htNameEntries == null) {
			htNameEntries = new Hashtable<LibraryVersion, String>();
			htFoundJars.put(name, htNameEntries);
		}
		htNameEntries.put(version, file);
	}

	/**
	 * @return a simple string representation of this object
	 */
	public String toString() {
		return "FS-Repository at " + SimpleTools.join(directories, ":");
	}

}
