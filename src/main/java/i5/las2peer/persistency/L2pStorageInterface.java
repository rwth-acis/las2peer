package i5.las2peer.persistency;

import java.io.Serializable;
import java.util.List;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public interface L2pStorageInterface {

	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs)
			throws EnvelopeAlreadyExistsException, StorageException;

	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler);

	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws ArtifactNotFoundException, StorageException;

	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler);

	public void removeEnvelope(String identifier) throws ArtifactNotFoundException, StorageException;

}
