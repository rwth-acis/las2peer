package i5.las2peer.p2p.pastry;

import i5.las2peer.p2p.ArtifactNotFoundException;

import java.util.Date;

import rice.Continuation;
import rice.p2p.past.PastContent;

/**
 * A Continuation for retrieving stored envelopes from the past p2p artifact storage.
 * 
 * For usage, you can either do something else while waiting for the result of the past(ry) retrieval
 * and check the Continuation's state via {@link #isFinished()} or do a (non) blocking wait for 
 * the result via {@link #waitForResult} or  {@link #getResultWaiting}.
 * 
 * @author Holger Janssen
 * @version $Revision: 1.11 $, $Date: 2013/04/15 02:58:58 $
 *
 * @param <T>
 */
public class PastGetContinuation<T> implements Continuation<PastContent,Exception>{
	
	public static final int DEFAULT_SLEEP_MS = 500;
	
	//private int randomId = new Random().nextInt();
	
	
	/**
	 * sleep interval in ms during "active" waiting for a result
	 */
	//private int sleepMs = DEFAULT_SLEEP_MS;
	
	/**
	 * the actual content 
	 */
	private T content = null;
	
	/**
	 * a possible exception occurred during retrieval of the content from the pastry backend
	 */
	private Exception ex = null;
	
	/**
	 * flag: is this continuation finished
	 */
	private boolean finished = false;
	
	

	/**
	 * Simple reference to the nested class.
	 * This is necessary for checking the class of a result received from the pastry backend.
	 * 
	 */
	private Class<T> genericClass;

	private int timeout;
	
	
	
	/**
	 * create a new contiuation for retrieval of an artifact of the given type from the past(ry) backend
	 * 
	 * @param genericClass	reference to the class of the expected result
	 */
	public PastGetContinuation ( Class<T> genericClass, int timeout ){
		this.genericClass = genericClass;
		this.timeout = timeout;
	}
	
	/**
	 * create a new continuation for retrieval of an artifact of the given type from the past(ry) backend
	 * 
	 * @param genericClass
	 * @param aMessage
	 */
	public PastGetContinuation ( Class <T> genericClass, int timeout, String aMessage ) {
		this ( genericClass, timeout );
	}
		
	/**
	 * this method will be called by the past(ry) backend, if an exception occurred
	 * during the attempted retrieval of the intended artifact
	 * 	
	 * @param ex	the (external) exception 
	 */
	@Override
	public void receiveException(Exception ex) {
		synchronized ( this ) {
			System.out.println ("got exception");
			
			this.ex = ex;
			finished = true;
			notifyAll();
		}
	}

	/**
	 * this method will be called by the past(ry) backend, if the artifact has been 
	 * feteched succesfully
	 * 
	 * @param content	the artifact fetched from the past(ry) backend
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void receiveResult(PastContent content) {
		synchronized ( this ) {
			try {
				if ( content instanceof ContentEnvelope ) {
					this.content = (T) ((ContentEnvelope)content).getContainedObject();
					if ( ! genericClass.isInstance(this.content)) {
						ex = new PastryStorageException ( "Content type '" + content.getClass().getName() + "' does not fit expected class!", new ClassCastException( "" ));
						this.content = null;
					}
					
				} else if ( content == null ) {
					ex = new ArtifactNotFoundException( "null content");
				} else {
					ex = new PastryStorageException ("expected a ContentEnvelope but got a " + content.getClass().getName());
				}
			} catch ( PastryStorageException e ) {
				ex = e;
				ex.printStackTrace();
			} finally {
				finished = true;
				notifyAll();
			}
		}
	}
	
	/**
	 * get the result (returns null, if not finished)
	 * @return	result of artifact retrieval, null, if not finished
	 */
	public T getResult() {
		return content;
	}
	
	/**
	 * wait for the result and return it
	 * 
	 * throws a possible exception if one occurred
	 * 
	 * @return result of artifact retrieval
	 * @throws Exception
	 */
	public T getResultWaiting () throws Exception {
		waitForResult();
		
		if ( ex != null)
			throw ex;
		
		return content;
	}
	
	/**
	 * get any exception which occurred during the retrieval
	 * @return	exception occurred during artifact retrieval
	 */
	public Exception getException () { return ex ; }
	
	/**
	 * has an exception occurred instead of successfully retrieving the result
	 * @return	true, if any exception occurred
	 */
	public boolean hasException () { return ex != null; }
	
	/**
	 * is the continuation finished? - Either successfully or with an exception
	 * @return	true, if a result or an exception has been received
	 */
	public boolean isFinished () { return finished; }
	
	/**
	 * do a non busy waiting to the result
	 * 
	 * @throws InterruptedException
	 */
	public void waitForResult () throws InterruptedException {
		synchronized ( this ) {
			long timestamp = new Date().getTime();
			boolean timedOut = false;
			
			if ( finished) return;
			
			do {
				long waitFor  = timestamp - (new Date().getTime()) +  timeout;				
				if (waitFor > 0 ) {
					//System.out.println( "   past waiting: " + message + " for " + waitFor + "ms");
					wait ( waitFor );
				} else
					wait ( 500 );
				timedOut = new Date().getTime() - timestamp > timeout;
			} while ( ! finished && ! timedOut );
			//System.out.println( "   past waiting finished : " + message);
			
			if ( timedOut && ! finished ) {
				//System.out.println( "   past waiting timed out! : " + message);
				throw new InterruptedException ( "waiting timed out!");
			}
		}
	}
	
	
	
}
