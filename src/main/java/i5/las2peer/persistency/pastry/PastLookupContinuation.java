package i5.las2peer.persistency.pastry;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.persistency.StorageLookupHandler;
import rice.Continuation;
import rice.p2p.past.PastContentHandle;

/**
 * This class is required by Past for the lookup process. It provides the interface to receive results and exceptions
 * occurring in the process. If such an event occurs and the respective handler is set it calls the handler in a new
 * thread. This provides some network response time safety as the handler may perform time consuming tasks, but the
 * actual method call is done directly from network stack.
 */
public class PastLookupContinuation implements Continuation<PastContentHandle[], Exception> {

	private final ExecutorService dispatcher;
	private final StorageLookupHandler lookupHandler;
	private final StorageExceptionHandler exceptionHandler;

	public PastLookupContinuation(ExecutorService dispatcher, StorageLookupHandler lookupHandler,
			StorageExceptionHandler exceptionHandler) {
		this.dispatcher = dispatcher;
		this.lookupHandler = lookupHandler;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void receiveResult(PastContentHandle[] result) { // ATTENTION! EXECUTED IN NETWORK THREAD!
		if (lookupHandler == null) {
			return;
		}
		// filter empty handles
		ArrayList<PastContentHandle> handles = new ArrayList<>();
		for (PastContentHandle handle : result) {
			if (handle != null) {
				handles.add(handle);
			}
		}
		// detach further processing from network thread
		dispatcher.execute(new Runnable() {
			@Override
			public void run() {
				lookupHandler.onLookup(handles);
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
