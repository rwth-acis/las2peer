package i5.las2peer.classLoaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

public class LoadedLibraryCache {

	/**
	 * loaded library that is cached
	 */
	private LoadedLibrary loadedLibrary;

	/**
	 * bundles using this cache
	 */
	private Set<BundleClassManager> bundles = new HashSet<>();

	/**
	 * cached resources
	 */
	// TODO check benefit of resource caching
	private Map<String, byte[]> resourceCache = new HashMap<>();

	/**
	 * create new cache for a library
	 * 
	 * @param loadedLibrary the LoadedLibrary that should be cached
	 */
	public LoadedLibraryCache(LoadedLibrary loadedLibrary) {
		this.loadedLibrary = loadedLibrary;
	}

	/**
	 * register a bundle for using this cache
	 * 
	 * @param bundle
	 */
	public void registerBundle(BundleClassManager bundle) {
		bundles.add(bundle);
	}

	/**
	 * unregister a bundle
	 * 
	 * @param bundle
	 */
	public void unregisterBundle(BundleClassManager bundle) {
		bundles.remove(bundle);
	}

	/**
	 * check if this cache is used
	 * 
	 * @return Returns true if this cache is used in a bundle
	 */
	public boolean isUsed() {
		return !bundles.isEmpty();
	}

	/**
	 * get the cached library
	 * 
	 * @return Returns the library instance contained in this cache
	 */
	public LoadedLibrary getLoadedLibrary() {
		return loadedLibrary;
	}

	/**
	 * get a cached resource
	 * 
	 * @param resourceName
	 * @return Returns the resource as binary blob
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	public byte[] getCachedResourceAsBinary(String resourceName) throws ResourceNotFoundException, IOException {
		if (resourceCache.containsKey(resourceName)) {
			return resourceCache.get(resourceName);
		} else {
			byte[] resource = loadedLibrary.getResourceAsBinary(resourceName);
			resourceCache.put(resourceName, resource);
			return resource;
		}
	}

	/**
	 * convience method for junit tests
	 * 
	 * creates a new cache for a jar library
	 * 
	 * @param filename
	 * @return Returns the instance created from the given jar file
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	protected static LoadedLibraryCache createFromJar(String filename) throws IllegalArgumentException, IOException {
		return new LoadedLibraryCache(LoadedJarLibrary.createFromJar(filename));
	}
}
