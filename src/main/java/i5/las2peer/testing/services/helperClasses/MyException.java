package i5.las2peer.testing.services.helperClasses;


/**
 * an exception inside the used library for testing purposes
 * 
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/02/16 23:43:57 $
 *
 */
public class MyException extends Exception {


	/**
	 * 
	 */
	private static final long serialVersionUID = -2389357315935563015L;

	/**
	 * generates a new Exception 
	 * 
	 * @param message
	 */
	public MyException ( String message ) {
		super ( message );
	}
	
}
