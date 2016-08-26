package i5.las2peer.persistency.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import i5.las2peer.api.StorageExceptionHandler;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MetadataEnvelope;
import i5.las2peer.persistency.StorageLookupHandler;
import i5.las2peer.persistency.pastry.PastLookupContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContentHandle;
import rice.pastry.commonapi.PastryIdFactory;

/**
 * This class is used to manage the process in order to find the latest version for a given identifier. It uses binary
 * search if the latest version is requested.
 */
public class LatestArtifactVersionFinder implements Runnable, StorageLookupHandler, StorageExceptionHandler {

	private static final L2pLogger logger = L2pLogger.getInstance(LatestArtifactVersionFinder.class);

	private final String identifier;
	private final long startVersion;
	private final StorageLookupHandler nodeLookupHandler;
	private final PastryIdFactory artifactIdFactory;
	private final Past pastStorage;
	private final int maxHandles;
	private final ExecutorService threadpool;
	private long currentVersion;
	private long currentOffset;
	private long unknownVersion;
	private long latestVersion;
	private final HashMap<Long, ArrayList<PastContentHandle>> versionToHandle;

	/**
	 * constructor
	 *
	 * @param identifier A textual or hash identifier for this envelope.
	 * @param startVersion A version number to start the search from. Usually the last known version.
	 * @param nodeLookupHandler A {@link StorageLookupHandler} implementation that is provided with handles to the
	 *            latest found version.
	 * @param artifactIdFactory An id factory to generate network ids during the search process.
	 * @param pastStorage A past storage that should be request for version handles.
	 * @param maxHandles The maximum number of handles to be queried from the network.
	 * @param threadpool A executor service to perform subtasks.
	 */
	public LatestArtifactVersionFinder(String identifier, long startVersion, StorageLookupHandler nodeLookupHandler,
			PastryIdFactory artifactIdFactory, Past pastStorage, int maxHandles, ExecutorService threadpool) {
		this.identifier = identifier;
		this.startVersion = Math.max(startVersion, Envelope.START_VERSION);
		this.nodeLookupHandler = nodeLookupHandler;
		this.artifactIdFactory = artifactIdFactory;
		this.pastStorage = pastStorage;
		this.maxHandles = maxHandles;
		this.threadpool = threadpool;
		currentOffset = 1;
		unknownVersion = Envelope.NULL_VERSION;
		latestVersion = Envelope.NULL_VERSION;
		versionToHandle = new HashMap<>();
	}

	@Override
	public void onLookup(ArrayList<PastContentHandle> metadataHandles) {
		logger.info("Lookup got " + metadataHandles.size() + " past handles for identifier '" + identifier
				+ "' and version " + currentVersion);
		versionToHandle.put(currentVersion, metadataHandles);
		if (metadataHandles.size() > 0) {
			latestVersion = currentVersion;
			if (unknownVersion == Envelope.NULL_VERSION) {
				// yet all lookups worked well
				requestLookup(startVersion + currentOffset);
				currentOffset *= 2;
			} else if (currentVersion == unknownVersion - 1) {
				// the next version is already known as missing
				queryVersion();
			} else {
				// this lookup worked but some before failed
				requestLookup(currentVersion + (unknownVersion - currentVersion) / 2);
			}
		} else if (currentVersion == Envelope.START_VERSION) {
			// if no version found return to callback with empty list
			nodeLookupHandler.onLookup(metadataHandles);
		} else if (latestVersion == currentVersion - 1) {
			// the version before this (failed) lookup worked, we're done!
			queryVersion();
		} else {
			// lookup returned no handles => version doesn't exist
			unknownVersion = currentVersion;
			requestLookup(latestVersion + (currentVersion - latestVersion) / 2);
		}
	}

	@Override
	public void onException(Exception e) {
		logger.info("Lookup exception (" + e.toString() + ") occurred assuimng zero handles");
		onLookup(new ArrayList<>());
	}

	@Override
	public void run() {
		requestLookup(startVersion);
	}

	private void requestLookup(long version) {
		currentVersion = version;
		Id checkId = MetadataEnvelope.buildMetadataId(artifactIdFactory, identifier, currentVersion);
		logger.info("Looking for artifact with identifier '" + identifier + "' and version " + currentVersion
				+ " at id " + checkId.toStringFull() + " ...");
		pastStorage.lookupHandles(checkId, maxHandles, new PastLookupContinuation(threadpool, this, this));
	}

	private void queryVersion() {
		logger.info("found latest version (" + latestVersion + ") for " + identifier);
		ArrayList<PastContentHandle> handles = versionToHandle.get(latestVersion);
		if (handles == null) {
			throw new RuntimeException("No handles for current version in map");
		}
		// hand this to node (parent) lookup handler
		nodeLookupHandler.onLookup(handles);
	}

}
