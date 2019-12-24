package i5.las2peer.registry;

import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.Callback;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.QueuingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.TxHashVerifier;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.registry.contracts.CommunityTagIndex;
import i5.las2peer.registry.contracts.ReputationRegistry;
import i5.las2peer.registry.contracts.ServiceRegistry;
import i5.las2peer.registry.contracts.UserRegistry;
import i5.las2peer.registry.exceptions.EthereumException;


/**
 * Wrapper for registry contracts instances.
 *
 * The contract instances should more accurately be named contract
 * accessors: They are <i>clients</i> of the actual contract instances
 * stored on the blockchain. Further, they contain user authentication.
 *
 * Thus it can make sense to have several instances, e.g., a read-only
 * instance and an instance for a currently used agent.
 *
 * They're also pretty complicated to instantiate, so that's done with
 * a builder.
 */
class Contracts {
	
	private Web3j web3j;

	final CommunityTagIndex communityTagIndex;
	final UserRegistry userRegistry;
	final ServiceRegistry serviceRegistry;
	final ReputationRegistry reputationRegistry;
	final TransactionManager transactionManager;

	static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	static ScheduledFuture<?> t;

	private static Map<String, Object> pendingTransactions = new ConcurrentHashMap<>();
	private static ConcurrentLinkedQueue<TransactionReceipt> transactionReceipts = new ConcurrentLinkedQueue<>();

	private static boolean isPolling = false;
	private static final long POLLING_FREQUENCY = 15000;

	protected static L2pLogger logger = L2pLogger.getInstance(Contracts.class);

	private Contracts(Web3j web3j, CommunityTagIndex communityTagIndex, UserRegistry userRegistry, ServiceRegistry serviceRegistry,
			ReputationRegistry reputationRegistry, TransactionManager transactionManager) {
		this.web3j = web3j;
		this.communityTagIndex = communityTagIndex;
		this.userRegistry = userRegistry;
		this.serviceRegistry = serviceRegistry;
		this.reputationRegistry = reputationRegistry;
		this.transactionManager = transactionManager;
	}

	public BigInteger getBlockTimestamp(BigInteger blockNumber) throws EthereumException
	{
		try {
			return web3j
				.ethGetBlockByNumber(DefaultBlockParameter.valueOf(blockNumber), true)
				.sendAsync().get()
				.getBlock()
				.getTimestamp();
		} catch (InterruptedException | ExecutionException e) {
			throw new EthereumException("cannot get block info for " + blockNumber.toString(), e);
		}
	}

	public Web3j getWeb3jClient() {
		return web3j;
	}

	public static Map<String, Object> getPendingTransactions() {
		return Contracts.pendingTransactions;
	}

	public static void setPendingTransactions(Map<String, Object> pendingTransactions) {
		Contracts.pendingTransactions = pendingTransactions;
	}

	public static void addPendingTXHash(String pendingTxHash) {
		logger.info("[TXManager]: added tx #"+pendingTxHash+" to list of pending transactions");
		Contracts.pendingTransactions.put(pendingTxHash, new Object());
	}

	public static ConcurrentLinkedQueue<TransactionReceipt> getTransactionReceipts() {
		return Contracts.transactionReceipts;
	}

	public static void setTransactionReceipts(ConcurrentLinkedQueue<TransactionReceipt> transactionReceipts) {
		Contracts.transactionReceipts = transactionReceipts;
	}

	public static void addTransactionReceipt(TransactionReceipt reciept) {
		logger.info("[TXManager] added tx receipt: " + Util.getOrDefault(reciept.getTransactionHash(), "??"));
		logger.info("[TXManager] > blockHash: " + Util.getOrDefault(reciept.getBlockHash(), "??"));
		logger.info("[TXManager] > gas used: " + Util.getOrDefault(reciept.getGasUsed(), "??"));
		logger.info("[TXManager] > senderAddress: " + Util.getOrDefault(reciept.getFrom(), "??"));
		logger.info("[TXManager] > recipientAddress: " + Util.getOrDefault(reciept.getTo(), "??"));
		Contracts.transactionReceipts.add(reciept);
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public StaticNonceRawTransactionManager tryGetNonceTransactionManager() throws EthereumException
	{
		if ( transactionManager instanceof StaticNonceRawTransactionManager ) {
			return (StaticNonceRawTransactionManager) transactionManager;
		} else {
			throw new EthereumException("cannot cast transactionManager to manage internal nonces. credentials == null?");
		}
	}

	public static void pollTransactionList() throws EthereumException {
		logger.info("[TX-QUEUE] polling transaction list");
		if (!isPolling)
			return;
		if (transactionReceipts.size() == 0)
			return;

		for (int i = 0; i < DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH && !pendingTransactions.isEmpty(); i++) {
			if (transactionReceipts.size() != 0)
			logger.info("[TX-QUEUE] attempt #" + i + ", found " + transactionReceipts.size() + " tx's");
			for (TransactionReceipt transactionReceipt : transactionReceipts) {
				if (transactionReceipt.getBlockHash().isEmpty()) {
					logger.info("[TX-QUEUE] omitting tx receipt, not mined yet: " + transactionReceipt.getTransactionHash());
					continue;//throw new EthereumException("polling tx receipt failed: block hash empty");
				}
				logger.info("[TX-QUEUE] observed tx receipt: " + Util.getOrDefault(transactionReceipt.getTransactionHash(), "??"));
				logger.info("[TX-QUEUE] > blockHash: " + Util.getOrDefault(transactionReceipt.getBlockHash(), "??"));
				logger.info("[TX-QUEUE] > gas used: " + Util.getOrDefault(transactionReceipt.getGasUsed(), "??"));
				logger.info("[TX-QUEUE] > senderAddress: " + Util.getOrDefault(transactionReceipt.getFrom(), "??"));
				logger.info("[TX-QUEUE] > recipientAddress: " + Util.getOrDefault(transactionReceipt.getTo(), "??"));
				pendingTransactions.remove(transactionReceipt.getTransactionHash());
				transactionReceipts.remove(transactionReceipt);
			}
		}
	}

	/**
	 * Contract config objects should uniquely identify the smart contract
	 * instances.
	 *
	 * If two configs are equal, we assume they refer to the same instances, and
	 * clients using the same config should be interchangeable.
	 */
	static class ContractsConfig {
		final String communityTagIndexAddress;
		final String userRegistryAddress;
		final String serviceRegistryAddress;
		final String reputationRegistryAddress;

		final String endpoint;

		ContractsConfig(String communityTagIndexAddress, String userRegistryAddress, String serviceRegistryAddress,
				String reputationRegistryAddress, String endpoint) {
			this.communityTagIndexAddress = communityTagIndexAddress;
			this.userRegistryAddress = userRegistryAddress;
			this.serviceRegistryAddress = serviceRegistryAddress;
			this.reputationRegistryAddress = reputationRegistryAddress;
			this.endpoint = endpoint;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ContractsConfig that = (ContractsConfig) o;
			return Objects.equals(communityTagIndexAddress, that.communityTagIndexAddress) &&
					Objects.equals(userRegistryAddress, that.userRegistryAddress) &&
					Objects.equals(serviceRegistryAddress, that.serviceRegistryAddress) &&
					Objects.equals(reputationRegistryAddress, that.reputationRegistryAddress) &&
					Objects.equals(endpoint, that.endpoint);
		}

		@Override
		public int hashCode() {
			return Objects.hash(communityTagIndexAddress, userRegistryAddress, serviceRegistryAddress, reputationRegistryAddress, endpoint);
		}
	}

	/**
	 * Sets up the contract wrappers (or: "clients"), possibly with
	 * credentials.
	 *
	 * Credentials are optional but required for all state-changing
	 * smart contract operations (see "call" vs "sendTransaction").
	 */
	static class ContractsBuilder {
		// Not all JSON RPC calls require a "from" address, e.g., https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_call
		// However, Web3j requires one, even for ReadOnlyTransactionManager,
		// so we can't just omit it -- instead, let's make one up.
		// If the `call`ed Solidity smart contracts don't check `msg.sender`,
		// the from address should not matter at all.
		// Still, this is probably not a great idea.
		private static final String DEFAULT_FROM_ADDRESS = "0x0000000000000000000000000000000000000000";

		private ContractsConfig config;
		private ContractGasProvider gasProvider;
		private Credentials credentials;

		/**
		 * Create builder with the mandatory arguments. Optional fields
		 * can then be set with the setter methods, before using the
		 * build method to construct a {@link Contracts} instance.
		 * @param config addresses of the registry contracts and
		 *               Ethereum client HTTP JSON RPC API endpoint
		 */
		public ContractsBuilder(ContractsConfig config) {
			this.config = config;
		}

		/**
		 * Configures the gas parameters of transactions sent via the
		 * contracts wrappers.
		 *
		 * Static gas parameters are used, i.e., all transactions use
		 * the same values.
		 *
		 * @param gasPrice price (in Wei) to be paid per gas unit.
		 *                 Offering a higher price will potentially
		 *                 prioritise transactions for mining.
		 * @param gasLimit maximum amount of gas a transaction may use.
		 *                 If exceeded, the transaction fails and all
		 *                 gas is still consumed.
		 * @return this builder instance. Allows chained method calls.
		 * @see <a href="https://ethereum.stackexchange.com/questions/3/what-is-meant-by-the-term-gas">Detailed explanations on StackExchange</a>
		 * @see <a href="https://web3j.readthedocs.io/en/latest/smart_contracts.html#dynamic-gas-price-and-limit">web3j dynamic gas configuration</a>
		 */
		public ContractsBuilder setGasOptions(long gasPrice, long gasLimit) {
			gasProvider = new StaticGasProvider(BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));
			return this;
		}

		/**
		 * Set credentials which are used to sign transactions sent via
		 * the contract wrappers.
		 * If no credentials are provided, the contracts must be used
		 * as read-only (no state-changing function invocations).
		 * @param credentials wrapper of key pair and Ethereum address
		 * @return this builder instance. Allows chained method calls.
		 */
		public ContractsBuilder setCredentials(Credentials credentials) {
			this.credentials = credentials;
			return this;
		}

		/**
		 * Constructs the Ethereum smart contract wrapper instances
		 * for the given configuration. Notably, the credentials are
		 * baked into the wrapper: They cannot be changed (or removed).
		 * @return instance of contracts wrapper, ready for use
		 */
		public Contracts build() {
			if (gasProvider == null) {
				gasProvider = new DefaultGasProvider();
			}

			Web3j web3j = Web3j.build((config.endpoint == null) ? new HttpService() : new HttpService(config.endpoint));

			TransactionManager transactionManager = constructTxManager(web3j, credentials);
			CommunityTagIndex communityTagIndex = CommunityTagIndex.load(config.communityTagIndexAddress, web3j, transactionManager, gasProvider);
			UserRegistry userRegistry = UserRegistry.load(config.userRegistryAddress, web3j, transactionManager, gasProvider);
			ServiceRegistry serviceRegistry = ServiceRegistry.load(config.serviceRegistryAddress, web3j, transactionManager, gasProvider);
			ReputationRegistry reputationRegistry = ReputationRegistry.load(config.reputationRegistryAddress, web3j, transactionManager, gasProvider);

			return new Contracts(web3j, communityTagIndex, userRegistry, serviceRegistry, reputationRegistry, transactionManager);
		}

		private TransactionManager constructTxManager(Web3j web3j, Credentials credentials) {
			if (credentials == null) {
				return new ReadonlyTransactionManager(web3j, DEFAULT_FROM_ADDRESS);
			} else {

				// FIXME: "nonce too low" error still occurs
				// it only seems to happen on the first couple of announcements, meaning it's
				// not really a problem
				// bit still, it should be fixed
				//
				// see:
				// https://ethereum.stackexchange.com/questions/63818/quick-web3j-transactions-to-the-same-destination-address-results-in-replacement
				// https://ethereum.stackexchange.com/questions/34502/how-could-i-send-transactions-continuously-by-web3j-generated-wrapper
				//
				// unfortunately, this is not working yet. the timeouts should be plenty:
				// service announcements every 30 secs with polling 3 secs should be perfectly
				// fine, but it's not.
				// so let's reduce this. whatever.
				//
				// okay, frankly, I'm not even sure if this can fix the nonce too low error (but
				// that's what the issue / StackEx suggest)
				FastRawTransactionManager transactionManager = new StaticNonceRawTransactionManager(
					web3j, credentials, 
					new QueuingTransactionReceiptProcessor(web3j,
						new Callback() {
							@Override
							public void accept(TransactionReceipt transactionReceipt) {
								Contracts.addTransactionReceipt(transactionReceipt);
							}
							@Override
							public void exception(Exception e) {
								e.printStackTrace();
							}
						}, 
					DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH, POLLING_FREQUENCY)
				);

				// schedule polling, will be created on first creation of contracts
				// https://www.baeldung.com/java-delay-code-execution
				if (!Contracts.isPolling) {
					Contracts.isPolling = true;
					Contracts.executorService.scheduleAtFixedRate(() -> {
						try {
							System.out.println("[TXQUEUE] txList poll execution");
							Contracts.pollTransactionList();
						} catch (EthereumException e) {
							Contracts.isPolling = false;
							e.printStackTrace();
						}
					}, 0, POLLING_FREQUENCY, TimeUnit.MILLISECONDS);
				}

				// txHashVerification throws false alarms (not sure why), disable check
				// TODO: figure out what's going and and reenable
				// see https://github.com/web3j/web3j/pull/584
				transactionManager.setTxHashVerifier(new NoopTxHashVerifier());
				return transactionManager;
			}

		}
	}

	static class NoopTxHashVerifier extends TxHashVerifier {
		@Override public boolean verify(String hash1, String hash2) {
			return true;
		}
	}
}
