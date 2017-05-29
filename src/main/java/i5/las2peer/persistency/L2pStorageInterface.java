package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Collection;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public interface L2pStorageInterface {

	/**
	 * Creates a new version of an Envelope. The Envelope uses by default the start version number.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param authorPubKey The authors public key. Validated on store operation.
	 * @param content The actual content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			AgentImpl... readers) throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates a new version of an Envelope. The Envelope uses by default the start version number.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param authorPubKey The authors public key. Validated on store operation.
	 * @param content The actual content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createEnvelope(String identifier, PublicKey authorPubKey, Serializable content,
			Collection<?> readers) throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates an continuous version instance for the given Envelope. This method copies the reader list from the
	 * previous Envelope instance.
	 * 
	 * @param previousVersion The previous version of the Envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates an continuous version instance for the given Envelope.
	 * 
	 * @param previousVersion The previous version of the Envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, AgentImpl... readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates an continous version instance for the given Envelope.
	 * 
	 * @param previousVersion The previous version of the Envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @param readers An arbitrary number of Agents, who are allowed to read the content.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates a new version of an unencrypted Envelope. The Envelope uses by default the start version number.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param authorPubKey The authors public key. Validated on store operation.
	 * @param content The updated content that should be stored.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createUnencryptedEnvelope(String identifier, PublicKey authorPubKey, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Creates an continous unencrypted version instance for the given Envelope.
	 * 
	 * @param previousVersion The previous version of the Envelope that should be updated.
	 * @param content The updated content that should be stored.
	 * @return Returns the Envelope instance.
	 * @throws IllegalArgumentException If the given identifier is {@code null}, the version number is below the start
	 *             version number or too high.
	 * @throws SerializationException If a problem occurs with object serialization.
	 * @throws CryptoException If an cryptographic issue occurs.
	 */
	public EnvelopeVersion createUnencryptedEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException;

	/**
	 * Stores the given Envelope in the network. The content is signed with the key from the given author. If an
	 * exception occurs it's wrapped as StorageException. With this method collisions are handled by throwing an
	 * {@link EnvelopeAlreadyExistsException}.
	 * 
	 * @param Envelope The Envelope to store in the network.
	 * @param author The author that is used to sign the content.
	 * @param timeoutMs A timeout after that an {@link EnvelopeException} is thrown.
	 * @throws EnvelopeAlreadyExistsException If an Envelope with the given identifier and version is already known in
	 *             the network.
	 * @throws EnvelopeException If an issue with the storage occurs.
	 */
	public void storeEnvelope(EnvelopeVersion Envelope, AgentImpl author, long timeoutMs)
			throws EnvelopeAlreadyExistsException, EnvelopeException;

	/**
	 * Stores the given Envelope in the network. The content is signed with the key from the given author. If an
	 * exception occurs the operation is canceled and the exception handler is called. Same for collisions. If the
	 * operations is completed the result handler is called.
	 * 
	 * @param Envelope The Envelope to store in the network.
	 * @param author The author that is used to sign the content.
	 * @param resultHandler A result handler that is called, if the operation terminates.
	 * @param collisionHandler A collision handler that is called, if an Envelope with the given identifier and version
	 *            already exists.
	 * @param exceptionHandler An exception handler that is called, if an exception occurs.
	 */
	public void storeEnvelopeAsync(EnvelopeVersion Envelope, AgentImpl author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler);

	/**
	 * Fetches the latest version for the given identifier from the network.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param timeoutMs A timeout after that an {@link EnvelopeException} is thrown.
	 * @return Returns the fetched Envelope from the network.
	 * @throws EnvelopeNotFoundException If no envelope or any part of it was not found in the network.
	 * @throws EnvelopeException If an issue with the storage occurs.
	 */
	public EnvelopeVersion fetchEnvelope(String identifier, long timeoutMs)
			throws EnvelopeNotFoundException, EnvelopeException;

	/**
	 * Fetches the latest version for the given identifier from the network.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @param envelopeHandler A result handler that is called, if the operation terminates.
	 * @param exceptionHandler An exception handler that is called, if an exception occurs.
	 */
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler);

	/**
	 * Removes the envelope with the given identifier from the network.
	 * 
	 * @param identifier An unique identifier for the Envelope.
	 * @throws EnvelopeNotFoundException If no envelope or any part of it was not found in the network.
	 * @throws EnvelopeException If an issue with the storage occurs.
	 */
	public void removeEnvelope(String identifier) throws EnvelopeNotFoundException, EnvelopeException;

}
