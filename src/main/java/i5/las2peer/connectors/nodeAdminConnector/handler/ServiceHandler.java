package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.net.HttpURLConnection;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;

public class ServiceHandler extends AbstractHandler {

	public ServiceHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	public void handle(HttpExchange exchange) {
		try {
			logger.info("Handler: " + getClass().getCanonicalName() + " Request-Path: "
					+ exchange.getRequestURI().getPath());
			exchange.getResponseHeaders().set("Server-Name", "las2peer " + getClass().getSimpleName());
			// FIXME reimplement ServiceHandler for REST invocations
//			final Node node = connector.getNode();
//			Path path = Paths.get(exchange.getRequestURI().getPath());
//			int nameCount = path.getNameCount();
//			String path0 = path.getName(0).toString();
//			// service call
//			String serviceName = path0.toString();
//			String serviceVersion = path.getName(1).toString();
//			ServiceNameVersion snv = new ServiceNameVersion(serviceName, serviceVersion);
//			NodeServiceCache serviceCache = node.getNodeServiceCache();
//			ServiceAgent localAgent = null;
//			try {
//				localAgent = serviceCache.getLocalService(snv);
//			} catch (AgentNotKnownException e) {
//			}
//			if (nameCount == 2 || localAgent == null) { // service status request
//				String response = "service: " + snv.toString() + "\n";
//				String status;
//				if (localAgent != null) {
//					status = "running";
//				} else {
//					status = "not running";
//				}
//				response += "Status: " + status + "\n";
//				sendPlainResponse(exchange, response);
//			} else { // service method call
//				sendPlainResponse(exchange, "service method call not implemented");
//			}
//			} else if (path0.matches(".+\\..+")) {
//			// else check if name[0] matches service name and list all versions for this service
//			String response = "service versions for '" + path0 + "':<br>";
//			response += "local:<br>";
//			List<ServiceVersion> versions = node.getNodeServiceCache().getLocalServiceVersions(path0);
//			if (versions != null) {
//				for (ServiceVersion version : versions) {
//					String strVersion = version.toString();
//					response += "<a href=\"" + path0 + "/" + strVersion + "\">" + path0 + "@" + strVersion
//							+ "</a><br>";
//				}
//			}
//			// fetch service versions envelope for given service name
//			response += "in network:<br>";
//			try {
//				String libName = L2pClassManager.getPackageName(path0);
//				String libId = SharedStorageRepository.getLibraryVersionsEnvelopeIdentifier(libName);
//				Envelope networkVersions = node.fetchEnvelope(libId);
//				Serializable content = networkVersions.getContent();
//				if (content instanceof ServiceVersionList) {
//					ServiceVersionList serviceversions = (ServiceVersionList) content;
//					for (String version : serviceversions) {
//						response += "<a href=\"" + path0 + "/" + version + "\">" + path0 + "@" + version
//								+ "</a><br>";
//					}
//				} else {
//					throw new ServicePackageException(
//							"Invalid version envelope expected " + List.class.getCanonicalName()
//									+ " but envelope contains " + content.getClass().getCanonicalName());
//				}
//			} catch (ArtifactNotFoundException e) {
//				response += "none<br>";
//			}
//			sendHtmlResponse(exchange, response);
			sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "test/plain", "404 (Not Found)");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unknown connector error", e);
			sendPlainResponse(exchange, e.toString());
		}
	}

}
