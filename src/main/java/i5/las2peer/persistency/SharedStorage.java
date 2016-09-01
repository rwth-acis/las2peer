package i5.las2peer.persistency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import i5.las2peer.api.Configurable;
import i5.las2peer.api.StorageCollisionHandler;
import i5.las2peer.api.StorageEnvelopeHandler;
import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.api.StorageStoreResultHandler;
import i5.las2peer.api.exceptions.EnvelopeAlreadyExistsException;
import i5.las2peer.api.exceptions.EnvelopeNotFoundException;
import i5.las2peer.api.exceptions.StopMergingException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.helper.ArtifactPartComparator;
import i5.las2peer.persistency.helper.FetchProcessHelper;
import i5.las2peer.persistency.helper.LatestArtifactVersionFinder;
import i5.las2peer.persistency.helper.MergeCounter;
import i5.las2peer.persistency.helper.MultiArtifactHandler;
import i5.las2peer.persistency.helper.MultiStoreResult;
import i5.las2peer.persistency.helper.StoreProcessHelper;
import i5.las2peer.persistency.pastry.PastFetchContinuation;
import i5.las2peer.persistency.pastry.PastInsertContinuation;
import i5.las2peer.persistency.pastry.PastLookupContinuation;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import i5.las2peer.tools.SerializeTools;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdFactory;
import rice.p2p.commonapi.Node;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastImpl;
import rice.p2p.past.PastPolicy.DefaultPastPolicy;
import rice.pastry.commonapi.PastryIdFactory;
import rice.persistence.Cache;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.PersistentStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;

public class SharedStorage extends Configurable implements L2pStorageInterface {

	/**
	 * Storage mode for the pastry node &ndash; either use only memory or the file system for stored artifacts.
	 */
	public enum STORAGE_MODE {
		FILESYSTEM,
		MEMORY,
	}

	private static final L2pLogger logger = L2pLogger.getInstance(SharedStorage.class);

	public static final int DEFAULT_NUM_OF_REPLICAS = 5;
	private int numOfReplicas = DEFAULT_NUM_OF_REPLICAS;

	public static final int DEFAULT_MAXIMUM_CACHE_SIZE = 200 * 1024 * 1024; // 200 MB
	private int maximumCacheSize = DEFAULT_MAXIMUM_CACHE_SIZE;

	public static final String DEFAULT_STORAGE_ROOT_DIR = "node-storage/";
	private String storageRootDir = DEFAULT_STORAGE_ROOT_DIR;

	public static final long DEFAULT_MAXIMUM_STORAGE_SIZE = 1000 * 1024 * 1024; // 1 GB
	private long maximumStorageSize = DEFAULT_MAXIMUM_STORAGE_SIZE;

	public static final long DEFAULT_ASYNC_INSERT_OPERATION_TIMEOUT = 5 * 60 * 1000 * 1000; // ms => 5 min
	private long asyncInsertOperationTimeout = DEFAULT_ASYNC_INSERT_OPERATION_TIMEOUT;

	private final PastImpl pastStorage;
	private final PastryIdFactory artifactIdFactory;
	private final ExecutorService threadpool;
	private final ConcurrentHashMap<String, Long> versionCache;

	public SharedStorage(Node node, STORAGE_MODE storageMode, ExecutorService threadpool) throws StorageException {
		IdFactory pastIdFactory = new PastryIdFactory(node.getEnvironment());
		Storage storage;
		if (storageMode == STORAGE_MODE.MEMORY) {
			storage = new MemoryStorage(pastIdFactory);
		} else if (storageMode == STORAGE_MODE.FILESYSTEM) {
			try {
				storage = new PersistentStorage(pastIdFactory, storageRootDir, maximumStorageSize,
						node.getEnvironment());
			} catch (IOException e) {
				throw new StorageException("Could not initialize persistent storage!", e);
			}
		} else {
			throw new StorageException("Unexpected storage mode '" + storageMode + "'");
		}
		// on the other nobody likes outdated data
		Cache cache = new LRUCache(storage, maximumCacheSize, node.getEnvironment());
		StorageManagerImpl manager = new StorageManagerImpl(pastIdFactory, storage, cache);
		pastStorage = new PastImpl(node, manager, numOfReplicas, "i5.las2peer.enterprise.storage",
				new DefaultPastPolicy());
		artifactIdFactory = new PastryIdFactory(node.getEnvironment());
		this.threadpool = threadpool;
		versionCache = new ConcurrentHashMap<>();
	}

	@Override
	public Envelope createEnvelope(String identifier, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(identifier, content, readers);
	}

	@Override
	public Envelope createEnvelope(Envelope previousVersion, Serializable content, List<Agent> readers)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content, readers);
	}

	@Override
	public Envelope createUnencryptedEnvelope(String identifier, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(identifier, content, new ArrayList<Agent>());
	}

	@Override
	public Envelope createUnencryptedEnvelope(Envelope previousVersion, Serializable content)
			throws IllegalArgumentException, SerializationException, CryptoException {
		return new Envelope(previousVersion, content, new ArrayList<Agent>());
	}

	@Override
	public void storeEnvelope(Envelope envelope, Agent author, long timeoutMs) throws StorageException {
		if (timeoutMs < 0) {
			throw new IllegalArgumentException("Timeout must be greater or equal to zero");
		}
		StoreProcessHelper resultHelper = new StoreProcessHelper();
		storeEnvelopeAsync(envelope, author, resultHelper, resultHelper, resultHelper);
		long startWait = System.nanoTime();
		while (System.nanoTime() - startWait < timeoutMs * 1000000) {
			try {
				if (resultHelper.getResult() >= 0) {
					return;
				}
			} catch (Exception e) {
				if (e instanceof StorageException) {
					throw (StorageException) e;
				} else {
					throw new StorageException(e);
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				throw new StorageException(e);
			}
		}
	}

	@Override
	public void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler) {
		logger.info("Storing envelope " + envelope + " ...");
		// insert envelope into DHT
		final MergeCounter mergeCounter = new MergeCounter();
		storeEnvelopeAsync(envelope, author, resultHandler, collisionHandler, exceptionHandler, mergeCounter);
	}

	private void storeEnvelopeAsync(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageCollisionHandler collisionHandler, StorageExceptionHandler exceptionHandler,
			MergeCounter mergeCounter) {
		// check for collision to offer merging
		threadpool.execute(new Runnable() {
			@Override
			public void run() {
				logger.info("Checking collision for " + envelope.toString());
				pastStorage.lookupHandles(MetadataEnvelope.buildMetadataId(artifactIdFactory, envelope),
						numOfReplicas + 1, new PastLookupContinuation(threadpool, new StorageLookupHandler() {
							@Override
							public void onLookup(ArrayList<PastContentHandle> metadataHandles) {
								if (metadataHandles.size() > 0) {
									// collision detected
									handleCollision(envelope, author, metadataHandles, resultHandler, collisionHandler,
											exceptionHandler, mergeCounter);
								} else {
									// no collision -> try insert
									insertEnvelope(envelope, author, resultHandler, exceptionHandler);
								}
							}
						}, exceptionHandler));
			}
		});
	}

	private void handleCollision(Envelope envelope, Agent author, ArrayList<PastContentHandle> metadataHandles,
			StorageStoreResultHandler resultHandler, StorageCollisionHandler collisionHandler,
			StorageExceptionHandler exceptionHandler, MergeCounter mergeCounter) {
		if (collisionHandler != null) {
			fetchWithMetadata(metadataHandles, new StorageEnvelopeHandler() {
				@Override
				public void onEnvelopeReceived(Envelope inNetwork) {
					try {
						Serializable mergedObj = collisionHandler.onCollision(envelope, inNetwork,
								mergeCounter.value());
						long mergedVersion = Math.max(envelope.getVersion(), inNetwork.getVersion()) + 1;
						List<PublicKey> mergedReaders = collisionHandler.mergeReaders(envelope.getReaderKeys(),
								inNetwork.getReaderKeys());
						Envelope merged = new Envelope(envelope.getIdentifier(), mergedVersion, mergedObj,
								mergedReaders);
						logger.info("Merged envelope (collisions: " + mergeCounter.value() + ") from network "
								+ inNetwork.toString() + " with local copy " + envelope.toString()
								+ " to merged version " + merged.toString());
						mergeCounter.increase();
						storeEnvelopeAsync(merged, author, resultHandler, collisionHandler, exceptionHandler,
								mergeCounter);
					} catch (StopMergingException | IllegalArgumentException | SerializationException
							| CryptoException e) {
						exceptionHandler.onException(e);
					}
				}
			}, exceptionHandler);
		} else if (exceptionHandler != null) {
			exceptionHandler.onException(
					new EnvelopeAlreadyExistsException("Duplicate DHT entry for envelope " + envelope.toString()));
		}
	}

	private void insertEnvelope(Envelope envelope, Agent author, StorageStoreResultHandler resultHandler,
			StorageExceptionHandler exceptionHandler) {
		long version = envelope.getVersion();
		if (version < 1) { // just to be sure
			if (exceptionHandler != null) {
				exceptionHandler.onException(
						new IllegalArgumentException("Version number (" + version + ") must be higher than zero"));
			}
			return;
		}
		// XXX only accept envelope if the content has changed?
		logger.info("Inserting parted envelope into network DHT");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(envelope);
			baos.close();
		} catch (IOException e) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(e);
			}
			// cancel insert operation
			return;
		}
		byte[] serialized = baos.toByteArray();
		int size = serialized.length;
		int parts = size / NetworkArtifact.MAX_SIZE;
		if (parts * NetworkArtifact.MAX_SIZE < size) {
			parts++; // this happens if max size is not a divider of size
		}
		int partsize = size / parts + 1;
		logger.info("Given object is serialized " + size + " bytes heavy, split into " + parts + " parts each "
				+ partsize + " bytes in size");
		MultiStoreResult multiResult = new MultiStoreResult(parts);
		String identifier = envelope.getIdentifier();
		try {
			int offset = 0;
			for (int part = 0; part < parts; part++) {
				byte[] rawPart = Arrays.copyOfRange(serialized, offset, offset + partsize);
				NetworkArtifact toStore = new NetworkArtifact(
						NetworkArtifact.buildArtifactId(artifactIdFactory, identifier, part, version), part, rawPart,
						author);
				logger.info("Storing part " + part + " for envelope "
						+ NetworkArtifact.getVersionPartIdentifier(identifier, part, version) + " with id "
						+ toStore.getId().toStringFull() + " at " + (System.currentTimeMillis() % 100000));
				pastStorage.insert(toStore, new PastInsertContinuation(threadpool, multiResult, multiResult, toStore));
				offset += partsize;
			}
		} catch (CryptoException | L2pSecurityException e) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(e);
			}
			return;
		}
		// wait for all part inserts
		long insertStart = System.currentTimeMillis();
		while (!multiResult.isDone()) {
			if (System.currentTimeMillis() - insertStart >= asyncInsertOperationTimeout) {
				// this point means the network layer did not receive positive or negative feedback
				if (exceptionHandler != null) {
					exceptionHandler.onException(new StorageException("Network communication timeout"));
				}
				return;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				if (exceptionHandler != null) {
					exceptionHandler.onException(e);
				}
				return;
			}
		}
		Exception exception = multiResult.getException();
		if (exception != null) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(exception);
			}
			return;
		}
		// all parts done? insert MetadataEnvelope to complete insert operation
		try {
			MetadataEnvelope metadataEnvelope = new MetadataEnvelope(identifier, version, parts);
			NetworkArtifact metadataArtifact = new NetworkArtifact(
					MetadataEnvelope.buildMetadataId(artifactIdFactory, identifier, version), 0,
					SerializeTools.serialize(metadataEnvelope), author);
			logger.info("Storing metadata for envelope " + NetworkArtifact.getVersionIdentifier(identifier, version)
					+ " with id " + metadataArtifact.getId().toStringFull() + " at "
					+ (System.currentTimeMillis() % 100000));
			pastStorage.insert(metadataArtifact,
					new PastInsertContinuation(threadpool, new StorageStoreResultHandler() {
						@Override
						public void onResult(Serializable envelope, int successfulOperations) {
							// all done - call actual user defined result handlers
							Exception e = multiResult.getException();
							if (e != null) {
								if (exceptionHandler != null) {
									exceptionHandler.onException(e);
								}
							} else if (resultHandler != null) {
								resultHandler.onResult(envelope, multiResult.getMinSuccessfulOperations());
							}
						}
					}, exceptionHandler, metadataArtifact));
		} catch (CryptoException | L2pSecurityException | SerializationException e) {
			if (exceptionHandler != null) {
				exceptionHandler.onException(e);
			}
		}
	}

	@Override
	public Envelope fetchEnvelope(String identifier, long timeoutMs) throws StorageException {
		return fetchEnvelope(identifier, Envelope.LATEST_VERSION, timeoutMs);
	}

	public Envelope fetchEnvelope(String identifier, long version, long timeoutMs) throws StorageException {
		if (timeoutMs < 0) {
			throw new IllegalArgumentException("Timeout must be greater or equal to zero");
		}
		FetchProcessHelper resultHelper = new FetchProcessHelper();
		fetchEnvelopeAsync(identifier, version, resultHelper, resultHelper);
		long startWait = System.nanoTime();
		while (System.nanoTime() - startWait < timeoutMs * 1000000) {
			try {
				Envelope result = resultHelper.getResult();
				if (result != null) {
					return result;
				}
			} catch (Exception e) {
				if (e instanceof StorageException) {
					throw (StorageException) e;
				} else {
					throw new StorageException(e);
				}
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				throw new StorageException(e);
			}
		}
		throw new StorageException("Fetch operation time out");
	}

	@Override
	public void fetchEnvelopeAsync(String identifier, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		fetchEnvelopeAsync(identifier, Envelope.LATEST_VERSION, envelopeHandler, exceptionHandler);
	}

	public void fetchEnvelopeAsync(String identifier, long version, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		if (envelopeHandler == null) {
			// fetching something without caring about the result makes no sense
			if (exceptionHandler != null) {
				exceptionHandler.onException(new NullPointerException("envelope handler must not be null"));
			}
			return;
		}
		// get handles for first part of the desired version
		if (version == Envelope.LATEST_VERSION) {
			// retrieve the latest version from the network
			Long startVersion = versionCache.get(identifier);
			if (startVersion == null) {
				startVersion = Envelope.START_VERSION;
			}
			logger.info("Starting latest version lookup for " + identifier + " at " + startVersion);
			threadpool.execute(new LatestArtifactVersionFinder(identifier, startVersion, new StorageLookupHandler() {
				@Override
				public void onLookup(ArrayList<PastContentHandle> metadataHandles) {
					if (metadataHandles.size() > 0) {
						fetchWithMetadata(metadataHandles, new StorageEnvelopeHandler() {
							@Override
							public void onEnvelopeReceived(Envelope result) {
								// this handler-in-the-middle updates the version cache,
								// before returning the result to the actual envelope handler
								versionCache.put(result.getIdentifier(), result.getVersion());
								envelopeHandler.onEnvelopeReceived(result);
							}
						}, exceptionHandler);
					} else {
						// not found
						if (exceptionHandler != null) {
							exceptionHandler.onException(new EnvelopeNotFoundException(
									"no version found for identifier '" + identifier + "'"));
						}
					}
				}
			}, artifactIdFactory, pastStorage, numOfReplicas + 1, threadpool));
		} else {
			Id checkId = MetadataEnvelope.buildMetadataId(artifactIdFactory, identifier, version);
			pastStorage.lookupHandles(checkId, numOfReplicas + 1,
					new PastLookupContinuation(threadpool, new StorageLookupHandler() {

						@Override
						public void onLookup(ArrayList<PastContentHandle> metadataHandles) {
							if (metadataHandles.size() > 0) {
								// call from first part
								fetchWithMetadata(metadataHandles, envelopeHandler, exceptionHandler);
							} else {
								// not found
								if (exceptionHandler != null) {
									exceptionHandler
											.onException(new EnvelopeNotFoundException("Envelope with identifier '"
													+ identifier + "' and version " + version + " ("
													+ checkId.toStringFull() + ") not found in shared storage!"));
								}
							}
						}

					}, exceptionHandler));
		}
	}

	private void fetchWithMetadata(ArrayList<PastContentHandle> metadataHandles, StorageEnvelopeHandler envelopeHandler,
			StorageExceptionHandler exceptionHandler) {
		fetchFromHandles(metadataHandles, new StorageArtifactHandler() {
			@Override
			public void onReceive(NetworkArtifact artifact) {
				try {
					Serializable received = SerializeTools.deserialize(artifact.getContent());
					if (received instanceof MetadataEnvelope) {
						MetadataEnvelope metadata = (MetadataEnvelope) received;
						// metadata received query all actual data parts
						int size = metadata.getEnvelopeNumOfParts();
						MultiArtifactHandler artifactHandler = new MultiArtifactHandler(size,
								new StoragePartsHandler() {
									@Override
									public void onPartsReceived(ArrayList<NetworkArtifact> parts) {
										try {
											Envelope result = buildFromParts(artifactIdFactory, metadata, parts);
											envelopeHandler.onEnvelopeReceived(result);
										} catch (IllegalArgumentException | StorageException e) {
											if (exceptionHandler != null) {
												exceptionHandler.onException(e);
											}
										}
									}
								}, exceptionHandler);
						for (int partIndex = 0; partIndex < size; partIndex++) {
							fetchPart(metadata.getEnvelopeIdentifier(), partIndex, metadata.getEnvelopeVersion(),
									artifactHandler);
						}
					} else if (exceptionHandler != null) {
						exceptionHandler.onException(
								new StorageException("expected " + MetadataEnvelope.class.getCanonicalName()
										+ " but got " + received.getClass().getCanonicalName() + " instead"));
					}
				} catch (SerializationException | VerificationFailedException e) {
					if (exceptionHandler != null) {
						exceptionHandler.onException(e);
					}
				}
			}
		}, exceptionHandler);
	}

	private void fetchPart(String identifier, int part, long version, MultiArtifactHandler artifactHandler) {
		Id checkId = NetworkArtifact.buildArtifactId(artifactIdFactory, identifier, part, version);
		String versionString = NetworkArtifact.getVersionPartIdentifier(identifier, part, version);
		logger.info("Fetching envelope " + versionString + " " + checkId.toStringFull() + "...");
		pastStorage.lookupHandles(checkId, numOfReplicas + 1,
				new PastLookupContinuation(threadpool, new StorageLookupHandler() {
					@Override
					public void onLookup(ArrayList<PastContentHandle> handles) {
						logger.info("Got " + handles.size() + " past handles for " + versionString);
						if (handles.size() < 1) {
							artifactHandler.onException(new EnvelopeNotFoundException("Envelope " + versionString + " ("
									+ checkId.toStringFull() + ") not found in shared storage!"));
						} else {
							fetchFromHandles(handles, artifactHandler, artifactHandler);
						}
					}
				}, artifactHandler));
	}

	private void fetchFromHandles(ArrayList<PastContentHandle> handles, StorageArtifactHandler artifactHandler,
			StorageExceptionHandler exceptionHandler) {
		if (handles.isEmpty()) {
			throw new IllegalArgumentException("No handles to fetch given");
		}
		// XXX pick the best fitting handle depending on nodeid (web-of-trust) or distance
		PastContentHandle bestHandle = handles.get(new Random().nextInt(handles.size()));
		// query fetch command
		pastStorage.fetch(bestHandle, new PastFetchContinuation(threadpool, artifactHandler, exceptionHandler));
	}

	private static Envelope buildFromParts(PastryIdFactory artifactIdFactory, MetadataEnvelope metadata,
			ArrayList<NetworkArtifact> artifacts) throws IllegalArgumentException, StorageException {
		if (artifacts == null || artifacts.isEmpty()) {
			throw new StorageException("No parts to build from");
		}
		// order parts
		Collections.sort(artifacts, ArtifactPartComparator.INSTANCE);
		try {
			String envelopeIdentifier = null;
			long envelopeVersion = Envelope.NULL_VERSION;
			int lastPartIndex = -1;
			int numberOfParts = 0;
			// concat raw byte arrays
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (NetworkArtifact artifact : artifacts) {
				logger.info("concating content of " + artifact.toString() + " with " + metadata.getEnvelopeNumOfParts()
						+ " parts");
				// check identifier
				String artifactIdentifier = metadata.getEnvelopeIdentifier();
				if (artifactIdentifier == null) {
					throw new StorageException("Artifact identifier must not be null");
				} else if (envelopeIdentifier == null) {
					envelopeIdentifier = artifactIdentifier;
				} else if (!artifactIdentifier.equals(metadata.getEnvelopeIdentifier())) {
					throw new StorageException("Aritfact identifier (" + artifactIdentifier
							+ ") did not match envelope identifier (" + envelopeIdentifier + ")");
				}
				// check version
				long artifactVersion = metadata.getEnvelopeVersion();
				if (artifactVersion < Envelope.START_VERSION) {
					throw new StorageException(
							"Artifact version (" + artifactVersion + ") must be bigger than " + Envelope.START_VERSION);
				} else if (envelopeVersion == Envelope.NULL_VERSION) {
					envelopeVersion = artifactVersion;
				} else if (envelopeVersion != artifactVersion) {
					throw new StorageException("Not matching versions for artifact (" + artifactVersion
							+ ") and envelope (" + envelopeVersion + ")");
				}
				// check part index
				int partIndex = artifact.getPartIndex();
				if (partIndex > lastPartIndex + 1) {
					throw new StorageException("Artifacts not sorted or missing part! Got " + partIndex + " instead of "
							+ (lastPartIndex + 1));
				}
				lastPartIndex++;
				// check number of parts
				int parts = metadata.getEnvelopeNumOfParts();
				if (parts < 1) {
					throw new StorageException("Number of parts given is to low " + parts);
				} else if (numberOfParts == 0) {
					numberOfParts = parts;
				} else if (numberOfParts != parts) {
					throw new StorageException("Number of parts from artifact (" + numberOfParts
							+ ") did not match with envelope parts number (" + parts + ")");
				}
				try {
					byte[] rawContent = artifact.getContent();
					// add content to buffer
					baos.write(rawContent);
				} catch (VerificationFailedException e) {
					throw new StorageException("Could not retrieve content from part", e);
				}
			}
			byte[] bytes = baos.toByteArray();
			// deserialize object
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object obj = ois.readObject();
			Envelope result;
			if (obj instanceof Envelope) {
				result = (Envelope) obj;
			} else {
				throw new StorageException("expected class " + Envelope.class.getCanonicalName() + " but got "
						+ obj.getClass().getCanonicalName() + " instead");
			}
			return result;
		} catch (IOException | ClassNotFoundException | ClassCastException | IllegalArgumentException e) {
			throw new StorageException("Building envelope from parts failed!", e);
		}
	}

	@Override
	public void removeEnvelope(String identifier) throws EnvelopeNotFoundException, StorageException {
		throw new StorageException("Delete not implemented in Past!");
	}

}
