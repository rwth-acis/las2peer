package i5.las2peer.persistency.helper;

import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.StorageEnvelopeHandler;
import i5.las2peer.persistency.StorageExceptionHandler;

public class FetchEnvelopeHelper implements StorageEnvelopeHandler, StorageExceptionHandler {

	private Exception exception;
	private EnvelopeVersion result;

	@Override
	public void onEnvelopeReceived(EnvelopeVersion result) {
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

	public EnvelopeVersion getResult() throws Exception {
		synchronized (this) {
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}

}
