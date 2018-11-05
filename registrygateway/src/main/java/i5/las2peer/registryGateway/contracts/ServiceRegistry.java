package i5.las2peer.registryGateway.contracts;

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
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import rx.Observable;
import rx.functions.Func1;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 3.6.0.
 */
public class ServiceRegistry extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b50604051602080610e7983398101806040528101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050610df6806100836000396000f300608060405260043610610078576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632f9267321461007d578063536add7e146100bc5780635c7460d614610105578063a79303e11461015c578063ce182267146101b9578063f6f43dd814610206575b600080fd5b34801561008957600080fd5b506100ba600480360381019080803560001916906020019092919080356000191690602001909291905050506102de565b005b3480156100c857600080fd5b506100eb6004803603810190808035600019169060200190929190505050610768565b604051808215151515815260200191505060405180910390f35b34801561011157600080fd5b5061011a610796565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b34801561016857600080fd5b506101b7600480360381019080803560001916906020019092919080356000191690602001909291908035906020019092919080359060200190929190803590602001909291905050506107bb565b005b3480156101c557600080fd5b506101e86004803603810190808035600019169060200190929190505050610807565b60405180826000191660001916815260200191505060405180910390f35b34801561021257600080fd5b5061023f60048036038101908080356000191690602001909291908035906020019092919050505061081f565b60405180866000191660001916815260200185815260200184815260200183815260200180602001828103825283818151815260200191508051906020019080838360005b8381101561029f578082015181840152602081019050610284565b50505050905090810190601f1680156102cc5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390f35b816000600102816000191614151515610385576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f576861746576657220746869732069732c206974206d757374206265206e6f6e81526020017f2d7a65726f2e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b81600060010281600019161415151561042c576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f576861746576657220746869732069732c206974206d757374206265206e6f6e81526020017f2d7a65726f2e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b8260008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b1580156104c757600080fd5b505af11580156104db573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250608081101561050557600080fd5b8101908080519060200190929190805164010000000081111561052757600080fd5b8281019050602081018481111561053d57600080fd5b815185600182028301116401000000008211171561055a57600080fd5b505092919060200180519060200190929190805164010000000081111561058057600080fd5b8281019050602081018481111561059657600080fd5b81518560018202830111640100000000821117156105b357600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515610689576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61069286610768565b1515610706576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206e616d6520616c72656164792074616b656e2e000000000081525060200191505060405180910390fd5b846001600088600019166000191681526020019081526020016000208160001916905550846000191686600019167f587ee397ee087d1766f5b574e2cd8250ba438936503a0aeb64ecbe2d558c6f5060405160405180910390a3505050505050565b6000806001026001600084600019166000191681526020019081526020016000205460001916149050919050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b61080060a06040519081016040528087600019168152602001858152602001848152602001838152602001602060405190810160405280600081525081525085610909565b5050505050565b60016020528060005260406000206000915090505481565b60026020528160005260406000208181548110151561083a57fe5b906000526020600020906005020160009150915050806000015490806001015490806002015490806003015490806004018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156108ff5780601f106108d4576101008083540402835291602001916108ff565b820191906000526020600020905b8154815290600101906020018083116108e257829003601f168201915b5050505050905085565b8060008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b1580156109a457600080fd5b505af11580156109b8573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f8201168201806040525060808110156109e257600080fd5b81019080805190602001909291908051640100000000811115610a0457600080fd5b82810190506020810184811115610a1a57600080fd5b8151856001820283011164010000000082111715610a3757600080fd5b5050929190602001805190602001909291908051640100000000811115610a5d57600080fd5b82810190506020810184811115610a7357600080fd5b8151856001820283011164010000000082111715610a9057600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515610b66576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b8260001916600160008660000151600019166000191681526020019081526020016000205460001916141515610c2a576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260238152602001807f50617373656420617574686f7220646f6573206e6f74206f776e20736572766981526020017f63652e000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b600260008560000151600019166000191681526020019081526020016000208490806001815401808255809150509060018203906000526020600020906005020160009091929091909150600082015181600001906000191690556020820151816001015560408201518160020155606082015181600301556080820151816004019080519060200190610cbf929190610d25565b505050508360000151600019167ff62f7cf7353931d14e66f186b11b77866f796c2148301a06a4288fd8de1be7d385602001518660400151876060015160405180848152602001838152602001828152602001935050505060405180910390a250505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10610d6657805160ff1916838001178555610d94565b82800160010185558215610d94579182015b82811115610d93578251825591602001919060010190610d78565b5b509050610da19190610da5565b5090565b610dc791905b80821115610dc3576000816000905550600101610dab565b5090565b905600a165627a7a723058203dc2389d0d3c5b9ca2c771b1a700e3568404121ba2816ddce68b87ef008255d80029";

    public static final String FUNC_USERREGISTRY = "userRegistry";

    public static final String FUNC_SERVICENAMETOAUTHOR = "serviceNameToAuthor";

    public static final String FUNC_SERVICENAMETORELEASES = "serviceNameToReleases";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_RELEASE = "release";

    public static final Event SERVICECREATED_EVENT = new Event("ServiceCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event SERVICERELEASED_EVENT = new Event("ServiceReleased", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1541421861694", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1537881365143", "0x531b6526887966d8f5f85ed57c82b56a2935c7fd");
        _addresses.put("1541353003952", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1541429234327", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1538948396210", "0xd58d24b40b96b40dc9bed699b032adb7608b779c");
        _addresses.put("1541006760161", "0x939f5660e8c88af96aeda17887ece6e2c04901a8");
        _addresses.put("1540910817715", "0x902edbee0906bbecf149a9756c9418217c11ea3b");
        _addresses.put("1540914412800", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1537802983234", "0x55226bf1e5f53144cd6501aca6cbc8ba337c96e4");
        _addresses.put("456719", "0x75c359d36b0cc959107ec82523c4f9d65bf69f03");
        _addresses.put("1538963171946", "0x8e5896c24dadcc8eccb24409b8b2f8fa0843fe60");
        _addresses.put("1538343946582", "0xa98fab7089a9f53300febbe1300ebfa45e8b75ff");
    }

    @Deprecated
    protected ServiceRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ServiceRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ServiceRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ServiceRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<String> userRegistry() {
        final Function function = new Function(FUNC_USERREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<byte[]> serviceNameToAuthor(byte[] param0) {
        final Function function = new Function(FUNC_SERVICENAMETOAUTHOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]>> serviceNameToReleases(byte[] param0, BigInteger param1) {
        final Function function = new Function(FUNC_SERVICENAMETORELEASES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<DynamicBytes>() {}));
        return new RemoteCall<Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]>>(
                new Callable<Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]>>() {
                    @Override
                    public Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<byte[], BigInteger, BigInteger, BigInteger, byte[]>(
                                (byte[]) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (byte[]) results.get(4).getValue());
                    }
                });
    }

    public static RemoteCall<ServiceRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ServiceRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ServiceRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ServiceRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ServiceRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ServiceRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ServiceRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ServiceRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public List<ServiceCreatedEventResponse> getServiceCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICECREATED_EVENT, transactionReceipt);
        ArrayList<ServiceCreatedEventResponse> responses = new ArrayList<ServiceCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ServiceCreatedEventResponse> serviceCreatedEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ServiceCreatedEventResponse>() {
            @Override
            public ServiceCreatedEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICECREATED_EVENT, log);
                ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ServiceCreatedEventResponse> serviceCreatedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICECREATED_EVENT));
        return serviceCreatedEventObservable(filter);
    }

    public List<ServiceReleasedEventResponse> getServiceReleasedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICERELEASED_EVENT, transactionReceipt);
        ArrayList<ServiceReleasedEventResponse> responses = new ArrayList<ServiceReleasedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceReleasedEventResponse typedResponse = new ServiceReleasedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ServiceReleasedEventResponse> serviceReleasedEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, ServiceReleasedEventResponse>() {
            @Override
            public ServiceReleasedEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICERELEASED_EVENT, log);
                ServiceReleasedEventResponse typedResponse = new ServiceReleasedEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<ServiceReleasedEventResponse> serviceReleasedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICERELEASED_EVENT));
        return serviceReleasedEventObservable(filter);
    }

    public RemoteCall<Boolean> nameIsAvailable(byte[] name) {
        final Function function = new Function(FUNC_NAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> register(byte[] serviceName, byte[] authorName) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> release(byte[] serviceName, byte[] authorName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch) {
        final Function function = new Function(
                FUNC_RELEASE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ServiceRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ServiceRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ServiceRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ServiceRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ServiceRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ServiceRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ServiceRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ServiceRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class ServiceCreatedEventResponse {
        public Log log;

        public byte[] name;

        public byte[] author;
    }

    public static class ServiceReleasedEventResponse {
        public Log log;

        public byte[] name;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;
    }
}
