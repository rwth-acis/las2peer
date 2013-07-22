package i5.las2peer.p2p.pastry;

import i5.las2peer.p2p.TimeoutException;

import java.util.Date;
import java.util.Vector;

import rice.Continuation;


/**
 * A continuation for putting artifacts into the past p2p storage
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class PastPutContinuation implements Continuation<Boolean[] ,Exception>{
	
	public static final int SLEEP_MS = 500;
	
	public static final int MAX_WAIT = 10000;
	
	private Vector<Exception> exceptions = new Vector<Exception>();
	
	private int countSuccess = 0;
	private int countFailures = 0;
	
		
	@Override
	public void receiveException(Exception ex) {
		synchronized ( this ) {
			exceptions.add ( ex );
			countFailures ++;
			
			notifyAll();
		}
	}

	@Override
	public void receiveResult(Boolean[] result) {
		synchronized ( this ) {
			for ( int i=0; i< result.length; i++)
				if ( result[i] )
					countSuccess ++;
				else
					countFailures ++;
			
			notifyAll();
		}
	}
	
	
	/**
	 * how many successful storages have been reported
	 * @return	number of successful storages
	 */
	public int getNumberOfSuccesses () {
		synchronized ( this ) {
			return countSuccess;
		}
	}
	
	/**
	 * how many storage failures have been reported?
	 * @return	number of storage failures
	 */
	public int getNumberOfFailuers () { 
		synchronized ( this ) {
			return countFailures;
		}
	}
	
	
	/**
	 * get any exception which occurred during the retrieval
	 * @return	array with all exceptions during storage attempts
	 */
	public Exception[] getExceptions () { 
		synchronized ( this ) {
			return exceptions.toArray ( new Exception[0] );
		}
	}
	
	/**
	 * has an exception occurred instead of successfully retrieving the result
	 * @return	true, if any exception occured
	 */
	public boolean hasException () { 
		synchronized ( this ) {
			return exceptions.size() > 0; 
		}
	}
	
	/**
	 * is the continuation finished? - Either successfully or with an exception
	 * @return	true, if storage procedure has finished
	 */
	public boolean isFinished () {
		synchronized ( this ) {
			return countSuccess > 0;
		}
	}
	
	/**
	 * do a non busy waiting to the result
	 * 
	 * @throws InterruptedException
	 */
	public void waitForResult () throws InterruptedException {
		synchronized ( this ) {
			long time = new Date().getTime();
			long now = new Date().getTime();
			
			while ( (! isFinished()) && ((now-time)<MAX_WAIT) ) {
				long diff = MAX_WAIT - now + time;
				//System.out.println( "    continuation sleeping " + diff);
				wait ( diff );
				now = new Date().getTime();
			}
			
			//System.out.println("   continuation back after " + (now-time));
			
			if ( ! isFinished () )
				receiveException(new TimeoutException ( "I've waited long enough!!"));
		}
	}

	
	/**
	 * is this put operation successful?
	 * @return	true, if success has been reported
	 */
	public boolean isSuccess() {
		synchronized ( this ) {
			return countSuccess > 0;
		}
	}
}
