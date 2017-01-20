package i5.las2peer.persistency;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.security.Agent;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class LocalStorage implements L2pStorageInterface {

	private final ConcurrentHashMap<String, Envelope> storedEnvelopes = new ConcurrentHashMap<>();

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, Agent... readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(identifier, content, Arrays.asList(readers));
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, Agent... readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content, Arrays.asList(readers));
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(identifier, content, new ArrayList<>());
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content, new ArrayList<>());
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException {
		Envelope stored = storedEnvelopes.get(envelope.getIdentifier());
		if (stored != null && envelope.getVersion() == stored.getVersion()) {
			throw new EnvelopeAlreadyExistsException("Duplicate envelope identifier");
		}
		storedEnvelopes.put(envelope.getIdentifier(), envelope);
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		Envelope toStore = envelope;
		// check for collision
		Envelope inStorage = storedEnvelopes.get(envelope.getIdentifier());
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
						toStore = new Envelope(envelope.getIdentifier(), mergedVersion, mergedContent, mergedReaders,
								mergedGroups);
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
	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws StorageException {
		Envelope inStorage = storedEnvelopes.get(identifier);
		if (inStorage == null) {
			throw new ArtifactNotFoundException(identifier);
		} else {
			return inStorage;
		}
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		Envelope inStorage = storedEnvelopes.get(identifier);
		if (inStorage == null) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(new ArtifactNotFoundException(identifier));
			}
		} else {
			if (envelopeHandler != null) {
				envelopeHandler.onEnvelopeReceived(inStorage);
			}
		}
	}

	@Override
	public void removeEnvelope(String identifier) throws StorageException {
		Envelope inStorage = storedEnvelopes.remove(identifier);
		if (inStorage == null) {
			throw new ArtifactNotFoundException(identifier);
		}
	}

}
