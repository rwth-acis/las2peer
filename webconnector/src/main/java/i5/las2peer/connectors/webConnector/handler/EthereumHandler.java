package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.security.UserAgent;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.registry.data.ServiceDeploymentData;
import i5.las2peer.security.*;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.util.AgentSession;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.registry.ReadOnlyRegistryClient;
import i5.las2peer.registry.ReadWriteRegistryClient;
import i5.las2peer.registry.data.GenericTransactionData;
import i5.las2peer.registry.data.UserData;
import i5.las2peer.registry.data.UserProfileData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

@Path(EthereumHandler.RESOURCE_PATH)
public class EthereumHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/eth";

	private final L2pLogger logger = L2pLogger.getInstance(EthereumHandler.class);

	private final WebConnector connector;
	private final Node node;
	private final EthereumNode ethereumNode;

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

	private JSONObject addAgentDetailsToJson(AgentImpl agent, JSONObject json)
			throws EthereumException, NotFoundException
	{
		// don't add mnemonic ( = defaults to false, override to true )
		return this.addAgentDetailsToJson(agent, json, false);
	}
	private JSONObject addAgentDetailsToJson(AgentImpl agent, JSONObject json, Boolean addMnemonic)
			throws EthereumException, NotFoundException {
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl userAgent = (UserAgentImpl) agent;
			json.put("username", userAgent.getLoginName());
			json.put("email", userAgent.getEmail());
		}
		if (agent instanceof EthereumAgent) 
		{
			EthereumAgent ethAgent = (EthereumAgent) agent;
			String ethAddress = ethAgent.getEthereumAddress();
			if ( ethAddress.length() > 0 )
			{
				json.put("ethAgentAddress", ethAddress);
				String accBalance = ethereumNode.getRegistryClient().getAccountBalance(ethAddress);
				json.put("ethAccBalance", accBalance);
				UserProfileData upd = null;
				try {
					upd = ethereumNode.getRegistryClient().getProfile(ethAddress);
					if (upd != null && !upd.getOwner().equals("0x0000000000000000000000000000000000000000")) {
						json.put("ethProfileOwner", upd.getOwner());
						json.put("ethCumulativeScore", upd.getCumulativeScore().toString());
						json.put("ethNoTransactionsSent", upd.getNoTransactionsSent().toString());
						json.put("ethNoTransactionsRcvd", upd.getNoTransactionsRcvd().toString());
						if ( upd.getNoTransactionsRcvd().compareTo(BigInteger.ZERO) == 0 )
						{
							json.put("ethRating", 0);
						}
						else
						{
							json.put("ethRating", upd.getCumulativeScore().divide(upd.getNoTransactionsRcvd()));
						}
					} else {
						json.put("ethRating", "0");
						json.put("ethCumulativeScore", "???");
						json.put("ethNoTransactionsSent", "???");
						json.put("ethNoTransactionsRcvd", "???");
					}
				}
				catch (EthereumException| NotFoundException e)
				{
					e.printStackTrace();
				}

			}
			
			json.put("ethAgentCredentialsAddress", ethAddress);
			
			if ( addMnemonic && !agent.isLocked())
			{
				json.put("ethMnemonic", ethAgent.getEthereumMnemonic());
			}
			
			
			//UserData ethUser = ethereumNode.getRegistryClient().getUser(ethAgent.getLoginName());
			//if ( ethUser != null ) { json.put("eth-user-address", ethUser.getOwnerAddress()); }
		}
		return json;
	}
	
	@POST
	@Path("/requestFaucet")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handlerequestFaucet(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) //throws Exception
	{
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}
		
		ReadWriteRegistryClient registryClient = ethereumNode.getRegistryClient();
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String ethAddress = ethAgent.getEthereumAddress();
		BigInteger faucetAmount = Convert.toWei("0.8", Convert.Unit.ETHER).toBigInteger(); // const FAUCET_AMOUNT
		JSONObject json = new JSONObject();
		json.put("agentid", agent.getIdentifier());
		json.put("eth-target-add", ethAddress);
		json.put("eth-faucet-amount", "0.8 ETH");
		
		TransactionReceipt txR = null;
		
		try {
			txR = registryClient.sendEtherFromCoinbase(ethAddress, faucetAmount);
		} catch (EthereumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ( txR == null || !txR.isStatusOK() )
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Transaction receipt not OK").build();
		}
		
		//Web3jUtils.weiToEther(gasUsed.multiply(Web3jConstants.GAS_PRICE))
		BigInteger gasUsed = txR.getCumulativeGasUsed();
		
		json.put("eth-gas-used", registryClient.weiToEther(gasUsed.multiply(registryClient.getGasPrice())).toString());
		
		
		json.put("code", Status.OK.getStatusCode());		
		json.put("text", Status.OK.getStatusCode() + " - Faucet triggered. Amount transferred");
		
		
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Path("/getEthWallet")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleGetWallet(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId) //throws Exception
	{
		JSONObject json = new JSONObject();

		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		
		AgentImpl agent = session.getAgent();
		try {
			json = addAgentDetailsToJson(agent, json, true);
			
		} catch (EthereumException e) {
			return Response.status(Status.NOT_FOUND).entity("Agent not found").build();
		} catch (NotFoundException e) {
			return Response.status(Status.NOT_FOUND).entity("Username not registered").build();
		}
		
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	private JSONObject transactionDataToJSONObject(GenericTransactionData genericTransactionData) {
		    
        JSONObject thisJSON = new JSONObject();

        thisJSON.put("txSender", genericTransactionData.getSender());
        thisJSON.put("txReceiver", genericTransactionData.getReceiver());
        thisJSON.put("txMessage", genericTransactionData.getMessage());
        thisJSON.put("txAmountInWei", genericTransactionData.getAmountInWei() );
        thisJSON.put("txAmountInEth", genericTransactionData.getAmountInEth());
        thisJSON.put("txTXHash", genericTransactionData.getTXHash() );
        thisJSON.put("txTimestamp", genericTransactionData.getTimestamp() );
        thisJSON.put("txDateTime", genericTransactionData.getTime());
        thisJSON.put("txTransactionType", genericTransactionData.getTransactionType() );

        return thisJSON;
    
	}

	@GET
	@Path("/getServicesByAgent")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServicesByAgent(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId)
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
		String agentName = ethAgent.getLoginName();
		JSONObject json = new JSONObject();
		
		ConcurrentMap<String, String> serviceAuthors = ethereumNode.getRegistryClient().getServiceAuthors();

		
		List<String> serviceMap = new ArrayList<String>();
		for(Map.Entry<String,String> entry : serviceAuthors.entrySet())
		{
			if ( entry.getValue().equals( agentName ) )
			{
				serviceMap.add(entry.getKey());
			}
		}
		json.put("services", serviceMap);
		

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
			rcvdJsonLog.add(transactionDataToJSONObject(genericTransactionData));
		}

		// parse sent log
		JSONArray sentJsonLog = new JSONArray();
		for (GenericTransactionData genericTransactionData : sentTxLog) {
			sentJsonLog.add(transactionDataToJSONObject(genericTransactionData));
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
		Random rand = new Random();
		//GroupAgentImpl groupAgent = MockAgentFactory.getGroup3();
		UserAgentImpl adamAgent = MockAgentFactory.getAdam();
		UserAgentImpl eveAgent = MockAgentFactory.getEve();
		UserAgentImpl abelAgent = MockAgentFactory.getAbel();
		
		//UserAgentImpl[] agents = { adamAgent, eveAgent, abelAgent };
		List<EthereumAgent> agents = new ArrayList<EthereumAgent>();
		//agents.add(adamAgent);
		//agents.add(eveAgent);
		//agents.add(abelAgent);
		
		//observer.getUserProfiles();
		//ConcurrentMap<String, String> profiles = ethereumNode.getRegistryClient().getUserProfiles();
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
			member.put("rating", rand.nextInt(6));
			
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
