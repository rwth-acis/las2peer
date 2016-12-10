package i5.las2peer.tools;

import java.io.File;
import java.io.IOException;

import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;
import i5.las2peer.classLoaders.libraries.LoadedNetworkLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.NodeStorageInterface;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgent;

public class PackageUploader {

	private static L2pLogger logger = L2pLogger.getInstance(PackageUploader.class);

	/**
	 * Uploads the complete service (jar) and all its dependencies into the given nodes shared storage to be used for
	 * network class loading. The dependencies are read from the "Import-Library" statement inside the jars manifest
	 * file. All the files extracted from the jars are signed with the given developers agent to prevent manipulations.
	 * 
	 * @param node The node storage, where the files should be uploaded into.
	 * @param serviceJarFile The service jar that should be uploaded.
	 * @param developerAgentXMLFile The developers agent, who is responsible for this service.
	 * @param developerPassword The password for the developers agent.
	 * @throws ServicePackageException If an issue occurs with the service jar itself or its dependencies (jars).
	 */
	public static void uploadServicePackage(NodeStorageInterface node, String serviceJarFile,
			String developerAgentXMLFile, String developerPassword) throws ServicePackageException {
		// early verify the developer agent to avoid needless heavy duty jar parsing
		Agent devAgent = unlockDeveloperAgent(developerAgentXMLFile, developerPassword);
		// XXX better upload or check for dependencies before uploading actual service jar?
		// publish service jar first
		LoadedNetworkLibrary netLib = publishLibrary(serviceJarFile, node, devAgent);
		// upload all dependencies
		String serviceJarDirectory = new File(serviceJarFile).getParent();
		for (LibraryDependency dependency : netLib.getDependencies()) {
			// XXX what if max version of dependency jar not found?
			String depJarFilename = serviceJarDirectory + File.separator + dependency.getName() + "-"
					+ dependency.getMax() + ".jar";
			publishLibrary(depJarFilename, node, devAgent);
		}
	}

	private static Agent unlockDeveloperAgent(String agentXMLFile, String developerPassword)
			throws ServicePackageException {
		try {
			// read agent from given XML file
			Agent agent = Agent.createFromXml(new File(agentXMLFile));
			if (!(agent instanceof PassphraseAgent)) {
				throw new ServicePackageException("Developer agent of type '" + PassphraseAgent.class.getCanonicalName()
						+ "' expected got '" + agent.getClass().getCanonicalName() + "' instead!");
			}
			// unlock agent (verify password)
			PassphraseAgent devAgent = (PassphraseAgent) agent;
			devAgent.unlockPrivateKey(developerPassword);
			return devAgent;
		} catch (MalformedXMLException | L2pSecurityException e) {
			throw new ServicePackageException(e);
		}
	}

	private static LoadedNetworkLibrary publishLibrary(String jarFilename, NodeStorageInterface node, Agent devAgent)
			throws ServicePackageException {
		try {
			// read jar as jar library
			LoadedJarLibrary jarLib = LoadedJarLibrary.createFromJar(jarFilename);
			// transform jar library into network library
			LoadedNetworkLibrary netLib = new LoadedNetworkLibrary(node, jarLib.getIdentifier(),
					jarLib.getDependencies());
			logger.info("publishing library '" + netLib.getIdentifier().toString() + "'");
			// upload network library as XML representation
			String libEnvId = SharedStorageRepository.getLibraryEnvelopeIdentifier(netLib.getIdentifier());
			String xmlNetLib = netLib.toXmlString();
			Envelope libEnv = node.createUnencryptedEnvelope(libEnvId, xmlNetLib);
			node.storeEnvelope(libEnv, devAgent);
			// upload all files from the jar library
			for (String filename : jarLib.getContainedFiles()) {
				byte[] fileRaw = jarLib.getResourceAsBinary(filename);
				String fileEnvId = SharedStorageRepository.getFileEnvelopeIdentifier(netLib.getIdentifier(), filename);
				Envelope fileEnv = node.createUnencryptedEnvelope(fileEnvId, fileRaw);
				node.storeEnvelope(fileEnv, devAgent);
			}
			return netLib;
		} catch (IllegalArgumentException | IOException | SerializationException | CryptoException | StorageException
				| ResourceNotFoundException e) {
			throw new ServicePackageException("Could not publish network library from jar '" + jarFilename + "'", e);
		}
	}

}
