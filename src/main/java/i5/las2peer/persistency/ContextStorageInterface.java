package i5.las2peer.persistency;

import java.io.Serializable;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public interface ContextStorageInterface extends NodeStorageInterface {

	public Envelope createEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope) throws StorageException;

	public void storeEnvelope(Envelope envelope, long timeoutMs) throws StorageException;

}
