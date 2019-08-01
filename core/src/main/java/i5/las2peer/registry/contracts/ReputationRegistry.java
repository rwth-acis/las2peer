package i5.las2peer.registry.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.3.0.
 */
public class ReputationRegistry extends Contract {
    private static final String BINARY = "0x608060405260056001557fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff60015402600255600260015402600355600280540260045534801561004e57600080fd5b5060405160208061128c8339810180604052602081101561006e57600080fd5b8101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506111bd806100cf6000396000f3fe608060405234801561001057600080fd5b50600436106100625760003560e01c8063153e0b47146100675780631eefe9b9146100bf5780635c7460d61461010d5780635e4177f214610157578063bbe15627146101af578063d4800b40146102ad575b600080fd5b6100a96004803603602081101561007d57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506102db565b6040518082815260200191505060405180910390f35b61010b600480360360408110156100d557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919050505061043a565b005b610115610a4f565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6101996004803603602081101561016d57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610a74565b6040518082815260200191505060405180910390f35b6101f1600480360360208110156101c557600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610bd3565b604051808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848152602001838152602001828103825285818151815260200191508051906020019080838360005b8381101561026f578082015181840152602081019050610254565b50505050905090810190601f16801561029c5780820380516001836020036101000a031916815260200191505b509550505050505060405180910390f35b6102d9600480360360208110156102c357600080fd5b8101908080359060200190929190505050610cbb565b005b6000816000600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614156103e8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f50726f66696c65206e6f7420666f756e642e000000000000000000000000000081525060200191505060405180910390fd5b6000600560008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020905080600301549350505050919050565b336000600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415610545576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f50726f66696c65206e6f7420666f756e642e000000000000000000000000000081525060200191505060405180910390fd5b836000600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415610650576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f50726f66696c65206e6f7420666f756e642e000000000000000000000000000081525060200191505060405180910390fd5b600154851315801561066457506002548512155b6106b9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260398152602001806111596039913960400191505060405180910390fd5b8573ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16141561075b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260148152602001807f43616e6e6f74207261746520796f757273656c6600000000000000000000000081525060200191505060405180910390fd5b600085600560008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060020154019050600086600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060040160008a73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205401905060035481131561083a5760035490505b60045481121561084a5760045490505b60006001600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206003015401905060006001600560008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301540190506108ea338b8b8761103c565b83600560008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206002018190555080600560008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206003018190555081600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206003018190555082600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060040160008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208190555050505050505050505050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6000816000600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415610b81576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f50726f66696c65206e6f7420666f756e642e000000000000000000000000000081525060200191505060405180910390fd5b6000600560008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020905080600201549350505050919050565b60056020528060005260406000206000915090508060000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690806001018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610ca55780601f10610c7a57610100808354040283529160200191610ca5565b820191906000526020600020905b815481529060010190602001808311610c8857829003601f168201915b5050505050908060020154908060030154905084565b33816000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663e96b462a83836040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019250505060206040518083038186803b158015610d6357600080fd5b505afa158015610d77573d6000803e3d6000fd5b505050506040513d6020811015610d8d57600080fd5b8101908080519060200190929190505050610e10576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260208152602001807f53656e64657220646f6573206e6f74206f776e2075736572206163636f756e7481525060200191505060405180910390fd5b336000600560008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614610f1a576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260158152602001807f50726f66696c652063616e6e6f742065786973742e000000000000000000000081525060200191505060405180910390fd5b6000600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600201819055506000600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206003018190555033600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555061103585336110e9565b5050505050565b7f9f082b72d789110e5f5e68dfc8698d117d09c3c8f37b99de480152963bca291084848484604051808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200183815260200182815260200194505050505060405180910390a150505050565b7f97ac5d16ba6a936f2c55834f0a22fa19a40cb24e9629f714bc2c63890139cb2f8282604051808381526020018273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019250505060405180910390a1505056fe526174696e67206d75737420626520616e20696e74206265747765656e205f5f616d6f756e744d696e20616e64205f5f616d6f756e744d6178a165627a7a72305820311e7db6854445a2bfaea573b5ffab72ae704b17d57e58c7987b1fc30d229c2d0029";

    public static final String FUNC_USERREGISTRY = "userRegistry";

    public static final String FUNC_PROFILES = "profiles";

    public static final String FUNC_GETNOTRANSACTIONS = "getNoTransactions";

    public static final String FUNC_GETCUMULATIVESCORE = "getCumulativeScore";

    public static final String FUNC_CREATEPROFILE = "createProfile";

    public static final String FUNC_ADDTRANSACTION = "addTransaction";

    public static final Event USERPROFILECREATED_EVENT = new Event("UserProfileCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event TRANSACTIONADDED_EVENT = new Event("TransactionAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Int256>() {}, new TypeReference<Int256>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("5777", "0x8019e6E67D63b16605d0D08b3844Fc003481A406");
    }

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<String> userRegistry() {
        final Function function = new Function(FUNC_USERREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<Tuple4<String, byte[], BigInteger, BigInteger>> profiles(String param0) {
        final Function function = new Function(FUNC_PROFILES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Int256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple4<String, byte[], BigInteger, BigInteger>>(
                new Callable<Tuple4<String, byte[], BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<String, byte[], BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<String, byte[], BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public List<UserProfileCreatedEventResponse> getUserProfileCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(USERPROFILECREATED_EVENT, transactionReceipt);
        ArrayList<UserProfileCreatedEventResponse> responses = new ArrayList<UserProfileCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserProfileCreatedEventResponse typedResponse = new UserProfileCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UserProfileCreatedEventResponse> userProfileCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UserProfileCreatedEventResponse>() {
            @Override
            public UserProfileCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(USERPROFILECREATED_EVENT, log);
                UserProfileCreatedEventResponse typedResponse = new UserProfileCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.owner = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UserProfileCreatedEventResponse> userProfileCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERPROFILECREATED_EVENT));
        return userProfileCreatedEventFlowable(filter);
    }

    public List<TransactionAddedEventResponse> getTransactionAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSACTIONADDED_EVENT, transactionReceipt);
        ArrayList<TransactionAddedEventResponse> responses = new ArrayList<TransactionAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransactionAddedEventResponse typedResponse = new TransactionAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.subject = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.grade = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.subjectNewScore = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransactionAddedEventResponse> transactionAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, TransactionAddedEventResponse>() {
            @Override
            public TransactionAddedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSACTIONADDED_EVENT, log);
                TransactionAddedEventResponse typedResponse = new TransactionAddedEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.subject = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.grade = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.subjectNewScore = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransactionAddedEventResponse> transactionAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSACTIONADDED_EVENT));
        return transactionAddedEventFlowable(filter);
    }

    public RemoteCall<BigInteger> getNoTransactions(String profileID) {
        final Function function = new Function(FUNC_GETNOTRANSACTIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(profileID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> getCumulativeScore(String profileID) {
        final Function function = new Function(FUNC_GETCUMULATIVESCORE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(profileID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> createProfile(byte[] username) {
        final Function function = new Function(
                FUNC_CREATEPROFILE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(username)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> addTransaction(String contrahent, BigInteger amount) {
        final Function function = new Function(
                FUNC_ADDTRANSACTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(contrahent), 
                new org.web3j.abi.datatypes.generated.Int256(amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class UserProfileCreatedEventResponse {
        public Log log;

        public byte[] name;

        public String owner;
    }

    public static class TransactionAddedEventResponse {
        public Log log;

        public String sender;

        public String subject;

        public BigInteger grade;

        public BigInteger subjectNewScore;
    }
}
