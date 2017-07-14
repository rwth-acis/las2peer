package i5.las2peer.api.persistency;

/**
 * This interface can be used to implement a handler that is called if a collision occurs during a store operation.
 *
 */
public interface EnvelopeCollisionHandler {
	/**
	 * This method is called if an envelope is already known for the given identifier and version.
	 *
	 * @param toStore This is the envelope that was requested to be stored in the network.
	 * @param inNetwork This is the colliding envelope that was fetched from the network.
	 * @return The merged content from both envelopes, which is automatically wrapped in a new store operation.
	 * @throws MergeFailedException If there should be made no further merging attempt.
	 */
	public Envelope onCollision(Envelope toStore, Envelope inNetwork) throws MergeFailedException;
}
