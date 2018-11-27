package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.ServiceRegistry;
import i5.las2peer.registryGateway.contracts.UserRegistry;
import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Contracts {
	CommunityTagIndex communityTagIndex;
	UserRegistry userRegistry;
	ServiceRegistry serviceRegistry;

	private Contracts(CommunityTagIndex communityTagIndex, UserRegistry userRegistry, ServiceRegistry serviceRegistry) {
		this.communityTagIndex = communityTagIndex;
		this.userRegistry = userRegistry;
		this.serviceRegistry = serviceRegistry;
	}

	public static class ContractsBuilder {
		Web3j web3j;
		Map<Class<? extends Contract>, String> contractAddresses;
		ContractGasProvider gasProvider;
		TransactionManager transactionManager;

		public ContractsBuilder(Web3j web3j, Map<Class<? extends Contract>, String> contractAddresses) {
			this(web3j, contractAddresses, new DefaultGasProvider());
		}

		public ContractsBuilder(Web3j web3j, Map<Class<? extends Contract>, String> contractAddresses, ContractGasProvider gasProvider) {
			this.web3j = web3j;
			this.contractAddresses = contractAddresses;
			this.gasProvider = gasProvider;
		}

		public Contracts build(Credentials credentials) {
			return build(new RawTransactionManager(this.web3j, credentials));
		}

		public Contracts buildReadOnly(String accountAddress) {
			return build(new ReadonlyTransactionManager(this.web3j, accountAddress));
		}

		public Contracts build(TransactionManager transactionManager) {
			this.transactionManager = transactionManager;

			Map<Class<? extends Contract>, Contract> contracts = new HashMap<>();

			// nominee for ugliest code 2018 in Cannes
			contractAddresses.forEach((k, address) -> contracts.put(k, k.getDeclaredConstructor(String.class, Web3j.class, TransactionManager.class, ContractGasProvider.class).newInstance(address, this.web3j, transactionManager, this.gasProvider))); // yeeeehaww
			return new Contracts((CommunityTagIndex) contracts.get(CommunityTagIndex.class),
					(UserRegistry) contracts.get(UserRegistry.class),
					(ServiceRegistry) contracts.get(ServiceRegistry.class));
		}
	}
}
