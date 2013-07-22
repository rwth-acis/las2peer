

package i5.las2peer.httpConnector.coder;

import java.io.Serializable;


/**
 * SomeClass.java
 *
 * @author Holger Jan&szlig;en
 */
public class SomeClass implements Serializable {
	
	private static final long serialVersionUID = 2288806517337791582L;
	protected int  i;
	
	
	public SomeClass () {
		System.out.println("In standard constructor");
		i = -1;
	}
	
	
	public SomeClass ( int i ) {
		this.i = i;
		
		System.out.println("In special constructor");
	}
	
	
	public int getI () {
		return i;
	}
	
		
		
}

