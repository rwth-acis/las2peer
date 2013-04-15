package i5.las2peer.tools;

/**
 * a simple thread waiting for a result
 * 
 * @author Holger Janssen
 * @version $Revision: 1.3 $, $Date: 2013/02/25 00:31:20 $
 *
 * @param <T>
 */
public class WaiterThread<T> extends Thread {

	
	private int waitingTime = 10000; // default: 10 seconds
	
	
	private Exception exception = null;
	
	private T result = null;
	
	
	private boolean collected = false;
	private boolean finished = false;
	private boolean stopped = false;
	
	
	/**
	 * create a new waiter thread with an arbitrary time out value
	 * @param waitingTime
	 */
	public WaiterThread ( int waitingTime ) {
		this.waitingTime = waitingTime;
	}
	
	/**
	 * create a new WaiterThread with a default timeout of 10 seconds
	 */
	public WaiterThread ( ) {}
	
	
	/**
	 * the main waiting method
	 */
	public void run () {
		try {
			synchronized( this ) {
				this.wait( waitingTime );
			}
			System.out.println( "out of wait!");
		} catch (InterruptedException e) {
			System.out.println ("interrupted!");
		}
		
		
		if ( ! stopped )
			finished = true;
	}
	
	/**
	 * stop the main thread from waiting
	 */
	public void stopWaiting () {
		stopped = true;
		notifyAll();
	}
	
	/**
	 * hand over the result to this WaiterThread
	 * @param result
	 */
	public void collectResult ( T result ) {
		synchronized ( this ) {
			this.result = result;
			this.collected = true;
			
			System.out.println ( "notifying!");
			this.notifyAll();
		}
	}
	
	/**
	 * hand over an (occurred) exception to this WaiterThread 
	 * 
	 * @param e 
	 */
	public void collectException ( Exception e ) {
		synchronized ( this ) {
			this.exception = e;
			notifyAll();
		}
	}
	
	/**
	 * 
	 * @return the collected result, null, if no one has been 
	 */
	public T getResult () {
		return result;
	}
	
	/**
	 * 
	 * @return a collected exception (if any was collected)
	 */
	public Exception getException () {
		return exception;
	}
	
	/**
	 * 
	 * @return true, if the thread is finished
	 */
	public boolean isFinished () {
		return finished;
	}
	
	
	/**
	 * 
	 * @return true, if the thread is finished and a result is collected
	 */
	public boolean isSuccess () {
		synchronized ( this ) {
			return isFinished() && collected;
		}
	}
	
	/**
	 * 
	 * @return true, if a result has been collected
	 */
	public boolean hasResult () {
		return result != null;
	}


	/**
	 * has this thread time out during waiting for
	 * a result?
	 * @return true, if the thread is timed out without collecting a result
	 */
	public boolean isTimedOut() {
		synchronized ( this ) {
			return isFinished() && ! collected && ! hasException();
		}
	}


	/**
	 * has this thread collected an exception ?
	 * @return true, if an exception has been collected
	 */
	public boolean hasException() {
		return exception != null;
	}
	
	
	
}

