package i5.las2peer.api;

import java.io.Serializable;

/**
 * This interface can be used to implement a handler that receives the result from a store operation.
 */
public interface StorageStoreResultHandler {

	/**
	 * This method is called in its own thread to handle the result of a store operation.
	 *
	 * @param serializable The serializable that was stored in the network.
	 * @param successfulOperations The number of successful insert operations (replications).
	 */
	public void onResult(Serializable serializable, int successfulOperations);

}
