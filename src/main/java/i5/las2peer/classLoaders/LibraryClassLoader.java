package i5.las2peer.classLoaders;

import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;
import i5.las2peer.classLoaders.libraries.LoadedLibrary;
import i5.las2peer.classLoaders.libraries.NotFoundException;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;



/**
 * a LibraryClassLoader is responsible for loading classes from one library bundle, probably loaded 
 * via a {@link i5.las2peer.classLoaders.libraries.Repository}
 *  
 * @author Holger Janssen
 * @version $Revision: 1.10 $, $Date: 2013/02/26 17:57:28 $
 *
 */
public class LibraryClassLoader extends ClassLoader {

	/**
	 * the library, this class loader is responsible for
	 */
	private LoadedLibrary myLibrary = null;
	
	/**
	 * registered parents, may be multiple, especially for 3rd party libraries or exchange libraries
	 */
	// TODO: hm, hashset implementation leads to non-deterministic classloading calls to parents!!!
	// especially of resources as well!!!
	private Set<BundleClassLoader> parents = new HashSet <BundleClassLoader> ();
	
	/**
	 * create a new class loader for a given library.
	 *
	 * @param lib
	 * @param parent
	 */
	public LibraryClassLoader ( LoadedLibrary lib, BundleClassLoader parent ) {
		this ( lib );
	
		parents.add ( parent );
	}
	
	/**
	 * create a new class loader for the given library
	 * 
	 * @param lib
	 */
	public LibraryClassLoader ( LoadedLibrary lib ) {
		this.myLibrary = lib;
	}
	
	
	/**
	 * create a LibraryClassLoader for the given Jar file
	 * @param jarfile
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public LibraryClassLoader ( String jarfile ) throws IllegalArgumentException, IOException {
		this.myLibrary = LoadedJarLibrary.createFromJar ( jarfile );
	}
		
	/**
	 * get the binary contents of the given resource
	 * 
	 * @param resourceName
	 * @return contents of the resource
	 * @throws NotFoundException
	 * @throws IOException
	 */
	private byte[] getResourceContent ( String resourceName ) throws ClassLoaderException, IOException {
		return myLibrary.getResourceAsBinary(resourceName);
	}
	
	
	
	/**
	 * findClass is the only method that's actually needed for classloading 
	 * this method will be called by loadClass, if the class is not already loaded and cannot be found 
	 * in the parent classloader.
	 *  
	 * @param	className
	 * @throws ClassNotFoundException 
	 */
	public Class<?> findClassInternal ( String className ) throws ClassNotFoundException {
		byte[] binaryDefinition;
		Logger.logFinding(this, className, null);
		
		try {
			binaryDefinition = getResourceContent( LoadedLibrary.classToResourceName(className) );
			
			Logger.logFinding(this, className, true);
			
			return defineClass (className, binaryDefinition, 0, binaryDefinition.length );
		} catch (Exception e) {
			Logger.logFinding(this, className, false);
			throw new ClassNotFoundException ( "The class " + className + " could not be loaded by this classloader!", e);
		}
	}
	
	
	
	public Class<?> findClass ( String className ) throws ClassNotFoundException {
		Logger.logMessage(this, className, "external loading via findclass");
		return loadClass ( className, true );
	}

	@Override
	protected synchronized Class<?> loadClass ( String name, boolean resolve ) throws ClassNotFoundException {
		return loadClass ( name, resolve, true);
	}
	
	/**
	 * loading classes in a Library classloader follows the following steps:
	 * check if the class has already been loaded
	 * look into all bundles class loaders this library class loader has been registered to
	 * look into the bootstrap classloader
	 * alert that the class cannot be found
	 * 
	 * @param name
	 * @param resolve
	 * @param lookUp
	 * 
	 * @return	class definition of the requested class
	 * 
	 * @throws ClassNotFoundException
	 */
    synchronized Class<?> loadClass(String name, boolean resolve, boolean lookUp)
		throws ClassNotFoundException {
    	    
		Logger.logLoading(this, name, null, lookUp);

		// Then check if the class is already known
		try {
			return getSystemClassLoader().loadClass(name);
		} catch (ClassNotFoundException e) {
			// Class in not known
		}

		// First, check if the class has already been loaded
		Class<?> c = findLoadedClass(name);

		if ( c == null ) {
		    try {
		    	return findClassInternal ( name );
		    } catch ( ClassNotFoundException e ) {
		    	// class not found in this Library
		    }
		}
		
		if ( c == null && lookUp ) {
			for ( Iterator<BundleClassLoader> it = parents.iterator(); it.hasNext() && c == null ; )
				c = it.next().loadClass ( name, resolve, this );
		}

		if (resolve && c != null) {
		    resolveClass(c);
		}
		
		if ( c == null  ) {
			Logger.logLoading(this, name, false, lookUp);
			throw new ClassNotFoundException();
		}

		Logger.logLoading(this, name, true, lookUp);
		return c;
    }
	
	
	
	/**
	 * register another parent bundle
	 * @param parent
	 */
	void registerParentLoader ( BundleClassLoader parent ) {
		parents.add( parent );
	}
	
	/**
	 * unregister a parent bundle (e.g. if it is removed from the environment)
	 * @param parent
	 */
	void unregisterParentLopader ( BundleClassLoader parent ) {
		parents.remove( parent );
	}

	
	/**
	 * get the library, this loader is responsible for
	 * @return library linked to this classloader
	 */
	public LoadedLibrary getLibrary () {
		return myLibrary;
	}

	/**
	 * get a resource from this class loader in form of an URL
	 * 
	 * this method is used to implement the actual {@link #getResource(String)} method to
	 * look up into all libraries of this bundle.
	 * To prevent endless loops, the corresponding bundle class loader will set the lookUp
	 * flag to false so that the search will stay local in this class loader.
	 * 
	 * @param resourceName
	 * @param lookUp 
	 * @return
	 */
	URL getResource(String resourceName, boolean lookUp) {
		try {
			return myLibrary.getResourceAsUrl(resourceName);
		} catch (ResourceNotFoundException e) {
			if ( lookUp ) {
				for ( Iterator<BundleClassLoader> p = parents.iterator(); p.hasNext(); ) {
					URL result = p.next().findResource ( resourceName, this );
					if(  result != null )
						return result;
				}

				return super.getResource ( resourceName );			
			} else
				return null;
		}
	}
	
	/**
	 * get the URL for a resource
	 */
	public URL getResource ( String resourceName ) {
		return getResource ( resourceName, true );
	}

	/**
	 * is this LibraryClassLoader registered within some BundleClassLoaders?
	 * 
	 * @return	true, if a parent class loader is registered to this library class loader
	 */
	public boolean hasParentLoaders () {
		return parents.size() > 0;
	}
	
	/**
	 * to how many BundleClassLoaders are registered to this LibraryClassLoader?
	 * 
	 * @return	number of BundleClassLoaders using this LibraryClassLoader
	 * 
	 */
	public int numberOfParentLoaders () {
		return parents.size();
	}
	
	

}
