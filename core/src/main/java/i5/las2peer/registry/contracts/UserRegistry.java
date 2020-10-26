package i5.las2peer.registry.contracts;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
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
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
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
 * <p>Generated with web3j version 4.6.1.
 */
@SuppressWarnings("rawtypes")
public class UserRegistry extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b50612411806100206000396000f3fe608060405234801561001057600080fd5b50600436106100b45760003560e01c80639d8ec097116100715780639d8ec0971461071f578063a63eaad314610765578063cea6ab9814610982578063d0e9552114610acf578063e96b462a14610c2b578063ebc1b8ff14610c91576100b4565b8063186a3d44146100b95780633b9dd1e1146100ff578063536add7e1461026557806363949ef6146102ab57806379a06d33146104be57806379ce9fac146106d1575b600080fd5b6100e5600480360360208110156100cf57600080fd5b8101908080359060200190929190505050610ded565b604051808215151515815260200191505060405180910390f35b6102636004803603608081101561011557600080fd5b81019080803590602001909291908035906020019064010000000081111561013c57600080fd5b82018360208201111561014e57600080fd5b8035906020019184600183028401116401000000008311171561017057600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156101d357600080fd5b8201836020820111156101e557600080fd5b8035906020019184600183028401116401000000008311171561020757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190929190505050610e61565b005b6102916004803603602081101561027b57600080fd5b8101908080359060200190929190505050610e74565b604051808215151515815260200191505060405180910390f35b6104bc600480360360a08110156102c157600080fd5b8101908080359060200190929190803590602001906401000000008111156102e857600080fd5b8201836020820111156102fa57600080fd5b8035906020019184600183028401116401000000008311171561031c57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561037f57600080fd5b82018360208201111561039157600080fd5b803590602001918460018302840111640100000000831117156103b357600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019064010000000081111561043657600080fd5b82018360208201111561044857600080fd5b8035906020019184600183028401116401000000008311171561046a57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610e98565b005b6106cf600480360360a08110156104d457600080fd5b8101908080359060200190929190803590602001906401000000008111156104fb57600080fd5b82018360208201111561050d57600080fd5b8035906020019184600183028401116401000000008311171561052f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561059257600080fd5b8201836020820111156105a457600080fd5b803590602001918460018302840111640100000000831117156105c657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019064010000000081111561064957600080fd5b82018360208201111561065b57600080fd5b8035906020019184600183028401116401000000008311171561067d57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506111f2565b005b61071d600480360360408110156106e757600080fd5b8101908080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909291905050506115af565b005b61074b6004803603602081101561073557600080fd5b81019080803590602001909291905050506116f1565b604051808215151515815260200191505060405180910390f35b610980600480360360c081101561077b57600080fd5b8101908080359060200190929190803590602001906401000000008111156107a257600080fd5b8201836020820111156107b457600080fd5b803590602001918460018302840111640100000000831117156107d657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561083957600080fd5b82018360208201111561084b57600080fd5b8035906020019184600183028401116401000000008311171561086d57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156108fa57600080fd5b82018360208201111561090c57600080fd5b8035906020019184600183028401116401000000008311171561092e57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050611701565b005b6109ae6004803603602081101561099857600080fd5b8101908080359060200190929190505050611adf565b6040518085815260200180602001806020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001838103835286818151815260200191508051906020019080838360005b83811015610a2a578082015181840152602081019050610a0f565b50505050905090810190601f168015610a575780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b83811015610a90578082015181840152602081019050610a75565b50505050905090810190601f168015610abd5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390f35b610c2960048036036060811015610ae557600080fd5b810190808035906020019092919080359060200190640100000000811115610b0c57600080fd5b820183602082011115610b1e57600080fd5b80359060200191846001830284011164010000000083111715610b4057600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190640100000000811115610ba357600080fd5b820183602082011115610bb557600080fd5b80359060200191846001830284011164010000000083111715610bd757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050611c5f565b005b610c7760048036036040811015610c4157600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050611c70565b604051808215151515815260200191505060405180910390f35b610deb60048036036060811015610ca757600080fd5b810190808035906020019092919080359060200190640100000000811115610cce57600080fd5b820183602082011115610ce057600080fd5b80359060200191846001830284011164010000000083111715610d0257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190640100000000811115610d6557600080fd5b820183602082011115610d7757600080fd5b80359060200191846001830284011164010000000083111715610d9957600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050611cde565b005b6000806000808481526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415915050919050565b610e6e8433858585611d22565b50505050565b6000610e7f826116f1565b8015610e915750610e8f82610ded565b155b9050919050565b60606040518060400160405280600481526020017febc1b8ff0000000000000000000000000000000000000000000000000000000081525090506060868686604051602001808481526020018060200180602001838103835285818151815260200191508051906020019080838360005b83811015610f24578082015181840152602081019050610f09565b50505050905090810190601f168015610f515780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610f8a578082015181840152602081019050610f6f565b50505050905090810190601f168015610fb75780820380516001836020036101000a031916815260200191505b5095505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff1660e01b81526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b8381101561107757808201518184015260208101905061105c565b50505050905090810190601f1680156110a45780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b838110156110dd5780820151818401526020810190506110c2565b50505050905090810190601f16801561110a5780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015611143578082015181840152602081019050611128565b50505050905090810190601f1680156111705780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b15801561119257600080fd5b505af41580156111a6573d6000803e3d6000fd5b505050506111e960405180608001604052808981526020018881526020018781526020018673ffffffffffffffffffffffffffffffffffffffff16815250611e8e565b50505050505050565b60606040518060400160405280600481526020017fd0e955210000000000000000000000000000000000000000000000000000000081525090506060868686600260008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054604051602001808581526020018060200180602001848152602001838103835286818151815260200191508051906020019080838360005b838110156112c45780820151818401526020810190506112a9565b50505050905090810190601f1680156112f15780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b8381101561132a57808201518184015260208101905061130f565b50505050905090810190601f1680156113575780820380516001836020036101000a031916815260200191505b509650505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff1660e01b81526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b838110156114185780820151818401526020810190506113fd565b50505050905090810190601f1680156114455780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b8381101561147e578082015181840152602081019050611463565b50505050905090810190601f1680156114ab5780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b838110156114e45780820151818401526020810190506114c9565b50505050905090810190601f1680156115115780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b15801561153357600080fd5b505af4158015611547573d6000803e3d6000fd5b50505050600260008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600081548092919060010191905055506115a68785888861211c565b50505050505050565b816000801b81141561160c576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001806123bb6022913960400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff1660008083815260200190815260200160002060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16146116e2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260198152602001807f53656e64657220646f6573206e6f74206f776e206e616d652e0000000000000081525060200191505060405180910390fd5b6116ec8383612286565b505050565b60008060001b8214159050919050565b818661170d8282611c70565b61171657600080fd5b60606040518060400160405280600481526020017fd0acaefe000000000000000000000000000000000000000000000000000000008152509050606089898989600260008b73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054604051602001808681526020018060200180602001858152602001848152602001838103835287818151815260200191508051906020019080838360005b838110156117ef5780820151818401526020810190506117d4565b50505050905090810190601f16801561181c5780820380516001836020036101000a031916815260200191505b50838103825286818151815260200191508051906020019080838360005b8381101561185557808201518184015260208101905061183a565b50505050905090810190601f1680156118825780820380516001836020036101000a031916815260200191505b50975050505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838389896040518563ffffffff1660e01b81526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b83811015611944578082015181840152602081019050611929565b50505050905090810190601f1680156119715780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b838110156119aa57808201518184015260208101905061198f565b50505050905090810190601f1680156119d75780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015611a105780820151818401526020810190506119f5565b50505050905090810190601f168015611a3d5780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b158015611a5f57600080fd5b505af4158015611a73573d6000803e3d6000fd5b50505050600260008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008154809291906001019190505550611ad38a878b8b8b611d22565b50505050505050505050565b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015611b915780601f10611b6657610100808354040283529160200191611b91565b820191906000526020600020905b815481529060010190602001808311611b7457829003601f168201915b505050505090806002018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015611c2f5780601f10611c0457610100808354040283529160200191611c2f565b820191906000526020600020905b815481529060010190602001808311611c1257829003601f168201915b5050505050908060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905084565b611c6b8333848461211c565b505050565b60008273ffffffffffffffffffffffffffffffffffffffff1660008084815260200190815260200160002060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614905092915050565b611d1d60405180608001604052808581526020018481526020018381526020013373ffffffffffffffffffffffffffffffffffffffff16815250611e8e565b505050565b8385611d2e8282611c70565b611d3757600080fd5b7f3cea55a3b3b8781f56f0d331039e879d191d28593bd16790b4535328decd4221878686864201600160008d815260200190815260200160002054604051808681526020018060200180602001858152602001848152602001838103835287818151815260200191508051906020019080838360005b83811015611dc8578082015181840152602081019050611dad565b50505050905090810190601f168015611df55780820380516001836020036101000a031916815260200191505b50838103825286818151815260200191508051906020019080838360005b83811015611e2e578082015181840152602081019050611e13565b50505050905090810190601f168015611e5b5780820380516001836020036101000a031916815260200191505b5097505050505050505060405180910390a143600160008981526020019081526020016000208190555050505050505050565b6000801b81600001511415611f0b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260168152602001807f4e616d65206d757374206265206e6f6e2d7a65726f2e0000000000000000000081525060200191505060405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff16816060015173ffffffffffffffffffffffffffffffffffffffff161415611fb2576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f4f776e65722061646472657373206d757374206265206e6f6e2d7a65726f2e0081525060200191505060405180910390fd5b611fbf8160000151610e74565b612031576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f4e616d6520616c72656164792074616b656e206f7220696e76616c69642e000081525060200191505060405180910390fd5b806000808360000151815260200190815260200160002060008201518160000155602082015181600101908051906020019061206e929190612315565b50604082015181600201908051906020019061208b929190612315565b5060608201518160030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050507f63c9969310f1091dd9bcfe2b7dc93e34f3e02e4c02d6e8890c7b05800491935b816000015142604051808381526020018281526020019250505060405180910390a150565b82846121288282611c70565b61213157600080fd5b7f3cea55a3b3b8781f56f0d331039e879d191d28593bd16790b4535328decd42218685856000600160008c815260200190815260200160002054604051808681526020018060200180602001858152602001848152602001838103835287818151815260200191508051906020019080838360005b838110156121c15780820151818401526020810190506121a6565b50505050905090810190601f1680156121ee5780820380516001836020036101000a031916815260200191505b50838103825286818151815260200191508051906020019080838360005b8381101561222757808201518184015260208101905061220c565b50505050905090810190601f1680156122545780820380516001836020036101000a031916815260200191505b5097505050505050505060405180910390a1436001600088815260200190815260200160002081905550505050505050565b8060008084815260200190815260200160002060030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055507f817c7b78adaf39aee990e279b002c658db5af0269244a5bba321a3f03d7c8417826040518082815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061235657805160ff1916838001178555612384565b82800160010185558215612384579182015b82811115612383578251825591602001919060010190612368565b5b5090506123919190612395565b5090565b6123b791905b808211156123b357600081600090555060010161239b565b5090565b9056fe456d707479206e616d65206973206e6f74206f776e656420627920616e796f6e652ea265627a7a723158206490fee50f5b012ea5df2fba39bd9e032d51a0ae568c9ae3b9ee97e26d91675864736f6c63430005100032";

    public static final String FUNC_USERS = "users";

    public static final String FUNC_NAMEISVALID = "nameIsValid";

    public static final String FUNC_NAMEISTAKEN = "nameIsTaken";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DELEGATEDREGISTER = "delegatedRegister";

    public static final String FUNC_TRANSFER = "transfer";

    public static final String FUNC_SETATTRIBUTE = "setAttribute";

    public static final String FUNC_DELEGATEDSETATTRIBUTE = "delegatedSetAttribute";

    public static final String FUNC_REVOKEATTRIBUTE = "revokeAttribute";

    public static final String FUNC_DELEGATEDREVOKEATTRIBUTE = "delegatedRevokeAttribute";

    public static final Event DIDATTRIBUTECHANGED_EVENT = new Event("DIDAttributeChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event USERREGISTERED_EVENT = new Event("UserRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event USERTRANSFERRED_EVENT = new Event("UserTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("5777", "0x253726ffB2D04351A494bd49C3bf3887aaDF8167");
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

    public List<DIDAttributeChangedEventResponse> getDIDAttributeChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DIDATTRIBUTECHANGED_EVENT, transactionReceipt);
        ArrayList<DIDAttributeChangedEventResponse> responses = new ArrayList<DIDAttributeChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DIDAttributeChangedEventResponse typedResponse = new DIDAttributeChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.userName = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.attrName = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.value = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.validTo = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.previousChange = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DIDAttributeChangedEventResponse> dIDAttributeChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DIDAttributeChangedEventResponse>() {
            @Override
            public DIDAttributeChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DIDATTRIBUTECHANGED_EVENT, log);
                DIDAttributeChangedEventResponse typedResponse = new DIDAttributeChangedEventResponse();
                typedResponse.log = log;
                typedResponse.userName = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.attrName = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.value = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.validTo = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.previousChange = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DIDAttributeChangedEventResponse> dIDAttributeChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DIDATTRIBUTECHANGED_EVENT));
        return dIDAttributeChangedEventFlowable(filter);
    }

    public List<UserRegisteredEventResponse> getUserRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(USERREGISTERED_EVENT, transactionReceipt);
        ArrayList<UserRegisteredEventResponse> responses = new ArrayList<UserRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UserRegisteredEventResponse> userRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, UserRegisteredEventResponse>() {
            @Override
            public UserRegisteredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(USERREGISTERED_EVENT, log);
                UserRegisteredEventResponse typedResponse = new UserRegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
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
        return web3j.ethLogFlowable(filter).map(new Function<Log, UserTransferredEventResponse>() {
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

    public RemoteFunctionCall<Tuple4<byte[], byte[], byte[], String>> users(byte[] param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_USERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Address>() {}));
        return new RemoteFunctionCall<Tuple4<byte[], byte[], byte[], String>>(function,
                new Callable<Tuple4<byte[], byte[], byte[], String>>() {
                    @Override
                    public Tuple4<byte[], byte[], byte[], String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<byte[], byte[], byte[], String>(
                                (byte[]) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue(), 
                                (byte[]) results.get(2).getValue(), 
                                (String) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Boolean> nameIsValid(byte[] name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAMEISVALID, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> nameIsTaken(byte[] name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAMEISTAKEN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> nameIsAvailable(byte[] name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isOwner(String claimedOwner, byte[] userName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(claimedOwner), 
                new org.web3j.abi.datatypes.generated.Bytes32(userName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> register(byte[] name, byte[] agentId, byte[] publicKey) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> delegatedRegister(byte[] name, byte[] agentId, byte[] publicKey, String consentee, byte[] consentSignature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELEGATEDREGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(byte[] name, String newOwner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAttribute(byte[] userName, String attrName, byte[] value, BigInteger validity) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(userName), 
                new org.web3j.abi.datatypes.Utf8String(attrName), 
                new org.web3j.abi.datatypes.DynamicBytes(value), 
                new org.web3j.abi.datatypes.generated.Uint256(validity)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> delegatedSetAttribute(byte[] userName, String attrName, byte[] value, BigInteger validity, String consentee, byte[] consentSignature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELEGATEDSETATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(userName), 
                new org.web3j.abi.datatypes.Utf8String(attrName), 
                new org.web3j.abi.datatypes.DynamicBytes(value), 
                new org.web3j.abi.datatypes.generated.Uint256(validity), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeAttribute(byte[] userName, String attrName, byte[] value) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REVOKEATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(userName), 
                new org.web3j.abi.datatypes.Utf8String(attrName), 
                new org.web3j.abi.datatypes.DynamicBytes(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> delegatedRevokeAttribute(byte[] userName, String attrName, byte[] value, String consentee, byte[] consentSignature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELEGATEDREVOKEATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(userName), 
                new org.web3j.abi.datatypes.Utf8String(attrName), 
                new org.web3j.abi.datatypes.DynamicBytes(value), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
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

    public static class DIDAttributeChangedEventResponse extends BaseEventResponse {
        public byte[] userName;

        public String attrName;

        public byte[] value;

        public BigInteger validTo;

        public BigInteger previousChange;
    }

    public static class UserRegisteredEventResponse extends BaseEventResponse {
        public byte[] name;

        public BigInteger timestamp;
    }

    public static class UserTransferredEventResponse extends BaseEventResponse {
        public byte[] name;
    }
}
