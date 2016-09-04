package i5.las2peer.persistency;

import java.io.Serializable;

import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public interface NodeStorageInterface extends L2pStorageInterface {

	public Envelope createEnvelope(String identifier, Serializable content, Agent reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException;

	public Envelope fetchEnvelope(String identifier) throws ArtifactNotFoundException, StorageException;

}
