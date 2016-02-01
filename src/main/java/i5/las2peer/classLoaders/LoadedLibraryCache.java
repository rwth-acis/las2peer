package i5.las2peer.classLoaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

public class LoadedLibraryCache { // TODO TESTS
	// TODO EINBAUEN
	
	private LoadedLibrary loadedLibrary;
	
	private Set<BundleClassManager> classLoaders = new HashSet<>();
	
	private Map<String,byte[]> resourceCache = new HashMap<>();  
	
	public LoadedLibraryCache(LoadedLibrary loadedLibrary) {
		this.loadedLibrary = loadedLibrary;
	}
	
	public void registerClassLoader(BundleClassManager classLoader) {
		classLoaders.add(classLoader);
	}
	
	public void unregisterClassLoader(BundleClassManager classLoader) {
		classLoaders.remove(classLoader);
	}
	
	public boolean isUsed() {
		return !classLoaders.isEmpty();
	}
	
	public LoadedLibrary getLoadedLibrary() {
		return loadedLibrary;
	}
	
	public byte[] getCachedResourceAsBinary(String resourceName) throws ResourceNotFoundException, IOException {
		if (resourceCache.containsKey(resourceName)) {
			return resourceCache.get(resourceName);
		}
		else {
			byte[] resource = loadedLibrary.getResourceAsBinary(resourceName);
			resourceCache.put(resourceName, resource);
			return resource;
		}
	}
	
}
