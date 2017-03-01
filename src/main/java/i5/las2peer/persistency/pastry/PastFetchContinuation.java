package i5.las2peer.persistency.pastry;

import java.util.concurrent.ExecutorService;

import i5.las2peer.persistency.AbstractArtifact;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.NetworkArtifact;
import i5.las2peer.persistency.StorageArtifactHandler;
import i5.las2peer.persistency.StorageExceptionHandler;
import rice.Continuation;
import rice.p2p.past.PastContent;

/**
 * This class is required by Past for the fetch process. It provides the interface to receive results and exceptions
 * occurring in the process. If such an event occurs and the respective handler is set it calls the handler in a new
 * thread. This provides some network response time safety as the handler may perform time consuming tasks, but the
 * actual method call is done directly from network stack.
 */
public class PastFetchContinuation implements Continuation<PastContent, Exception> {

	private final ExecutorService dispatcher;
	private final StorageArtifactHandler resultHandler;
	private final StorageExceptionHandler exceptionHandler;

	public PastFetchContinuation(ExecutorService dispatcher, StorageArtifactHandler resultHandler,
			StorageExceptionHandler exceptionHandler) {
		this.dispatcher = dispatcher;
		this.resultHandler = resultHandler;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void receiveResult(PastContent result) { // ATTENTION! EXECUTED IN NETWORK THREAD!
		if (resultHandler == null) {
			return;
		}
		// detach further processing from network thread
		dispatcher.execute(new Runnable() {
			@Override
			public void run() {
				if (result instanceof AbstractArtifact) {
					resultHandler.onReceive((AbstractArtifact) result);
				} else {
					receiveException(new IllegalArgumentException(result.getClass().getCanonicalName()
							+ " is not instance of " + EnvelopeVersion.class.getCanonicalName()));
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
