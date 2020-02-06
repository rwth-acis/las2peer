package i5.las2peer.connectors.webConnector.util;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.registry.data.BlockchainTransactionData;
import i5.las2peer.registry.data.GenericTransactionData;
import i5.las2peer.registry.data.SenderReceiverDoubleKey;
import i5.las2peer.registry.data.UserProfileData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.security.UserAgentImpl;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.utils.Convert;

import java.util.concurrent.ConcurrentMap;
import java.util.List;

import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class L2P_JSONUtil {
	private static final L2pLogger logger = L2pLogger.getInstance(L2P_JSONUtil.class);

	public static List<String> parseContactGroupsResponse(String input) throws ParseException
	{
		JSONObject jsonObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(input);
		List<String> contactGroups = new ArrayList<String>();
		jsonObject.keySet().forEach(keyStr ->
	    {
	        Object keyvalue = jsonObject.get(keyStr);	        
	        //System.out.println("groupID: "+ keyStr + "\n" + "groupName: " + keyvalue);
	        contactGroups.add(keyvalue.toString());
	    });
		return contactGroups;
		
	}
	
	public static float parseMobSOSSuccessResponse(String input) throws ParseException
	{
		JSONObject jsonObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(input);
		
		String rawDataStr = jsonObject.get("data").toString();
		// sanitize input
		rawDataStr = rawDataStr.replace("\\n", "").replace("[\"", "").replace("\"]", "");
		// | is regex metacharacter, needs to be escaped (\\)
		String[] rawDataParts = rawDataStr.split("\\|"); 
		// expected format of QV output: 
		//					0					1				2			3
		// 		["Row count: {ROW_COUNT}| {SQL_QUERY} \n| {VALUE_TYPE}\n| {VALUE}\n"]
		
		return Float.parseFloat(rawDataParts[3]);
	}
	
	public static List<String> parseMobSOSGroupURLs(String input) throws ParseException
	{
		JSONObject jsonObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(input);
		List<String> serviceURLs = new ArrayList<String>();
		jsonObject.keySet().forEach(keyStr ->
	    {
	        Object keyvalue = jsonObject.get(keyStr);	        
	        //System.out.println("service: "+ keyStr + "\n" + "serviceURL: " + keyvalue);
	        serviceURLs.add(keyvalue.toString());
	    });
		return serviceURLs;
	}

	public static List<String> parseMobSOSGroupServiceNames(String input) throws ParseException
	{
		JSONObject jsonObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(input);
		List<String> serviceURLs = new ArrayList<String>();
		jsonObject.keySet().forEach(keyStr ->
	    {
	        Object keyvalue = jsonObject.get(keyStr);	        
	        //System.out.println("service: "+ keyStr + "\n" + "serviceURL: " + keyvalue);
	        serviceURLs.add(keyStr.toString());
	    });
		return serviceURLs;
	}
	
	public static List<String> parseMobSOSModels(String input) throws ParseException
	{
		JSONObject jsonObject = (JSONObject) new JSONParser(JSONParser.MODE_PERMISSIVE).parse(input);
		List<String> groupURLs = new ArrayList<String>();
		jsonObject.keySet().forEach(keyStr ->
	    {
	        Object keyvalue = jsonObject.get(keyStr);	        
	        //System.out.println("group: "+ keyStr + "\n" + "groupURL: " + keyvalue);
	        groupURLs.add(keyvalue.toString());
	    });
		return groupURLs;
	}

	public static JSONObject genericTransactionDataToJSON(GenericTransactionData genericTransactionData, ConcurrentMap<String, String> addressToUsername)
	{
		JSONObject thisJSON = new JSONObject();

		thisJSON.put("txSenderAddress", addressToUsername.getOrDefault(genericTransactionData.getSender(), genericTransactionData.getSender()));
		thisJSON.put("txReceiverAddress", addressToUsername.getOrDefault(genericTransactionData.getReceiver(), genericTransactionData.getReceiver()));
		thisJSON.put("txMessage", genericTransactionData.getMessage());
		thisJSON.put("txAmountInWei", genericTransactionData.getAmountInWei());
		thisJSON.put("txAmountInEth", genericTransactionData.getAmountInEth());
		thisJSON.put("txTXHash", genericTransactionData.getTXHash());
		thisJSON.put("txTimestamp", genericTransactionData.getTimestamp());
		thisJSON.put("txDateTime", genericTransactionData.getTime());
		thisJSON.put("txTransactionType", genericTransactionData.getTransactionType());

		return thisJSON;	
	}

	public static JSONObject nodeInformationToJSON(NodeInformation nodeInfo)
	{
		JSONObject thisJSON = new JSONObject();
		if ( nodeInfo.getDescription() != null )
		{
			thisJSON.put("description", nodeInfo.getDescription());
		}
		if ( nodeInfo.getAdminName() != null )
		{
			thisJSON.put("admin-name", nodeInfo.getAdminName());
		}
		if ( nodeInfo.getAdminEmail() != null )
		{
			thisJSON.put("admin-mail", nodeInfo.getAdminEmail());
		}
		if ( nodeInfo.getOrganization() != null )
		{
			thisJSON.put("organization", nodeInfo.getOrganization());
		}

		thisJSON.put("service-count", nodeInfo.getHostedServices().size());

		if ( nodeInfo.getHostedServices().size() > 0 )
		{
			JSONArray serviceList = new JSONArray();
			for (ServiceNameVersion snv : nodeInfo.getHostedServices()) 
			{
				JSONObject serviceInfo = new JSONObject();
				serviceInfo.put("service-name", snv.getName());
				serviceInfo.put("service-version", snv.getVersion().toString());
				serviceList.add(serviceInfo);
			}
			thisJSON.put("services", serviceList);
		}
		return thisJSON;
	}

	public static JSONObject addAgentDetailsToJson(EthereumNode ethereumNode, AgentImpl agent, JSONObject json, Boolean addMnemonic, Boolean addTxLog)
			throws EthereumException, NotFoundException {
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl userAgent = (UserAgentImpl) agent;
			json.put("username", userAgent.getLoginName());
			json.put("email", userAgent.getEmail());
		}
		if (!(agent instanceof EthereumAgent)) {
			return json;
		}
		EthereumAgent ethAgent = (EthereumAgent) agent;
		String ethAddress = ethAgent.getEthereumAddress();
		if (ethAddress.length() == 0) {
			return json;
		}

		if (addMnemonic && !agent.isLocked()) {
			json.put("ethMnemonic", ethAgent.getEthereumMnemonic());
		}

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
				if (upd.getNoTransactionsRcvd().compareTo(BigInteger.ZERO) == 0) {
					json.put("ethRating", 0);
				} else {
					json.put("ethRating", upd.getStarRating());
				}
			} else {
				json.put("ethRating", "0");
				json.put("ethCumulativeScore", "???");
				json.put("ethNoTransactionsSent", "???");
				json.put("ethNoTransactionsRcvd", "???");
			}
		} catch (EthereumException | NotFoundException e) {
			e.printStackTrace();
		}

		if ( addTxLog ) addTXLogInfo(ethereumNode, json, ethAddress);

		json.put("ethAgentCredentialsAddress", ethAddress);

		return json;
	}

	public static String timestampToString(BigInteger timestamp)
	{
        Instant instant = Instant.ofEpochSecond(timestamp.longValue());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return fmt.format(instant.atZone(ZoneId.systemDefault()));
	}

	public static JSONObject transactionToJSON( BlockchainTransactionData btd )
	{
		JSONObject txJSON = new JSONObject();
		if ( btd.getFrom() != null && btd.getFrom().length() > 0 )
			txJSON.put("from", btd.getFrom());
		if ( btd.getTo() != null && btd.getTo().length() > 0 )
			txJSON.put("to", btd.getTo());
		if ( btd.getValue() != null )
			txJSON.put("value", Convert.fromWei(btd.getValue().toString(), Convert.Unit.ETHER));
		if ( btd.getBlockTimeStamp() != null ) {
			txJSON.put("blockTimeStamp", btd.getBlockTimeStamp());
			txJSON.put("blockDateTime", timestampToString(btd.getBlockTimeStamp()));
		}

		return txJSON;
	}

	public static void addTXLogInfo(EthereumNode ethereumNode, JSONObject json, String ethAddress) {
		ConcurrentMap<SenderReceiverDoubleKey, List<BlockchainTransactionData>> txLog = ethereumNode.getRegistryClient().getTransactionLog();
		
		int senderTxCount = 0;
		int receiverTxCount = 0;
		JSONArray sentTx = new JSONArray();
		JSONArray rcvdTx = new JSONArray();
		json.put("txLogSize", txLog.entrySet().size());
		for( Map.Entry<SenderReceiverDoubleKey,List<BlockchainTransactionData>> entry : txLog.entrySet() )
		{
			if ( entry.getKey().equalsSender(ethAddress) ) {
				senderTxCount += entry.getValue().size();
				for( BlockchainTransactionData t : entry.getValue() ) {
					sentTx.add(transactionToJSON(t));
				}		
			}
			else if ( entry.getKey().equalsReceiver(ethAddress) ) {
				receiverTxCount += entry.getValue().size();	
				for( BlockchainTransactionData t : entry.getValue() )  {
					rcvdTx.add(transactionToJSON(t));
				}				
			}
			else {
				continue;
			}
		}
		
		json.put("senderTxCount", senderTxCount);
		json.put("receiverTxCount", receiverTxCount);
		json.put("sentTx", sentTx);
		json.put("rcvdTx", rcvdTx);
	}
}