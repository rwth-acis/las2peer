package i5.las2peer.communication;

import java.io.Serializable;
import java.util.Date;

/**
 * a simple content for ping and pong message
 * 
 * 
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
