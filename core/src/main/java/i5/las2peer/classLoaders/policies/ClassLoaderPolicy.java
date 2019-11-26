package i5.las2peer.classLoaders.policies;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract policy to define restrictions to Java packages and classes.
 * 
 * Only whitelisted packages / classes (and their subpackages) can be loaded. If the empty package is whitelisted, all classes can
 * be loaded.
 * 
 * Blacklisted paths and their subpaths are never allowed. Blacklisting has priority over whitelisting.
 * 
 * Note that the security manager needs to be enabled and deny access to the system class loader in order to make this to work!
 *
 */
public abstract class ClassLoaderPolicy {

	private Set<String> allowedPaths = new HashSet<>();
	private Set<String> deniedPaths = new HashSet<>();

	protected void allow(String path) {
		allowedPaths.add(path);
	}
	
	protected void deny(String path) {
		deniedPaths.add(path);
	}

	public boolean canLoad(String className) {
		
		// deny empty package
		if (deniedPaths.contains("")) {
			return false;
		}
		
		// check if any parent package or the package/class itself is blacklisted
		String[] split = className.split("\\.");
		String current = "";
		for (String part : split) {
			current += (current.equals("")) ? part : ("." + part);
			if (deniedPaths.contains(current)) {
				return false;
			}
		}
		
		// allow empty package
		if (allowedPaths.contains("")) {
			return true;
		}
		
		// check if any parent package or the package/class itself is whitelisted
		current = "";
		for (String part : split) {
			current += (current.equals("")) ? part : ("." + part);
			if (allowedPaths.contains(current)) {
				return true;
			}
		}
		
		// otherwise deny access
		return false;
	}

}
