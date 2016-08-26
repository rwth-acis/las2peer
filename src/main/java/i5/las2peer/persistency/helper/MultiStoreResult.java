package i5.las2peer.persistency.helper;

import java.io.Serializable;
import java.util.ArrayList;

import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.StorageException;

/**
 * This class is used to handle the multiple store results generated from a parted artifact insert. It provides the
 * result handlers for the insert process itself and methods to wait for the result or an exception. Both can be
 * retrieved via methods, too.
 */
public class MultiStoreResult implements StorageStoreResultHandler, StorageExceptionHandler {

	private final int parts;
	private ArrayList<Integer> results;
	private int minSuccessfulOperations;
	private boolean failed;
	private Exception exception;

	/**
	 * Initiates the store result container for the given number of parts.
	 *
	 * @param parts The number of parts that must be stored to complete this store operation.
	 */
	public MultiStoreResult(int parts) {
		this.parts = parts;
		results = new ArrayList<>(parts);
		minSuccessfulOperations = Integer.MAX_VALUE;
		failed = false;
	}

	@Override
	public void onResult(Serializable serializable, int successfulOperations) {
		synchronized (this) {
			results.add(successfulOperations);
			if (successfulOperations < minSuccessfulOperations) {
				minSuccessfulOperations = successfulOperations;
			}
			if (minSuccessfulOperations < 1) {
				failed = true; // if one part fails, we're busted
				exception = new StorageException("Artifact part insert failed!");
			}
		}
	}

	@Override
	public void onException(Exception e) {
		synchronized (this) {
			if (!failed) { // if this result is already failed ignore further errors
				failed = true;
				exception = e;
			}
		}
	}

	/**
	 * Checks if the underlying store result is completed or not.
	 *
	 * @return Returns {@code true} if the store operation is finished.
	 */
	public boolean isDone() {
		synchronized (this) {
			if (failed) {
				return failed;
			} else {
				return results.size() >= parts; // bigger should equal to failed actually
			}
		}
	}

	/**
	 * Gets the minimal number of successful operations for any part of this store operation.
	 *
	 * @return Returns the minimal number of replications.
	 */
	public int getMinSuccessfulOperations() {
		synchronized (this) {
			return minSuccessfulOperations;
		}
	}

	/**
	 * Gets the exception from this store operation.
	 *
	 * @return Returns an exception or {@code null} if there is none.
	 */
	public Exception getException() {
		synchronized (this) {
			return exception;
		}
	}

}
