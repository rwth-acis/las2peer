package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.classLoaders.libraries.SharedStorageRepository;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.persistency.EnvelopeVersion;
import i5.las2peer.tools.L2pNodeLauncher;
import i5.las2peer.tools.PackageUploader.ServiceVersionList;
import i5.las2peer.tools.ServicePackageException;

public abstract class AbstractHandler implements HttpHandler {

	protected static final int NO_RESPONSE_BODY = -1;

	protected final L2pLogger logger = L2pLogger.getInstance(getClass());
	protected final NodeAdminConnector connector;

	protected AbstractHandler(NodeAdminConnector connector) {
		this.connector = connector;
	}

	protected void sendPlainResponse(HttpExchange exchange, String text) {
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, "text/plain", text);
	}

	protected void sendHtmlResponse(HttpExchange exchange, String html) {
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, "text/html", html);
	}

	protected void sendStringResponse(HttpExchange exchange, int responseCode, String mime, String response) {
		try {
			byte[] content = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
			exchange.sendResponseHeaders(responseCode, content.length);
			OutputStream os = exchange.getResponseBody();
			os.write(content);
			os.close();
		} catch (Exception e) {
			logger.log(Level.WARNING, e.toString());
		}
	}

	protected void sendInternalErrorResponse(HttpExchange exchange, String msg) {
		sendInternalErrorResponse(exchange, msg, null);
	}

	protected void sendInternalErrorResponse(HttpExchange exchange, String msg, Exception e) {
		logger.log(Level.SEVERE, msg, e);
		String reason = "";
		if (e != null) {
			reason = " - Reason: " + e.toString();
		}
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "text/plain", msg + reason);
	}

	protected void sendRedirect(HttpExchange exchange, String location, boolean permanent) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		int statusCode = HttpURLConnection.HTTP_SEE_OTHER;
		if (permanent) {
			statusCode = HttpURLConnection.HTTP_MOVED_PERM;
		}
		exchange.sendResponseHeaders(statusCode, NO_RESPONSE_BODY);
		exchange.getResponseBody().close();
	}

	protected String getCoreVersion() {
		return L2pNodeLauncher.getVersion();
	}

	protected int getCPULoad(Node node) {
		return (int) (node.getNodeCpuLoad() * 100);
	}

	protected List<ServiceNameVersion> getNetworkServices(Node node, String searchName) {
		List<ServiceNameVersion> result = new LinkedList<>();
		try {
			String libName = L2pClassManager.getPackageName(searchName);
			String libId = SharedStorageRepository.getLibraryVersionsEnvelopeIdentifier(libName);
			EnvelopeVersion networkVersions = node.fetchEnvelope(libId);
			Serializable content = networkVersions.getContent();
			if (content instanceof ServiceVersionList) {
				ServiceVersionList serviceversions = (ServiceVersionList) content;
				for (String version : serviceversions) {
					result.add(new ServiceNameVersion(searchName, version));
				}
			} else {
				throw new ServicePackageException("Invalid version envelope expected " + List.class.getCanonicalName()
						+ " but envelope contains " + content.getClass().getCanonicalName());
			}
		} catch (EnvelopeNotFoundException e) {
			logger.fine(e.toString());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not load service versions from network", e);
		}
		return result;
	}

}
