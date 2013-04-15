package i5.las2peer.security;

/**
 * basic exception thrown by agents on any problem
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/01/14 07:29:53 $
 *
 */
public class AgentException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3022805629155395398L;

	public AgentException ( String message ) {
		super ( message );
	}
	
	public AgentException ( String message, Throwable cause ) {
		super ( message, cause );
	}
}
