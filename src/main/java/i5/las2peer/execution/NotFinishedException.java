package i5.las2peer.execution;

/**
 * Exception thrown on access to a result of a L2pThread, when the job is not done yet
 * 
 * 
 *
 */
public class NotFinishedException extends L2pServiceException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * create a new exception 
	 * @param message
	 */
	public NotFinishedException ( String message ){
		super ( message );
	}
	
}
