package i5.las2peer.persistency;

import java.io.Serializable;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public interface ContextStorageInterface extends NodeStorageInterface {

	/**
	 * Creates a new version of an Envelope. The Envelope uses by default the start version number. The content is
	 * signed with the key from the current main agent.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param content The actual content that should be stored.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is null, the version number is below the start version
	 *             number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public Envelope createEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Stores the given Envelope in the network. The content is signed with the key from the current main agent. If an
	 * exception occurs its wrapped as StorageException. With this method collisions are handled by throwing an
	 * {@link i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException}. This method uses the default timeout defined
	 * by the acting node.
	 * 
	 * @param envelope The Envelope to store in the network.
	 * @throws StorageException If an issue with the storage occurs.
	 */
	public void storeEnvelope(Envelope envelope) throws StorageException;

	/**
	 * Fetches the latest version for the given identifier from the network. This method uses the default timeout
	 * defined by the acting node.
	 * 
	 * @param envelope The Envelope to store in the network.
	 * @param timeoutMs A timeout after that an {@link StorageException} is thrown.
	 * @throws StorageException If an issue with the storage occurs.
	 */
	public void storeEnvelope(Envelope envelope, long timeoutMs) throws StorageException;

}
