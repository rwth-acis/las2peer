package i5.las2peer.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

/**
 * Base (abstract) super type for classes that may be configurable via property files.
 * 
 * The basic idea is, that the runtime system will look for .property files in the current runtime
 * directory or the subdirectories ./config and ./properties for a file named
 * as the sub class of Configurable (including the package name).
 * 
 * All protected (non-static) fields can be set via the {@link #setFieldValues} method.
 * This won't be done automatically, thins subclasses may want to add some extra configuration 
 * behavior.
 *  
 * @author Holger Jan&szlig;en
 *
 */
public abstract class Configurable {
	
	
	/**
	 * Used to determine, if this service should be monitored.
	 * Can be overwritten by service configuration file.
	 * Deactivated per default.
	 */
	protected boolean monitor = false;
	
	/**
	 * Tries to find a (service) class specific property file
	 * 
	 * The following dirs will be checked:
	 * <ul>
	 * 	<li>./</li>
	 *  <li>./config/</li>
	 *  <li>./properties/</li>
	 * </ul>
	 * 
	 * The name of the property file is the name of the implementing service class
	 * followed by .properties.
	 * 
	 * @return hashtable with all property entries
	 */
	protected Hashtable<String, String> getProperties () {
		Hashtable<String, String> result = new Hashtable<String, String>();
		
		String propFile = findPropertyFile ();
		if ( propFile == null ) 
			return result;
		
		try {
			Properties props = new Properties();
			props.load(new FileInputStream ( propFile ));
						
			for ( Object propname: props.keySet() )
				result.put( (String) propname, (String) props.get(propname)); 
		} catch (FileNotFoundException e) {
			System.err.println( "Unable to open property file " + propFile);
		} catch (IOException e) {
			System.err.println( "Error opening property file " + propFile + ": " + e.getMessage());
		}
		
		return result;			
	}
	

	/**
	 * Sets a field to the given value.
	 * @param f
	 * @param value
	 * 
	 * @throws IllegalAccessException
	 */
	private void setField ( Field f, String value ) throws IllegalAccessException {
		Class<?> fieldType = f.getType();
		
		if ( fieldType.equals( String.class ) ) {
			f.set( this, value);
		} else if ( fieldType.equals( long.class ) ) {
			f.setLong (this, Long.parseLong( value ));
		} else if ( fieldType.equals ( int.class )) {
			f.setInt( this, Integer.parseInt( value ) );
		} else if ( fieldType.equals ( short.class )) {
			f.setShort(this, Short.parseShort(value));
		} else if ( fieldType.equals( boolean.class) ) {
			f.setBoolean(this, Boolean.parseBoolean(value));
		} else if ( fieldType.equals (float.class)) {
			f.setFloat(this, Float.parseFloat(value));
		} else if ( fieldType.equals( double.class)) {
			f.setDouble(this, Double.parseDouble( value) );
		} else {
			System.err.println( "Unknown class of field: " + f.getName() + ": " + f.getType() );
		}
	}
	
	
	/**
	 * Sets all field values from the classes property file.
	 * This method uses {@link #getProperties} to get the value stored in the classes property file
	 * to set all fields with the name of the properties.
	 * 
	 * Note that the name "monitor" is reserved to switch the monitoring on and off.
	 */
	protected void setFieldValues () {
		setFieldValues (null);
	}
	
	
	/**
	 * Sets all field values from the classes property file.
	 * This method uses {@link #getProperties} to get the value stored in the classes property file
	 * to set all fields with the name of the properties.
	 * Also checks, if the field contains the value for the monitoring switch and sets it if so.

	 * All fields mentioned in <i>except</i> will be left out. 
	 * 
	 */
	protected void setFieldValues ( Set<String> except ) {
		Hashtable<String, String> props = getProperties ();
		
		for ( String key: props.keySet() ) {
			if (key.equals("monitor")){
				this.monitor = 	Boolean.parseBoolean(props.get(key));
				continue;
			}
			if ( (except != null && except.contains(key))){
				continue;
			}
			try {
				Field f = getClass().getDeclaredField(key);
				if (! Modifier.isFinal(f.getModifiers())
						&& ! Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					setField ( f, props.get(key));
					System.out.println ( "Class: " + getClass().getSimpleName() + " using: " + key + " -> " + props.get(key));
				}
			} catch ( NoSuchFieldException e ) {
				System.err.println( "field not found: " + key );
			} catch ( IllegalAccessException e ) {
				System.err.println( "illegal access: " + key );
				e.printStackTrace();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}	
		
	}

	
	/**
	 * Looks for a property file named after the service class calling this method.
	 * 
	 * @return name of a property file
	 */
	private String findPropertyFile () {
		String filename = getClass().getName() + ".properties";
		String [] testDirs = new String[] { "./", "config/", "properties/" };
		
		for ( String dir : testDirs )
			if ( new File( dir + filename).exists() )
				return dir + filename;
		
		return null;
	}	
}
