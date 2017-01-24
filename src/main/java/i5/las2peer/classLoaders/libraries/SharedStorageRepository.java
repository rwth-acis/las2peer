package i5.las2peer.classLoaders.libraries;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.LibraryNotFoundException;
import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class SharedStorageRepository implements Repository {

	private static final L2pLogger logger = L2pLogger.getInstance(SharedStorageRepository.class);

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
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException, UnresolvedDependenciesException {
		return fetchLibraryInformation(name);
	}

	@Override
	public LoadedLibrary findLibrary(LibraryIdentifier libId) throws LibraryNotFoundException {
		return fetchLibraryInformation(libId.toString());
	}

	private LoadedLibrary fetchLibraryInformation(String libId) throws LibraryNotFoundException {
		try {
			String libEnvId = getLibraryEnvelopeIdentifier(libId);
			Envelope fetched = node.fetchEnvelope(libEnvId, FETCH_TIMEOUT);
			String xmlStr = (String) fetched.getContent();
			return LoadedNetworkLibrary.createFromXml(node, xmlStr);
		} catch (StorageException e) {
			throw new LibraryNotFoundException("Could not fetch library '" + libId + "' information from network", e);
		} catch (CryptoException | L2pSecurityException | SerializationException e) {
			throw new LibraryNotFoundException(
					"Could not read library '" + libId + "' information from network envelope", e);
		} catch (MalformedXMLException e) {
			throw new LibraryNotFoundException("Could not read library '" + libId + "' xml representation", e);
		}
	}

	@Override
	public LoadedLibrary findMatchingLibrary(LibraryDependency dep) throws LibraryNotFoundException {
		// how to find matching version in network? iterate over all (possible) versions would take some time ...
		LibraryIdentifier idMax = new LibraryIdentifier(dep.getName(), dep.getMax());
		try {
			// look for the newest version first
			return findLibrary(idMax);
		} catch (LibraryNotFoundException e) {
			if (dep.getMax().equals(dep.getMin())) {
				throw e;
			} else {
				// look for the minimum version as fallback
				LibraryIdentifier idMin = new LibraryIdentifier(dep.getName(), dep.getMin());
				logger.warning("Did not find library '" + idMax.toString() + "' in network. Trying minimum version '"
						+ idMin.toString() + "' as fallback");
				return findLibrary(idMin);
			}
		}
	}

}
