package i5.las2peer.registryGateway.contracts;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
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
 * <p>Generated with web3j version 4.0.1.
 */
public class UserRegistry extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b50610e33806100206000396000f300608060405260043610610083576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063186a3d44146100885780632a28e5a3146100d1578063536add7e1461014857806379ce9fac146101915780639d8ec097146101e2578063ccaf54a81461022b578063cea6ab98146102a2575b600080fd5b34801561009457600080fd5b506100b760048036038101908080356000191690602001909291905050506103fa565b604051808215151515815260200191505060405180910390f35b3480156100dd57600080fd5b506101466004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610460565b005b34801561015457600080fd5b5061017760048036038101908080356000191690602001909291905050506104f7565b604051808215151515815260200191505060405180910390f35b34801561019d57600080fd5b506101e06004803603810190808035600019169060200190929190803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061051b565b005b3480156101ee57600080fd5b506102116004803603810190808035600019169060200190929190505050610742565b604051808215151515815260200191505060405180910390f35b34801561023757600080fd5b506102a06004803603810190808035600019169060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610756565b005b3480156102ae57600080fd5b506102d16004803603810190808035600019169060200190929190505050610914565b604051808560001916600019168152602001806020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001838103835286818151815260200191508051906020019080838360005b8381101561035557808201518184015260208101905061033a565b50505050905090810190601f1680156103825780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156103bb5780820151818401526020810190506103a0565b50505050905090810190601f1680156103e85780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390f35b6000806000808460001916600019168152602001908152602001600020905060008160020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415915050919050565b6104b4608060405190810160405280846000191681526020018381526020013373ffffffffffffffffffffffffffffffffffffffff1681526020016020604051908101604052806000815250815250610a94565b7f78997021e09413de1e36500ed07f9f6c73541162817fc3ea6a115e5e3d3affb98260405180826000191660001916815260200191505060405180910390a15050565b600061050282610742565b80156105145750610512826103fa565b155b9050919050565b8160006001028160001916141515156105c2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f456d707479206e616d65206973206e6f74206f776e656420627920616e796f6e81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff16600080836000191660001916815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415156106a2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260198152602001807f53656e64657220646f6573206e6f74206f776e206e616d652e0000000000000081525060200191505060405180910390fd5b81600080856000191660001916815260200190815260200160002060020160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055507f817c7b78adaf39aee990e279b002c658db5af0269244a5bba321a3f03d7c84178360405180826000191660001916815260200191505060405180910390a1505050565b600080600102826000191614159050919050565b8160006001028160001916141515156107fd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f456d707479206e616d65206973206e6f74206f776e656420627920616e796f6e81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff16600080836000191660001916815260200190815260200160002060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415156108dd576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260198152602001807f53656e64657220646f6573206e6f74206f776e206e616d652e0000000000000081525060200191505060405180910390fd5b816000808560001916600019168152602001908152602001600020600301908051906020019061090e929190610ce2565b50505050565b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156109c65780601f1061099b576101008083540402835291602001916109c6565b820191906000526020600020905b8154815290600101906020018083116109a957829003601f168201915b5050505050908060020160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1690806003018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610a8a5780601f10610a5f57610100808354040283529160200191610a8a565b820191906000526020600020905b815481529060010190602001808311610a6d57829003601f168201915b5050505050905084565b600060010281600001516000191614151515610b18576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260168152602001807f4e616d65206d757374206265206e6f6e2d7a65726f2e0000000000000000000081525060200191505060405180910390fd5b6000816040015173ffffffffffffffffffffffffffffffffffffffff1614151515610bab576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f4f776e65722061646472657373206d757374206265206e6f6e2d7a65726f2e0081525060200191505060405180910390fd5b610bb881600001516104f7565b1515610c2c576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f4e616d6520616c72656164792074616b656e206f7220696e76616c69642e000081525060200191505060405180910390fd5b80600080836000015160001916600019168152602001908152602001600020600082015181600001906000191690556020820151816001019080519060200190610c77929190610d62565b5060408201518160020160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506060820151816003019080519060200190610cdb929190610d62565b5090505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10610d2357805160ff1916838001178555610d51565b82800160010185558215610d51579182015b82811115610d50578251825591602001919060010190610d35565b5b509050610d5e9190610de2565b5090565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10610da357805160ff1916838001178555610dd1565b82800160010185558215610dd1579182015b82811115610dd0578251825591602001919060010190610db5565b5b509050610dde9190610de2565b5090565b610e0491905b80821115610e00576000816000905550600101610de8565b5090565b905600a165627a7a7230582089334ec6dce9e944ad19414652514dd5e0b6ef64dcc64eadcb82896774f4e8ef0029";

    public static final String FUNC_USERS = "users";

    public static final String FUNC_NAMEISVALID = "nameIsValid";

    public static final String FUNC_NAMEISTAKEN = "nameIsTaken";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_SETSUPPLEMENT = "setSupplement";

    public static final String FUNC_TRANSFER = "transfer";

    public static final Event USERREGISTERED_EVENT = new Event("UserRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final Event USERTRANSFERRED_EVENT = new Event("UserTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1541421861694", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542788782705", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543166959423", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542660356610", "0x522dbc32a688dd887bee78ef593b8ba3bdea263c");
        _addresses.put("1541429234327", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1538948396210", "0x896d2ea49d60a7d490566c0ff7c0353cae494b80");
        _addresses.put("1540910817715", "0x39eaaef93bb162bbffb25207ffd8366f097621ba");
        _addresses.put("1543173884204", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1540914412800", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543175223552", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1538963171946", "0x5fe12658b58b3a4af2ec8c4a3a2a51805b59056b");
        _addresses.put("1542029033973", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543166567414", "0x10294d2950378c3f811c7be05da0481fc5b64387");
        _addresses.put("1337", "0xcd3bf94d85ebd9c8a31a3fb9905eb5dc7ff66182");
        _addresses.put("1543180833463", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543175929774", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543173626032", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542106474778", "0xf300eb5556912ce4aeca505a7d9308fe7c6cd6bd");
        _addresses.put("1542106226176", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1537802983234", "0x641b3fa5e1dfe5dc5fad4c42f2aa1e46ffb52c57");
        _addresses.put("1542729634872", "0x5f00ea380b2027db95ef20f0d0d72ab778daa22f");
        _addresses.put("1543176208799", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("456719", "0x7117983d3be99e1cbe296dfeaf034c91db3cd02b");
        _addresses.put("1538343946582", "0xc06b03b0871c21550c1222306304c7d2307d9316");
        _addresses.put("1543176413402", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543182664634", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1537881365143", "0x777f6c40d84df9d9e43efd6590c482576d2daaf9");
        _addresses.put("1541353003952", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542059718816", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542059511560", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543180431517", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1541006760161", "0x9adc3881b3858377a82f39a564536fd9a4a2a929");
        _addresses.put("1541606582953", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543183353235", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543178229634", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543176596896", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542554349057", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
    }

    @Deprecated
    protected UserRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected UserRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected UserRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected UserRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<Tuple4<byte[], byte[], String, byte[]>> users(byte[] param0) {
        final Function function = new Function(FUNC_USERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Address>() {}, new TypeReference<DynamicBytes>() {}));
        return new RemoteCall<Tuple4<byte[], byte[], String, byte[]>>(
                new Callable<Tuple4<byte[], byte[], String, byte[]>>() {
                    @Override
                    public Tuple4<byte[], byte[], String, byte[]> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<byte[], byte[], String, byte[]>(
                                (byte[]) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (byte[]) results.get(3).getValue());
                    }
                });
    }

    public List<UserRegisteredEventResponse> getUserRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(USERREGISTERED_EVENT, transactionReceipt);
        ArrayList<UserRegisteredEventResponse> responses = new ArrayList<UserRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UserRegisteredEventResponse> userRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UserRegisteredEventResponse>() {
            @Override
            public UserRegisteredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(USERREGISTERED_EVENT, log);
                UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UserRegisteredEventResponse> userRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERREGISTERED_EVENT));
        return userRegisteredEventFlowable(filter);
    }

    public List<UserTransferredEventResponse> getUserTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(USERTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<UserTransferredEventResponse> responses = new ArrayList<UserTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserTransferredEventResponse typedResponse = new UserTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UserTransferredEventResponse> userTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UserTransferredEventResponse>() {
            @Override
            public UserTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(USERTRANSFERRED_EVENT, log);
                UserTransferredEventResponse typedResponse = new UserTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UserTransferredEventResponse> userTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERTRANSFERRED_EVENT));
        return userTransferredEventFlowable(filter);
    }

    public RemoteCall<Boolean> nameIsValid(byte[] name) {
        final Function function = new Function(FUNC_NAMEISVALID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> nameIsTaken(byte[] name) {
        final Function function = new Function(FUNC_NAMEISTAKEN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> nameIsAvailable(byte[] name) {
        final Function function = new Function(FUNC_NAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> register(byte[] name, byte[] agentId) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> setSupplement(byte[] name, byte[] supplement) {
        final Function function = new Function(
                FUNC_SETSUPPLEMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(supplement)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> transfer(byte[] name, String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static UserRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new UserRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static UserRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new UserRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static UserRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new UserRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static UserRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new UserRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<UserRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(UserRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<UserRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(UserRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<UserRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(UserRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<UserRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(UserRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class UserRegisteredEventResponse {
        public Log log;

        public byte[] name;
    }

    public static class UserTransferredEventResponse {
        public Log log;

        public byte[] name;
    }
}
