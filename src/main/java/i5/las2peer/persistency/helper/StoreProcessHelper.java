package i5.las2peer.persistency.helper;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.persistency.Envelope;

public class StoreProcessHelper implements StorageStoreResultHandler, StorageCollisionHandler, StorageExceptionHandler {

	private Exception exception;
	private Integer successfulOperations = -1;

	@Override
	public void onResult(Serializable serializable, int successfulOperations) {
		synchronized (this) {
			this.successfulOperations = successfulOperations;
		}
	}

	@Override
	public Serializable onCollision(Envelope toStore, Envelope inNetwork, long numberOfCollisions)
			throws StopMergingException {
		synchronized (this) {
			exception = new EnvelopeAlreadyExistsException(
					"Envelope with this version already exists in network! Use a collision handler or increment version.");
			// stop merging process
			throw new StopMergingException();
		}
	}

	@Override
	public List<PublicKey> mergeReaders(HashMap<PublicKey, byte[]> toStoreReaders,
			HashMap<PublicKey, byte[]> inNetworkReaders) {
		// nothing to merge here in store operations there should be nothing to merge
		return new ArrayList<>();
	}

	@Override
	public void onException(Exception e) {
		synchronized (this) {
			if (exception == null) {
				exception = e;
			}
		}
	}

	public Integer getResult() throws Exception {
		synchronized (this) {
			if (exception != null) {
				throw exception;
			}
			return successfulOperations;
		}
	}

}
