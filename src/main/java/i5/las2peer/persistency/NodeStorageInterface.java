package i5.las2peer.persistency;

import java.io.Serializable;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public interface NodeStorageInterface extends L2pStorageInterface {

	public Envelope createEnvelope(String identifier, Serializable content, Agent reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

//	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
//			throws IllegalArgumentException, SerializationException, CryptoException;

	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent reader)
			throws IllegalArgumentException, SerializationException, CryptoException;

//	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
//			throws IllegalArgumentException, SerializationException, CryptoException;

//	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
//			throws IllegalArgumentException, SerializationException, CryptoException;

//	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
//			throws IllegalArgumentException, SerializationException, CryptoException;

	public void storeEnvelope(Envelope envelope, Agent author) throws StorageException;

//	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws SharedStorageException;

//	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
//			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler);

	public Envelope fetchEnvelope(String identifier) throws StorageException;

//	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws SharedStorageException;

//	public Envelope fetchEnvelope(String identifier, long version, long timeoutMs) throws SharedStorageException;

//	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
//			StorageExceptionHandler exceptionHandler);

//	public void fetchEnvelopeAsync(String identifier, long version, StorageEnvelopeHandler envelopeHandler,
//			StorageExceptionHandler exceptionHandler);

}
