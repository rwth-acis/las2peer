package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.connectors.nodeAdminConnector.multipart.FormDataPart;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.PackageUploader;
import i5.las2peer.tools.PackageUploader.ServiceVersionList;
import i5.las2peer.tools.ServicePackageException;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class ServicesHandler extends AbstractHandler {

	public ServicesHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters, PassphraseAgentImpl activeAgent,
			byte[] requestBody) throws Exception {
		final String path = exchange.getRequestURI().getPath();
		if (path.equalsIgnoreCase("/services/search")) {
			handleSearchService(exchange, node, parameters);
		} else if (path.equalsIgnoreCase("/services/upload")) {
			handleServicePackageUpload(exchange, node, activeAgent, parameters);
		} else {
			sendEmptyResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND);
		}
	}

	private void handleSearchService(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters) {
		// use host header, so browsers do not block subsequent ajax requests to an unknown host
		String hostHeader = exchange.getRequestHeaders().getFirst("Host");
		// search the network for services with the given name and return as JSONArray
		String searchName = parameters.getSingle("searchname");
		JSONObject result = new JSONObject();
		try {
			JSONArray instances = new JSONArray();
			if (searchName == null || searchName.isEmpty()) {
				// iterate local services
				List<String> serviceNames = node.getNodeServiceCache().getLocalServiceNames();
				for (String serviceName : serviceNames) {
					// add service versions from network
					instances.addAll(getNetworkServices(node, hostHeader, serviceName));
				}
			} else {
				// search for service version in network
				instances.addAll(getNetworkServices(node, hostHeader, searchName));
			}
			result.put("instances", instances);
		} catch (EnvelopeNotFoundException | IllegalArgumentException e) {
			result.put("msg", "'" + searchName + "' not found");
		} catch (Exception e) {
			result.put("msg", e.toString());
		}
		sendJSONResponse(exchange, result);
	}

	private JSONArray getNetworkServices(Node node, String hostHeader, String searchName) throws Exception {
		JSONArray result = new JSONArray();
		String libName = L2pClassManager.getPackageName(searchName);
		String libId = SharedStorageRepository.getLibraryVersionsEnvelopeIdentifier(libName);
		EnvelopeVersion networkVersions = node.fetchEnvelope(libId);
		Serializable content = networkVersions.getContent();
		if (content instanceof ServiceVersionList) {
			ServiceVersionList serviceversions = (ServiceVersionList) content;
			for (String version : serviceversions) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", searchName);
				jsonObject.put("version", version);
				jsonObject.put("swagger", "https://" + hostHeader + RMIHandler.RMI_PATH + "/" + searchName + "/"
						+ version + "/swagger.json");
				result.add(jsonObject);
			}
		} else {
			throw new ServicePackageException("Invalid version envelope expected " + List.class.getCanonicalName()
					+ " but envelope contains " + content.getClass().getCanonicalName());
		}
		return result;
	}

	private void handleServicePackageUpload(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl activeAgent,
			ParameterMap parameters) throws Exception {
		if (activeAgent == null) {
			sendJSONResponseBadRequest(exchange, "You have to be logged in to upload");
			return;
		}
		Object jarParam = parameters.get("jarfile");
		if (jarParam == null) {
			sendJSONResponseBadRequest(exchange, "No jar file provided");
			return;
		}
		if (!(jarParam instanceof FormDataPart)) {
			sendInternalErrorResponse(exchange, FormDataPart.class.getCanonicalName() + " expected, but got "
					+ jarParam.getClass().getCanonicalName(), null);
			return;
		}
		byte[] jarFileContent = ((FormDataPart) jarParam).getContentRaw();
		if (jarFileContent.length < 1) {
			sendJSONResponseBadRequest(exchange, "No file content provided");
			return;
		}
		// create jar from inputstream
		JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(jarFileContent));
		// read general service information from jar manifest
		Manifest manifest = jarStream.getManifest();
		if (manifest == null) {
			jarStream.close();
			sendJSONResponseBadRequest(exchange, "Service jar package contains no manifest file");
			return;
		}
		String serviceName = manifest.getMainAttributes().getValue("las2peer-service-name");
		String serviceVersion = manifest.getMainAttributes().getValue("las2peer-service-version");
		// read files from jar and generate hashes
		HashMap<String, byte[]> depHashes = new HashMap<>();
		HashMap<String, byte[]> jarFiles = new HashMap<>();
		JarEntry entry = null;
		while ((entry = jarStream.getNextJarEntry()) != null) {
			if (!entry.isDirectory()) {
				byte[] bytes = SimpleTools.toByteArray(jarStream);
				jarStream.closeEntry();
				byte[] hash = CryptoTools.getSecureHash(bytes);
				String filename = entry.getName();
				depHashes.put(filename, hash);
				jarFiles.put(filename, bytes);
			}
		}
		jarStream.close();
		try {
			PackageUploader.uploadServicePackage(node, serviceName, serviceVersion, depHashes, jarFiles, activeAgent);
			JSONObject json = new JSONObject();
			json.put("code", HttpURLConnection.HTTP_OK);
			json.put("text", HttpURLConnection.HTTP_OK + " - Service package upload successful");
			json.put("msg", "Service package upload successful");
			sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
		} catch (EnvelopeAlreadyExistsException e) {
			sendJSONResponseBadRequest(exchange,
					"Service package upload failed! Version is already known in the network. To update increase version number");
			return;
		} catch (ServicePackageException e) {
			sendJSONResponseBadRequest(exchange, "Service package upload failed - Reason: " + e.toString());
			return;
		}
	}

}
