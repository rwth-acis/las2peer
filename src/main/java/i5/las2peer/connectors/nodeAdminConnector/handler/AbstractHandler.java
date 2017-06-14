package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import i5.las2peer.connectors.nodeAdminConnector.AgentSession;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.L2pNodeLauncher;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public abstract class AbstractHandler implements HttpHandler {

	protected static final int NO_RESPONSE_BODY = -1;

	protected final L2pLogger logger = L2pLogger.getInstance(getClass());
	protected final NodeAdminConnector connector;

	protected AbstractHandler(NodeAdminConnector connector) {
		this.connector = connector;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			final String method = exchange.getRequestMethod();
			logger.fine("Handler: " + getClass().getSimpleName() + " Request-Method: " + method + " Request-Path: "
					+ exchange.getRequestURI().getPath());
			exchange.getResponseHeaders().set("Server-Name", "las2peer " + getClass().getSimpleName());
			String origin = exchange.getRequestHeaders().getFirst("Origin");
			if (origin != null) {
				exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
			} else {
				exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			}
			exchange.getResponseHeaders().add("Access-Control-Max-Age", "60");
			exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
			ParameterMap parameters = (ParameterMap) exchange.getAttribute("parameters");
			List<String> cookies = exchange.getRequestHeaders().get(HttpHeaders.COOKIE);
			AgentSession requestingAgentSession = connector.getSessionFromCookies(cookies);
			PassphraseAgentImpl activeAgent = null;
			if (requestingAgentSession != null) {
				exchange.getResponseHeaders().add(HttpHeaders.SET_COOKIE, NodeAdminConnector.COOKIE_SESSION_KEY + "="
						+ requestingAgentSession.getSessionId() + "; Path=/; Secure; HttpOnly");
				activeAgent = requestingAgentSession.getAgent();
			} else { // default to anonymous agent
				activeAgent = AnonymousAgentImpl.getInstance();
				activeAgent.unlock(AnonymousAgentImpl.PASSPHRASE);
			}
			if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
				handleOptionsRequest(exchange);
			} else {
				// read request body
				byte[] requestBody = new byte[0];
				InputStream is = exchange.getRequestBody();
				if (is != null) {
					requestBody = SimpleTools.toByteArray(is);
					is.close();
				}
				// TODO limit request body to 5M?
				handleSub(exchange, connector.getNode(), parameters, activeAgent, requestBody);
			}
		} catch (Exception e) {
			sendInternalErrorResponse(exchange, "Unknown connector error", e);
		}
	}

	protected abstract void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception;

	private void handleOptionsRequest(HttpExchange exchange) {
		String requestedHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");
		if (requestedHeaders != null) {
			// we accept all headers (at the moment)
			exchange.getResponseHeaders().add("Access-Control-Allow-Headers", requestedHeaders);
		} else {
			// we accept all methods (at the moment)
			exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
		}
		sendEmptyResponse(exchange, HttpURLConnection.HTTP_OK);
	}

	protected void sendPlainResponse(HttpExchange exchange, String text) {
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, MediaType.TEXT_PLAIN, text);
	}

	protected void sendHtmlResponse(HttpExchange exchange, String html) {
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, MediaType.TEXT_HTML, html);
	}

	protected void sendJSONResponseBadRequest(HttpExchange exchange, String reason) {
		sendJSONResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request", reason);
	}

	protected void sendJSONResponseForbidden(HttpExchange exchange, String reason) {
		sendJSONResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN, "Forbidden", reason);
	}

	protected void sendJSONResponse(HttpExchange exchange, int responseCode, String text, String reason) {
		JSONObject json = new JSONObject();
		json.put("code", responseCode);
		json.put("text", responseCode + " - " + text);
		json.put("reason", reason);
		sendJSONResponse(exchange, responseCode, json);
	}

	protected void sendJSONResponse(HttpExchange exchange, JSONObject json) {
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	protected void sendJSONResponse(HttpExchange exchange, int responseCode, JSONObject json) {
		sendStringResponse(exchange, responseCode, MediaType.APPLICATION_JSON, json.toJSONString());
	}

	protected void sendJSONResponse(HttpExchange exchange, JSONArray json) {
		sendStringResponse(exchange, HttpURLConnection.HTTP_OK, MediaType.APPLICATION_JSON, json.toJSONString());
	}

	protected void sendStringResponse(HttpExchange exchange, int responseCode, String mime, String response) {
		try {
			byte[] content = response.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, mime + "; charset=utf-8");
			exchange.sendResponseHeaders(responseCode, content.length);
			OutputStream os = exchange.getResponseBody();
			os.write(content);
			os.close();
		} catch (Exception e) {
			logger.log(Level.WARNING, e.toString());
		}
	}

	protected void sendRawResponse(HttpExchange exchange, int responseCode, byte[] content) {
		try {
			exchange.sendResponseHeaders(responseCode, content.length);
			OutputStream os = exchange.getResponseBody();
			os.write(content);
			os.close();
		} catch (Exception e) {
			logger.log(Level.WARNING, e.toString());
		}
	}

	protected void sendEmptyResponse(HttpExchange exchange, int responseCode) {
		try {
			exchange.sendResponseHeaders(responseCode, 0);
			exchange.getResponseBody().close(); // close to signal client
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
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, MediaType.TEXT_PLAIN, msg + reason);
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

}
