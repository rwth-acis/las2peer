package i5.las2peer.execution;

import i5.las2peer.security.AgentException;


/**
 * basic exception thrown on service (invocation) problems
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/01/25 13:51:28 $
 *
 */
public class L2pServiceException extends AgentException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2266707294292209691L;
	
	public L2pServiceException ( String message) {
		super ( message );
	}
	
	public L2pServiceException ( String message, Throwable cause) {
		super ( message, cause);
	}
}
