package i5.las2peer.testing.services.helperClasses;

import java.io.Serializable;


/**
 * a simple serializable class storing just an integer value for demonstration and testing purposes
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/02/16 23:43:57 $
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
