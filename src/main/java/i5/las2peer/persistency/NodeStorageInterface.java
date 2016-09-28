package i5.las2peer.persistency;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;

public interface NodeStorageInterface extends L2pStorageInterface {

	/**
	 * Stores the given Envelope in the network. The content is signed with the key from the given author. If an
	 * exception occurs its wrapped as StorageException. With this method collisions are handled by throwing an
	 * {@link EnvelopeAlreadyExistsException}. This method uses the default timeout defined by the acting node.
	 * 
	 * @param envelope The Envelope to store in the network.
	 * @param author The author that is used to sign the content.
	 * @throws StorageException If an issue with the storage occurs.
	 */
	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException;

	/**
	 * Fetches the latest version for the given identifier from the network. This method uses the default timeout
	 * defined by the acting node.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @return Returns the fetched Envelope from the network.
	 * @throws ArtifactNotFoundException If no envelope or any part of it was not found in the network.
	 * @throws StorageException If an issue with the storage occurs.
	 */
	public Envelope fetchEnvelope(String identifier) throws ArtifactNotFoundException, StorageException;

}
