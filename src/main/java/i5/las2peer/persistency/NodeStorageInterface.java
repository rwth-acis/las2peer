package i5.las2peer.persistency;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;

public interface NodeStorageInterface extends L2pStorageInterface {

	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException;

	public Envelope fetchEnvelope(String identifier) throws ArtifactNotFoundException, StorageException;

}
