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
import java.util.concurrent.ExecutionException;

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
import org.web3j.protocol.core.methods.response.Transaction;
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
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.data.SenderReceiverDoubleKey;
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
import rice.pastry.PastryNode;

@Path(EthereumHandler.RESOURCE_PATH)
public class EthereumHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/eth";

	private final L2pLogger logger = L2pLogger.getInstance(EthereumHandler.class);

	private final WebConnector connector;
	private final Node node;
	private final PastryNode pastryNode;
	private final EthereumNode ethereumNode;

	public EthereumHandler(WebConnector connector) {
		this.connector = connector;
		this.node = connector.getL2pNode();
		this.pastryNode = (node instanceof PastryNodeImpl) ? ((PastryNodeImpl) node).getPastryNode() : null;
		this.ethereumNode = (node instanceof EthereumNode) ? (EthereumNode) node : null;
	}

	public float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	private int queryLocalServices(Map<String, String> ethAgentAdminServices, Boolean isLocalAdmin) {
		ServiceAgentImpl[] localServices = node.getRegisteredServices();
		if (isLocalAdmin) {
			for (ServiceAgentImpl localServiceAgent : localServices) {
				ServiceNameVersion nameVersion = localServiceAgent.getServiceNameVersion();
				logger.info("[local SVC]: found service " + nameVersion.toString());
				ethAgentAdminServices.putIfAbsent(nameVersion.getName(), node.getNodeId().toString());
			}
		} else {
			logger.info("[local SVC]: ethAgent is not local admin, omitting local services");
		}

		return localServices.length;
	}

	private int queryRemoteServicesWithoutBlockchain(Map<String, String> ethAgentAdminServices, String agentEmail) {
		Collection<NodeHandle> knownNodes = ethereumNode.getPastryNode().getLeafSet().getUniqueSet();
		logger.info("[remote nodeInfo-SVC] checking " + knownNodes.size() + " known remote nodes, see if " + agentEmail
				+ " is admin there...");
		for (NodeHandle remoteNodeHandle : knownNodes) {
			NodeInformation remoteNodeInfo;
			try {
				remoteNodeInfo = ethereumNode.getNodeInformation(remoteNodeHandle);
				logger.info("[remote nodeInfo-SVC]: querying node #" + remoteNodeHandle.getNodeId() + ", admin: "
						+ remoteNodeInfo.getAdminEmail());
				// is ethAgent admin of remote node?
				if (remoteNodeInfo.getAdminEmail().equals(agentEmail)) {
					// yes, query services
					List<ServiceNameVersion> servicesOnRemoteNode = remoteNodeInfo.getHostedServices();
					for (ServiceNameVersion removeSNV : servicesOnRemoteNode) {
						logger.info("[remote nodeInfo-SVC]: found service " + removeSNV.toString());
						ethAgentAdminServices.putIfAbsent(removeSNV.getName(), remoteNodeHandle.toString());
					}
				} else {
					logger.info("[remote nodeInfo-SVC]: ethAgent is not remote admin, omitting node");
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

	public List<String> getNodeIDsOfAdminNodes(String agentEmail) throws EthereumException {
		List<String> adminNodeIDs = new ArrayList<>();
		if (ethereumNode.isLocalAdmin(agentEmail)) {
			adminNodeIDs.add(pastryNode.getNodeId().toStringFull());
		}

		// get remote nodes info
		Collection<NodeHandle> knownNodes = pastryNode.getLeafSet().getUniqueSet();
		for (NodeHandle nodeHandle : knownNodes) {
			NodeInformation nodeInfo;
			try {
				nodeInfo = ethereumNode.getNodeInformation(nodeHandle);
				// is ethAgent admin of remote node?
				if (nodeInfo.getAdminEmail().equals(agentEmail)) {
					adminNodeIDs.add(nodeHandle.getNodeId().toStringFull());
				}
			} catch (NodeNotFoundException e) {
				logger.severe("trying to access node " + nodeHandle.getNodeId() + " | " + nodeHandle.getId());
				e.printStackTrace();
				continue;
				// return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Trying to get
				// nodeInformation failed: node not found").build();
			}
		}
		return adminNodeIDs;
	}

	@POST
	@Path("/requestFaucet")
	@Produces(MediaType.APPLICATION_JSON)
	public Response handleRequestFaucet(@CookieParam(WebConnector.COOKIE_SESSIONID_KEY) String sessionId,
			@FormDataParam("groupID") String groupID) {
		AgentSession session = connector.getSessionById(sessionId);
		if (session == null) {
			return Response.status(Status.FORBIDDEN).entity("You have to be logged in").build();
		}
		AgentImpl agent = session.getAgent();
		if (!(agent instanceof EthereumAgent)) {
			return Response.status(Status.FORBIDDEN).entity("Must be EthereumAgent").build();
		}

		if (groupID.length() < 120) 
		{
			groupID = "";
		}

		if ( groupID == "" )
		{
			logger.info("[ETH Faucet]: no valid groupID provided, skipping service calculation" );
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("no valid groupID provided, no service calculation possible").build();
		}

		ReadWriteRegistryClient registryClient = ethereumNode.getRegistryClient();
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String coinbase;
		try {
			coinbase = registryClient.getCoinbase().getResult();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Couldn't get ethereum address: ").build();
		}
		String agentLogin = ethAgent.getLoginName();
		String agentEmail = ethAgent.getEmail();
		String ethAddress = ethAgent.getEthereumAddress();

		String apiBaseURL = "http://tech4comp.dbis.rwth-aachen.de:32319";//connector.getHttpEndpoint();
		String successBaseURL = apiBaseURL + "/mobsos-success-modeling";
		String successModelsURL = successBaseURL + "/apiv2/models";

		String successGroupURL = successModelsURL + "/" + groupID;

		
		//float baseFaucetAmount = RegistryConfiguration.Faucet_baseFaucetAmount;
		float reward = 0f;
		float hostingServicesScore_Raw = 0f;
		float developServicesScore_Raw = 0f;
		float userRatingScore_Raw = 0f;

		JSONArray hostingServicesRatedByFaucet = new JSONArray();
		JSONArray developServicesRatedByFaucet = new JSONArray();
		List<String> developedServices = new ArrayList<>();
		Map<String, Integer> hostingServicesToAnnouncementCount = new HashMap<>();
		Map<String, Integer> authoredServicesToAnnouncementCount = new HashMap<>();
		
		Map<String, Float> hostingServiceValue = new HashMap<>();
		Map<String, Float> developServiceValue = new HashMap<>();

		// get admin nodeIDs
		List<String> adminNodeIDs = new ArrayList<>();
		try {
			adminNodeIDs = getNodeIDsOfAdminNodes(agentEmail);
		}
		catch( EthereumException e )
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Couldn't query local node info").build();
		}



		// get transaction log and find last transaction sent from coinbase to ethAgent's account
		SenderReceiverDoubleKey coinbaseToEthAgent = new SenderReceiverDoubleKey(coinbase, ethAddress);
		BigInteger largestBlockNo = BigInteger.ZERO;
		largestBlockNo = queryLargestBlockNo(registryClient, coinbaseToEthAgent);
		
		logger.info("[ETH Faucet]: last transaction to this ethAgent was on block " + largestBlockNo );




		// query mobsos success models to see for which services we have a succes model for this group
		logger.info("[ETH Faucet/MobSOS]: accessing success modeling group #" + groupID);
		String successGroupResponse;
		List<String> servicesWithSuccessModel = new ArrayList<>();
		try {
			successGroupResponse = L2P_HTTPUtil.getHTTP(successGroupURL, "GET");
			servicesWithSuccessModel = L2P_JSONUtil.parseMobSOSGroupServiceNames(successGroupResponse);
		} catch (MalformedURLException | ServiceNotFoundException | ParseException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Couldn't query mobsos success model for group #" + groupID).build();
		}
		logger.info("[ETH Faucet/MobSOS]: found " + servicesWithSuccessModel.size() + " services with success model.");

		if ( servicesWithSuccessModel.size() == 0 )
		{
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Mobsos success model for group #" + groupID + " seems empty, aborting").build();
		}

		// go through services with success model and see if they're running and how often were they announced
		for( String serviceWithSuccessModel : servicesWithSuccessModel )
		{
			logger.info("[ETH Faucet/MobSOS]: processing service '"+serviceWithSuccessModel+"':");

			// check mobsos success model:

			logger.info("[ETH Faucet/MobSOS]: querying service Hosting value: ");
			String serviceSuccessMeasureURL_Hosting = successGroupURL + "/" + serviceWithSuccessModel + "/" + RegistryConfiguration.MobSOS_SuccessMeasure_Hosting_Label;
			float serviceHostingValue = getSuccessMeasure(serviceSuccessMeasureURL_Hosting);

			logger.info("[ETH Faucet/MobSOS]: querying service Develop value: ");
			String serviceSuccessMeasureURL_Develop = successGroupURL + "/" + serviceWithSuccessModel + "/" + RegistryConfiguration.MobSOS_SuccessMeasure_Develop_Label;
			float serviceDevelopValue = getSuccessMeasure(serviceSuccessMeasureURL_Develop);



			// check if ethAgent is developer/author of service
			String authorOfService = registryClient.getServiceAuthor(serviceWithSuccessModel);
			Boolean isAuthorOfService = false;
			if ( authorOfService != "" )
			{
				logger.info("[ETH Faucet/MobSOS]: checking for service authorship (comparing '" + authorOfService + "' with '"+agentLogin+"'):");
				if ( authorOfService.equals(agentLogin) )
				{
					logger.info("         .!.         ethAgent is developer/author of this service.");
					isAuthorOfService = true;
					developedServices.add(serviceWithSuccessModel);
				}
			}

			logger.info("[ETH Faucet/MobSOS]: checking how often service '" + serviceWithSuccessModel + "' has been announced since last faucet request.");

			HashMap<String, Integer> serviceAnnouncementsPerNodeID = registryClient.getNoOfServiceAnnouncementSinceBlockOrderedByHostingNode(
				largestBlockNo, serviceWithSuccessModel
			);

			// check if service is hosted by ethAgent
			for( String adminNodeID: adminNodeIDs)
			{
				Integer serviceAnnouncementCount = serviceAnnouncementsPerNodeID.get(adminNodeID);
				// check if one of the nodes running this service is administered by ethAgent
				if ( serviceAnnouncementsPerNodeID.containsKey(adminNodeID) )
				{
					// found a service that has a success model and is hosted by ethAgent
					logger.info("         .!.         found " + serviceAnnouncementCount + " announcements for node # " + adminNodeID);
					hostingServicesToAnnouncementCount.putIfAbsent(serviceWithSuccessModel, 0);
					hostingServicesToAnnouncementCount.merge( 
						serviceWithSuccessModel, // key
						serviceAnnouncementCount, // value to 'merge' with
						Integer::sum // function to use for mergin
					);
				}
				// check if this service was developed by ethAgent
				if ( isAuthorOfService )
				{
					authoredServicesToAnnouncementCount.putIfAbsent(serviceWithSuccessModel, 0);
					authoredServicesToAnnouncementCount.merge(
						serviceWithSuccessModel, // key
						serviceAnnouncementCount, // value to 'merge' with
						Integer::sum // function to use for mergin
					);
				}
			}
			float serviceWasAnnouncedByEthAgent = hostingServicesToAnnouncementCount.getOrDefault( serviceWithSuccessModel, 0 );
			serviceWasAnnouncedByEthAgent *= serviceHostingValue;
			if ( serviceWasAnnouncedByEthAgent > 0 )
			{
				logger.info("[ETH Faucet]: service '" + serviceWithSuccessModel + "' is valued for hosting at "+serviceHostingValue+" and was announced " + (serviceWasAnnouncedByEthAgent/serviceHostingValue) + " times. ");
				hostingServiceValue.put( serviceWithSuccessModel, serviceWasAnnouncedByEthAgent );
				hostingServicesRatedByFaucet.add(serviceWithSuccessModel);
			}

			float serviceDevelopedByEthAgentWasAnnounced = authoredServicesToAnnouncementCount.getOrDefault( serviceWithSuccessModel, 0 );
			serviceDevelopedByEthAgentWasAnnounced *= serviceDevelopValue;
			if ( serviceDevelopedByEthAgentWasAnnounced > 0 )
			{
				logger.info("[ETH Faucet]: service '" + serviceWithSuccessModel + "' was developed by agent. Its development is rated at "+serviceDevelopValue+" and it was announced " + (serviceDevelopedByEthAgentWasAnnounced/serviceDevelopValue) + " times. ");
				developServiceValue.put( serviceWithSuccessModel, serviceDevelopedByEthAgentWasAnnounced );
				developServicesRatedByFaucet.add(serviceWithSuccessModel);
			}
		}

		// QUERY USER REPUTATION PROFILE
		userRatingScore_Raw = ethereumNode.getRegistryClient().getUserRating(ethAddress);

		// user needs ether to request reputation profile
		// user without profile will have score of 0
		// multiplying score onto faucet amount will disallow fresh users from entering the community
		if ( userRatingScore_Raw == 0f ) 
		{
			userRatingScore_Raw = 1f;
			logger.info("[ETH Faucet]: user has no profile. setting userRating multipler to 1");
		}

		// sum all values of hosted and developed services
		for( Float serviceSucces: hostingServiceValue.values() )
		{
			hostingServicesScore_Raw += serviceSucces;
		}
		
		for( Float serviceSucces: developServiceValue.values() )
		{
			developServicesScore_Raw += serviceSucces;
		}

		float userRatingScore = userRatingScore_Raw * RegistryConfiguration.Faucet_userScoreMultiplier;
		float hostingServicesScore = hostingServicesScore_Raw * RegistryConfiguration.Faucet_serviceHostingScoreMultiplier;
		float developServicesScore = developServicesScore_Raw * RegistryConfiguration.Faucet_serviceDevelopScoreMultiplier;

		if ( RegistryConfiguration.Faucet_serviceMaxScore != -1f )
		{
			logger.info("[ETH Faucet]: hosting max score ("+RegistryConfiguration.Faucet_serviceMaxScore+") applied [was: "+hostingServicesScore+"].");
			hostingServicesScore = Math.min(RegistryConfiguration.Faucet_serviceMaxScore, hostingServicesScore);
		}

		if ( RegistryConfiguration.Faucet_developMaxScore != -1f )
		{
			logger.info("[ETH Faucet]: develop max score ("+RegistryConfiguration.Faucet_developMaxScore+") applied [was: "+developServicesScore+"].");
			developServicesScore = Math.min(RegistryConfiguration.Faucet_developMaxScore, developServicesScore);
		}

		reward = ( hostingServicesScore + developServicesScore );
		if ( userRatingScore != 0f ) reward *= userRatingScore;
		logger.info("[ETH Faucet]: calculating faucet amount:" );
		if ( userRatingScore != 0f )
		{
			logger.info(	
				"> userRatingScore: " + 
				"   " + Float.toString(RegistryConfiguration.Faucet_userScoreMultiplier) + "*" + 
				"   " + Float.toString(userRatingScore_Raw) + " = " + 
				"   " + Float.toString(userRatingScore) + " ETH \n" + 
				"> * \n"
			);
		}
			logger.info("    ( \n" + 
				"       hostingServicesScore: " + 
				"        " + Float.toString(RegistryConfiguration.Faucet_serviceHostingScoreMultiplier) + "*" + 
				"        " + Float.toString(hostingServicesScore_Raw) + " = " + 
				"        " + Float.toString(hostingServicesScore) + " ETH \n" +
				"     + developServicesScore: " + 
				"        " + Float.toString(RegistryConfiguration.Faucet_serviceDevelopScoreMultiplier) + "*" + 
				"        " + Float.toString(developServicesScore_Raw) + " = " + 
				"        " + Float.toString(developServicesScore) + " ETH \n" +
				"    ) \n" + 
				"------------------------\n" +
				"= " + reward + " ETH"
			 );

		if ( reward == 0f && largestBlockNo.equals(BigInteger.ZERO) )
		{
			// user never fauceted, payout base amount
			reward = RegistryConfiguration.Faucet_baseFaucetAmount;
			logger.info("[ETH Faucet]: detected new user (no reward, no last transaction).");
			logger.info("[ETH Faucet]: setting payout to " + Float.toString(reward) + " ETH.");
		}

		BigInteger faucetAmount = Convert.toWei(Float.toString(reward), Convert.Unit.ETHER).toBigInteger();

		JSONObject json = new JSONObject();
		json.put("agentid", agent.getIdentifier());
		json.put("ethTargetAdd", ethAddress);
		json.put("ethFaucetAmount", Float.toString(reward) + " ETH");

		json.put("rewardDetails", 
			new JSONObject()
				.appendField("userRatingScore", userRatingScore)
				.appendField("hostingServicesScore", hostingServicesScore)
				.appendField("developServicesScore", developServicesScore)
				.appendField("rewardedForServicesHosting", hostingServicesRatedByFaucet)
				.appendField("rewardedForServicesDevelop", developServicesRatedByFaucet)
				.appendField("u", RegistryConfiguration.Faucet_userScoreMultiplier)
				.appendField("h", RegistryConfiguration.Faucet_serviceHostingScoreMultiplier)
				.appendField("d", RegistryConfiguration.Faucet_serviceDevelopScoreMultiplier)
		);

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

		json.put("ethGasUsed", registryClient.weiToEther(gasUsed.multiply(registryClient.getGasPrice())).toString());

		json.put("code", Status.OK.getStatusCode());
		json.put("text", Status.OK.getStatusCode() + " - Faucet triggered. Amount transferred");

		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	private BigInteger queryLargestBlockNo(
		ReadWriteRegistryClient registryClient, 
		SenderReceiverDoubleKey searchKey
	) {
		BigInteger largestBlockNo = BigInteger.ZERO;
		if ( registryClient.getTransactionLog().containsKey(searchKey) )
		{
			List<Transaction> coinbaseTransactionLog = registryClient.getTransactionLog().get(searchKey);
			if ( coinbaseTransactionLog.size() > 0 )
			{
				logger.info("[ETH TxLog]: found "+coinbaseTransactionLog.size()+" transactions, finding largest block no " );
				for(Transaction transaction: coinbaseTransactionLog)
				{
					if ( transaction.getBlockNumber().compareTo(largestBlockNo) == 1 )
					{
						largestBlockNo = transaction.getBlockNumber();
						logger.info("[ETH TxLog]:   found transaction on block " + largestBlockNo );
					}
				}
				logger.info("[ETH TxLog]: got " + largestBlockNo.toString() + " as largest block. " );
			}
		}
		return largestBlockNo;
	}

	private float getSuccessMeasure(String serviceSuccessMeasureURL) {
		String serviceSuccessMeasureResponse;
		float successMeasure = 0f;
		try {
			serviceSuccessMeasureResponse = L2P_HTTPUtil.getHTTP(serviceSuccessMeasureURL, "GET");
			successMeasure = L2P_JSONUtil.parseMobSOSSuccessResponse(serviceSuccessMeasureResponse);
		} catch (MalformedURLException | ServiceNotFoundException | ParseException e) {
			logger.severe("Couldn't query mobsos success model for service: \n" + serviceSuccessMeasureURL);
			e.printStackTrace();
		}
		successMeasure = clamp( successMeasure, RegistryConfiguration.Faucet_minRatingPerService, RegistryConfiguration.Faucet_maxRatingPerService);
		logger.info("[ETH Faucet]: service is rated " + successMeasure + " / " + RegistryConfiguration.Faucet_maxRatingPerService);
		return successMeasure;
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
			json = L2P_JSONUtil.addAgentDetailsToJson(ethereumNode, agent, json, true, true);

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
		Boolean isLocalAdmin = ethereumNode.isLocalAdmin(agentEmail);
		json.put("is-local-admin", isLocalAdmin);

		// FIXME: query blockchain services instead of pastry nodeInfos
		json.put("local-service-count", queryLocalServices(ethAgentAdminServices, isLocalAdmin));
		
		// query remote node infos
		json.put("known-node-count", queryRemoteServicesWithoutBlockchain(ethAgentAdminServices, agentEmail) );
		
		// translate local and remote service info into JSON array
		JSONArray jsonServices = new JSONArray();
		for (Map.Entry<String, String> entry : ethAgentAdminServices.entrySet()) 
		{
			JSONObject jsonService = new JSONObject();
			jsonService.put("serviceName", entry.getKey());
			jsonService.put("serviceVersion", entry.getValue());
			jsonServices.add(jsonService);
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
				userAgent = ethereumNode.getAgentByDetail(null, username, null);
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
		JSONArray agentList = new JSONArray();
		
		for( EthereumAgent ethAgent: agents)
		{
			String ownerAddress = "";
			try {
				ownerAddress = ethereumNode.getRegistryClient().getUser(ethAgent.getLoginName()).getOwnerAddress();
				logger.info("found user ["+ethAgent.getLoginName()+"|"+ownerAddress+"]");
			} catch (EthereumException | NotFoundException e) {
				throw new BadRequestException("cannot get ethereum owner address for user agent " + ethAgent.getLoginName());
			}
			if ( ownerAddress == "" ) {
				ownerAddress = ethAgent.getEthereumAddress();
			}
			JSONObject agent = new JSONObject();
			agent.put("agentid", ethAgent.getIdentifier());
			agent.put("address", ownerAddress);
			
			agent.put("username", ethAgent.getLoginName());
			agent.put("email", ethAgent.getEmail());
			//agent.put("rating", rand.nextInt(6));
			
			agentList.add(agent);
		}
		
		json.put("agents", agentList);
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
				userAgent = ethereumNode.getAgentByDetail(null, username, null);
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
		JSONArray agentList = new JSONArray();
		
		for (EthereumAgent ethAgent : agents) {
			String ownerAddress = ethAgent.getEthereumAddress();
			UserProfileData profile;
			try {
				logger.info("accessing profile of " + ownerAddress);
				profile = ethereumNode.getRegistryClient().getProfile(ownerAddress);
			} catch (EthereumException | NotFoundException e) {
				e.printStackTrace();
				throw new BadRequestException("cannot get profile for agent", e);
			}
			
			JSONObject agent = addProfileInformationToJSON(ethAgent, ownerAddress, profile);

			agentList.add(agent);
		}
		
		json.put("agents", agentList);
		return Response.ok(json.toJSONString(), MediaType.APPLICATION_JSON).build();
	}

	private JSONObject addProfileInformationToJSON(EthereumAgent ethAgent, String ownerAddress, UserProfileData profile) {
		JSONObject agent = new JSONObject();
		agent.put("agentid", ethAgent.getIdentifier());
		agent.put("address", ownerAddress);
		agent.put("username", ethAgent.getLoginName());
		//agent.put("email", agent.getEmail());
		BigInteger cumulativeScore = profile.getCumulativeScore();
		BigInteger noTransactionsSent = profile.getNoTransactionsSent();
		BigInteger noTransactionsRcvd = profile.getNoTransactionsRcvd();
		agent.put("ethProfileOwner", profile.getOwner());
		agent.put("cumulativeScore", cumulativeScore.toString());
		agent.put("noOfTransactionsSent", noTransactionsSent.toString());
		agent.put("noOfTransactionsRcvd", noTransactionsRcvd.toString());
		if ( noTransactionsRcvd.compareTo(BigInteger.ZERO) == 0 )
		{
			agent.put("rating", 0);
		}
		else
		{
			agent.put("rating", cumulativeScore.divide(noTransactionsRcvd));
		}
		return agent;
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
