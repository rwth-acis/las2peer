package i5.las2peer.persistency.pastry;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;

import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import rice.Continuation;

/**
 * This class is required by Past for the insert process. It provides the interface to receive results and exceptions
 * occurring in the process. If such an event occurs and the respective handler is set it calls the handler in a new
 * thread. This provides some network response time safety as the handler may perform time consuming tasks, but the
 * actual method call is done directly from network stack.
 */
public class PastInsertContinuation implements Continuation<Boolean[], Exception> {

	private final ExecutorService dispatcher;
	private final StorageStoreResultHandler resultHandler;
	private final StorageExceptionHandler exceptionHandler;
	private final Serializable serializable;

	public PastInsertContinuation(ExecutorService dispatcher, StorageStoreResultHandler resultHandler,
			StorageExceptionHandler exceptionHandler, Serializable serializable) {
		this.dispatcher = dispatcher;
		this.resultHandler = resultHandler;
		this.exceptionHandler = exceptionHandler;
		this.serializable = serializable;
	}

	@Override
	public void receiveResult(Boolean[] result) { // ATTENTION! EXECUTED IN NETWORK THREAD!
		if (resultHandler == null) {
			return;
		}
		// detach further processing from network thread
		dispatcher.execute(new Runnable() {
			@Override
			public void run() {
				try {
					int successfulSaveOperations = 0;
					for (Boolean b : result) {
						if (b != null && b.booleanValue()) {
							successfulSaveOperations++;
						}
					}
					resultHandler.onResult(serializable, successfulSaveOperations);
				} catch (Exception e) {
					receiveException(e);
				}
			}
		});
	}

	@Override
	public void receiveException(Exception exception) { // ATTENTION! EXECUTED IN NETWORK THREAD!
		if (exceptionHandler == null) {
			return;
		}
		// detach further processing from network thread
		dispatcher.execute(new Runnable() {
			@Override
			public void run() {
				exceptionHandler.onException(exception);
			}
		});
	}

}
