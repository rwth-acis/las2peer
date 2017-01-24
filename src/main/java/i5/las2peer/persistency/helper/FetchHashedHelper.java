package i5.las2peer.persistency.helper;

import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.persistency.AbstractArtifact;
import i5.las2peer.persistency.HashedArtifact;
import i5.las2peer.persistency.StorageArtifactHandler;

public class FetchHashedHelper implements StorageArtifactHandler, StorageExceptionHandler {

	private Exception exception;
	private HashedArtifact result;

	@Override
	public void onReceive(AbstractArtifact result) {
		if (result instanceof HashedArtifact) {
			synchronized (this) {
				this.result = (HashedArtifact) result;
			}
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

	public HashedArtifact getResult() throws Exception {
		synchronized (this) {
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}

}
