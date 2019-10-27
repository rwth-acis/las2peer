package i5.las2peer.registry;

import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.contracts.ServiceRegistry;
import i5.las2peer.registry.contracts.UserRegistry;
import i5.las2peer.registry.data.RegistryConfiguration;
import i5.las2peer.registry.data.UserData;
import i5.las2peer.registry.exceptions.EthereumException;
import i5.las2peer.security.EthereumAgent;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.serialization.SerializeTools;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Facade providing simple read/write access to the registry smart
 * contracts.
 *
 * Requires Ethereum credentials with sufficient Ether funds to pay
 * for transaction gas.
 *
 * @see ReadOnlyRegistryClient
 */
public class ReadWriteRegistryClient extends ReadOnlyRegistryClient {
	private final L2pLogger logger = L2pLogger.getInstance(ReadWriteRegistryClient.class);

	/**
	 * Create client providing access to both read and write registry
	 * functions.
	 * @param registryConfiguration addresses of registry contracts and
	 *                              Ethereum client HTTP JSON RPC API
	 *                              endpoint
	 */
	public ReadWriteRegistryClient(RegistryConfiguration registryConfiguration, Credentials credentials) {
		super(registryConfiguration, credentials);
	}

	/**
	 * Create new tag on blockchain.
	 * @param tagName tag name consisting of 1 to 32 UTF-8 characters
	 * @param tagDescription tag description of arbitrary length
	 * @throws EthereumException if transaction failed for some reason (gas? networking?)
	 */
	public void createTag(String tagName, String tagDescription) throws EthereumException {
		try {
			contracts.communityTagIndex.create(Util.padAndConvertString(tagName, 32), tagDescription).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Register an Ethereum agent in the blockchain.
	 *
	 * This will register the user (login) name to the agent's Ethereum
	 * account and store some additional fields that help others verify
	 * the user's identity. (E.g., the las2peer public key is stored in
	 * order to verify message signatures etc.)
	 *
	 * As with the other delegated registry methods, the registration
	 * transaction itself is performed by the node operator; but the agent's
	 * credentials are used to sign a message consenting to the registration.
	 * The registry smart contracts verify that signature and perform the
	 * registration as if the transaction came directly from the agent.
	 * (This simply means that the user name is registered to the agent's
	 * account, not the node operator's.)
	 *
	 * This means that the node operator pays the transaction fees.
	 */
	public void registerUser(EthereumAgent agent) throws EthereumException, AgentLockedException,
			SerializationException {
		// TODO: check that this encoding is appropriate
		//       (e.g., is there a more space-efficient way to store the public key? probably.)
		// TODO: reconsider which fields are actually strictly necessary
		//       (e.g., since we have login name and pubkey, do we really need the agentId? what for?)
		byte[] name = Util.padAndConvertString(agent.getLoginName(), 32);
		byte[] agentId = Util.padAndConvertString(agent.getIdentifier(), 128);
		byte[] publicKey = SerializeTools.serialize(agent.getPublicKey());

		final Function function = new Function(
				UserRegistry.FUNC_REGISTER,
				Arrays.asList(new org.web3j.abi.datatypes.generated.Bytes32(name),
						new org.web3j.abi.datatypes.DynamicBytes(agentId),
						new org.web3j.abi.datatypes.DynamicBytes(publicKey)),
				Collections.emptyList());

		String consentee = agent.getEthereumAddress();
		byte[] signature = SignatureUtils.signFunctionCall(function, agent.getEthereumCredentials());

		try {
			contracts.userRegistry.delegatedRegister(name, agentId, publicKey, consentee, signature).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register user", e);
		}
	}
	
	public void registerUserProfile(UserAgentImpl agent) throws EthereumException
	{
		if ( !agent.hasLoginName() )
		{
			throw new EthereumException("Could not register profile: agent has no login name");
		}
		
		byte[] profileName = Util.padAndConvertString(agent.getLoginName(), 32);
		logger.fine( "registering user profile: " + agent.getLoginName() + " | " + profileName.toString() );
		
		try {
			contracts.reputationRegistry.createProfile(profileName).send();
		} catch (Exception e) {
			throw new EthereumException("Could not register profile: " + e.getMessage(), e);
		}
	}
	
	public void addUserRating(UserData receivingAgent, Integer rating ) throws EthereumException
	{
		// see reputation registry
	    int __amountMax = 5;
	    int __amountMin = __amountMax * -1;
		if ( rating > __amountMax || rating < __amountMin )
			throw new EthereumException("Could not add user rating: rating outside bounds (" + __amountMin + " - " + __amountMax + ")");
		
		logger.fine( "rating user profile: " + receivingAgent.getName() + " | " + rating + "(" + BigInteger.valueOf(rating).intValue() + ")" );
		
		try {
			contracts.reputationRegistry.addTransaction(receivingAgent.getPublicKey().toString(), BigInteger.valueOf(rating)).send();
		} catch (Exception e) {
			throw new EthereumException("Could not add user rating: " + e.getMessage(), e);
		}
	}

	/**
	 * Register a service name to the given author.
	 *
	 * The registration call is delegated, see description of
	 * {@link #registerUser(EthereumAgent)} for details.
	 *
	 * @param serviceName service (package) name of arbitrary length
	 * @param agent Ethereum agent of the service author
	 */
	public void registerService(String serviceName, EthereumAgent agent) throws EthereumException,
			AgentLockedException {
		byte[] authorName = Util.padAndConvertString(agent.getLoginName(), 32);

		final Function function = new Function(ServiceRegistry.FUNC_REGISTER,
				Arrays.asList(new org.web3j.abi.datatypes.Utf8String(serviceName),
						new org.web3j.abi.datatypes.generated.Bytes32(authorName)), Collections.emptyList());

		String consentee = agent.getEthereumAddress();
		byte[] signature = SignatureUtils.signFunctionCall(function, agent.getEthereumCredentials());

		try {
			contracts.serviceRegistry.delegatedRegister(serviceName, authorName, consentee, signature).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to register service", e);
		}
	}

	/** @see #releaseService(String, int, int, int, EthereumAgent, byte[]) */
	public void releaseService(String serviceName, String versionString, EthereumAgent author)
			throws EthereumException, AgentLockedException {
		releaseService(serviceName, versionString, author, null);
	}

	/** @see #releaseService(String, int, int, int, EthereumAgent, byte[]) */
	public void releaseService(String serviceName, String versionString, EthereumAgent author, byte[] supplementHash)
			throws EthereumException, AgentLockedException {
		int[] version = Util.parseVersion(versionString);
		releaseService(serviceName, version[0], version[1], version[2], author, supplementHash);
	}

	/** @see #releaseService(String, int, int, int, EthereumAgent, byte[]) */
	public void releaseService(String serviceName, int versionMajor, int versionMinor, int versionPatch,
			EthereumAgent author) throws EthereumException, AgentLockedException {
		releaseService(serviceName, versionMajor, versionMinor, versionPatch, author, null);
	}

	/**
	 * Announce the release of a specific version of a service.
	 *
	 * The release call is delegated, see description of
	 * {@link #registerUser(EthereumAgent)} for details.
	 *
	 * @param serviceName name of existing service
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 * @param author agent of author of this version
	 * @param supplementHash hash of supplemental data in
	 *                                    shared storage
	 */
	public void releaseService(String serviceName, int versionMajor, int versionMinor, int versionPatch,
			EthereumAgent author, byte[] supplementHash)throws EthereumException, AgentLockedException {
		if (observer.getReleaseByVersion(serviceName, versionMajor, versionMinor, versionPatch) != null) {
			logger.warning("Tried to submit duplicate release (name / version already exist), ignoring!");
			// TODO: handle in contracts, cause this is a race condition
			return;
		}

		byte[] authorName = Util.padAndConvertString(author.getLoginName(), 32);

		// TODO: hash parameter is unused. instead, we check whether the library NetworkArtifact signature matches the
		// author (pubkey registered in the blockchain), and consider that good enough
		// (this leaves *some* room for undesired behavior: an author modifying his node code and replacing an already
		// released version, but the potential for abuse seems pretty low)
		// NOTE: actually, hash is now (ab)used for supplement (which happens to also be a hash)

		final Function function = new Function(
				ServiceRegistry.FUNC_RELEASE,
				Arrays.asList(new org.web3j.abi.datatypes.Utf8String(serviceName),
						new org.web3j.abi.datatypes.generated.Bytes32(authorName),
						new org.web3j.abi.datatypes.generated.Uint256(versionMajor),
						new org.web3j.abi.datatypes.generated.Uint256(versionMinor),
						new org.web3j.abi.datatypes.generated.Uint256(versionPatch),
						new org.web3j.abi.datatypes.DynamicBytes(supplementHash)),
				Collections.emptyList());

		String consentee = author.getEthereumAddress();
		byte[] signature = SignatureUtils.signFunctionCall(function, author.getEthereumCredentials());

		try {
			contracts.serviceRegistry.delegatedRelease(serviceName, authorName,
					BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
					supplementHash, consentee, signature).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service release", e);
		}
	}

	/**
	 * Announce the deployment of a specific version of a service at
	 * the given node.
	 * @param servicePackageName name of service (package). E.g.
	 *                           <code>com.example.some.package</code>.
	 * @param serviceClassName name of the service class that is run.
	 *                         E.g., <code>ExampleService</code>.
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 * @param nodeId identifier of the node running the deployment
	 */
	public void announceDeployment(String servicePackageName, String serviceClassName,
								   int versionMajor, int versionMinor, int versionPatch,
								   String nodeId) throws EthereumException {
		try {
			contracts.serviceRegistry.announceDeployment(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment announcement ("
					+ "DEBUG: " + serviceClassName + ", " + e.getMessage() + ")", e);
		}
	}

	/**
	 * Announce that a service deployment is stopping or no longer
	 * accessible.
	 * @param servicePackageName name of service (package). E.g.
	 *                           <code>com.example.some.package</code>.
	 * @param serviceClassName name of the service class that is run.
	 *                         E.g., <code>ExampleService</code>.
	 * @param versionMajor major version, as used in semantic versioning
	 * @param versionMinor minor version, as used in semantic versioning
	 * @param versionPatch patch version, as used in semantic versioning
	 * @param nodeId identifier of the node running the deployment
	 */
	// TODO: is there a more elegant way? referencing the tx is possible of course
	// but not really preferable
	public void announceDeploymentEnd(String servicePackageName, String serviceClassName,
									  int versionMajor, int versionMinor, int versionPatch,
									  String nodeId) throws EthereumException {
		try {
			contracts.serviceRegistry.announceDeploymentEnd(servicePackageName, serviceClassName,
				BigInteger.valueOf(versionMajor), BigInteger.valueOf(versionMinor), BigInteger.valueOf(versionPatch),
				nodeId).send();
		} catch (Exception e) {
			throw new EthereumException("Failed to submit service deployment *end* announcement ("
					+ "DEBUG: " + serviceClassName + ", " + e.getMessage() + ")", e);
		}
	}

	/**
	 * Send ether to the recipient address.
	 *
	 * This sends ether from the "owner" of this registry client (i.e., the
	 * account specified by the credentials which were used to construct this
	 * client), to the given recipient address.
	 */
	// this is (so far) only for debugging / testing / etc.
	public void sendEther(String recipientAddress, BigDecimal valueInWei) throws EthereumException {
		try {
			TransactionReceipt receipt = Transfer.sendFunds(web3j, credentials, recipientAddress, valueInWei, Convert.Unit.WEI).send();
			if (!receipt.isStatusOK()) {
				throw new EthereumException("TX status field is not OK. TX failed.");
			}
		} catch (Exception e) {
			throw new EthereumException("Could not send ether to address '" + recipientAddress + "'", e);
		}
	}
	
	public TransactionReceipt sendEtherFromCoinbase(String recipientAddress, BigInteger valueInWei) throws EthereumException {
		
		// see
		// https://www.reddit.com/r/ethereum/comments/5g8ia6/attention_miners_we_recommend_raising_gas_limit/
		BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);

		// http://ethereum.stackexchange.com/questions/1832/cant-send-transaction-exceeds-block-gas-limit-or-intrinsic-gas-too-low
		BigInteger GAS_LIMIT_ETHER_TX = BigInteger.valueOf(21_000);

		TransactionReceipt txR;
		try {
			String coinbase = this.getCoinbase().getResult();
			BigInteger nonce = this.getNonce(coinbase);
			// this is a contract method call -> gas limit higher than simple fund transfer
			BigInteger gasPrice = GAS_PRICE;
			BigInteger gasLimit = GAS_LIMIT_ETHER_TX; 
			Transaction transaction = Transaction.createEtherTransaction(
					coinbase, 
					nonce, 
					gasPrice, 
					gasLimit, 
					recipientAddress, 
					valueInWei);

			logger.info("[ETH] Preparing coinbase transaction...");
			logger.info("[ETH] > Coinbase: " + coinbase );
			logger.info("[ETH] > nonce: " + nonce );
			logger.info("[ETH] > gasPrice: " + gasPrice );
			logger.info("[ETH] > gasLimit: " + gasLimit );
			logger.info("[ETH] > recipientAddress: " + recipientAddress );
			logger.info("[ETH] > valueInWei: " + valueInWei );
			
			EthSendTransaction ethSendTransaction = web3j
					.ethSendTransaction(transaction)
			//		.sendAsync().get();
					.send();

			if ( ethSendTransaction.hasError() )
			{
				Response.Error error = ethSendTransaction.getError();
				throw new EthereumException("Eth Transaction Error [" + error.getCode() + "]: " + error.getMessage());
			}
			String txHash = ethSendTransaction.getTransactionHash();
			logger.info("[ETH] Sent transaction ["+txHash+"], waiting for receipt... ");
			if ( txHash.length() < 2 )
			{
				throw new EthereumException("Could not create ethereum transaction");
			}
			
			txR = waitForReceipt(txHash);
		} catch( InterruptedException | ExecutionException | IOException e )
		{
			throw new EthereumException("Could not send ether to address '" + recipientAddress + "'", e);
		}
		
		if ( txR == null )
		{
			throw new EthereumException("Could not create faucet transaction.");
		}
		
		return txR;
	}
	
	
}
