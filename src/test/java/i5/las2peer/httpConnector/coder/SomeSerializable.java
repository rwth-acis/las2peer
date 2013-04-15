package i5.las2peer.httpConnector.coder;

import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;


/**
 * The Class SomeSerializable is a class for testing the Serialization methods
 * of the {@link XmlCoder} / {@link XmlDecoder} and to demonstrate how serialization
 * works at all.
 * <br />
 * The class itself has one field (j), which would be serialized by standard
 * java procedures. The field i of the super class {@link SomeClass} won't be
 * serialized at all since SomeClass does not implement Serializable.
 * Therefore, if this value is to be serialized, the class SomeSerializable
 * has to implement the writeObject and readObject methods to care about the
 * i field.
 * <br />
 * Next point in the demonstration is, that the standard constructor of the
 * superclass ({@link SomeClass#()}) is called in the deserialization.
 * It is the standard constructor of the first superclass not implementing
 * Serializable. If this class does not have a standard constructor, an exception
 * is thrown during the deserialization process.
 * <br />
 * Feel free to comment out the constructor of SomeClass or the writeObject and
 * readObject methods of this class to observe the described behaviour while running
 * the {@link TestSerializable} TestCase.
 *
 *
 * @author Holger Jan�en
 * @version $Revision: 1.1 $, $Date: 2013/01/23 20:08:04 $
 */


public class SomeSerializable extends SomeClass implements Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -956756823889928006L;
	
	
	int j = -1;
	
	public SomeSerializable () {
		super (100);
		
		System.out.println("Standard Constructor of SomeSerializable");
	}
	
	
	public SomeSerializable ( int i ) {
		super (i);
		
		j = i+10;
		
		System.out.println("Special Constructor of SomeSerializable");
	}
	
	
	public int getJ () {
		return j;
	}
	
	private void writeObject ( ObjectOutputStream oos ) throws IOException {
		oos.defaultWriteObject();
		
		oos.writeInt ( i );
		
		System.out.println("Writing...");
		
	}
	
	
	private void readObject ( ObjectInputStream oos ) throws ClassNotFoundException, IOException {
		oos.defaultReadObject();
		
		i = oos.readInt ();
		
		System.out.println("Reading...");
	}
		
}

