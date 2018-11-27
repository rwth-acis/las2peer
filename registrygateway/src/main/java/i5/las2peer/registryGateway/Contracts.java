package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.ServiceRegistry;
import i5.las2peer.registryGateway.contracts.UserRegistry;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;

class Contracts {
	final CommunityTagIndex communityTagIndex;
	final UserRegistry userRegistry;
	final ServiceRegistry serviceRegistry;

	private Contracts(CommunityTagIndex communityTagIndex, UserRegistry userRegistry, ServiceRegistry serviceRegistry) {
		this.communityTagIndex = communityTagIndex;
		this.userRegistry = userRegistry;
		this.serviceRegistry = serviceRegistry;
	}

	static class ContractsBuilder {
		private Web3j web3j;
		private ContractGasProvider gasProvider;

		private String communityTagIndexAddress;
		private String userRegistryAddress;
		private String serviceRegistryAddress;

		public ContractsBuilder(Web3j web3j) {
			this(web3j, new DefaultGasProvider());
		}

		public ContractsBuilder(Web3j web3j, ContractGasProvider gasProvider) {
			this.web3j = web3j;
			this.gasProvider = gasProvider;
		}

		public void setCommunityTagIndexAddress(String communityTagIndexAddress) {
			this.communityTagIndexAddress = communityTagIndexAddress;
		}

		public void setUserRegistryAddress(String userRegistryAddress) {
			this.userRegistryAddress = userRegistryAddress;
		}

		public void setServiceRegistryAddress(String serviceRegistryAddress) {
			this.serviceRegistryAddress = serviceRegistryAddress;
		}

		public Contracts build(Credentials credentials) {
			return build(new RawTransactionManager(this.web3j, credentials));
		}

		public Contracts buildReadOnly(String accountAddress) {
			return build(new ReadonlyTransactionManager(this.web3j, accountAddress));
		}

		public Contracts build(TransactionManager transactionManager) {
			CommunityTagIndex communityTagIndex = CommunityTagIndex.load(communityTagIndexAddress, this.web3j, transactionManager, this.gasProvider);
			UserRegistry userRegistry = UserRegistry.load(userRegistryAddress, this.web3j, transactionManager, this.gasProvider);
			ServiceRegistry serviceRegistry = ServiceRegistry.load(serviceRegistryAddress, this.web3j, transactionManager, this.gasProvider);

			return new Contracts(communityTagIndex, userRegistry, serviceRegistry);
		}
	}
}
