package i5.las2peer.classLoaders.libraries;

import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.classLoaders.LibraryNotFoundException;
import i5.las2peer.classLoaders.UnresolvedDependenciesException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.NodeStorageInterface;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

public class SharedStorageRepository implements Repository {

	private static final L2pLogger logger = L2pLogger.getInstance(SharedStorageRepository.class);

	private static final String LIBRARY_PREFIX = "network-lib_";
	private static final String FILE_INFIX = "_file_";
	private static final long FETCH_LIB_TIMEOUT = 30000;

	private final NodeStorageInterface storage;

	public static String getLibraryEnvelopeIdentifier(LibraryIdentifier libId) {
		return getLibraryEnvelopeIdentifier(libId.toString());
	}

	private static String getLibraryEnvelopeIdentifier(String libName) {
		return LIBRARY_PREFIX + libName;
	}

	public static String getFileEnvelopeIdentifier(LibraryIdentifier libId, String filename) {
		return getLibraryEnvelopeIdentifier(libId) + FILE_INFIX + filename;
	}

	public SharedStorageRepository(NodeStorageInterface storage) {
		this.storage = storage;
	}

	@Override
	public LoadedLibrary findLibrary(String name) throws LibraryNotFoundException, UnresolvedDependenciesException {
		return fetchLibraryInformation(name);
	}

	@Override
	public LoadedLibrary findLibrary(LibraryIdentifier lib) throws LibraryNotFoundException {
		return fetchLibraryInformation(lib.toString());
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

	private LoadedLibrary fetchLibraryInformation(String libName) throws LibraryNotFoundException {
		try {
			String libEnvId = getLibraryEnvelopeIdentifier(libName);
			EnvelopeVersion fetched = storage.fetchEnvelope(libEnvId, FETCH_LIB_TIMEOUT);
			String xmlStr = (String) fetched.getContent();
			return LoadedNetworkLibrary.createFromXml(storage, xmlStr);
		} catch (EnvelopeException e) {
			throw new LibraryNotFoundException("Could not fetch library '" + libName + "' information from network", e);
		} catch (CryptoException | L2pSecurityException | SerializationException e) {
			throw new LibraryNotFoundException("Could not read library '" + libName
					+ "' information from network envelope", e);
		} catch (MalformedXMLException e) {
			throw new LibraryNotFoundException("Could not read library '" + libName + "' xml representation", e);
		}
	}

}
