package i5.las2peer.nodeAdminConnector.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupDir;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.api.persistency.EnvelopeAlreadyExistsException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.nodeAdminConnector.AgentSession;
import i5.las2peer.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.nodeAdminConnector.handler.pojo.PojoService;
import i5.las2peer.nodeAdminConnector.multipart.FormDataPart;
import i5.las2peer.p2p.AgentNotRegisteredException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.CryptoTools;
import i5.las2peer.tools.PackageUploader;
import i5.las2peer.tools.PackageUploader.ServiceVersionList;
import i5.las2peer.tools.ServicePackageException;
import i5.las2peer.tools.SimpleTools;

public class FrontendHandler extends AbstractHandler {

	public static final String ROOT_NAME = "www";

	public static final String ROOT_PATH = "/" + ROOT_NAME;
	public static final String LOGOUT_PATH = ROOT_PATH + "/logout";
	public static final String STATUS_PATH = ROOT_PATH + "/status";
	public static final String LOGO_PATH = ROOT_PATH + "/las2peer-logo.svg";

	private static final String TEMPLATES_SUBPATH = "/inline";
	private static final String USER_ACCOUNT_PREFIX = "useraccount-";

	private final String adminToken;
	private STGroupDir templateGroup;

	public FrontendHandler(NodeAdminConnector connector, String adminToken) {
		super(connector);
		this.adminToken = adminToken;
		reloadTemplates();
	}

	// TODO create reload method for admins to recreate/update group
	private void reloadTemplates() {
		templateGroup = new STGroupDir(ROOT_NAME, '$', '$');
	}

	@Override
	public void handle(HttpExchange exchange) {
		try {
			exchange.getResponseHeaders().set("Server-Name", "las2peer " + getClass().getSimpleName());
			final PastryNodeImpl node = connector.getNode();
			final AgentSession requestingAgentSession = connector
					.getSessionFromCookies(exchange.getRequestHeaders().get("Cookie"));
			String sessionId = null;
			String agentId = null;
			UserAgentImpl activeAgent = null;
			if (requestingAgentSession != null) {
				sessionId = requestingAgentSession.getSessionId();
				activeAgent = requestingAgentSession.getAgent();
				agentId = activeAgent.getSafeId();
			}
			logger.info("Handler: " + getClass().getCanonicalName() + " Method: " + exchange.getRequestMethod()
					+ " Request-Path: " + exchange.getRequestURI().getPath());
			handleRequest(exchange, node, sessionId, agentId, activeAgent);
		} catch (Exception e) {
			sendInternalErrorResponse(exchange, "Unknown connector error", e);
		}
	}

	private void handleRequest(HttpExchange exchange, PastryNodeImpl node, String sessionId, String agentId,
			UserAgentImpl activeAgent) throws Exception {
		final String method = exchange.getRequestMethod();
		final String path = exchange.getRequestURI().getPath();
		if (path.isEmpty() || ROOT_PATH.equalsIgnoreCase(path)) {
			sendRedirect(exchange, STATUS_PATH, true);
			return;
		} else if (LOGOUT_PATH.equalsIgnoreCase(path)) {
			if (sessionId != null) {
				connector.destroySession(sessionId);
			}
			try {
				node.unregisterReceiver(activeAgent);
			} catch (AgentNotRegisteredException e) {
				// actually nobody cares
				logger.log(Level.FINE, "Could not unregister agent on logout", e);
			}
			sendRedirect(exchange, STATUS_PATH, false);
			return;
		} else if (LOGO_PATH.equalsIgnoreCase(path)) {
			if ("get".equalsIgnoreCase(method)) {
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
				InputStream is = getClass().getClassLoader().getResourceAsStream(path.substring(1));
				byte[] bytes = SimpleTools.toByteArray(is);
				OutputStream os = exchange.getResponseBody();
				os.write(bytes);
				os.close();
			}
			return;
		} // TODO handle favicon requests
		reloadTemplates(); // TODO create reload method for admins to recreate/update group
		ST mainTemplate = templateGroup.getInstanceOf("/index");
		if (mainTemplate == null) {
			sendInternalErrorResponse(exchange, "Templates not initialized correctly");
			return;
		}
		String subPath = path.substring(ROOT_PATH.length());
		ST template = templateGroup.getInstanceOf(TEMPLATES_SUBPATH + subPath);
		String content = "404 (Not Found)";
		if (template != null) {
			ParameterMap parameters = (ParameterMap) exchange.getAttribute("parameters");
			// FIXME replace with path constants?
			if (subPath.toLowerCase().startsWith("/admin")) {
				handleAdmin(exchange, node, parameters, template);
			} else if (subPath.equalsIgnoreCase("/status")) {
				template.add("nodeid", node.getNodeId());
				template.add("cpuload", getCPULoad(node) + "%");
				template.add("publicKey", node.getPublicNodeKey().toString());
				template.add("storageSize", humanReadableByteCount(node.getLocalStorageSize(), true));
				template.add("maxStorageSize", humanReadableByteCount(node.getLocalMaxStorageSize(), true));
				template.add("uptime", getUptime(node));
				template.add("localServices", getLocalServices(node));
				template.add("otherNodes", node.getOtherKnownNodes());
			} else if (subPath.equalsIgnoreCase("/login")) {
				if ("post".equalsIgnoreCase(method)) {
					handleAuthenticateRequest(exchange, node, activeAgent, template);
					return;
				}
			} else if (subPath.equalsIgnoreCase("/services")) {
				String searchname = parameters.getSingle("searchname");
				if (searchname != null && !searchname.isEmpty()) {
					template.add("searchname", searchname);
				} else {
					template.add("searchname", "i5.las2peer.services.fileService.FileService");
				}
				template.add("services", getServices(node, searchname));
			} else if (subPath.equalsIgnoreCase("/upload")) {
				template.add("agentid", agentId);
				if ("post".equalsIgnoreCase(method)) {
					handlePackageUpload(exchange, node, parameters, activeAgent, template);
				}
			}
			content = template.render();
		}
		mainTemplate.add("title", "las2peer Node Frontend");
		mainTemplate.add("version", getCoreVersion());
		mainTemplate.add("content", content);
		mainTemplate.add("sessionid", sessionId);
		mainTemplate.add("agentid", agentId);
		sendHtmlResponse(exchange, mainTemplate.render());
	}

	// Source: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	private static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	private String getUptime(PastryNodeImpl node) {
		Date startTime = node.getStartTime();
		if (startTime == null) {
			return "node stopped";
		} else {
			long uptimeInSeconds = (new Date().getTime() - node.getStartTime().getTime()) / 1000;
			long hours = uptimeInSeconds / 3600;
			long minutes = uptimeInSeconds / 60 % 60;
			long seconds = uptimeInSeconds % 60;
			return hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
		}
	}

	private void handleAdmin(HttpExchange exchange, Node node, ParameterMap parameters, ST template) {
		// verify token
		if (adminToken == null || !adminToken.equals(parameters.get("token"))) {
			return;
		} // FIXME alternatively check for admin envelope
		String startServiceName = parameters.getSingle("startService");
		String stopServiceName = parameters.getSingle("stopService");
		if (startServiceName != null) {
			// start a service
			if (!startServiceName.matches(".+\\..+")) {
				template.add("error", "Not a service name");
			} else {
				ServiceNameVersion snv = ServiceNameVersion.fromString(startServiceName);
				if (snv.getVersion().equals("*")) {
					// search for latest network version
					List<ServiceNameVersion> networkServices = getNetworkServices(node, startServiceName);
					if (networkServices.isEmpty()) {
						template.add("error", "Could not find service versions in network");
						snv = null;
					} else {
						// determine latest version
						ServiceNameVersion latest = null;
						for (ServiceNameVersion v : networkServices) {
							if (latest == null || latest.getVersion().isSmallerThan(v.getVersion())) {
								latest = v;
							}
						}
						snv = latest;
					}
				}
				if (snv != null) {
					// check if service is already running
					try {
						node.getNodeServiceCache().getLocalService(snv);
						template.add("result", "Service already running");
					} catch (AgentNotRegisteredException e) {
						// try to start the service
						try {
							// TODO is adminToken a good password?
							ServiceAgentImpl agent = ServiceAgentImpl.createServiceAgent(snv, adminToken);
							agent.unlock(adminToken);
							node.registerReceiver(agent);
							// FIXME store service agent locally
						} catch (CryptoException | L2pSecurityException | AgentException e2) {
							logger.log(Level.SEVERE, "Could not start service '" + startServiceName + "'", e2);
							template.add("error", e2.toString());
						}
						template.add("result", "Service started");
					}
				}
			}
			template.add("startService", startServiceName);
		} else {
			template.add("startService", "i5.las2peer.services.fileService.FileService");
			if (stopServiceName != null) {
				// stop local service
				ServiceNameVersion service = ServiceNameVersion.fromString(stopServiceName);
				if (service.getVersion().equals("*")) {
					template.add("stopError", "No version given");
				} else {
					try {
						ServiceAgentImpl agent = node.getLocalServiceAgent(service);
						if (agent == null) {
							throw new ServiceNotFoundException(stopServiceName);
						} else {
							try {
								node.unregisterReceiver(agent);
								template.add("stopResult", "Service stopped");
							} catch (AgentNotRegisteredException | NodeException e) {
								logger.log(Level.SEVERE, "Could not stop service '" + stopServiceName + "'", e);
								template.add("stopError", e.toString());
							}
						}
					} catch (ServiceNotFoundException e) {
						template.add("stopError", "Service not running locally");
					}
				}
			}
		}
		template.add("token", adminToken);
		template.add("localServices", getLocalServices(node));
	}

	private List<PojoService> getLocalServices(Node node) {
		List<String> serviceNames = node.getNodeServiceCache().getLocalServiceNames();
		List<PojoService> result = new LinkedList<>();
		for (String serviceName : serviceNames) {
			List<ServiceVersion> serviceVersions = node.getNodeServiceCache().getLocalServiceVersions(serviceName);
			for (ServiceVersion version : serviceVersions) {
				result.add(new PojoService(serviceName, version.toString()));
			}
		}
		return result;
	}

	private List<PojoService> getServices(Node node, String searchName) {
		List<PojoService> result = new LinkedList<>();
		if (searchName == null || searchName.isEmpty()) {
			// iterate local services
			List<String> serviceNames = node.getNodeServiceCache().getLocalServiceNames();
			for (String serviceName : serviceNames) {
				// add service versions from network
				result.addAll(getPojoNetworkServices(node, serviceName));
			}
		} else {
			// search for service version in network
			result.addAll(getPojoNetworkServices(node, searchName));
		}
		return result;
	}

	private List<PojoService> getPojoNetworkServices(Node node, String searchName) {
		List<PojoService> result = new LinkedList<>();
		try {
			String libName = L2pClassManager.getPackageName(searchName);
			String libId = SharedStorageRepository.getLibraryVersionsEnvelopeIdentifier(libName);
			EnvelopeVersion networkVersions = node.fetchEnvelope(libId);
			Serializable content = networkVersions.getContent();
			if (content instanceof ServiceVersionList) {
				ServiceVersionList serviceversions = (ServiceVersionList) content;
				for (String version : serviceversions) {
					result.add(new PojoService(searchName, version));
				}
			} else {
				throw new ServicePackageException("Invalid version envelope expected " + List.class.getCanonicalName()
						+ " but envelope contains " + content.getClass().getCanonicalName());
			}
		} catch (EnvelopeNotFoundException e) {
			result.add(new PojoService(searchName, "not found"));
		} catch (Exception e) {
			result.add(new PojoService(searchName, e.toString()));
		}
		return result;
	}

	private void handleAuthenticateRequest(HttpExchange exchange, PastryNodeImpl node, AgentImpl activeAgent, ST template)
			throws Exception {
		ParameterMap parameters = (ParameterMap) exchange.getAttribute("parameters");
		String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
		if (contentType == null || contentType.isEmpty()) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "text/plain", "400 (no content type)\n");
		} else {
			boolean isAuthenticate = parameters.get("authenticate") != null;
			boolean isRegister = parameters.get("register") != null;
			String email = parameters.getSingle("email").trim();
			String password = parameters.getSingle("password").trim();
			if (email == null || email.isEmpty()) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "text/plain",
						"400 (no email specified)\n");
			} else if (password == null || password.isEmpty()) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "text/plain",
						"400 (no password specified)\n");
			} else {
				final String identifier = USER_ACCOUNT_PREFIX + SimpleTools
						.byteToHexString(CryptoTools.getSecureHash((email).getBytes(StandardCharsets.UTF_8)));
				UserAgentImpl agent;
				try {
					logger.info("looking for account id " + identifier);
					EnvelopeVersion accountEnv = node.fetchEnvelope(identifier);
					String agentId = (String) accountEnv.getContent();
					agent = (UserAgentImpl) node.getAgent(agentId);
					agent.unlock(password);
				} catch (EnvelopeNotFoundException e) {
					if (isAuthenticate) {
						// account does not yet exist
						sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, "text/plain",
								"401 (Unauthorized)");
						return;
					} else if (isRegister) {
						// try to create an account
						logger.info("account not found, creating it");
						try {
							agent = UserAgentImpl.createUserAgent(password);
							agent.unlock(password);
							node.storeAgent(agent);
							EnvelopeVersion accountEnv = node.createUnencryptedEnvelope(identifier, agent.getSafeId());
							node.storeEnvelope(accountEnv, agent);
							logger.info("created new account successfully");
						} catch (Exception e2) {
							sendInternalErrorResponse(exchange, "Could not create account", e2);
							return;
						}
					} else {
						throw new IllegalStateException("Neither authenticate nor register");
					}
				} catch (ClassCastException e) {
					// content is not an agent id, or it's not a UserAgent, what now?
					sendInternalErrorResponse(exchange, "Could not read agent id from account envelope", e);
					return;
				} catch (AgentNotFoundException e) {
					// this should not happen, but we can re-create a new agent?
					sendInternalErrorResponse(exchange, "Could not read agent from network storage", e);
					return;
				} catch (L2pSecurityException e) {
					// TODO show error in template instead
					sendStringResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, "text/plain",
							"Could not unlock agent with given password - Reason: " + e.toString());
					return;
				}
				// register session and set cookie
				AgentSession session = connector.getOrCreateSession(agent);
				node.registerReceiver(agent);
				exchange.getResponseHeaders().add("Set-Cookie", "agent-session-id=" + session.getSessionId());
				// FIXME auto add node to trusted list
				// redirect to status page
				exchange.getResponseHeaders().add("Location", "/" + FrontendHandler.ROOT_NAME);
				exchange.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, NO_RESPONSE_BODY);
				exchange.getResponseBody().close();
			}
		}
	}

	private void handlePackageUpload(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			AgentImpl activeAgent, ST template) throws Exception {
		if (activeAgent == null) {
			template.add("error", "You have to be logged in to upload");
			return;
		}
		Object jarParam = parameters.get("jarfile");
		if (jarParam == null) {
			template.add("error", "No jar file provided");
			return;
		}
		if (!(jarParam instanceof FormDataPart)) {
			sendInternalErrorResponse(exchange, FormDataPart.class.getCanonicalName() + " expected, but got "
					+ jarParam.getClass().getCanonicalName(), null);
			return;
		}
		byte[] jarFileContent = ((FormDataPart) jarParam).getContentRaw();
		if (jarFileContent.length < 1) {
			template.add("error", "No file content provided");
			return;
		}
		// create jar from inputstream
		JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(jarFileContent));
		// read general service information from jar manifest
		Manifest manifest = jarStream.getManifest();
		if (manifest == null) {
			jarStream.close();
			template.add("error", "Service jar package contains no manifest file");
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
			template.add("msg", "Service package upload successful");
		} catch (EnvelopeAlreadyExistsException e) {
			template.add("error",
					"Service package upload failed! Version is already known in the network. To update increase version number");
		} catch (ServicePackageException e) {
			template.add("error", "Service package upload failed - Reason: " + e.toString());
			return;
		}
	}

}
