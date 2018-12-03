package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.ServiceRegistry;
import i5.las2peer.registryGateway.contracts.UserRegistry;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

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
 * They're also pretty complicated to instantiate, so let's use a
 * builder pattern and wrap some data.
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
			return communityTagIndexAddress.equals(that.communityTagIndexAddress) &&
					userRegistryAddress.equals(that.userRegistryAddress) &&
					serviceRegistryAddress.equals(that.serviceRegistryAddress) &&
					endpoint.equals(that.endpoint);
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

		public ContractsBuilder(ContractsConfig config) {
			this.config = config;
		}

		// TODO: check out whether these are needed when in read-only mode
		public ContractsBuilder setGasOptions(long gasPrice, long gasLimit) {
			gasProvider = new StaticGasProvider(BigInteger.valueOf(gasPrice), BigInteger.valueOf(gasLimit));
			return this;
		}

		public ContractsBuilder setCredentials(Credentials credentials) {
			this.credentials = credentials;
			return this;
		}

		public Contracts build() {
			if (gasProvider == null) {
				gasProvider = new DefaultGasProvider();
			}

			Web3j web3j = Web3j.build((config.endpoint == null) ? new HttpService() : new HttpService(config.endpoint));

			TransactionManager transactionManager;
			if (credentials == null) {
				transactionManager = new ReadonlyTransactionManager(web3j, DEFAULT_FROM_ADDRESS);
			} else {
				transactionManager = new RawTransactionManager(web3j, credentials);
			}

			CommunityTagIndex communityTagIndex = CommunityTagIndex.load(config.communityTagIndexAddress, web3j, transactionManager, gasProvider);
			UserRegistry userRegistry = UserRegistry.load(config.userRegistryAddress, web3j, transactionManager, gasProvider);
			ServiceRegistry serviceRegistry = ServiceRegistry.load(config.serviceRegistryAddress, web3j, transactionManager, gasProvider);

			return new Contracts(communityTagIndex, userRegistry, serviceRegistry);
		}
	}
}
