package i5.las2peer.api;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;

import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.persistency.Envelope;

/**
 * This interface can be used to implement a handler that is called if a collision occurs during a store operation.
 */
public interface StorageCollisionHandler {

	/**
	 * This method is called if an envelope is already known for the given identifier and version.
	 *
	 * @param toStore This is the envelope that was requested to be stored in the network.
	 * @param inNetwork This is the colliding envelope that was fetched from the network.
	 * @param numberOfCollisions This is an increasing counter on how often this method was called. It should be a hint
	 *            to the developer to reconsider his storage structure if this value reaches often high values.
	 * @return Returns the merged content from both envelopes, which is automatically wrapped in a new store operation.
	 * @throws StopMergingException If there should be made no further merging attempt.
	 */
	public Serializable onCollision(Envelope toStore, Envelope inNetwork, long numberOfCollisions)
			throws StopMergingException;

	public List<PublicKey> mergeReaders(HashMap<PublicKey, byte[]> toStoreReaders,
			HashMap<PublicKey, byte[]> inNetworkReaders);

}
