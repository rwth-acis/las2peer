package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.core.HttpHeaders;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.nodeAdminConnector.AgentSession;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class AuthHandler extends AbstractHandler {

	/**
	 * the login process takes at least this time to prevent brute force attacks
	 */
	private static final long BRUTE_FORCE_LOGIN_DELAY = 500; // ms

	public AuthHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception {
		final String path = exchange.getRequestURI().getPath();
		if (path.equalsIgnoreCase("/auth/login")) {
			handleAuthentication(exchange, node);
		} else if (path.equalsIgnoreCase("/auth/create")) {
			handleRegistration(exchange, node, parameters, requestBody);
		} else if (path.equalsIgnoreCase("/auth/logout")) {
			handleLogout(exchange, node, activeAgent);
		} else if (path.equalsIgnoreCase("/auth/validate")) {
			handleValidate(exchange, node, activeAgent);
		} else {
			sendEmptyResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND);
		}
	}

	private void handleRegistration(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			byte[] requestBody) throws Exception {
		if (requestBody.length == 0) {
			sendJSONResponseBadRequest(exchange, "No request body");
			return;
		}
		JSONObject payload;
		try {
			payload = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(requestBody);
		} catch (ParseException e) {
			sendJSONResponseBadRequest(exchange, "Could not parse json request body");
			return;
		}
		String username = (String) payload.get("username");
		if (username == null || username.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No username provided");
			return;
		}
		String password = (String) payload.get("password");
		if (password == null || password.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No password provided");
			return;
		}
		// check if username is already taken
		try {
			node.getAgentIdForLogin(username);
			sendJSONResponseBadRequest(exchange, "Username already taken");
			return;
		} catch (AgentNotFoundException e) {
			// expected
		}
		// create new user agent and store in network
		UserAgentImpl agent = UserAgentImpl.createUserAgent(password);
		agent.unlock(password);
		agent.setLoginName(username);
		node.storeAgent(agent);
		registerAgentSession(exchange, node, agent, 0);
	}

	private void handleAuthentication(HttpExchange exchange, PastryNodeImpl node) throws Exception {
		// TODO is already logged in? destroy old session?
		long loginStarted = System.currentTimeMillis();
		String[] namePass = getNamePassword(exchange);
		if (namePass == null) {
			return; // response already sent
		}
		String userid = namePass[0];
		String password = namePass[1];
		try {
			String id;
			try {
				id = node.getAgentIdForEmail(userid);
			} catch (AgentNotFoundException e) {
				// given userid is not a known email, try as login name
				id = node.getAgentIdForLogin(userid);
			}
			AgentImpl agent = node.getAgent(id);
			if (!(agent instanceof PassphraseAgentImpl)) {
				sendJSONResponseBadRequest(exchange, "Invalid agent type");
				return;
			}
			PassphraseAgentImpl passphraseAgent = (PassphraseAgentImpl) agent;
			passphraseAgent.unlock(password);
			registerAgentSession(exchange, node, passphraseAgent, loginStarted);
		} catch (AgentNotFoundException e) {
			sendJSONResponseBadRequest(exchange, "Agent not found");
		} catch (AgentAccessDeniedException e) {
			sendJSONResponseForbidden(exchange, "Invalid passphrase");
		}
	}

	private void registerAgentSession(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl agent,
			long loginStarted) throws Exception {
		// register session, set cookie and send response
		AgentSession session = connector.getOrCreateSession(agent);
		exchange.getResponseHeaders().add(HttpHeaders.SET_COOKIE,
				NodeAdminConnector.COOKIE_SESSION_KEY + "=" + session.getSessionId() + "; Path=/; Secure; HttpOnly");
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - Login OK");
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl user = (UserAgentImpl) agent;
			json.put("username", user.getLoginName());
			json.put("email", user.getEmail());
		}
		// delay login process, to prevent brute-force attacks
		long toSleep = BRUTE_FORCE_LOGIN_DELAY - (System.currentTimeMillis() - loginStarted);
		if (toSleep > 0) {
			logger.info("Delaying auth request for " + toSleep + "ms to prevent brute-force");
			Thread.sleep(toSleep);
		}
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private String[] getNamePassword(HttpExchange exchange) {
		String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
		if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
			sendJSONResponseBadRequest(exchange, "No basic auth header set");
			return null;
		}
		String[] parts = authHeader.split(" ");
		if (parts.length != 2) {
			sendJSONResponseBadRequest(exchange, "Malformed basic auth header");
			return null;
		}
		String base64 = parts[1];
		String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
		String[] namePass = decoded.split(":");
		if (namePass.length != 2) {
			sendJSONResponseBadRequest(exchange, "Malformed auth token");
			return null;
		}
		if (namePass[0] == null || namePass[0].isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No username provided");
			return null;
		}
		if (namePass[1] == null || namePass[1].isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No password provided");
			return null;
		}
		return namePass;
	}

	private void handleLogout(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl activeAgent)
			throws Exception {
		connector.destroySession(activeAgent);
		sendEmptyResponse(exchange, HttpURLConnection.HTTP_OK);
	}

	private void handleValidate(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl activeAgent) {
		if (activeAgent != null && !(activeAgent instanceof AnonymousAgentImpl)) {
			JSONObject json = new JSONObject();
			json.put("code", HttpURLConnection.HTTP_OK);
			json.put("text", HttpURLConnection.HTTP_OK + " - Session OK");
			json.put("agentid", activeAgent.getIdentifier());
			sendJSONResponse(exchange, json);
		} else {
			JSONObject json = new JSONObject();
			json.put("code", HttpURLConnection.HTTP_UNAUTHORIZED);
			json.put("text", HttpURLConnection.HTTP_UNAUTHORIZED + " - Session Invalid");
			sendJSONResponse(exchange, json);
		}
	}

}
