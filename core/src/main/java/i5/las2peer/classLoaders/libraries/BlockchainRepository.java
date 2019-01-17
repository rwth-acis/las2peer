package i5.las2peer.classLoaders.libraries;

import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.registry.ReadOnlyRegistryClient;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.security.InternalSecurityException;
import i5.las2peer.serialization.SerializationException;

import java.security.PublicKey;

public class BlockchainRepository extends SharedStorageRepository {
	ReadOnlyRegistryClient registryClient;

	public BlockchainRepository(EthereumNode node) {
		super(node);
		registryClient = node.getRegistryClient();
	}

	@Override
	protected void authenticateLibraryAuthor(String libId, EnvelopeVersion libraryEnvelope)
			throws InternalSecurityException {
		PublicKey signingKey = libraryEnvelope.getAuthorPublicKey();
		String servicePackageName = libId.substring(0, libId.indexOf(';'));

		try {
			String ownerOfServiceName = registryClient.getServiceAuthor(servicePackageName);
			PublicKey ownerPublicKey = registryClient.getUser(ownerOfServiceName).getPublicKey();

			if (!signingKey.equals(ownerPublicKey)) {
				throw new InternalSecurityException("Registered service owner key does not match actual signing key.");
			}
		} catch (NotFoundException e) {
			throw new InternalSecurityException("Could not verify author because author or service are not registered."
					+ "\nThis should not happen, i.e., the registry/shared storage state is inconsistent/illegal.");
		} catch (EthereumException e) {
			throw new InternalSecurityException("Could not verify author due to unexpected Registry error.", e);
		} catch (SerializationException e) {
			throw new InternalSecurityException("Could not verify author because registered public key is unreadable.", e);
		}
	}
}
