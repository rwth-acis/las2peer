package i5.las2peer.security;

/**
 * Basic exception thrown by agents on any problem.
 * 
 * 
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
