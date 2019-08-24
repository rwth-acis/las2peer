package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.security.UserAgent;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.security.*;
import org.glassfish.jersey.media.multipart.FormDataParam;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.util.AgentSession;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

@Path(AgentsHandler.RESOURCE_PATH)
public class AgentsHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/agents";

	private final L2pLogger logger = L2pLogger.getInstance(AgentsHandler.class);

	private final WebConnector connector;
	private final Node node;

	public AgentsHandler(WebConnector connector) {
		this.connector = connector;
		this.node = connector.getL2pNode();
	}

	@POST
	@Path("/createAgent")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleCreateAgent(@FormDataParam("username") String username, @FormDataParam("email") String email,
			@FormDataParam("mnemonic") String ethereumMnemonic, @FormDataParam("password") String password)
			throws Exception {
		if (password == null || password.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No password provided").build();
		}
		if (username != null && !username.isEmpty()) {
			// check if username is already taken
			try {
				node.getAgentIdForLogin(username);
				return Response.status(Status.BAD_REQUEST).entity("Username already taken").build();
			} catch (AgentNotFoundException e) {
				// expected
			}
		}
		if (email != null && !email.isEmpty()) {
			// check if email is already taken
			try {
				node.getAgentIdForEmail(email);
				return Response.status(Status.BAD_REQUEST).entity("Email already taken").build();
			} catch (AgentNotFoundException e) {
				// expected
			}
		}
		// create new user agent and store in network
		// TODO: deduplicate this / AuthHandler#handleRegistration
		UserAgentImpl agent;
		if (node instanceof EthereumNode) {
			if (ethereumMnemonic != null && !ethereumMnemonic.isEmpty()) {
				agent = EthereumAgent.createEthereumAgent(username, password, ethereumMnemonic);
			} else {
				agent = EthereumAgent.createEthereumAgent(username, password);
			}
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
		JSONObject json = new JSONObject()
				.appendField("code", Status.OK.getStatusCode())
				.appendField("text", Status.OK.getStatusCode() + " - Agent created")
				.appendField("agentid", agent.getIdentifier())
				.appendField("username", agent.getLoginName())
				.appendField("email", agent.getEmail());
		if (agent instanceof EthereumAgent) {
			json.put("registryAddress", ((EthereumAgent) agent).getEthereumAddress());
		}
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/getAgent")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetAgent(@FormDataParam("agentid") String agentId, @FormDataParam("username") String username,
			@FormDataParam("email") String email) throws Exception {
		AgentImpl agent = getAgentByDetail(agentId, username, email);
		JSONObject json = new JSONObject();
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl userAgent = (UserAgentImpl) agent;
			json.put("username", userAgent.getLoginName());
			json.put("email", userAgent.getEmail());
		}
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/exportAgent")
	public Response handleExportAgent(@FormDataParam("agentid") String agentId,
			@FormDataParam("username") String username, @FormDataParam("email") String email) throws Exception {
		AgentImpl agent = getAgentByDetail(agentId, username, email);
		return Response.ok(agent.toXmlString(), MediaType.APPLICATION_XML).build();
	}

	private AgentImpl getAgentByDetail(String agentId, String username, String email) throws Exception {
		try {
			if (agentId == null || agentId.isEmpty()) {
				if (username != null && !username.isEmpty()) {
					agentId = node.getAgentIdForLogin(username);
				} else if (email != null && !email.isEmpty()) {
					agentId = node.getAgentIdForEmail(email);
				} else {
					throw new BadRequestException("No required agent detail provided");
				}
			}
			return node.getAgent(agentId);
		} catch (AgentNotFoundException e) {
			throw new BadRequestException("Agent not found");
		}
	}

	@POST
	@Path("/uploadAgent")
	public Response handleUploadAgent(@FormDataParam("agentFile") InputStream agentFile,
			@FormDataParam("password") String password,
			@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws Exception {
		if (agentFile == null) {
			return Response.status(Status.BAD_REQUEST).entity("No agent file provided").build();
		}
		AgentImpl agent = AgentImpl.createFromXml(agentFile);
		if (agent instanceof PassphraseAgentImpl) {
			PassphraseAgentImpl passphraseAgent = (PassphraseAgentImpl) agent;
			if (password == null) {
				return Response.status(Status.BAD_REQUEST).entity("No password provided").build();
			}
			try {
				passphraseAgent.unlock(password);
			} catch (AgentAccessDeniedException e) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid agent password").build();
			}
		} else if (agent instanceof GroupAgentImpl) {
			GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
			AgentSession session = connector.getSessionById(sessionId);
			if (session == null) {
				return Response.status(Status.BAD_REQUEST)
						.entity("You have to be logged in, to unlock and update a group").build();
			}
			try {
				groupAgent.unlock(session.getAgent());
			} catch (AgentAccessDeniedException e) {
				return Response.status(Status.FORBIDDEN).entity("You have to be a member of the uploaded group")
						.build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST)
					.entity("Invalid agent type '" + agent.getClass().getSimpleName() + "'").build();
		}
		node.storeAgent(agent);
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Agent uploaded");
		json.put("agentid", agent.getIdentifier());
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/changePassphrase")
	public Response handleChangePassphrase(@FormDataParam("agentid") String agentId,
			@FormDataParam("passphrase") String passphrase, @FormDataParam("passphraseNew") String passphraseNew,
			@FormDataParam("passphraseNew2") String passphraseNew2) throws Exception {
		if (agentId == null || agentId.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No agentid provided").build();
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentId);
		} catch (AgentNotFoundException e) {
			return Response.status(Status.BAD_REQUEST).entity("Agent not found").build();
		}
		if (!(agent instanceof PassphraseAgentImpl)) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid agent type").build();
		}
		PassphraseAgentImpl passAgent = (PassphraseAgentImpl) agent;
		if (passphrase == null || passphrase.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No passphrase provided").build();
		}
		try {
			passAgent.unlock(passphrase);
		} catch (AgentAccessDeniedException e) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid passphrase").build();
		}
		if (passphraseNew == null || passphraseNew.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No passphrase to change provided").build();
		}
		if (passphraseNew2 == null || passphraseNew2.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No passphrase repetition provided").build();
		}
		if (!passphraseNew.equals(passphraseNew2)) {
			return Response.status(Status.BAD_REQUEST).entity("New passphrase and repetition do not match").build();
		}
		passAgent.changePassphrase(passphraseNew);
		node.storeAgent(passAgent);
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Passphrase changed");
		json.put("agentid", agent.getIdentifier());
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/createGroup")
	public Response handleCreateGroup(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("members") String members) throws Exception {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in to create a group").build();
		}
		if (members == null) {
			return Response.status(Status.BAD_REQUEST).entity("No members provided").build();
		}
		JSONArray jsonMembers = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(members);
		if (jsonMembers.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Members list empty").build();
		}
		ArrayList<AgentImpl> memberAgents = new ArrayList<>(jsonMembers.size());
		for (Object objMember : jsonMembers) {
			if (objMember instanceof JSONObject) {
				JSONObject jsonMember = (JSONObject) objMember;
				String agentId = jsonMember.getAsString("agentid");
				try {
					memberAgents.add(node.getAgent(agentId));
				} catch (Exception e) {
					throw new ServerErrorException("Could not get member " + agentId, Status.INTERNAL_SERVER_ERROR, e);
				}
			}
		}
		GroupAgentImpl groupAgent = GroupAgentImpl
				.createGroupAgent(memberAgents.toArray(new AgentImpl[memberAgents.size()]));
		groupAgent.unlock(session.getAgent());
		node.storeAgent(groupAgent);
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - GroupAgent created");
		json.put("agentid", groupAgent.getIdentifier());
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/listAgents")
	public Response handleListAgents(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws AgentException, MalformedXMLException, IOException {
		/*
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in to load a group").build();
		}*/
		Random rand = new Random();
		//GroupAgentImpl groupAgent = MockAgentFactory.getGroup3();
		UserAgentImpl adamAgent = MockAgentFactory.getAdam();
		UserAgentImpl eveAgent = MockAgentFactory.getEve();
		UserAgentImpl abelAgent = MockAgentFactory.getAbel();
		
		UserAgentImpl[] agents = { adamAgent, eveAgent, abelAgent };
		
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - GroupAgent loaded");
		JSONArray memberList = new JSONArray();
		
		// TODO: query agent list from blockchain
		// TODO: figure out mapping blockchain <-> actors
		for( UserAgentImpl agent: agents)
		{
			JSONObject member = new JSONObject();
			member.put("agentid", agent.getIdentifier());
			
			member.put("username", agent.getLoginName());
			member.put("email", agent.getEmail());
			member.put("rating", rand.nextInt(6));
			
			memberList.add(member);
		}
		
		
		// TODO: query agent list from blockchain
		// TODO: figure out mapping blockchain <-> actors
		/*for (String memberid : groupAgent.getMemberList()) {
			JSONObject member = new JSONObject();
			member.put("agentid", memberid);
			try {
				AgentImpl memberAgent = node.getAgent(memberid);
				member.put("class", memberAgent.getClass().toString());
				if (memberAgent instanceof UserAgentImpl) {
					UserAgentImpl memberUserAgent = (UserAgentImpl) memberAgent;
					member.put("username", memberUserAgent.getLoginName());
					member.put("email", memberUserAgent.getEmail());
					member.put("rating", rand.nextInt(6));
				}
			} catch (AgentException e) {
				logger.log(Level.WARNING, "Could not retrieve group member agent from network", e);
			}
			memberList.add(member);
		}*/
		/*
		JSONObject member = new JSONObject();
		
		member.put("agentid", adamAgent.getIdentifier());
		member.put("username", adamAgent.getLoginName());
		member.put("email", adamAgent.getEmail());
		member.put("rating", rand.nextInt(6));
		memberList.add(member);
		*/
		
		json.put("members", memberList);
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/loadGroup")
	public Response handleLoadGroup(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("agentid") String agentId) throws AgentException {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in to load a group").build();
		}
		if (agentId == null || agentId.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No agent id provided").build();
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentId);
		} catch (AgentNotFoundException e) {
			return Response.status(Status.BAD_REQUEST).entity("Agent not found").build();
		}
		if (!(agent instanceof GroupAgentImpl)) {
			return Response.status(Status.BAD_REQUEST).entity("Agent is not a GroupAgent").build();
		}
		GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
		try {
			groupAgent.unlock(session.getAgent());
		} catch (AgentAccessDeniedException e) {
			return Response.status(Status.BAD_REQUEST).entity("You must be a member of this group").build();
		}
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - GroupAgent loaded");
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
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/changeGroup")
	public Response handleChangeGroup(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("agentid") String agentId, @FormDataParam("members") String members)
			throws AgentException, CryptoException, SerializationException, ParseException {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in to change a group").build();
		}
		if (agentId == null || agentId.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("No agent id provided").build();
		}
		if (members == null) {
			return Response.status(Status.BAD_REQUEST).entity("No members to change provided").build();
		}
		JSONArray changedMembers = (JSONArray) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(members);
		if (changedMembers.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Changed members list must not be empty").build();
		}
		AgentImpl agent;
		try {
			agent = node.getAgent(agentId);
		} catch (AgentNotFoundException e) {
			return Response.status(Status.BAD_REQUEST).entity("Agent not found").build();
		}
		if (!(agent instanceof GroupAgentImpl)) {
			return Response.status(Status.BAD_REQUEST).entity("Agent is not a GroupAgent").build();
		}
		GroupAgentImpl groupAgent = (GroupAgentImpl) agent;
		try {
			groupAgent.unlock(session.getAgent());
		} catch (AgentAccessDeniedException e) {
			return Response.status(Status.BAD_REQUEST).entity("You must be a member of this group").build();
		}
		// add new members
		HashSet<String> memberIds = new HashSet<>();
		for (Object obj : changedMembers) {
			if (obj instanceof JSONObject) {
				JSONObject json = (JSONObject) obj;
				String memberid = json.getAsString("agentid");
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
		if (!memberIds.contains(session.getAgent().getIdentifier().toLowerCase())) {
			return Response.status(Status.BAD_REQUEST).entity("You can't remove yourself from a group").build();
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
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - GroupAgent changed");
		json.put("agentid", groupAgent.getIdentifier());
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

}
