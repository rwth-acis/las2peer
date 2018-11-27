package i5.las2peer.registryGateway;

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

public class Contracts extends HashMap<Class<? extends Contract>, Contract> {
	public <T extends Contract> T byClass(Class<T> clazz) {
		return (T) this.get(clazz);
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

			Contracts contracts = new Contracts();
			for (Map.Entry<Class<? extends Contract>, String> entry : contractAddresses.entrySet()) {
				Class<? extends Contract> contractClass = entry.getKey();
				String address = entry.getValue();

				try {
					// wow, Java sure is simple
					Constructor<?> ctor = contractClass.getDeclaredConstructor(String.class, Web3j.class, TransactionManager.class, ContractGasProvider.class);
					Contract contract = (Contract) ctor.newInstance(address, web3j, transactionManager, gasProvider);
					contracts.put(contractClass, contract);
				} catch (NoSuchMethodException|InstantiationException|IllegalAccessException| InvocationTargetException e) {
				}
			}
			return contracts;
		}
	}
}
