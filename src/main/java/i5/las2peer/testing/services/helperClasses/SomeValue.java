package i5.las2peer.testing.services.helperClasses;

import java.io.Serializable;


/**
 * A simple serializable class storing just an integer value for demonstration and testing purposes.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class SomeValue implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8923511849143558498L;

	
	private int myValue;
	
	
	/**
	 * create a new value instance
	 * 
	 * @param aValue
	 */
	public SomeValue ( int aValue ) {
		myValue = aValue;
	}
	
	/**
	 * get the stored value
	 * @return the stored value
	 */
	public int getValue() {
		return myValue;
	}
	
	
}
