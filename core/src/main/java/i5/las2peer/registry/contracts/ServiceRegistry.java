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
    private static final String BINARY = "0x608060405234801561001057600080fd5b506040516020806118e183398101806040528101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505061185e806100836000396000f3006080604052600436106100af576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806309cf8201146100b45780630a790eb7146101c75780630b0e7f151461028057806317c523d41461030557806335341fb4146103865780635c7460d6146103e35780635c953cd61461043a578063656afdee146105155780637fea39a31461058c578063df975418146106a9578063e585417d14610753575b600080fd5b3480156100c057600080fd5b506101c5600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001909291908035906020019092919080359060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610807565b005b3480156101d357600080fd5b506101f6600480360381019080803560001916906020019092919050505061093d565b60405180806020018360001916600019168152602001828103825284818151815260200191508051906020019080838360005b83811015610244578082015181840152602081019050610229565b50505050905090810190601f1680156102715780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b34801561028c57600080fd5b506102e7600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506109f9565b60405180826000191660001916815260200191505060405180910390f35b34801561031157600080fd5b5061036c600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610ad1565b604051808215151515815260200191505060405180910390f35b34801561039257600080fd5b506103bf600480360381019080803560001916906020019092919080359060200190929190505050610b0a565b60405180848152602001838152602001828152602001935050505060405180910390f35b3480156103ef57600080fd5b506103f8610b50565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b34801561044657600080fd5b50610513600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190803590602001909291908035906020019092919080359060200190929190803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290505050610b75565b005b34801561052157600080fd5b5061058a600480360381019080803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192908035600019169060200190929190505050610fee565b005b34801561059857600080fd5b506106a7600480360381019080803590602001908201803590602001908080601f0160208091040260200160405190810160405280939291908181526020018383808284378201915050505050509192919290803590602001908201803590602001908080601f016020809104026020016040519081016040528093929190818152602001838380828437820191505050505050919291929080359060200190929190803590602001909291908035906020019092919080359060200190929190803590602001908201803590602001908080601f01602080910402602001604051908101604052809392919081815260200183838082843782019150505050505091929192905050506114c0565b005b3480156106b557600080fd5b506106d860048036038101908080356000191690602001909291905050506115ff565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156107185780820151818401526020810190506106fd565b50505050905090810190601f1680156107455780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b34801561075f57600080fd5b5061078c6004803603810190808035600019169060200190929190803590602001909291905050506116bf565b6040518080602001828103825283818151815260200191508051906020019080838360005b838110156107cc5780820151818401526020810190506107b1565b50505050905090810190601f1680156107f95780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b610810866109f9565b600019167ff510cb69914e1d56476e7a7a845dc43456f60abcc0dd9ad00b1f36abc3e3a51f8686868686604051808060200186815260200185815260200184815260200180602001838103835288818151815260200191508051906020019080838360005b83811015610890578082015181840152602081019050610875565b50505050905090810190601f1680156108bd5780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156108f65780820151818401526020810190506108db565b50505050905090810190601f1680156109235780820380516001836020036101000a031916815260200191505b5097505050505050505060405180910390a2505050505050565b6001602052806000526040600020600091509050806000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156109e95780601f106109be576101008083540402835291602001916109e9565b820191906000526020600020905b8154815290600101906020018083116109cc57829003601f168201915b5050505050908060010154905082565b6000816040516020018082805190602001908083835b602083101515610a345780518252602082019150602081019050602083039250610a0f565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040516020818303038152906040526040518082805190602001908083835b602083101515610a9d5780518252602082019150602081019050602083039250610a78565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390209050919050565b60008060010260016000610ae4856109f9565b600019166000191681526020019081526020016000206001015460001916149050919050565b600260205281600052604060002081815481101515610b2557fe5b9060005260206000209060030201600091509150508060000154908060010154908060020154905083565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b60008560008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b158015610c1257600080fd5b505af1158015610c26573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f820116820180604052506080811015610c5057600080fd5b81019080805190602001909291908051640100000000811115610c7257600080fd5b82810190506020810184811115610c8857600080fd5b8151856001820283011164010000000082111715610ca557600080fd5b5050929190602001805190602001909291908051640100000000811115610ccb57600080fd5b82810190506020810184811115610ce157600080fd5b8151856001820283011164010000000082111715610cfe57600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515610dd4576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b610ddd896109f9565b925087600019166001600085600019166000191681526020019081526020016000206001015460001916141515610ea2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260238152602001807f50617373656420617574686f7220646f6573206e6f74206f776e20736572766981526020017f63652e000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b6002600084600019166000191681526020019081526020016000206060604051908101604052808981526020018881526020018781525090806001815401808255809150509060018203906000526020600020906003020160009091929091909150600082015181600001556020820151816001015560408201518160020155505050600360008460001916600019168152602001908152602001600020602060405190810160405280868152509080600181540180825580915050906001820390600052602060002001600090919290919091506000820151816000019080519060200190610f9392919061178d565b5050505082600019167ff62f7cf7353931d14e66f186b11b77866f796c2148301a06a4288fd8de1be7d388888860405180848152602001838152602001828152602001935050505060405180910390a2505050505050505050565b60008261100a60206040519081016040528060008152506109f9565b60001916611017826109f9565b6000191614151515611091576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260188152602001807f537472696e67206d757374206265206e6f6e2d7a65726f2e000000000000000081525060200191505060405180910390fd5b826000600102816000191614151515611138576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f576861746576657220746869732069732c206974206d757374206265206e6f6e81526020017f2d7a65726f2e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b8360008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663cea6ab98836040518263ffffffff167c0100000000000000000000000000000000000000000000000000000000028152600401808260001916600019168152602001915050600060405180830381600087803b1580156111d357600080fd5b505af11580156111e7573d6000803e3d6000fd5b505050506040513d6000823e3d601f19601f82011682018060405250608081101561121157600080fd5b8101908080519060200190929190805164010000000081111561123357600080fd5b8281019050602081018481111561124957600080fd5b815185600182028301116401000000008211171561126657600080fd5b505092919060200180519060200190929190805164010000000081111561128c57600080fd5b828101905060208101848111156112a257600080fd5b81518560018202830111640100000000821117156112bf57600080fd5b505092919050505050925050503373ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16141515611395576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f53656e646572206d757374206f776e20636c61696d65642075736572206e616d81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61139e87610ad1565b1515611412576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206e616d6520616c72656164792074616b656e2e000000000081525060200191505060405180910390fd5b61141b876109f9565b945060408051908101604052808881526020018760001916815250600160008760001916600019168152602001908152602001600020600082015181600001908051906020019061146d92919061178d565b5060208201518160010190600019169055905050856000191685600019167f587ee397ee087d1766f5b574e2cd8250ba438936503a0aeb64ecbe2d558c6f5060405160405180910390a350505050505050565b6114c9876109f9565b600019167fc08958a80ffb663c0dc2d0a5d36134e5b01103a3433014c88b0279a24671348c878787878787604051808060200187815260200186815260200185815260200184815260200180602001838103835289818151815260200191508051906020019080838360005b83811015611550578082015181840152602081019050611535565b50505050905090810190601f16801561157d5780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156115b657808201518184015260208101905061159b565b50505050905090810190601f1680156115e35780820380516001836020036101000a031916815260200191505b509850505050505050505060405180910390a250505050505050565b60606001600083600019166000191681526020019081526020016000206000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156116b35780601f10611688576101008083540402835291602001916116b3565b820191906000526020600020905b81548152906001019060200180831161169657829003601f168201915b50505050509050919050565b6003602052816000526040600020818154811015156116da57fe5b9060005260206000200160009150915050806000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156117835780601f1061175857610100808354040283529160200191611783565b820191906000526020600020905b81548152906001019060200180831161176657829003601f168201915b5050505050905081565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106117ce57805160ff19168380011785556117fc565b828001600101855582156117fc579182015b828111156117fb5782518255916020019190600101906117e0565b5b509050611809919061180d565b5090565b61182f91905b8082111561182b576000816000905550600101611813565b5090565b905600a165627a7a72305820676812a556bd72f8a57a9f9fa3d19aae6ca7c468b7a657a4e8e77be772f209bf0029";

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

    public static final String FUNC_ANNOUNCEDEPLOYMENTEND = "announceDeploymentEnd";

    public static final Event SERVICECREATED_EVENT = new Event("ServiceCreated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event SERVICERELEASED_EVENT = new Event("ServiceReleased",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SERVICEDEPLOYMENT_EVENT = new Event("ServiceDeployment",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event SERVICEDEPLOYMENTEND_EVENT = new Event("ServiceDeploymentEnd",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
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
        _addresses.put("1337", "0x0530420575bcbaacdfc0377ca3aa6f40c542a30f");
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
            typedResponse.className = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(5).getValue();
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
                typedResponse.className = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
                typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(5).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceDeploymentEventResponse> serviceDeploymentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICEDEPLOYMENT_EVENT));
        return serviceDeploymentEventFlowable(filter);
    }

    public List<ServiceDeploymentEndEventResponse> getServiceDeploymentEndEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICEDEPLOYMENTEND_EVENT, transactionReceipt);
        ArrayList<ServiceDeploymentEndEventResponse> responses = new ArrayList<ServiceDeploymentEndEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceDeploymentEndEventResponse typedResponse = new ServiceDeploymentEndEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.className = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceDeploymentEndEventResponse> serviceDeploymentEndEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ServiceDeploymentEndEventResponse>() {
            @Override
            public ServiceDeploymentEndEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICEDEPLOYMENTEND_EVENT, log);
                ServiceDeploymentEndEventResponse typedResponse = new ServiceDeploymentEndEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.className = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceDeploymentEndEventResponse> serviceDeploymentEndEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICEDEPLOYMENTEND_EVENT));
        return serviceDeploymentEndEventFlowable(filter);
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

    public RemoteCall<TransactionReceipt> announceDeployment(String serviceName, String className, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, BigInteger timestamp, String nodeId) {
        final Function function = new Function(
                FUNC_ANNOUNCEDEPLOYMENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName),
                new org.web3j.abi.datatypes.Utf8String(className),
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor),
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor),
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch),
                new org.web3j.abi.datatypes.generated.Uint256(timestamp),
                new org.web3j.abi.datatypes.Utf8String(nodeId)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> announceDeploymentEnd(String serviceName, String className, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, String nodeId) {
        final Function function = new Function(
                FUNC_ANNOUNCEDEPLOYMENTEND,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName),
                new org.web3j.abi.datatypes.Utf8String(className),
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor),
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor),
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch),
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

        public String className;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public BigInteger timestamp;

        public String nodeId;
    }

    public static class ServiceDeploymentEndEventResponse {
        public Log log;

        public byte[] nameHash;

        public String className;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public String nodeId;
    }
}
