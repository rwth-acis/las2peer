package i5.las2peer.classLoaders.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashSet;

import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;

/**
 * a loaded library represents a library in the l2p classloader context
 * 
 * 
 *
 */
public abstract class LoadedLibrary {

	private HashSet<LoadedLibrary> resolvedDependencies;
	private LibraryDependency[] initialDependencies;

	private LibraryIdentifier myLibrary;

	/**
	 * generates a new CL without any dependencies i.e. this ClassLoader may not use any other registered libraries
	 * 
	 * @param libraryIdentifier identifier of the library bound to this ClassLoader
	 */
	LoadedLibrary(String libraryIdentifier) {
		super();

		myLibrary = new LibraryIdentifier(libraryIdentifier);
	}

	/**
	 * generates a new CL without any dependencies i.e. this ClassLoader may not use any other registered libraries
	 * 
	 * @param lib identifier of the library bound to this ClassLoader
	 */
	LoadedLibrary(LibraryIdentifier lib) {
		super();

		myLibrary = lib;
	}

	/**
	 * generates a new CL with dependencies which may be used for loading classes
	 * 
	 * @param libraryIdentifier identifier of the library bound to this ClassLoader
	 * @param initialDependencies array with dependency information
	 */
	LoadedLibrary(String libraryIdentifier, LibraryDependency[] initialDependencies) {
		this(new LibraryIdentifier(libraryIdentifier), initialDependencies);
	}

	/**
	 * generates a new CL with dependencies which may be used in class loading
	 * 
	 * @param lib identifier of the library bound to this ClassLoader
	 * @param deps array with ClassLoaders this one may use for class loading
	 */
	LoadedLibrary(LibraryIdentifier lib, LibraryDependency[] initialDependencies) {
		this(lib);
		this.initialDependencies = initialDependencies;
	}

	/**
	 * 
	 * @return the identifier (name and version) for the library to which this ClassLoader is bound
	 */
	public LibraryIdentifier getLibraryIdentifier() {
		return myLibrary;
	}

	/**
	 * 
	 * @return array with dependency information
	 */
	public LibraryDependency[] getDependencies() {
		return initialDependencies;
	}

	/**
	 * set the (resolved) library dependencies of this library
	 * 
	 * @param libs
	 */
	void setResolvedDependencies(LoadedLibrary[] libs) {
		resolvedDependencies = new HashSet<LoadedLibrary>();
		for (int i = 0; i < libs.length; i++) {
			resolvedDependencies.add(libs[i]);
		}
	}

	/**
	 * add a loaded library to the dependencies of this one
	 * 
	 * @param lib
	 */
	void addResolvedDependency(LoadedLibrary lib) {
		resolvedDependencies.add(lib);
	}

	/**
	 * for class and resource loading: get the URL for the specified resource
	 * 
	 * @param name
	 * 
	 * @return a URL for the requested ressource
	 * 
	 * @throws ResourceNotFoundException the given resource does not exists in this library
	 */
	public abstract URL getResourceAsUrl(String name) throws ResourceNotFoundException;

	/**
	 * for class and resource loading: get the specified resource as an InputStream
	 * 
	 * @param resourceName
	 * 
	 * @return an InputStream for the requested resource
	 * 
	 * @throws ResourceNotFoundException the given resource does not exists in this library
	 */
	abstract InputStream getResourceAsStream(String resourceName) throws ResourceNotFoundException;

	/**
	 * get the size in bytes of the resource contents
	 * 
	 * @param resourceName
	 * @return size in bytes
	 * @throws ResourceNotFoundException the given resource does not exists in this library
	 */
	abstract long getSizeOfResource(String resourceName) throws ResourceNotFoundException;

	/**
	 * get the contents of a resource as a String
	 * 
	 * @param resourceName
	 * 
	 * @return content of the given resource as string
	 * 
	 * @throws IOException
	 * @throws ResourceNotFoundException the given resource does not exists in this library
	 */
	public String getResourceAsString(String resourceName) throws IOException, ResourceNotFoundException {
		InputStream is = getResourceAsStream(resourceName);

		InputStreamReader input;
		try {
			input = new InputStreamReader(is, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// should not occur!
			throw new IOException("UTF-8 not supported!", e);
		}

		final int READ_SIZE = 5000;
		final char[] buffer = new char[READ_SIZE];
		StringBuilder result = new StringBuilder(READ_SIZE);
		for (int read = input.read(buffer, 0, buffer.length); read != -1; read = input.read(buffer, 0, buffer.length)) {
			result.append(buffer, 0, read);
		}

		return result.toString();
	}

	/**
	 * get the contents of a resource as a byte array
	 * 
	 * @param resourceName
	 * @return contents of the requested resource
	 * @throws IOException
	 * @throws ResourceNotFoundException the given resource does not exists in this library
	 */
	public byte[] getResourceAsBinary(String resourceName) throws IOException, ResourceNotFoundException {
		InputStream input = getResourceAsStream(resourceName);
		long size = getSizeOfResource(resourceName);

		if (size > Integer.MAX_VALUE)
			throw new IOException("the requested resoure is too large to fit into a byte array!");

		byte[] result = new byte[(int) size];

		// for some reason the read method does sometimes not read all the
		// content of the iostream. binary 0 in the class code may be an explanation
		// it should work with the loop (holger)
		int alreadyRead = 0;
		int iRead = 0;
		do {
			iRead = input.read(result, alreadyRead, (int) (size - alreadyRead));
			alreadyRead += iRead;
		} while (iRead > 0 && alreadyRead < size);

		if (alreadyRead != size)
			throw new IOException("Error reading class contents (size mismatch)!");

		return result;
	}

	/**
	 * returns the library identifier for the library, this object is responsible for (including name and version)
	 * 
	 * @return indentifier of the nested library
	 */
	public LibraryIdentifier getIdentifier() {
		return this.myLibrary;
	}

	/**
	 * Helper method to get the name of a class file from the corresponding name of the class
	 *
	 * @param className name of a class
	 *
	 * @return a String
	 *
	 */
	public static String classToResourceName(String className) {
		return className.replace('.', '/') + ".class";
	}

	/**
	 * Helper method to transform the name of an jar file entry into a java classname.
	 *
	 * @param entryName name of a jar file entry
	 *
	 * @return class name
	 *
	 */
	public static String resourceToClassName(String entryName) {
		if (!entryName.endsWith(".class"))
			throw new IllegalArgumentException("This is not a class file!");

		return entryName.replace('/', '.').substring(0, entryName.length() - 6);
	}

}
