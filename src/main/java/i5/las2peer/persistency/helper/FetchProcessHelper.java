package i5.las2peer.persistency.helper;

import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.persistency.Envelope;

public class FetchProcessHelper implements StorageEnvelopeHandler, StorageExceptionHandler {

	private Exception exception;
	private Envelope result;

	@Override
	public void onEnvelopeReceived(Envelope result) {
		synchronized (this) {
			this.result = result;
		}
	}

	@Override
	public void onException(Exception e) {
		synchronized (this) {
			if (exception == null) {
				exception = e;
			}
		}
	}

	public Envelope getResult() throws Exception {
		synchronized (this) {
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}

}
