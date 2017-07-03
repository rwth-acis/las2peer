package i5.las2peer.persistency;

/**
 * This interface can be used to implement a handler that recieves exceptions occurring during network operations.
 *
 */
public interface StorageExceptionHandler {

	/**
	 * This method is called in its own thread to handle the given exception.
	 *
	 * @param e An execption that occurred during a network operation. Usually the reason or metadata should be known
	 *            from the surrounding context and should be provided to this handler, too.
	 */
	public void onException(Exception e);

}
