package i5.las2peer.classLoaders.libraries;

import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.classLoaders.LibraryNotFoundException;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;

public class SharedStorageRepository implements Repository {

	// private static final L2pLogger logger = L2pLogger.getInstance(SharedStorageRepository.class);

	private static final String SERVICE_PACKAGE_PREFIX = "las2peer-service-package-";
	private static final String SERVICE_VERSIONS_PREFIX = "las2peer-service-versions-";

	private static final long FETCH_TIMEOUT = 30000;

	private final PastryNodeImpl node;

	public static String getLibraryEnvelopeIdentifier(LibraryIdentifier libId) {
		return getLibraryEnvelopeIdentifier(libId.toString());
	}

	private static String getLibraryEnvelopeIdentifier(String libId) {
		return SERVICE_PACKAGE_PREFIX + libId;
	}

	public static String getLibraryVersionsEnvelopeIdentifier(String libName) {
		return SERVICE_VERSIONS_PREFIX + libName;
	}

	public SharedStorageRepository(PastryNodeImpl node) {
		this.node = node;
	}

	@Override
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException {
		return fetchLibraryInformation(name);
	}

	@Override
	public LoadedLibrary findLibrary(LibraryIdentifier libId) throws LibraryNotFoundException {
		return fetchLibraryInformation(libId.toString());
	}

	private LoadedLibrary fetchLibraryInformation(String libId) throws LibraryNotFoundException {
		try {
			String libEnvId = getLibraryEnvelopeIdentifier(libId);
			EnvelopeVersion fetched = node.fetchEnvelope(libEnvId, FETCH_TIMEOUT);
			authenticateLibraryAuthor(libId, fetched);
			String xmlStr = (String) fetched.getContent();
			return LoadedNetworkLibrary.createFromXml(node, xmlStr);
		} catch (EnvelopeException e) {
			throw new LibraryNotFoundException("Could not fetch library '" + libId + "' information from network", e);
		} catch (CryptoException | SerializationException | InternalSecurityException e) {
			throw new LibraryNotFoundException(
					"Could not read library '" + libId + "' information from network envelope", e);
		} catch (MalformedXMLException e) {
			throw new LibraryNotFoundException("Could not read library '" + libId + "' xml representation", e);
		}
	}

	protected void authenticateLibraryAuthor(String libId, EnvelopeVersion libraryEnvelope) throws
			InternalSecurityException {
		// intentionally empty, but subclasses may override and implement checks
	}
}
