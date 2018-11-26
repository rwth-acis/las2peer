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
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;
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
public class ServiceRegistry extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b506040516020806115d983398101806040528101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050611556806100836000396000f3006080604052600436106100a4576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806306dc3b33146100a95780630a790eb7146101805780630b0e7f151461023957806317c523d4146102be57806335341fb41461033f5780635c7460d61461039c5780635c953cd6146103f3578063656afdee146104ce578063df97541814610545578063e585417d146105ef575b600080fd5b3480156100b557600080fd5b5061017e600480360381019080803590602001908201803590602001908080601f016020809104026020016040519081016040528093929190818152602001838380828437820191505050505050919291929080359060200190929190803590602001909291908035906020019092919080359060200190929190803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506106a3565b005b34801561018c57600080fd5b506101af6004803603810190808035600019169060200190929190505050610774565b60405180806020018360001916600019168152602001828103825284818151815260200191508051906020019080838360005b838110156101fd5780820151818401526020810190506101e2565b50505050905090810190601f16801561022a5780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b34801561024557600080fd5b506102a0600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610830565b60405180826000191660001916815260200191505060405180910390f35b3480156102ca57600080fd5b50610325600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610908565b604051808215151515815260200191505060405180910390f35b34801561034b57600080fd5b50610378600480360381019080803560001916906020019092919080359060200190929190505050610941565b60405180848152602001838152602001828152602001935050505060405180910390f35b3480156103a857600080fd5b506103b1610987565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b3480156103ff57600080fd5b506104cc600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001909291908035906020019092919080359060200190929190803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506109ac565b005b3480156104da57600080fd5b50610543600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190505050610e25565b005b34801561055157600080fd5b5061057460048036038101908080356000191690602001909291905050506112f7565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156105b4578082015181840152602081019050610599565b50505050905090810190601f1680156105e15780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156105fb57600080fd5b506106286004803603810190808035600019169060200190929190803590602001909291905050506113b7565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561066857808201518184015260208101905061064d565b50505050905090810190601f1680156106955780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6106ac86610830565b600019167f3723a1336bcba4ef9bc652b490619680c97929daf40ad483bedd933673b85a2f86868686866040518086815260200185815260200184815260200183815260200180602001828103825283818151815260200191508051906020019080838360005b8381101561072e578082015181840152602081019050610713565b50505050905090810190601f16801561075b5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390a2505050505050565b6001602052806000526040600020600091509050806000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156108205780601f106107f557610100808354040283529160200191610820565b820191906000526020600020905b81548152906001019060200180831161080357829003601f168201915b5050505050908060010154905082565b6000816040516020018082805190602001908083835b60208310151561086b5780518252602082019150602081019050602083039250610846565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040516020818303038152906040526040518082805190602001908083835b6020831015156108d457805182526020820191506020810190506020830392506108af565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390209050919050565b6000806001026001600061091b85610830565b600019166000191681526020019081526020016000206001015460001916149050919050565b60026020528160005260406000208181548110151561095c57fe5b9060005260206000209060030201600091509150508060000154908060010154908060020154905083565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008560008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b158015610a4957600080fd5b505af1158015610a5d573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f820116820180604052506080811015610a8757600080fd5b81019080805190602001909291908051640100000000811115610aa957600080fd5b82810190506020810184811115610abf57600080fd5b8151856001820283011164010000000082111715610adc57600080fd5b5050929190602001805190602001909291908051640100000000811115610b0257600080fd5b82810190506020810184811115610b1857600080fd5b8151856001820283011164010000000082111715610b3557600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515610c0b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b610c1489610830565b925087600019166001600085600019166000191681526020019081526020016000206001015460001916141515610cd9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260238152602001807f50617373656420617574686f7220646f6573206e6f74206f776e20736572766981526020017f63652e000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b6002600084600019166000191681526020019081526020016000206060604051908101604052808981526020018881526020018781525090806001815401808255809150509060018203906000526020600020906003020160009091929091909150600082015181600001556020820151816001015560408201518160020155505050600360008460001916600019168152602001908152602001600020602060405190810160405280868152509080600181540180825580915050906001820390600052602060002001600090919290919091506000820151816000019080519060200190610dca929190611485565b5050505082600019167ff62f7cf7353931d14e66f186b11b77866f796c2148301a06a4288fd8de1be7d388888860405180848152602001838152602001828152602001935050505060405180910390a2505050505050505050565b600082610e416020604051908101604052806000815250610830565b60001916610e4e82610830565b6000191614151515610ec8576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260188152602001807f537472696e67206d757374206265206e6f6e2d7a65726f2e000000000000000081525060200191505060405180910390fd5b826000600102816000191614151515610f6f576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f576861746576657220746869732069732c206974206d757374206265206e6f6e81526020017f2d7a65726f2e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b8360008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b15801561100a57600080fd5b505af115801561101e573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250608081101561104857600080fd5b8101908080519060200190929190805164010000000081111561106a57600080fd5b8281019050602081018481111561108057600080fd5b815185600182028301116401000000008211171561109d57600080fd5b50509291906020018051906020019092919080516401000000008111156110c357600080fd5b828101905060208101848111156110d957600080fd5b81518560018202830111640100000000821117156110f657600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415156111cc576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b6111d587610908565b1515611249576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206e616d6520616c72656164792074616b656e2e000000000081525060200191505060405180910390fd5b61125287610830565b94506040805190810160405280888152602001876000191681525060016000876000191660001916815260200190815260200160002060008201518160000190805190602001906112a4929190611485565b5060208201518160010190600019169055905050856000191685600019167f587ee397ee087d1766f5b574e2cd8250ba438936503a0aeb64ecbe2d558c6f5060405160405180910390a350505050505050565b60606001600083600019166000191681526020019081526020016000206000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156113ab5780601f10611380576101008083540402835291602001916113ab565b820191906000526020600020905b81548152906001019060200180831161138e57829003601f168201915b50505050509050919050565b6003602052816000526040600020818154811015156113d257fe5b9060005260206000200160009150915050806000018054600181600116156101000203166002900480601f01602080910402602001604051908101604052809291908181526020018280546001816001161561010002031660029004801561147b5780601f106114505761010080835404028352916020019161147b565b820191906000526020600020905b81548152906001019060200180831161145e57829003601f168201915b5050505050905081565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106114c657805160ff19168380011785556114f4565b828001600101855582156114f4579182015b828111156114f35782518255916020019190600101906114d8565b5b5090506115019190611505565b5090565b61152791905b8082111561152357600081600090555060010161150b565b5090565b905600a165627a7a72305820a453ad59e045f97ebc7480dcddd93c4708398292f3b314c06b8ae08465437b5f0029";

    public static final String FUNC_SERVICES = "services";

    public static final String FUNC_SERVICEVERSIONS = "serviceVersions";

    public static final String FUNC_USERREGISTRY = "userRegistry";

    public static final String FUNC_SERVICEVERSIONMETADATA = "serviceVersionMetadata";

    public static final String FUNC_STRINGHASH = "stringHash";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_HASHTONAME = "hashToName";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_RELEASE = "release";

    public static final String FUNC_ANNOUNCEDEPLOYMENT = "announceDeployment";

    public static final Event SERVICECREATED_EVENT = new Event("ServiceCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event SERVICERELEASED_EVENT = new Event("ServiceReleased", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SERVICEDEPLOYMENT_EVENT = new Event("ServiceDeployment", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1541421861694", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542788782705", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543166959423", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542660356610", "0x4a30c74760864d53b72d0ee58933713e1e3460b1");
        _addresses.put("1541429234327", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1538948396210", "0xd58d24b40b96b40dc9bed699b032adb7608b779c");
        _addresses.put("1540910817715", "0x902edbee0906bbecf149a9756c9418217c11ea3b");
        _addresses.put("1543173884204", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1540914412800", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543175223552", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1538963171946", "0x8e5896c24dadcc8eccb24409b8b2f8fa0843fe60");
        _addresses.put("1542029033973", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543166567414", "0xd4d7aa7f9f4f1e3175576089836bb0cedf859030");
        _addresses.put("1337", "0x6d96b52d5d0e43e286d422e45d7480966ee586b6");
        _addresses.put("1543180833463", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543175929774", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543173626032", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542106474778", "0xfd44f06fc6fe0604898d51c4fe97964b09263ffc");
        _addresses.put("1542106226176", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1537802983234", "0x55226bf1e5f53144cd6501aca6cbc8ba337c96e4");
        _addresses.put("1542729634872", "0x97a7230a500d63bb2e015e184033052ae7d5b0e6");
        _addresses.put("1543176208799", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("456719", "0x97c4cf2c2318171aa9c075bf66b27be57321aeee");
        _addresses.put("1538343946582", "0xa98fab7089a9f53300febbe1300ebfa45e8b75ff");
        _addresses.put("1543176413402", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543182664634", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1537881365143", "0x531b6526887966d8f5f85ed57c82b56a2935c7fd");
        _addresses.put("1541353003952", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542059718816", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542059511560", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543180431517", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1541006760161", "0x939f5660e8c88af96aeda17887ece6e2c04901a8");
        _addresses.put("1541606582953", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543183353235", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543178229634", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1543176596896", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
        _addresses.put("1542554349057", "0xdd934d1dfb15be4f3e5a50199963ead449392bae");
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

    public RemoteCall<Tuple2<String, byte[]>> services(byte[] param0) {
        final Function function = new Function(FUNC_SERVICES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}));
        return new RemoteCall<Tuple2<String, byte[]>>(
                new Callable<Tuple2<String, byte[]>>() {
                    @Override
                    public Tuple2<String, byte[]> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, byte[]>(
                                (String) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue());
                    }
                });
    }

    public RemoteCall<Tuple3<BigInteger, BigInteger, BigInteger>> serviceVersions(byte[] param0, BigInteger param1) {
        final Function function = new Function(FUNC_SERVICEVERSIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteCall<Tuple3<BigInteger, BigInteger, BigInteger>>(
                new Callable<Tuple3<BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteCall<String> userRegistry() {
        final Function function = new Function(FUNC_USERREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<String> serviceVersionMetadata(byte[] param0, BigInteger param1) {
        final Function function = new Function(FUNC_SERVICEVERSIONMETADATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public List<ServiceCreatedEventResponse> getServiceCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICECREATED_EVENT, transactionReceipt);
        ArrayList<ServiceCreatedEventResponse> responses = new ArrayList<ServiceCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceCreatedEventResponse> serviceCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ServiceCreatedEventResponse>() {
            @Override
            public ServiceCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICECREATED_EVENT, log);
                ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceCreatedEventResponse> serviceCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICECREATED_EVENT));
        return serviceCreatedEventFlowable(filter);
    }

    public List<ServiceReleasedEventResponse> getServiceReleasedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICERELEASED_EVENT, transactionReceipt);
        ArrayList<ServiceReleasedEventResponse> responses = new ArrayList<ServiceReleasedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceReleasedEventResponse typedResponse = new ServiceReleasedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceReleasedEventResponse> serviceReleasedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ServiceReleasedEventResponse>() {
            @Override
            public ServiceReleasedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICERELEASED_EVENT, log);
                ServiceReleasedEventResponse typedResponse = new ServiceReleasedEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceReleasedEventResponse> serviceReleasedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICERELEASED_EVENT));
        return serviceReleasedEventFlowable(filter);
    }

    public List<ServiceDeploymentEventResponse> getServiceDeploymentEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICEDEPLOYMENT_EVENT, transactionReceipt);
        ArrayList<ServiceDeploymentEventResponse> responses = new ArrayList<ServiceDeploymentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceDeploymentEventResponse typedResponse = new ServiceDeploymentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceDeploymentEventResponse> serviceDeploymentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ServiceDeploymentEventResponse>() {
            @Override
            public ServiceDeploymentEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICEDEPLOYMENT_EVENT, log);
                ServiceDeploymentEventResponse typedResponse = new ServiceDeploymentEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceDeploymentEventResponse> serviceDeploymentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICEDEPLOYMENT_EVENT));
        return serviceDeploymentEventFlowable(filter);
    }

    public RemoteCall<byte[]> stringHash(String name) {
        final Function function = new Function(FUNC_STRINGHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<Boolean> nameIsAvailable(String serviceName) {
        final Function function = new Function(FUNC_NAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<String> hashToName(byte[] hashOfName) {
        final Function function = new Function(FUNC_HASHTONAME, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashOfName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> register(String serviceName, byte[] authorName) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> release(String serviceName, byte[] authorName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, String dhtSupplement) {
        final Function function = new Function(
                FUNC_RELEASE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch), 
                new org.web3j.abi.datatypes.Utf8String(dhtSupplement)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> announceDeployment(String serviceName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, BigInteger timestamp, String nodeId) {
        final Function function = new Function(
                FUNC_ANNOUNCEDEPLOYMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch), 
                new org.web3j.abi.datatypes.generated.Uint256(timestamp), 
                new org.web3j.abi.datatypes.Utf8String(nodeId)), 
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

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class ServiceCreatedEventResponse {
        public Log log;

        public byte[] nameHash;

        public byte[] author;
    }

    public static class ServiceReleasedEventResponse {
        public Log log;

        public byte[] nameHash;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;
    }

    public static class ServiceDeploymentEventResponse {
        public Log log;

        public byte[] nameHash;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public BigInteger timestamp;

        public String nodeId;
    }
}
