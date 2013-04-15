package i5.las2peer.p2p;


/**
 * a simple exception thrown on time outs
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/28 12:47:31 $
 *
 */
public class TimeoutException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8509715321020571787L;
	
	
	public TimeoutException ( String message ) {
		super ( message );
	}
	
	public TimeoutException () {
		super ();
	}
	
}
