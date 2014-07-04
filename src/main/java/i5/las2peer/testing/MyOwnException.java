package i5.las2peer.testing;


/**
 * A simple exception for testing, thrown inside the {@link TestService}.
 * 
 * 
 *
 */
public class MyOwnException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6139168295331054554L;

	
	public MyOwnException ( String message ) {
		super ( message );
	}
	
}
