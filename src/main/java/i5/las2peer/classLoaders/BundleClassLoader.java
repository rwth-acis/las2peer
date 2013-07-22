package i5.las2peer.classLoaders;

import i5.las2peer.classLoaders.helpers.LibraryIdentifier;

import java.net.URL;
import java.util.Iterator;
import java.util.Set;


/**
 * A BundleClassLoader is responsible of providing all classes for a main class / service via separate libraries.
 *  
 * @author Holger Jan&szlig;en
 *
 */
public class BundleClassLoader extends ClassLoader {

	private LibraryClassLoader[] libraryLoaders;
	
	/**
	 * generate a new BundleClassLoader without a parent ClassLoader
	 * 
	 * constructor without parent loader just for unit testing, therefore only package viewable
	 * 
	 * @param loader
	 */
	BundleClassLoader ( LibraryClassLoader loader ) {
		super ( );
		
		initLoaders ( new LibraryClassLoader[] {loader} );
	}
	
	/**
	 * generate a new BundleClassLoader without a parent ClassLoader
	 * 
	 * constructor without parent loader just for unit testing, therefore only package viewable
	 * 
	 * @param lib
	 */
	BundleClassLoader ( LibraryClassLoader[] loaders ) {
		super ();
		
		initLoaders(loaders);		
	}
	
	/**
	 * generate a new BundleClassLoader without a parent ClassLoader
	 * 
	 * constructor without parent loader just for unit testing, therefore only package viewable
	 * 
	 * @param mainLoader
	 * @param depLoaders
	 */
	BundleClassLoader ( LibraryClassLoader mainLoader, Set<LibraryClassLoader> depLoaders ) {
		super ();
		
		initLoaders ( mainLoader, depLoaders );
	}
	
	
	/**
	 * create a simple BundleClassLoader which uses only one library
	 * 
	 * @param parent
	 * @param loader
	 */
	public BundleClassLoader ( ClassLoader parent, LibraryClassLoader loader ) {
		super ( parent );
		
		libraryLoaders = new LibraryClassLoader[] { loader };	
		
		Logger.logSubLibrary(this, loader);
	}
	
	/**
	 * create a BundleClassLoader for a bunch of libraries
	 * the first library of the given array is the main library of this bundle
	 * 
	 * @param parent
	 * @param loadersbs
	 */
	public BundleClassLoader ( ClassLoader parent, LibraryClassLoader[] loadersbs ) {		
		super ( parent );
		
		initLoaders(loadersbs);		
	}

	
	/**
	 * Create a new BundleClassLoader for a set of libraries and a specified main library
	 * It does not matter, if the given set of libraries contains the main library.
	 *   
	 * @param parent
	 * @param mainLoader
	 * @param depLoaders
	 */
	public BundleClassLoader ( ClassLoader parent, LibraryClassLoader mainLoader, Set<LibraryClassLoader> depLoaders) {
		super ( parent );
		initLoaders(mainLoader, depLoaders);		
	}


	/**
	 * just for constructors: copy a given array of LibraryClassLoaders into this loader 
	 * 
	 * @param loaders
	 * 
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	private void initLoaders(LibraryClassLoader[] loaders) throws NullPointerException, IllegalArgumentException {
		if ( loaders == null )
			throw new NullPointerException ();
		if ( loaders.length == 0)
			throw new IllegalArgumentException ("You have to state at least one Library for a BundleClassLoader");
		
		libraryLoaders = new LibraryClassLoader[ loaders.length ];
		for ( int i=0; i<loaders.length; i++ ) {
			libraryLoaders[i] = loaders[i];
			Logger.logSubLibrary(this, loaders[i]);
		}
		
		notifyLoaders ();
	}

	/**
	 * just for constructors: init the array of loaded libraries from a given main library
	 * and a set of sub libraries
	 * 
	 * @param mainLoader
	 * @param depLoaders
	 * 
	 * @throws NullPointerException
	 */
	private void initLoaders(LibraryClassLoader mainLoader, Set<LibraryClassLoader> depLoaders) throws NullPointerException {
		if ( mainLoader  == null ) 
			throw new NullPointerException ();
		
		int size = 1;
		if ( depLoaders != null ) {
			size += depLoaders.size();
			
			if ( depLoaders.contains( mainLoader ))
				size--;
		}
		
		libraryLoaders = new LibraryClassLoader [ size ];
		libraryLoaders[0] = mainLoader;

		
		if ( depLoaders != null) {
			int i = 1;
			Iterator<LibraryClassLoader> it = depLoaders.iterator();
			while ( it.hasNext() ) {
				LibraryClassLoader next = it.next();
				if (! next.equals( mainLoader )) {
					libraryLoaders[i] = it.next();
					Logger.logSubLibrary(this, libraryLoaders[i]);
					i++;
				}
			}
		}
		
		notifyLoaders ();
	}
	
	/**
	 * notify all registered LibraryClassLoaders of this BundleClassLoader
	 */
	private void notifyLoaders () {
		for ( int i=0; i<libraryLoaders.length; i++)
			libraryLoaders[i].registerParentLoader(this);
	}
	
	/**
	 * remove a library class loader from this bundle loader
	 * i.e. for hotplugging a new version
	 * @param ident
	 */
	public void removeLibrary ( LibraryIdentifier ident ) {
		boolean done = false;
		for ( int i=0; i < this.libraryLoaders.length && ! done; i++ )
			if ( libraryLoaders[i].getLibrary().getIdentifier().equals(ident)) {
				removeLibraryLoader (i);
				done = true;
			}
	}
	
	/**
	 * remove a library class loader from this bundle loader
	 * i.e. for hotplugging a new version
	 * 
	 * @param loader
	 */
	public void removeLibraryLoader ( LibraryClassLoader loader ) {
		boolean done = false;
		for ( int i=0; i < this.libraryLoaders.length && ! done; i++ )
			if ( libraryLoaders[i].equals( loader )) {
				removeLibraryLoader ( i);
				done = true;
			}
	}
	
	/**
	 * helper method for the other removeLibraryLoader methods
	 * the real removal takes place here
	 * 
	 * @param index
	 */
	private void removeLibraryLoader ( int index ) {
		LibraryClassLoader[] temp = libraryLoaders;
		libraryLoaders = new LibraryClassLoader[ temp.length-1 ];
		for ( int i=0; i<index; i++ )
			libraryLoaders[i] = temp[i];
		for ( int i=index; i<libraryLoaders.length; i++)
			libraryLoaders[i] = temp[i+1];
		
		temp[index].unregisterParentLopader(this);
	}
	
	
	/**
	 * register a new LibraryClassLoader with this BundleClassLoader
	 * @param loader
	 */
	public void registerLibraryLoader ( LibraryClassLoader loader ) {
		LibraryClassLoader[] temp = libraryLoaders;
		libraryLoaders = new LibraryClassLoader[ temp.length+1 ];
		for ( int i=0; i<temp.length; i++ )
			libraryLoaders[i] = temp[i];
		libraryLoaders[libraryLoaders.length-1] = loader;
		
		loader.registerParentLoader(this);
	}
	
	
	/**
	 * 
	 * @return identifier of the main library of this bundle class loader
	 */
	public LibraryIdentifier getMainLibraryIdentifier () {
		return libraryLoaders[0].getLibrary().getLibraryIdentifier();
	}
	
	/**
	 * get an array with all identifiers for the libraries registered to this BundleClassLoader
	 * 
	 * @return array with the identifiers of all registered libraries
	 */
	public LibraryIdentifier[] getLibraryIdentifiers () {
		LibraryIdentifier[] result = new LibraryIdentifier [ libraryLoaders.length ];
		
		for ( int i=0; i<result.length; i++ )
			result[i] = libraryLoaders[i].getLibrary().getLibraryIdentifier();
		
		return result;
	}
	
	@Override
	public URL getResource ( String resourceName ) {
		// just delegate the resource loading to the first registered LibraryClassLoader
		// via {@link findResource} all other child loaders will be asked as well
		return libraryLoaders[0].getResource ( resourceName ) ;
	}
	
	
	/**
	 * Method to actually find a resource.
	 * Currently, a class using this class loader may only get resources from its own jar.
	 *
	 * @param resourceName
	 * @param calledFromChild
	 * 
	 * @return an URL
	 */
	public URL findResource( String resourceName, LibraryClassLoader calledFromChild ) {
		for ( int i=0; i<libraryLoaders.length; i++ ) {
			if ( libraryLoaders[i] != calledFromChild ) { 
				try {
					return libraryLoaders[i].getResource(resourceName, false);
				} catch ( Exception e ) {
					System.err.println( "some other than NotFoundException: " + e );
				}
			}
		}
		
		// ok, delegate to upper class?! - or the parent?
		// parent findResource is not visible!
		return super.findResource ( resourceName );
	}
	
		
	@Override
	public Class<?> loadClass ( String className ) throws ClassNotFoundException {
		// delegate class loading to the first registered LibraryClassLoader since
		// the bundle class loader does no actual class loading itself.
		//
		// via loadClass ( String, boolean, LibraryClassLoader) all other child LibraryClassLoaders
		// will be asked as well.
		Logger.logLoading(this, className, null, null);
		
		try {
			Class<?> result = libraryLoaders[0].loadClass( className );
			
			Logger.logLoading(this, className, true, null);
			return result;
		} catch ( ClassNotFoundException e ) {
			Logger.logLoading(this, className, false, null);
			throw e;
		}
	}

	
	/**
	 * Will be called by a child LibraryClassLoader if a class cannot be found there.
	 * 
	 * The parameter for the child library is necessary to prevent loops of loadClass calls.
	 * 
	 * @param className
	 * @param resolve
	 * @param child
	 * @return	the loaded class
	 * @throws ClassNotFoundException
	 */
	public Class<?> loadClass ( String className, boolean resolve, LibraryClassLoader child ) throws ClassNotFoundException {
		Logger.logLoading(this, className, null, "by child " + child.getLibrary().getIdentifier() + " - try " + libraryLoaders.length + " children");
		
		Class<?> result = null; 
		for ( int i=0; i<libraryLoaders.length; i++ ) {
			if ( libraryLoaders[i] != child ) {
				Logger.logLoading(this, className, null, "by child " + child.getLibrary().getIdentifier() + " - try child " + i + libraryLoaders[i].getLibrary().getIdentifier());
				
				try {
					result = libraryLoaders[i].loadClass ( className, resolve, false );
					if ( result != null) {
						Logger.logLoading(this, className, true, "by child" + child.getLibrary().getIdentifier());					
						return result;
					}
				} catch ( ClassNotFoundException e ) {
					// just try the next one!
				}
			}
		}
		
		Logger.logLoading(this, className, false, "by child " + child.getLibrary().getIdentifier());					
		throw new ClassNotFoundException ();
	}

	
	/**
	 * get an array with all registered LibraryLoadres
	 * @return	an array with all registered library loaders
	 */
	public LibraryClassLoader[] getLibraryLoaders() {
		return libraryLoaders.clone();
	}
	
	
}
