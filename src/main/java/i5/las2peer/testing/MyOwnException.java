package i5.las2peer.testing;


/**
 * a simple exception for testing, thrown inside the {@link TestService}
 * 
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
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
