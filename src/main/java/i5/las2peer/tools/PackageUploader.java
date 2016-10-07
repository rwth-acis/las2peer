package i5.las2peer.tools;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.classLoaders.helpers.LibraryDependency;
import i5.las2peer.classLoaders.libraries.LoadedJarLibrary;
import i5.las2peer.classLoaders.libraries.LoadedNetworkLibrary;
import i5.las2peer.classLoaders.libraries.ResourceNotFoundException;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.NodeStorageInterface;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

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
		AgentImpl devAgent = unlockDeveloperAgent(developerAgentXMLFile, developerPassword);
		try {
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
		} catch (EnvelopeAlreadyExistsException e) {
			logger.log(Level.SEVERE,
					"Service package upload failed! Version is already known in the network. To update increase version number");
			return;
		}
	}

	private static AgentImpl unlockDeveloperAgent(String agentXMLFile, String developerPassword)
			throws ServicePackageException {
		try {
			// read agent from given XML file
			AgentImpl agent = AgentImpl.createFromXml(new File(agentXMLFile));
			if (!(agent instanceof PassphraseAgentImpl)) {
				throw new ServicePackageException("Developer agent of type '"
						+ PassphraseAgentImpl.class.getCanonicalName() + "' expected got '"
						+ agent.getClass().getCanonicalName() + "' instead!");
			}
			// unlock agent (verify password)
			PassphraseAgentImpl devAgent = (PassphraseAgentImpl) agent;
			devAgent.unlock(developerPassword);
			return devAgent;
		} catch (MalformedXMLException | AgentAccessDeniedException e) {
			throw new ServicePackageException(e);
		}
	}

	private static LoadedNetworkLibrary publishLibrary(String jarFilename, NodeStorageInterface node, AgentImpl devAgent)
			throws ServicePackageException, EnvelopeAlreadyExistsException {
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
			EnvelopeVersion libEnv = node.createUnencryptedEnvelope(libEnvId, xmlNetLib);
			node.storeEnvelope(libEnv, devAgent);
			// upload all files from the jar library
			for (String filename : jarLib.getContainedFiles()) {
				byte[] fileRaw = jarLib.getResourceAsBinary(filename);
				String fileEnvId = SharedStorageRepository.getFileEnvelopeIdentifier(netLib.getIdentifier(), filename);
				EnvelopeVersion fileEnv = node.createUnencryptedEnvelope(fileEnvId, fileRaw);
				try {
					node.storeEnvelope(fileEnv, devAgent);
				} catch (EnvelopeAlreadyExistsException e) {
					logger.info("The file '" + fileEnvId + "' already exists in network and is not republished");
				}
			}
			return netLib;
		} catch (EnvelopeAlreadyExistsException e) {
			throw e; // is handled in calling function
		} catch (IllegalArgumentException | IOException | SerializationException | CryptoException | EnvelopeException
				| ResourceNotFoundException e) {
			throw new ServicePackageException("Could not publish network library from jar '" + jarFilename + "'", e);
		}
	}

}
