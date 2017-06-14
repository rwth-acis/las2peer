package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.SimpleTools;

public class AppHandler extends AbstractHandler {

	public static final String DEFAULT_ROUTE = "/app/view-status";
	public static final String INDEX_PATH = "/app/index.html";

	public AppHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception {
		final String path = exchange.getRequestURI().getPath();
		if (path.equalsIgnoreCase("/")) {
			serveFile(exchange, node, INDEX_PATH);
		} else if (path.endsWith("/")) { // do not list directories
			sendRedirect(exchange, DEFAULT_ROUTE, true);
		} else if (path.toLowerCase().startsWith("/app/") && !path.matches(".+/[^/]+\\.[^/]+")) {
			// must be some kind of route path
			serveFile(exchange, node, INDEX_PATH);
		} else {
			serveFile(exchange, node, path);
		}
	}

	private void serveFile(HttpExchange exchange, PastryNodeImpl node, String filename) throws IOException {
		InputStream is = getClass().getResourceAsStream(filename);
		if (is == null) {
			sendEmptyResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND);
			return;
		}
		byte[] bytes = SimpleTools.toByteArray(is);
		String strContent = new String(bytes, StandardCharsets.UTF_8);
		if (filename.equalsIgnoreCase("/app/index.html")) {
			strContent = strContent.replace("<base href=\"/\">", "<base href=\"/app/\">");
			// just return host header, so browsers do not block subsequent ajax requests to an possible insecure host
			String host = exchange.getRequestHeaders().getFirst("Host");
			strContent = strContent.replace("$connector_address$", host);
		}
		String lName = filename.toLowerCase();
		String mime = "application/octet-stream";
		if (lName.endsWith(".html") || lName.endsWith(".htm")) {
			mime = "text/html";
		} else if (lName.endsWith(".css")) {
			mime = "text/css";
		} else if (lName.endsWith(".js")) {
			mime = "text/javascript";
		} else if (lName.endsWith(".json")) {
			mime = "application/json";
		} else if (lName.endsWith(".ico")) {
			mime = "image/x-icon";
		} else {
			logger.log(Level.WARNING, "Unknown file type '" + filename + "' using " + mime + " as mime");
		}
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, mime, strContent);
	}

}
