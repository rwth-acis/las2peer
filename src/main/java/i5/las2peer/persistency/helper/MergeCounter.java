package i5.las2peer.persistency.helper;

import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.persistency.Envelope;

/**
 * This class is used in the collision handling process. It counts the number of collisions (merging attempts) and helps
 * finding a reasonable limit to terminate the merging process.
 *
 * To terminate the merging process a {@link i5.las2peer.api.StorageCollisionHandler} should throw an
 * {@link StopMergingException}.
 *
 */
public class MergeCounter {

	private long counter;

	/**
	 * default constructor
	 */
	public MergeCounter() {
		counter = 1;
	}

	/**
	 * This method is called on each merging attempt and increases the counter.
	 *
	 * @throws StopMergingException If the counter is greater than the {@value Envelope#MAX_UPDATE_CYCLES} to prevent
	 *             some infinite lookps.
	 */
	public void increase() throws StopMergingException {
		synchronized (this) {
			if (counter >= Long.MAX_VALUE - Envelope.MAX_UPDATE_CYCLES) {
				throw new StopMergingException(counter);
			}
			counter++;
		}
	}

	/**
	 * Gets the current value of this counter.
	 *
	 * @return Returns the value.
	 */
	public long value() {
		synchronized (this) {
			return counter;
		}
	}

}
