package i5.las2peer.tools;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentAccessDeniedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

import i5.las2peer.classLoaders.helpers.LibraryIdentifier;
import i5.las2peer.classLoaders.libraries.LoadedNetworkLibrary;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;

public class PackageUploader {

	private static L2pLogger logger = L2pLogger.getInstance(PackageUploader.class);

	public static class ServiceVersionList extends LinkedList<String> {

		private static final long serialVersionUID = 1L;

	}

	/**
	 * Uploads the complete service (jar) and all its dependencies into the given nodes shared storage to be used for
	 * network class loading. The dependencies are read from the "Import-Library" statement inside the jars manifest
	 * file. All the files extracted from the jars are signed with the given developers agent to prevent manipulations.
	 * 
	 * @param node The node storage, where the files should be uploaded into.
	 * @param serviceJarFilename The service jar that should be uploaded.
	 * @param developerAgentXMLFilename The developers agent, who is responsible for this service.
	 * @param developerPassword The password for the developers agent.
	 * @throws ServicePackageException If an issue occurs with the service jar itself or its dependencies (jars).
	 */
	public static void uploadServicePackage(PastryNodeImpl node, String serviceJarFilename,
			String developerAgentXMLFilename, String developerPassword) throws ServicePackageException {
		// early verify the developer agent to avoid needless heavy duty jar parsing
		AgentImpl devAgent = unlockDeveloperAgent(developerAgentXMLFilename, developerPassword);
		JarFile serviceJar = null;
		try {
			long uploadStart = System.currentTimeMillis();
			serviceJar = new JarFile(serviceJarFilename);
			// read general service information from jar manifest
			Manifest manifest = serviceJar.getManifest();
			if (manifest == null) {
				throw new ServicePackageException("Service jar package contains no manifest file");
			}
			String serviceName = manifest.getMainAttributes().getValue("las2peer-service-name");
			String serviceVersion = manifest.getMainAttributes().getValue("las2peer-service-version");
			// read files from jar and generate hashes
			HashMap<String, byte[]> depHashes = new HashMap<>();
			HashMap<String, byte[]> jarFiles = new HashMap<>();
			Enumeration<JarEntry> jarEntries = serviceJar.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry entry = jarEntries.nextElement();
				if (!entry.isDirectory()) {
					byte[] bytes = SimpleTools.toByteArray(serviceJar.getInputStream(entry));
					byte[] hash = CryptoTools.getSecureHash(bytes);
					String filename = entry.getName();
					depHashes.put(filename, hash);
					jarFiles.put(filename, bytes);
				}
			}
			uploadServicePackage(node, serviceName, serviceVersion, depHashes, jarFiles, devAgent);
			long uploadTime = System.currentTimeMillis() - uploadStart;
			System.out.println("Service package '" + serviceJarFilename + "' uploaded in " + uploadTime + " ms");
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Service package upload failed! " + e.toString());
		} catch (IOException | CryptoException | EnvelopeException | SerializationException e) {
			logger.log(Level.SEVERE, "Service package upload failed!", e);
		} finally {
			if (serviceJar != null) {
				try {
					serviceJar.close();
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Exception while closing jar file", e);
				}
			}
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

	public static void uploadServicePackage(PastryNodeImpl node, String serviceName, String serviceVersion,
			HashMap<String, byte[]> depHashes, HashMap<String, byte[]> jarFiles, AgentImpl devAgent)
			throws IllegalArgumentException, SerializationException, CryptoException, EnvelopeException,
			ServicePackageException {
		if (serviceName == null) {
			throw new ServicePackageException(
					"No service name value in manifest file. Please specify 'las2peer-service-name'");
		} else if (serviceVersion == null) {
			throw new ServicePackageException(
					"No service version value in manifest file. Please specify 'las2peer-service-version'");
		}
		LibraryIdentifier libId = new LibraryIdentifier(serviceName, serviceVersion);
		// store metadata envelope for service
		LoadedNetworkLibrary netLib = new LoadedNetworkLibrary(node, libId, depHashes);
		// upload network library as XML representation
		String libEnvId = SharedStorageRepository.getLibraryEnvelopeIdentifier(netLib.getIdentifier());
		logger.info("publishing library '" + netLib.getIdentifier().toString() + "' to '" + libEnvId + "'");
		EnvelopeVersion libEnv = node.createUnencryptedEnvelope(libEnvId, netLib.toXmlString());
		try {
			node.storeEnvelope(libEnv, devAgent);
		} catch (EnvelopeAlreadyExistsException e) {
			// TODO actually compare old and new service version to determine exact version change required
			throw new ServicePackageException(
					"Service package upload failed! Version is already known in the network. To update increase version number",
					e);
		}
		// TODO upload all files async to the network ignore already existing files
		for (Entry<String, byte[]> entry : jarFiles.entrySet()) {
			logger.info("publishing file '" + entry.getKey() + "' from jar");
			node.storeHashedContent(entry.getValue());
		}
		// add service version to general service envelope
		String envVersionId = SharedStorageRepository.getLibraryVersionsEnvelopeIdentifier(serviceName);
		logger.info("publishing version information to '" + envVersionId + "'");
		// fetch or create versions envelope
		EnvelopeVersion versionEnv = null;
		try {
			EnvelopeVersion storedVersions = node.fetchEnvelope(envVersionId);
			// add version to list
			Serializable content = storedVersions.getContent();
			if (content instanceof ServiceVersionList) {
				ServiceVersionList versions = (ServiceVersionList) content;
				versions.add(libId.getVersion().toString());
				versionEnv = node.createUnencryptedEnvelope(storedVersions, versions);
			} else {
				throw new ServicePackageException("Invalid version envelope expected " + List.class.getCanonicalName()
						+ " but envelope contains " + content.getClass().getCanonicalName());
			}
		} catch (EnvelopeNotFoundException e) {
			ServiceVersionList versions = new ServiceVersionList();
			versions.add(libId.getVersion().toString());
			versionEnv = node.createUnencryptedEnvelope(envVersionId, versions);
		} catch (L2pSecurityException e) {
			throw new ServicePackageException("Unencrypted content in service versions envelope expected", e);
		}
		// store envelope with service version information
		node.storeEnvelope(versionEnv, devAgent);
		// TODO wait for all async uploads
	}

}
