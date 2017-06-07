package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.connectors.nodeAdminConnector.multipart.FormDataPart;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class AgentsHandler extends AbstractHandler {

	public AgentsHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters, String sessionId,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception {
		final String path = exchange.getRequestURI().getPath();
		if (path.equalsIgnoreCase("/agents/uploadAgent")) {
			handleUploadAgent(exchange, node, parameters, activeAgent);
		} else {
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
			if (path.equalsIgnoreCase("/agents/createAgent")) {
				handleCreateAgent(exchange, node, payload);
			} else if (path.equalsIgnoreCase("/agents/getAgent")) {
				handleGetAgent(exchange, node, payload);
			} else if (path.equalsIgnoreCase("/agents/exportAgent")) {
				handleExportAgent(exchange, node, payload);
			} else if (path.equalsIgnoreCase("/agents/changePassphrase")) {
				handleChangePassphrase(exchange, node, payload);
			} else if (path.equalsIgnoreCase("/agents/createGroup")) {
				handleCreateGroup(exchange, node, activeAgent, payload);
			} else if (path.equalsIgnoreCase("/agents/loadGroup")) {
				handleLoadGroup(exchange, node, activeAgent, payload);
			} else if (path.equalsIgnoreCase("/agents/changeGroup")) {
				handleChangeGroup(exchange, node, activeAgent, payload);
			} else {
				sendEmptyResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND);
			}
		}
	}

	private void handleCreateAgent(HttpExchange exchange, Node node, JSONObject payload) throws Exception {
		String password = (String) payload.get("password");
		if (password == null || password.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No password provided");
			return;
		}
		String username = (String) payload.get("username");
		// check if username is already taken
		try {
			node.getAgentIdForLogin(username);
			sendJSONResponseBadRequest(exchange, "Username already taken");
			return;
		} catch (AgentNotFoundException e) {
			// expected
		}
		String email = (String) payload.get("email");
		// check if email is already taken
		try {
			node.getAgentIdForEmail(email);
			sendJSONResponseBadRequest(exchange, "Email already taken");
			return;
		} catch (AgentNotFoundException e) {
			// expected
		}
		// create new user agent and store in network
		UserAgentImpl agent = UserAgentImpl.createUserAgent(password);
		agent.unlock(password);
		if (username != null && !username.isEmpty()) {
			agent.setLoginName(username);
		}
		if (email != null && !email.isEmpty()) {
			agent.setEmail(email);
		}
		node.storeAgent(agent);
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - Agent Created");
		json.put("agentid", agent.getIdentifier());
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private void handleGetAgent(HttpExchange exchange, PastryNodeImpl node, JSONObject payload) throws Exception {
		AgentImpl agent = getAgentFromBodyPayload(exchange, node, payload);
		if (agent != null) {
			JSONObject json = new JSONObject();
			json.put("agentid", agent.getIdentifier());
			if (agent instanceof UserAgentImpl) {
				UserAgentImpl userAgent = (UserAgentImpl) agent;
				json.put("username", userAgent.getLoginName());
				json.put("email", userAgent.getEmail());
			}
			sendJSONResponse(exchange, json);
		}
	}

	private void handleExportAgent(HttpExchange exchange, PastryNodeImpl node, JSONObject payload) throws Exception {
		AgentImpl agent = getAgentFromBodyPayload(exchange, node, payload);
		if (agent != null) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_OK, "application/xml", agent.toXmlString());
		} // otherwise answer already sent
	}

	private AgentImpl getAgentFromBodyPayload(HttpExchange exchange, PastryNodeImpl node, JSONObject payload)
			throws Exception {
		// check for agentid
		String agentid = (String) payload.get("agentid");
		String username = (String) payload.get("username");
		String email = (String) payload.get("email");
		try {
			if (agentid == null || agentid.isEmpty()) {
				if (username != null && !username.isEmpty()) {
					agentid = node.getAgentIdForLogin(username);
				} else if (email != null && !email.isEmpty()) {
					agentid = node.getAgentIdForEmail(email);
				} else {
					sendJSONResponseBadRequest(exchange, "No required agent detail provided");
					return null;
				}
			}
			return node.getAgent(agentid);
		} catch (AgentNotFoundException e) {
			sendJSONResponseBadRequest(exchange, "Agent not found");
			return null;
		}
	}

	private void handleUploadAgent(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			PassphraseAgentImpl activeAgent) throws Exception {
		Object formPart = parameters.get("agentFile");
		if (formPart == null || !(formPart instanceof FormDataPart)) {
			sendJSONResponseBadRequest(exchange, "No agent file provided");
			return;
		}
		String agentFile = ((FormDataPart) formPart).getContent();
		AgentImpl agent = AgentImpl.createFromXml(agentFile);
		if (agent instanceof PassphraseAgentImpl) {
			PassphraseAgentImpl passphraseAgent = (PassphraseAgentImpl) agent;
			Object objPasswd = parameters.get("agentPassword");
			if (objPasswd == null || !(objPasswd instanceof FormDataPart)) {
				sendJSONResponseBadRequest(exchange, "No password provided");
				return;
			}
			String password = ((FormDataPart) objPasswd).getContent();
			try {
				passphraseAgent.unlock(password);
			} catch (AgentAccessDeniedException e) {
				sendJSONResponseBadRequest(exchange, "Invalid agent password");
				return;
			}
		} else if (agent instanceof GroupAgentImpl) {
			GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
			if (activeAgent == null) {
				sendJSONResponseBadRequest(exchange, "You have to be logged in, to unlock and update a group");
				return;
			}
			try {
				groupAgent.unlock(activeAgent);
			} catch (AgentAccessDeniedException e) {
				sendJSONResponseForbidden(exchange, "You have to be a member of the uploaded group");
				return;
			}
		} else {
			sendJSONResponseBadRequest(exchange, "Invalid agent type '" + agentFile.getClass().getSimpleName() + "'");
			return;
		}
		node.storeAgent(agent);
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - Agent Uploaded");
		json.put("agentid", agent.getIdentifier());
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private void handleChangePassphrase(HttpExchange exchange, Node node, JSONObject payload) throws Exception {
		String agentid = (String) payload.get("agentid");
		if (agentid == null || agentid.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No agentid provided");
			return;
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentid);
		} catch (AgentNotFoundException e) {
			sendJSONResponseBadRequest(exchange, "Agent not found");
			return;
		}
		if (!(agent instanceof PassphraseAgentImpl)) {
			sendJSONResponseBadRequest(exchange, "Invalid agent type");
			return;
		}
		PassphraseAgentImpl passAgent = (PassphraseAgentImpl) agent;
		String passphrase = (String) payload.get("passphrase");
		if (passphrase == null || passphrase.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No passphrase provided");
			return;
		}
		try {
			passAgent.unlock(passphrase);
		} catch (AgentAccessDeniedException e) {
			sendJSONResponseBadRequest(exchange, "Invalid passphrase");
			return;
		}
		String passphraseNew = (String) payload.get("passphraseNew");
		if (passphraseNew == null || passphraseNew.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No passphrase to change provided");
			return;
		}
		String passphraseNew2 = (String) payload.get("passphraseNew2");
		if (passphraseNew2 == null || passphraseNew2.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No passphrase repetition provided");
			return;
		}
		if (!passphraseNew.equals(passphraseNew2)) {
			sendJSONResponseBadRequest(exchange, "New passphrase and repetition do not match");
			return;
		}
		passAgent.changePassphrase(passphraseNew);
		node.storeAgent(passAgent);
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - Passphrase changed");
		json.put("agentid", agent.getIdentifier());
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private void handleCreateGroup(HttpExchange exchange, PastryNodeImpl node, AgentImpl activeAgent,
			JSONObject payload) throws Exception {
		if (activeAgent == null) {
			sendJSONResponseForbidden(exchange, "You have to be logged in to create a group");
			return;
		}
		Object objMembers = payload.get("members");
		if (objMembers == null || !(objMembers instanceof JSONArray)) {
			sendJSONResponseBadRequest(exchange, "No members provided");
			return;
		}
		JSONArray jsonMembers = (JSONArray) objMembers;
		if (jsonMembers.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "Members list empty");
			return;
		}
		ArrayList<AgentImpl> memberAgents = new ArrayList<>(jsonMembers.size());
		for (Object objMember : jsonMembers) {
			if (objMember instanceof JSONObject) {
				JSONObject jsonMember = (JSONObject) objMember;
				String agentId = (String) jsonMember.get("agentid");
				try {
					memberAgents.add(node.getAgent(agentId));
				} catch (Exception e) {
					sendInternalErrorResponse(exchange, "Could not get member " + agentId, e);
					return;
				}
			}
		}
		GroupAgentImpl groupAgent = GroupAgentImpl
				.createGroupAgent(memberAgents.toArray(new AgentImpl[memberAgents.size()]));
		groupAgent.unlock(activeAgent);
		node.storeAgent(groupAgent);
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - GroupAgent Created");
		json.put("agentid", groupAgent.getIdentifier());
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private void handleLoadGroup(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl activeAgent,
			JSONObject payload) throws AgentException {
		if (activeAgent == null) {
			sendJSONResponseForbidden(exchange, "You have to be logged in to load a group");
			return;
		}
		String agentid = (String) payload.get("agentid");
		if (agentid == null || agentid.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No agent id provided");
			return;
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentid);
		} catch (AgentNotFoundException e) {
			sendJSONResponseBadRequest(exchange, "Agent not found");
			return;
		}
		if (!(agent instanceof GroupAgentImpl)) {
			sendJSONResponseBadRequest(exchange, "Agent is not a GroupAgent");
			return;
		}
		GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
		try {
			groupAgent.unlock(activeAgent);
		} catch (AgentAccessDeniedException e) {
			sendJSONResponseBadRequest(exchange, "You must be a member of this group");
			return;
		}
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - GroupAgent Loaded");
		json.put("agentid", groupAgent.getIdentifier());
		JSONArray memberList = new JSONArray();
		for (String memberid : groupAgent.getMemberList()) {
			JSONObject member = new JSONObject();
			member.put("agentid", memberid);
			try {
				AgentImpl memberAgent = node.getAgent(memberid);
				if (memberAgent instanceof UserAgentImpl) {
					UserAgentImpl memberUserAgent = (UserAgentImpl) memberAgent;
					member.put("username", memberUserAgent.getLoginName());
					member.put("email", memberUserAgent.getEmail());
				}
			} catch (AgentException e) {
				logger.log(Level.WARNING, "Could not retrieve group member agent from network", e);
			}
			memberList.add(member);
		}
		json.put("members", memberList);
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

	private void handleChangeGroup(HttpExchange exchange, PastryNodeImpl node, PassphraseAgentImpl activeAgent,
			JSONObject payload) throws AgentException, CryptoException, SerializationException {
		if (activeAgent == null) {
			sendJSONResponseForbidden(exchange, "You have to be logged in to change a group");
			return;
		}
		String agentid = (String) payload.get("agentid");
		if (agentid == null || agentid.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "No agent id provided");
			return;
		}
		Object objMembers = payload.get("members");
		if (objMembers == null || !(objMembers instanceof JSONArray)) {
			sendJSONResponseBadRequest(exchange, "No members to change provided");
			return;
		}
		JSONArray changedMembers = (JSONArray) objMembers;
		if (changedMembers.isEmpty()) {
			sendJSONResponseBadRequest(exchange, "Changed members list must not be empty");
			return;
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentid);
		} catch (AgentNotFoundException e) {
			sendJSONResponseBadRequest(exchange, "Agent not found");
			return;
		}
		if (!(agent instanceof GroupAgentImpl)) {
			sendJSONResponseBadRequest(exchange, "Agent is not a GroupAgent");
			return;
		}
		GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
		try {
			groupAgent.unlock(activeAgent);
		} catch (AgentAccessDeniedException e) {
			sendJSONResponseBadRequest(exchange, "You must be a member of this group");
			return;
		}
		// add new members
		HashSet<String> memberIds = new HashSet<>();
		for (Object obj : changedMembers) {
			if (obj instanceof JSONObject) {
				JSONObject json = (JSONObject) obj;
				String memberid = (String) json.get("agentid");
				if (memberid == null || memberid.isEmpty()) {
					logger.fine("Skipping invalid member id '" + memberid + "'");
					continue;
				}
				memberIds.add(memberid.toLowerCase());
				try {
					AgentImpl memberAgent = node.getAgent(memberid);
					groupAgent.addMember(memberAgent);
					logger.info("Added new member '" + memberid + "' to group");
				} catch (AgentException e) {
					logger.log(Level.WARNING, "Could not retrieve group member agent from network", e);
					continue;
				}
			} else {
				logger.info("Skipping invalid member object '" + obj.getClass().getCanonicalName() + "'");
			}
		}
		if (!memberIds.contains(activeAgent.getIdentifier().toLowerCase())) {
			sendJSONResponseBadRequest(exchange, "You can't remove yourself from a group");
			return;
		}
		// remove all non members
		for (String oldMemberId : groupAgent.getMemberList()) {
			if (!memberIds.contains(oldMemberId)) {
				groupAgent.removeMember(oldMemberId);
				logger.info("Removed old member '" + oldMemberId + "' from group");
			}
		}
		// store changed group
		node.storeAgent(groupAgent);
		JSONObject json = new JSONObject();
		json.put("code", HttpURLConnection.HTTP_OK);
		json.put("text", HttpURLConnection.HTTP_OK + " - GroupAgent Changed");
		json.put("agentid", groupAgent.getIdentifier());
		sendJSONResponse(exchange, HttpURLConnection.HTTP_OK, json);
	}

}
