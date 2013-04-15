package i5.las2peer.communication;

import java.io.Serializable;
import java.util.Date;

/**
 * a simple content for ping and pong message
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/01/14 07:29:53 $
 *
 */
public class PingPongContent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2353759617748130156L;
	public long timestamp; 
	
	public PingPongContent () {
		timestamp = new Date().getTime();
	}
	
	public long getTimestamp () { return timestamp; }
	
}
