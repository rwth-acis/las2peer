package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class LocalStorage implements L2pStorageInterface {

	private final ConcurrentHashMap<String, EnvelopeVersion> storedEnvelopes = new ConcurrentHashMap<>();

	@Override
	public EnvelopeVersion createEnvelope(String identifier, Serializable content, AgentImpl... readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(identifier, content, Arrays.asList(readers));
	}

	@Override
	public EnvelopeVersion createEnvelope(String identifier, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(identifier, content, readers);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(previousVersion, content);
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, AgentImpl... readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(previousVersion, content, Arrays.asList(readers));
	}

	@Override
	public EnvelopeVersion createEnvelope(EnvelopeVersion previousVersion, Serializable content, Collection<?> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(previousVersion, content, readers);
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(identifier, content, new ArrayList<>());
	}

	@Override
	public EnvelopeVersion createUnencryptedEnvelope(EnvelopeVersion previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new EnvelopeVersion(previousVersion, content, new ArrayList<>());
	}

	@Override
	public void storeEnvelope(EnvelopeVersion envelope, AgentImpl author, long timeoutMs) throws EnvelopeException {
		EnvelopeVersion stored = storedEnvelopes.get(envelope.getIdentifier());
		if (stored != null && envelope.getVersion() == stored.getVersion()) {
			throw new EnvelopeAlreadyExistsException("Duplicate envelope identifier");
		}
		storedEnvelopes.put(envelope.getIdentifier(), envelope);
	}

	@Override
	public void storeEnvelopeAsync(EnvelopeVersion envelope, AgentImpl author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		EnvelopeVersion toStore = envelope;
		// check for collision
		EnvelopeVersion inStorage = storedEnvelopes.get(envelope.getIdentifier());
		if (inStorage != null) {
			if (collisionHandler != null) {
				try {
					long mergedVersion = Math.max(envelope.getVersion(), inStorage.getVersion()) + 1;
					Serializable mergedContent = collisionHandler.onCollision(envelope, inStorage, 1);
					Set<PublicKey> mergedReaders = collisionHandler.mergeReaders(envelope.getReaderKeys().keySet(),
							inStorage.getReaderKeys().keySet());
					Set<String> mergedGroups = collisionHandler.mergeGroups(envelope.getReaderGroupIds(),
							inStorage.getReaderGroupIds());
					try {
						toStore = new EnvelopeVersion(envelope.getIdentifier(), mergedVersion, mergedContent,
								mergedReaders, mergedGroups);
					} catch (IllegalArgumentException | SerializationException | CryptoException e) {
						if (exceptionHandler != null) {
							exceptionHandler.onException(e);
						}
						return;
					}
				} catch (StopMergingException e) {
					return;
				}
			}
		}
		// store new
		try {
			storedEnvelopes.put(envelope.getIdentifier(), toStore);
		} catch (Exception e) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(e);
			}
			return;
		}
		if (resultHandler != null) {
			resultHandler.onResult(envelope, 1);
		}
	}

	@Override
	public EnvelopeVersion fetchEnvelope(String identifier, long timeoutMs) throws EnvelopeException {
		EnvelopeVersion inStorage = storedEnvelopes.get(identifier);
		if (inStorage == null) {
			throw new EnvelopeNotFoundException(identifier);
		} else {
			return inStorage;
		}
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		EnvelopeVersion inStorage = storedEnvelopes.get(identifier);
		if (inStorage == null) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(new EnvelopeNotFoundException(identifier));
			}
		} else {
			if (envelopeHandler != null) {
				envelopeHandler.onEnvelopeReceived(inStorage);
			}
		}
	}

	@Override
	public void removeEnvelope(String identifier) throws EnvelopeException {
		EnvelopeVersion inStorage = storedEnvelopes.remove(identifier);
		if (inStorage == null) {
			throw new EnvelopeNotFoundException(identifier);
		}
	}

}
