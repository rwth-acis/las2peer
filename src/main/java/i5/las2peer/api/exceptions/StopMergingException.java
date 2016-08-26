package i5.las2peer.api.exceptions;

/**
 * This exception should be thrown by a {@link StorageCollisionHandler} if there should be no further merging attempt.
 */
public class StopMergingException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Long collisions;

	/**
	 * Default constructor
	 */
	public StopMergingException() {
		this(null);
	}

	/**
	 * This constructor sets only the message for this exception and leaves the cause to {@code null}.
	 *
	 * @param message A message that describes the error.
	 */
	public StopMergingException(String message) {
		this(message, null);
	}

	/**
	 * This constructor sets only the number of collisions and leaves the message to {@code null}.
	 *
	 * @param collisions The number of collisions that occurred till this point.
	 */
	public StopMergingException(long collisions) {
		this(null, collisions);
	}

	/**
	 * This constructor allows to set message and number of collisions for this exception.
	 *
	 * @param message A message that describes the error.
	 * @param collisions The number of collisions that occurred till this point.
	 */
	public StopMergingException(String message, Long collisions) {
		super(message);
		this.collisions = collisions;
	}

	/**
	 * Gets the number of collisions till this point.
	 *
	 * @return Returns the number of collisions or {@code null}.
	 */
	public Long getCollisions() {
		return collisions;
	}

	@Override
	public String toString() {
		if (getMessage() == null && collisions != null) {
			return super.toString() + "Collisions: " + collisions;
		} else {
			return super.toString();
		}
	}

}
