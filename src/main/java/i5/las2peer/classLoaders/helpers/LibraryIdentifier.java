package i5.las2peer.classLoaders.helpers;



/**
 * a basic class managing a library identifier of the format
 * name-number
 * where number fits the format of {@link LibraryVersion}
 * 
 * 
 *
 */
public class LibraryIdentifier implements Comparable<LibraryIdentifier> {

	private String name;
	private LibraryVersion version = null;
	
	/**
	 * generate a new LibraryIdentifier from its string representation
	 * @param name
	 * @throws IllegalArgumentException
	 */
	public LibraryIdentifier ( String name ) throws IllegalArgumentException {
		int index = name.indexOf (";version=\""); 
		if (  index == -1 )
			throw new IllegalArgumentException ("String does not include version info");
		
		if ( name.charAt(name.length()-1) != '"')
			throw new IllegalArgumentException ( "Version info not in qoutes!");
		
		this.name = name.substring(0, index);
		String versionInfo =  name.substring(index+10, name.length()-1);
		
		this.version = new LibraryVersion ( versionInfo );
	}
	
	/**
	 * generate a new identifier
	 * @param name
	 * @param version
	 * @throws IllegalArgumentException
	 */
	public LibraryIdentifier ( String name, String version ) throws IllegalArgumentException {
		if ( version != null ) {
			this.name = name;
			this.version = new LibraryVersion ( version );
		} else 
			throw new IllegalArgumentException ( "null given as version information");
	}
	
	/**
	 * generate a new library identifier 
	 * 
	 * @param name
	 * @param version
	 */
	public LibraryIdentifier ( String name, LibraryVersion version ) {
		this.name = name;
		this.version = version;
	}
	
	/**
	 * 
	 * @return version of this library
	 */
	public LibraryVersion getVersion () {
		return version;
	}
	
	/**
	 * 
	 * @return name of the library
	 */
	public String getName () {
		return name;
	}
	
	/**
	 * @return a string representation of this identifier
	 */
	public String toString () {
		return name + ";version=\"" + version.toString() + "\"";
	}
	
	
	/**
	 * compares this identifier to another one
	 * @param i
	 * @return true, if the given identifier is the same as this
	 */
	public boolean equals ( LibraryIdentifier i ) {
		return this.toString().equals ( i.toString ());
	}

	
	/**
	 * compares this identifier against other objects
	 * if a string is given, the string representation of this identifier is compared
	 * to the given string
	 *  
	 * @param o
	 * 
	 * @return true, if the given object is an identifier and is the same as this
	 */
	@Override
	public boolean equals ( Object o ) {
		if ( o instanceof LibraryIdentifier )
			return this.equals ( (LibraryIdentifier) o );
		else if ( o instanceof String ) 
			return this.toString().equals( (String) o );
		else
			return false;
	}
	
	/**
	 * checks, if this library matches the given range 
	 * 
	 * @param min
	 * @param max
	 * 
	 * @return true, if this version is included in the given range 
	 */
	public boolean matchesRange ( LibraryVersion min, LibraryVersion max ) {
		return this.version.isBetween(min, max);
	}
	
	/**
	 * checks, if this library matches the given range 
	 * 
	 * @param min
	 * @param max
	 * 
	 * @return	true, if this version is included in the given range
	 */
	public boolean matchesRange ( String min, String max ) {
		return this.version.isBetween( min,  max );
	}
	
	
	/**
	 * since equals is overridden, we should implement an own hashCode.
	 * 
	 * @return	a hash code
	 */
	@Override
	public int hashCode () {
		return (this.toString()).hashCode();
	}

	
	/**
	 * does the given library identifier match this library dependency
	 * 
	 * @param dep
	 * @return	true, if this version fits the given dependency
	 */
	public boolean matches ( LibraryDependency dep ) {
		return dep.fits ( this ); 
	}

	/**
	 * implement Comparable<LibraryDependency> so that the L2pClassLoader may
	 * keep a (sorted) TreeMap of all registered Libraries
	 * 
	 * @return comparison code
	 */
	@Override
	public int compareTo(LibraryIdentifier other) {
		if ( this.getName().equals( other.getName() )) 
			return this.getVersion().compareTo(other.getVersion());
		else
			return this.getName().compareTo( other.getName() );
	}

	
	
	
	

}
