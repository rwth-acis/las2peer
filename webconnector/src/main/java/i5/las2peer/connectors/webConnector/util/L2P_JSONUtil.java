package i5.las2peer.connectors.webConnector.util;

import java.math.BigInteger;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.p2p.NodeInformation;
import i5.las2peer.registry.data.GenericTransactionData;
import i5.las2peer.registry.data.UserProfileData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.registry.exceptions.NotFoundException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.security.UserAgentImpl;
import net.minidev.json.JSONObject;

public class L2P_JSONUtil {

	public static JSONObject genericTransactionDataToJSON(GenericTransactionData genericTransactionData)
	{
		JSONObject thisJSON = new JSONObject();

		thisJSON.put("txSender", genericTransactionData.getSender());
		thisJSON.put("txReceiver", genericTransactionData.getReceiver());
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
			JSONObject serviceList = new JSONObject();
			for (ServiceNameVersion snv : nodeInfo.getHostedServices()) 
			{
				serviceList.put("service-name", snv.getName());
				serviceList.put("service-version", snv.getVersion().toString());
			}
			thisJSON.put("services", serviceList);
		}
		return thisJSON;
	}

	public static JSONObject addAgentDetailsToJson(EthereumNode ethereumNode, AgentImpl agent, JSONObject json, Boolean addMnemonic)
			throws EthereumException, NotFoundException {
		json.put("agentid", agent.getIdentifier());
		if (agent instanceof UserAgentImpl) {
			UserAgentImpl userAgent = (UserAgentImpl) agent;
			json.put("username", userAgent.getLoginName());
			json.put("email", userAgent.getEmail());
		}
		if (agent instanceof EthereumAgent) {
			EthereumAgent ethAgent = (EthereumAgent) agent;
			String ethAddress = ethAgent.getEthereumAddress();
			if (ethAddress.length() > 0) {
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
							json.put("ethRating", upd.getCumulativeScore().divide(upd.getNoTransactionsRcvd()));
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

			}

			json.put("ethAgentCredentialsAddress", ethAddress);

			if (addMnemonic && !agent.isLocked()) {
				json.put("ethMnemonic", ethAgent.getEthereumMnemonic());
			}

			// UserData ethUser =
			// ethereumNode.getRegistryClient().getUser(ethAgent.getLoginName());
			// if ( ethUser != null ) { json.put("eth-user-address",
			// ethUser.getOwnerAddress()); }
		}
		return json;
	}
}