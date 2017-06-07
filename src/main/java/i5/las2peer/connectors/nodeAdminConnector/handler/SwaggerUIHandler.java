package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import javax.ws.rs.core.HttpHeaders;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.SimpleTools;

public class SwaggerUIHandler extends AbstractHandler {

	public static final String ROOT_PATH = "/swagger-ui";
	public static final String SWAGGER_UI_JAR_PREFIX = "META-INF/resources/webjars/swagger-ui/2.2.10";

	public SwaggerUIHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters, String sessionId,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception {
		// serve files from swagger-ui jar
		String path = exchange.getRequestURI().getPath();
		String subPath = path.substring(ROOT_PATH.length());
		if (subPath.isEmpty() || subPath.equalsIgnoreCase("/")) {
			sendRedirect(exchange, "/swagger-ui/index.html", true);
			return;
		} else if (subPath.endsWith("/")) { // do not serve directory listings
			sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "text/plain", "404 (not found)");
			return;
		}
		String res = SWAGGER_UI_JAR_PREFIX + subPath;
		InputStream is = getClass().getClassLoader().getResourceAsStream(res);
		if (is == null) {
			logger.info("404 (not found)");
			sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "text/plain", "404 (not found)");
			return;
		} else {
			String mime = "text/html";
			String lPath = path.toLowerCase();
			if (lPath.endsWith(".css")) {
				mime = "text/css";
			} else if (lPath.endsWith(".js")) {
				mime = "text/javascript";
			}
			exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, mime);
			byte[] bytes = SimpleTools.toByteArray(is);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
			OutputStream os = exchange.getResponseBody();
			os.write(bytes);
			os.close();
			return;
		}
	}

}
