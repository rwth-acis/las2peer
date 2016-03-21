package i5.las2peer.classLoaders.libraries;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.helpers.LibraryVersion;

/**
 * a loaded jar library implements a library on the basis of a standard jar file
 * 
 * 
 *
 */
public class LoadedJarLibrary extends LoadedLibrary {

	// name of the jar file
	private String sJarFileName;

	// internal jar file handler
	private JarFile jfFile;

	/**
	 * create a new LodadJarLibrary
	 * 
	 * @param filename
	 * @param ident
	 * @param deps
	 * @throws IOException
	 */
	public LoadedJarLibrary(String filename, LibraryIdentifier ident, LibraryDependency[] deps) throws IOException {
		super(ident, deps);

		sJarFileName = filename;
		jfFile = new JarFile(filename);
	}

	@Override
	public URL getResourceAsUrl(String name) throws ResourceNotFoundException {
		JarEntry je = jfFile.getJarEntry(name);

		if (je == null)
			throw new ResourceNotFoundException(name, sJarFileName);

		try {
			return new URL("jar:file:" + this.sJarFileName + "!/" + name);
		} catch (MalformedURLException e) {
			// should not occur
			e.printStackTrace();
			return null;
		}

	}

	@Override
	InputStream getResourceAsStream(String resourceName) throws ResourceNotFoundException {
		URL url = getResourceAsUrl(resourceName);

		try {
			return url.openStream();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns all classes stored in the corresponding jar file.
	 *
	 * @return a String[] array with the class names (in . - notation )
	 *
	 */
	public String[] getContainedClasses() {
		// compute size
		int iSize = 0;
		for (Enumeration<JarEntry> entries = jfFile.entries(); entries.hasMoreElements();) {
			if (entries.nextElement().getName().endsWith(".class"))
				iSize++;
		}

		// compute array with class names
		String[] asResult = new String[iSize];
		int i = 0;
		for (Enumeration<JarEntry> entries = jfFile.entries(); entries.hasMoreElements();) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().endsWith(".class")) {
				asResult[i] = LoadedLibrary.resourceToClassName(entry.getName());
				i++;
			}
		}

		return asResult;
	}

	/**
	 * returns an array with names of resources (other than classes) contained in the jar archive of this ClassLoader
	 *
	 * @return a String[]
	 *
	 */
	public String[] getContainedResources() {
		// compute size
		int iSize = 0;
		for (Enumeration<JarEntry> entries = jfFile.entries(); entries.hasMoreElements();) {
			JarEntry entry = entries.nextElement();
			if (!entry.getName().endsWith(".class") && !entry.getName().endsWith("/"))
				iSize++;
		}

		// compute array with class names
		String[] asResult = new String[iSize];
		int i = 0;
		for (Enumeration<JarEntry> entries = jfFile.entries(); entries.hasMoreElements();) {
			JarEntry entry = entries.nextElement();
			if (!entry.getName().endsWith(".class") && !entry.getName().endsWith("/")) {
				asResult[i] = entry.getName();
				i++;
			}
		}

		return asResult;
	}

	/**
	 * Returns the name of the corresponding jar file.
	 *
	 * @return a String
	 *
	 */
	public String getJarFileName() {
		return sJarFileName;
	}

	@Override
	long getSizeOfResource(String resourceName) throws ResourceNotFoundException {
		JarEntry je = jfFile.getJarEntry(resourceName);

		if (je == null)
			throw new ResourceNotFoundException(resourceName, sJarFileName);

		return je.getSize();
	}

	/**
	 * factory: create a LoadedJarLibrary from a JAR file and the information contained in its manifest
	 * 
	 * @param filename filename of the jar file
	 * 
	 * @return a loaded jar library representing the given file
	 * 
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static LoadedJarLibrary createFromJar(String filename) throws IllegalArgumentException, IOException {
		JarFile jfFile = new JarFile(filename);

		String sImport = jfFile.getManifest().getMainAttributes().getValue("Import-Library");
		String sName = jfFile.getManifest().getMainAttributes().getValue("Library-SymbolicName");
		String sVersion = jfFile.getManifest().getMainAttributes().getValue("Library-Version");

		// fill in version and name info from file name
		if (sName == null || sVersion == null) {
			String file = filename.substring(0, filename.length() - 4);
			
			Pattern versionPattern = Pattern.compile("-[0-9]+(?:.[0-9]+(?:.[0-9]+)?)?(?:-[0-9]+)?$");
			Matcher m = versionPattern.matcher(file);
			
			if (m.find()) { // look for version information in filename
				if (sVersion == null) {
					sVersion = m.group().substring(1); 
				}

				if (sName == null) {
					sName = file.substring(0, m.start());
				}
			}
			else {
				if (sName == null) {
					sName = file;
				}
			}
		}

		jfFile.close();
		return new LoadedJarLibrary(filename, new LibraryIdentifier(sName, sVersion),
				LibraryDependency.fromString(sImport));

	}

}
