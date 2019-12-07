package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.util.AgentSession;
import i5.las2peer.connectors.webConnector.util.L2P_HTTPUtil;
import i5.las2peer.connectors.webConnector.util.L2P_JSONUtil;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.p2p.NodeNotFoundException;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.registry.ReadWriteRegistryClient;
import i5.las2peer.registry.data.GenericTransactionData;
import i5.las2peer.registry.data.UserProfileData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.tools.CryptoException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;
import rice.pastry.NodeHandle;

@Path(EthereumHandler.RESOURCE_PATH)
public class EthereumHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/eth";

	private final L2pLogger logger = L2pLogger.getInstance(EthereumHandler.class);

	private final WebConnector connector;
	private final Node node;
	private final EthereumNode ethereumNode;

	private final String SuccessMeasure_Label = "$_Success";

	public EthereumHandler(WebConnector connector) {
		this.connector = connector;
		this.node = connector.getL2pNode();
		ethereumNode = (node instanceof EthereumNode) ? (EthereumNode) node : null;
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

	public float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	private NodeInformation getLocalNodeInfo() throws EthereumException {
		NodeInformation localNodeInfo = null;
		try {
			localNodeInfo = node.getNodeInformation();
		} catch (CryptoException e) {
			logger.severe("trying to local access node info");
			e.printStackTrace();
			throw new EthereumException("Could not get nodeInformation of local node", e);
		}
		return localNodeInfo;
	}

	private Boolean isLocalAdmin(String agentEmail) throws EthereumException {
		NodeInformation localNodeInfo = getLocalNodeInfo();
		if (localNodeInfo == null)
			throw new EthereumException("local node info null");
		Boolean isLocalNodeAdmin = localNodeInfo.getAdminEmail().equals(agentEmail);
		return isLocalNodeAdmin;
	}

	private int queryLocalServices(Map<String, String> ethAgentAdminServices, Boolean isLocalAdmin) {
		ServiceAgentImpl[] localServices = node.getRegisteredServices();
		if (!isLocalAdmin) {
			for (ServiceAgentImpl localServiceAgent : localServices) {
				ServiceNameVersion nameVersion = localServiceAgent.getServiceNameVersion();
				ethAgentAdminServices.putIfAbsent(nameVersion.getName(), node.getNodeId().toString());
			}
		}
		return localServices.length;
	}

	private int queryRemoteServices(Map<String, String> ethAgentAdminServices, String agentEmail) {
		Collection<NodeHandle> knownNodes = ethereumNode.getPastryNode().getLeafSet().getUniqueSet();
		for (NodeHandle remoteNodeHandle : knownNodes) {
			NodeInformation remoteNodeInfo;
			try {
				remoteNodeInfo = ethereumNode.getNodeInformation(remoteNodeHandle);
				// is ethAgent admin of remote node?
				if (remoteNodeInfo.getAdminEmail().equals(agentEmail)) {
					// yes, query services
					List<ServiceNameVersion> servicesOnRemoteNode = remoteNodeInfo.getHostedServices();
					for (ServiceNameVersion removeSNV : servicesOnRemoteNode) {
						ethAgentAdminServices.putIfAbsent(removeSNV.getName(), remoteNodeHandle.toString());
					}
				}
			} catch (NodeNotFoundException e) {
				// logger.severe("trying to access node " + remoteNodeHandle.getNodeId() + " | "
				// + remoteNodeHandle.getId());
				// ignore malformed nodeinfo / missing node
				continue;
			}
		}

		return knownNodes.size();
	}

	@POST
	@Path("/requestFaucet")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleRequestFaucet(
		@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
		@FormDataParam("groupID") String groupID) 
	{
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}

		if ( groupID.length() < 120 )
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Must provide group agent id").build();
		}

		ReadWriteRegistryClient registryClient = ethereumNode.getRegistryClient();
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String agentEmail = ethAgent.getEmail();
		String ethAddress = ethAgent.getEthereumAddress();

		String apiBaseURL = "http://tech4comp.dbis.rwth-aachen.de:32319";//connector.getHttpEndpoint();
		String successBaseURL = apiBaseURL + "/mobsos-success-modeling";
		String successModelsURL = successBaseURL + "/apiv2/models";

		String successGroupURL = successModelsURL + "/" + groupID;
		
		Map<String, String> runningAdminServices = new HashMap<>();

		// get list of services for which this group has a success model
		float baseFaucetAmount = 1f;
		float servicesScore = 0f;
		float reward = 0f;
		try {
			logger.info("[ETH Faucet]: accessing success modeling group #" + groupID);
			String successGroupResponse = L2P_HTTPUtil.getHTTP(successGroupURL, "GET");
			List<String> servicesWithSuccessModel = L2P_JSONUtil.parseMobSOSGroup(successGroupResponse);
			
			logger.info("[ETH Faucet]: found " + servicesWithSuccessModel.size() + " services with success model.");
			int localServicesCount = queryLocalServices(runningAdminServices, isLocalAdmin(agentEmail));
			int remoteServicesCount = queryRemoteServices(runningAdminServices, agentEmail);

			logger.info("[ETH Faucet]: found " + ( localServicesCount + remoteServicesCount ) + " services ran by ethAgent");
			if ( localServicesCount + remoteServicesCount == 0 )
			{
				// no services running that the agent is admin of.
				servicesScore = 0f;
			}
			else
			{
				for(String svc: servicesWithSuccessModel)
				{
					if ( runningAdminServices.containsKey(svc) )
					{
						logger.info("[ETH Faucet]: found service ("+svc+") ran by ethAgent with a success model!");

						// svc is the name of the service which both: 
						// has a success model and is run by the ethAgent.
						String serviceSuccessMeasureURL = successGroupURL + "/" + svc + "/" + SuccessMeasure_Label;
						String serviceSuccessMeasureResponse = L2P_HTTPUtil.getHTTP(serviceSuccessMeasureURL, "GET");
						float successMeasure = L2P_JSONUtil.parseMobSOSSuccessResponse(serviceSuccessMeasureResponse);
						successMeasure = clamp( successMeasure, 0f, 5f);
						logger.info("[ETH Faucet]: service is rated " + successMeasure + " / 5");
						servicesScore += successMeasure;
						logger.info("[ETH Faucet]: score for services = " + servicesScore);
					}
				}
			}

		} catch (MalformedURLException | ServiceNotFoundException | ParseException | EthereumException e) {
			logger.severe("[ETH Faucet]: cannot get info from mobsos success modeling for given group: " + groupID);
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity("cannot get info from mobsos success modeling for given group: " + e.getMessage()).build();
		}
		reward = baseFaucetAmount + servicesScore;
		logger.info("[ETH Faucet]: calculating faucet amount: " + (reward) );

		BigInteger faucetAmount = Convert.toWei(Float.toString(reward), Convert.Unit.ETHER).toBigInteger();
		JSONObject json = new JSONObject();
		json.put("agentid", agent.getIdentifier());
		json.put("eth-target-add", ethAddress);
		json.put("eth-faucet-amount", Float.toString(reward) + " ETH");

		TransactionReceipt txR = null;

		try {
			txR = registryClient.sendEtherFromCoinbase(ethAddress, faucetAmount);
		} catch (EthereumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (txR == null || !txR.isStatusOK()) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction receipt not OK").build();
		}

		// Web3jUtils.weiToEther(gasUsed.multiply(Web3jConstants.GAS_PRICE))
		BigInteger gasUsed = txR.getCumulativeGasUsed();

		json.put("eth-gas-used", registryClient.weiToEther(gasUsed.multiply(registryClient.getGasPrice())).toString());

		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Faucet triggered. Amount transferred");

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/getEthWallet")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetWallet(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) // throws
																										// Exception
	{
		JSONObject json = new JSONObject();

		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}

		AgentImpl agent = session.getAgent();
		try {
			json = L2P_JSONUtil.addAgentDetailsToJson(ethereumNode, agent, json, true);

		} catch (EthereumException e) {
			return Response.status(Status.NOT_FOUND).entity("Agent not found").build();
		} catch (NotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity("Username not registered").build();
		}

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
	
	@GET
	@Path("/getAdminServices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetAdminServices(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws Exception {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}
		// get session eth agent
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String agentEmail = ethAgent.getEmail();
		JSONObject json = new JSONObject();

		Map<String, String> ethAgentAdminServices = new HashMap<>();
		
		// is ethAgent admin of local node?
		Boolean isLocalAdmin = isLocalAdmin(agentEmail);
		json.put("is-local-admin", isLocalAdmin);
		json.put("local-service-count", queryLocalServices(ethAgentAdminServices, isLocalAdmin));
		
		// query remote node infos
		json.put("known-node-count", queryRemoteServices(ethAgentAdminServices, agentEmail) );
		
		// translate local and remote service info into JSON array
		JSONObject jsonServices = new JSONObject();
		for (Map.Entry<String, String> entry : ethAgentAdminServices.entrySet()) 
		{
			jsonServices.put(entry.getKey(), entry.getValue());
		}
		json.put("eth-agent-admin-services", jsonServices );

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}



	@GET
	@Path("/getAdminNodes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetKnownNodeInfo(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws Exception {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}
		// get session eth agent
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String agentEmail = ethAgent.getEmail();
		JSONObject json = new JSONObject();

		JSONArray nodeList = new JSONArray();
		JSONArray adminNodeList = new JSONArray();

		// get local node info
		NodeInformation localNodeInfo = null;
		try {
			localNodeInfo = node.getNodeInformation();
		} catch (CryptoException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not get nodeInformation of local node").build();
		}
		if ( localNodeInfo != null )
		{
			nodeList.add(L2P_JSONUtil.nodeInformationToJSON(localNodeInfo));
			// is ethAgent admin of local node?
			if ( localNodeInfo.getAdminEmail().equals(agentEmail) )
			{
				adminNodeList.add(L2P_JSONUtil.nodeInformationToJSON(localNodeInfo));
			}
		}

		// get remote nodes info
		Collection<NodeHandle> knownNodes = ethereumNode.getPastryNode().getLeafSet().getUniqueSet();
		for (NodeHandle nodeHandle : knownNodes) {
			NodeInformation nodeInfo;
			try {
				nodeInfo = ethereumNode.getNodeInformation(nodeHandle);
				nodeList.add(L2P_JSONUtil.nodeInformationToJSON(nodeInfo));
				// is ethAgent admin of remote node?
				if ( nodeInfo.getAdminEmail().equals(agentEmail) )
				{
					adminNodeList.add(L2P_JSONUtil.nodeInformationToJSON(nodeInfo));
				}
			} catch (NodeNotFoundException e) {
				logger.severe("trying to access node " + nodeHandle.getNodeId() + " | " + nodeHandle.getId());
				e.printStackTrace();
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Trying to get nodeInformation failed: node not found").build();
			}
		}
		json.put("nodes", nodeList);
		json.put("adminNodes", nodeList);

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/getGenericTxLog")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetGenericTxLogSent(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId)
	{
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}
		// get session eth agent
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String agentAddress = ethAgent.getEthereumAddress();

		// query transaction log events
		List<GenericTransactionData> sentTxLog = ethAgent.getRegistryClient().getTransactionLogBySender(agentAddress);
		List<GenericTransactionData> rcvdTxLog = ethAgent.getRegistryClient().getTransactionLogByReceiver(agentAddress);
		
		// parse received log
		JSONObject json = new JSONObject();
		JSONArray rcvdJsonLog = new JSONArray();
		for (GenericTransactionData genericTransactionData : rcvdTxLog) {
			rcvdJsonLog.add(L2P_JSONUtil.genericTransactionDataToJSON( genericTransactionData ));
		}

		// parse sent log
		JSONArray sentJsonLog = new JSONArray();
		for (GenericTransactionData genericTransactionData : sentTxLog) {
			sentJsonLog.add(L2P_JSONUtil.genericTransactionDataToJSON( genericTransactionData ));
		}
		
		json.put("rcvdJsonLog", rcvdJsonLog);
		json.put("sentJsonLog", sentJsonLog);
		
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/registerProfile")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleRegisterProfile(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws MalformedXMLException, IOException
	{
		JSONObject json = new JSONObject();
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			throw new BadRequestException("You have to be logged in");
		}
		
		EthereumAgent agent = (EthereumAgent) session.getAgent();
		try {
			String txHash = agent.getRegistryClient().registerReputationProfile(agent);
			json.put("callTransactionHash", txHash);
		} catch (EthereumException e) {		
			e.printStackTrace();	
			throw new BadRequestException("Profile registration failed", e);
		}
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Profile creation successful");
		
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/listAgents")
	public Response handleListAgents(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) throws MalformedXMLException, IOException {
		/*
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in to load a group").build();
		}
		*/
		
		List<EthereumAgent> agents = new ArrayList<EthereumAgent>();
		ConcurrentMap<String, String> userRegistrations = ethereumNode.getRegistryClient().getUserRegistrations();
		
		for( Map.Entry<String, String> userRegistration : userRegistrations.entrySet() )
		{
			String username = userRegistration.getKey().toString();
			AgentImpl userAgent = null;
			try {
				userAgent = getAgentByDetail(null, username, null);
				userAgent = ethereumNode.getAgent(userAgent.getIdentifier());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if ( userAgent instanceof EthereumAgent )
			{
				agents.add((EthereumAgent)userAgent);
			}
		}
		
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - UserList loaded");
		JSONArray memberList = new JSONArray();
		
		for( EthereumAgent agent: agents)
		{
			String ownerAddress = "";
			try {
				ownerAddress = ethereumNode.getRegistryClient().getUser(agent.getLoginName()).getOwnerAddress();
				logger.info("found user ["+agent.getLoginName()+"|"+ownerAddress+"]");
			} catch (EthereumException | NotFoundException e) {
				throw new BadRequestException("cannot get ethereum owner address for user agent " + agent.getLoginName());
			}
			if ( ownerAddress == "" ) {
				ownerAddress = agent.getEthereumAddress();
			}
			JSONObject member = new JSONObject();
			member.put("agentid", agent.getIdentifier());
			member.put("address", ownerAddress);
			
			
			member.put("username", agent.getLoginName());
			member.put("email", agent.getEmail());
			//member.put("rating", rand.nextInt(6));
			
			memberList.add(member);
		}
		
		json.put("members", memberList);
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/listProfiles")
	public Response handleListProfiles(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) {
		JSONObject json = new JSONObject();
		
		List<EthereumAgent> agents = new ArrayList<EthereumAgent>();
		ConcurrentMap<String, String> userProfiles = ethereumNode.getRegistryClient().getUserProfiles();
		for (Map.Entry<String, String> userProfile : userProfiles.entrySet()) {
			String owner = userProfile.getKey().toString();
			String username = userProfile.getValue().toString();
			logger.info("found profile: " + username + " @ " + owner);
			AgentImpl userAgent = null;
			try {
				userAgent = getAgentByDetail(null, username, null);
				String agentId = userAgent.getIdentifier();
				logger.fine("found matching user agent: " + agentId);
				userAgent = ethereumNode.getAgent(agentId);
				logger.fine("found matching eth agent: " + agentId);
			} catch (Exception e) {
				e.printStackTrace();
				throw new BadRequestException("cannot get ethereum agent by username", e);
			}
			if (userAgent instanceof EthereumAgent) {
				agents.add((EthereumAgent) userAgent);
			}
		}

		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - UserList loaded, found " + agents.size() + " agents");
		JSONArray memberList = new JSONArray();
		
		for (EthereumAgent agent : agents) {
			String ownerAddress = agent.getEthereumAddress();
			UserProfileData profile;
			try {
				logger.info("accessing profile of " + ownerAddress);
				profile = ethereumNode.getRegistryClient().getProfile(ownerAddress);
			} catch (EthereumException | NotFoundException e) {
				e.printStackTrace();
				throw new BadRequestException("cannot get profile for agent", e);
			}
			
			JSONObject member = new JSONObject();
			member.put("agentid", agent.getIdentifier());
			member.put("address", ownerAddress);
			member.put("username", agent.getLoginName());
			//member.put("email", agent.getEmail());
			BigInteger cumulativeScore = profile.getCumulativeScore();
			BigInteger noTransactionsSent = profile.getNoTransactionsSent();
			BigInteger noTransactionsRcvd = profile.getNoTransactionsRcvd();
			member.put("ethProfileOwner", profile.getOwner());
			member.put("cumulativeScore", cumulativeScore.toString());
			member.put("noOfTransactionsSent", noTransactionsSent.toString());
			member.put("noOfTransactionsRcvd", noTransactionsRcvd.toString());
			if ( noTransactionsRcvd.compareTo(BigInteger.ZERO) == 0 )
			{
				member.put("rating", 0);
			}
			else
			{
				member.put("rating", cumulativeScore.divide(noTransactionsRcvd));
			}

			memberList.add(member);
		}
		
		json.put("members", memberList);
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/rateAgent")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleRateAgent(
			@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("agentid") String agentId,
			@FormDataParam("rating") Integer rating
		) throws Exception {
		
		// check login
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			throw new BadRequestException("You have to be logged in to rate");
		}
		EthereumAgent ethAgent = (EthereumAgent) session.getAgent();

		EthereumAgent recipientAgent = null;
		try {
			recipientAgent = (EthereumAgent) ethereumNode.getAgent(agentId);
			if (!ethereumNode.getRegistryClient().hasReputationProfile(recipientAgent.getEthereumAddress())) {
				throw new NotFoundException("recipient profile not found");
			}
			// add transaction
			// rating must be between amountMin and amountMax
			try {
				ethAgent.getRegistryClient().addUserRating(recipientAgent, rating);
			} catch (EthereumException e) {
				throw new BadRequestException("Profile rating failed: ",e);
			}
		}
		catch (AgentException e)
		{
			throw new NotFoundException("recipient eth agent not found",e);
		}
		
		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - UserProfile rating added");
		json.put("recipientaddress", recipientAgent.getEthereumAddress());
		json.put("recipientname", recipientAgent.getLoginName());
		json.put("rating", rating);
		
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Path("/addTransaction")
	public Response handleAddTransaction(
			@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("agentid") String agentId, 
			@FormDataParam("weiAmount") Float weiAmount,
			@FormDataParam("message") String message
		) throws Exception {
		// check login
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			throw new BadRequestException("You have to be logged in to rate");
		}
		EthereumAgent ethAgent = (EthereumAgent) session.getAgent();

		BigDecimal weiAmountBD = Convert.toWei(weiAmount.toString(), Convert.Unit.ETHER);
		BigInteger weiAmountBI = weiAmountBD.toBigInteger();

		logger.info("[ETH] sending weiAmount: " + weiAmount.toString() );

		EthereumAgent recipientAgent = null;
		try {
			recipientAgent = (EthereumAgent) ethereumNode.getAgent(agentId);
			try {
				ethAgent.getRegistryClient().addGenericTransaction(ethAgent, recipientAgent, message, weiAmountBI);
			} catch (EthereumException e) {
				throw new BadRequestException("Generic transaction failed: ", e);
			}
		} catch (AgentException e) {
			throw new NotFoundException("recipient eth agent not found", e);
		}

		JSONObject json = new JSONObject();
		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Generic Ether Transaction added");
		json.put("recipientAddress", recipientAgent.getEthereumAddress());
		json.put("recipientName", recipientAgent.getLoginName());
		json.put("weiAmount", Convert.fromWei(Convert.toWei(weiAmount.toString(), Convert.Unit.ETHER), Convert.Unit.ETHER));
		json.put("message", message);

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();		
	}

}
