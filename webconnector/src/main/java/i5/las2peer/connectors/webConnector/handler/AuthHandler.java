package i5.las2peer.connectors.webConnector.handler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.util.AgentSession;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.*;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

@Path(AuthHandler.RESOURCE_PATH)
public class AuthHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/auth";

	private final WebConnector connector;
	private final Node node;

	public AuthHandler(WebConnector connector) {
		this.connector = connector;
		this.node = connector.getL2pNode();
	}

	@GET
	@Path("/login")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogin(@HeaderParam(HttpHeaders.AUTHORIZATION) String authHeader) throws Exception {
		// TODO is already logged in? destroy old session?
		String[] namePass = getNamePassword(authHeader);
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
				throw new BadRequestException("Invalid agent type");
			}
			PassphraseAgentImpl passphraseAgent = (PassphraseAgentImpl) agent;
			passphraseAgent.unlock(password);
			return registerAgentSession(passphraseAgent);
		} catch (AgentNotFoundException e) {
			throw new NotAuthorizedException("Agent not found", e);
		} catch (AgentAccessDeniedException e) {
			throw new ForbiddenException("Invalid passphrase", e);
		}
	}

	private String[] getNamePassword(String authHeader) {
		if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
			throw new BadRequestException("No basic auth header set");
		}
		String[] parts = authHeader.split(" ");
		if (parts.length != 2) {
			throw new BadRequestException("Malformed basic auth header");
		}
		String base64 = parts[1];
		String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
		String[] namePass = decoded.split(":");
		if (namePass.length != 2) {
			throw new BadRequestException("Malformed auth token");
		}
		if (namePass[0] == null || namePass[0].isEmpty()) {
			throw new BadRequestException("No username provided");
		}
		if (namePass[1] == null || namePass[1].isEmpty()) {
			throw new BadRequestException("No password provided");
		}
		return namePass;
	}

	@POST
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleRegistration(String requestBody) throws Exception {
		if (requestBody.trim().isEmpty()) {
			throw new BadRequestException("No request body");
		}
		JSONObject payload;
		try {
			payload = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(requestBody);
		} catch (ParseException e) {
			throw new BadRequestException("Could not parse json request body");
		}
		String password = payload.getAsString("password");
		if (password == null || password.isEmpty()) {
			throw new BadRequestException("No password provided");
		}
		String username = payload.getAsString("username");
		if (username != null && !username.isEmpty()) {
			// check if username is already taken
			try {
				node.getAgentIdForLogin(username);
				throw new BadRequestException("Username already taken");
			} catch (AgentNotFoundException e) {
				// expected
			}
		}
		String email = payload.getAsString("email");
		if (email != null && !email.isEmpty()) {
			// check if email is already taken
			try {
				node.getAgentIdForEmail(email);
				throw new BadRequestException("Username already taken");
			} catch (AgentNotFoundException e) {
				// expected
			}
		}
		// create new user agent and store in network
		UserAgentImpl agent;
		if (node instanceof EthereumNode) {
			agent = EthereumAgent.createEthereumAgent(username, password);
		} else {
			agent = UserAgentImpl.createUserAgent(password);
		}
		agent.unlock(password);
		if (username != null && !username.isEmpty()) {
			agent.setLoginName(username);
		}
		if (email != null && !email.isEmpty()) {
			agent.setEmail(email);
		}
		node.storeAgent(agent);
		return registerAgentSession(agent);
	}

	private Response registerAgentSession(PassphraseAgentImpl agent) throws Exception {
		// register session, set cookie and send response
		AgentSession session = connector.getOrCreateSession(agent);
		NewCookie cookie = new NewCookie(WebConnector.COOKIE_SESSIONID_KEY, session.getSessionId(), "/", null, 1, null,
				-1, null, true, true);
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Login OK");
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl user = (UserAgentImpl) agent;
			json.put("username", user.getLoginName());
			json.put("email", user.getEmail());
		}
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).cookie(cookie).build();
	}

	@GET
	@Path("/logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogout(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws Exception {
		return handleLogout(sessionId);
	}

	@POST
	@Path("/logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postLogout(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws Exception {
		return handleLogout(sessionId);
	}

	private Response handleLogout(String sessionId) {
		connector.destroySession(sessionId);
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Logout successful");
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@GET
	@Path("/validate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getValidate(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) {
		AgentSession session = connector.getSessionById(sessionId);
		if (session != null) {
			AgentImpl activeAgent = session.getAgent();
			if (activeAgent != null && !(activeAgent instanceof AnonymousAgentImpl)) {
				JSONObject json = new JSONObject();
				json.put("code", Status.OK.getStatusCode());
				json.put("text", Status.OK.getStatusCode() + " - Session OK");
				json.put("agentid", activeAgent.getIdentifier());
				return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
			}
		}
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Session invalid");
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

}
