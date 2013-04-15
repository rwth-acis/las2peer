package i5.las2peer.communication;

import java.io.Serializable;

/**
 * a simple message content for an {@link Message} indicating an exception
 * thrown via an remote method invocation
 *  
 * @author Holger Janssen
 * @version $Revision: 1.2 $, $Date: 2013/02/12 18:10:24 $
 *
 */
public class RMIExceptionContent implements Serializable {
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3811848605195392152L;
	private Exception exception;

	public RMIExceptionContent ( Exception content ) {
		exception = content;
	}
	
	public Exception getException () { return exception; }
	
	
}
