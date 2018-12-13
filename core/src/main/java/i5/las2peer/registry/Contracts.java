package i5.las2peer.registry;

import i5.las2peer.registry.contracts.CommunityTagIndex;
import i5.las2peer.registry.contracts.ServiceRegistry;
import i5.las2peer.registry.contracts.UserRegistry;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.math.BigInteger;
import java.util.Objects;

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
	final CommunityTagIndex communityTagIndex;
	final UserRegistry userRegistry;
	final ServiceRegistry serviceRegistry;

	private Contracts(CommunityTagIndex communityTagIndex, UserRegistry userRegistry, ServiceRegistry serviceRegistry) {
		this.communityTagIndex = communityTagIndex;
		this.userRegistry = userRegistry;
		this.serviceRegistry = serviceRegistry;
	}

	/**
	 * Contract config objects should uniquely identify the smart
	 * contract instances.
	 *
	 * If two configs are equal, we assume they refer to the same
	 * instances, and clients using the same config should be
	 * interchangeable.
	 */
	static class ContractsConfig {
		final String communityTagIndexAddress;
		final String userRegistryAddress;
		final String serviceRegistryAddress;

		final String endpoint;

		ContractsConfig(String communityTagIndexAddress, String userRegistryAddress, String serviceRegistryAddress, String endpoint) {
			this.communityTagIndexAddress = communityTagIndexAddress;
			this.userRegistryAddress = userRegistryAddress;
			this.serviceRegistryAddress = serviceRegistryAddress;
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
					Objects.equals(endpoint, that.endpoint);
		}

		@Override
		public int hashCode() {
			return Objects.hash(communityTagIndexAddress, userRegistryAddress, serviceRegistryAddress, endpoint);
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
		// Not all JSON RPC calls require a "from" address, e.g.,
		//     https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_call
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

			TransactionManager transactionManager;
			if (credentials == null) {
				transactionManager = new ReadonlyTransactionManager(web3j, DEFAULT_FROM_ADDRESS);
			} else {
				// use FastRaw... instead of Raw... since Raw lead to this:
				// https://ethereum.stackexchange.com/questions/63818/quick-web3j-transactions-to-the-same-destination-address-results-in-replacement
				// also use faster polling interval
				// https://ethereum.stackexchange.com/questions/34502/how-could-i-send-transactions-continuously-by-web3j-generated-wrapper
				long pollingIntervalMillisecs = 3000;
				int attempts = 40; // total: 3s*40 = 2min -- it definitely should not take that long, no idea what's appropriate
				TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, pollingIntervalMillisecs, attempts);
				transactionManager = new FastRawTransactionManager(web3j, credentials, receiptProcessor);
			}

			CommunityTagIndex communityTagIndex = CommunityTagIndex.load(config.communityTagIndexAddress, web3j, transactionManager, gasProvider);
			UserRegistry userRegistry = UserRegistry.load(config.userRegistryAddress, web3j, transactionManager, gasProvider);
			ServiceRegistry serviceRegistry = ServiceRegistry.load(config.serviceRegistryAddress, web3j, transactionManager, gasProvider);

			return new Contracts(communityTagIndex, userRegistry, serviceRegistry);
		}
	}
}
