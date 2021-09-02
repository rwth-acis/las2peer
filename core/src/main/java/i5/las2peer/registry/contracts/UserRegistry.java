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
    private static final String BINARY = "0x608060405234801561001057600080fd5b50611c1b806100206000396000f3fe6080604052600436106100a4576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063186a3d44146100a95780633c114577146100fc578063536add7e1461026557806363949ef6146102b857806379ce9fac146104d857806381089014146105335780639d8ec09714610753578063cea6ab98146107a6578063e96b462a14610900578063ebc1b8ff14610973575b600080fd5b3480156100b557600080fd5b506100e2600480360360208110156100cc57600080fd5b8101908080359060200190929190505050610adc565b604051808215151515815260200191505060405180910390f35b34801561010857600080fd5b506102636004803603606081101561011f57600080fd5b81019080803590602001909291908035906020019064010000000081111561014657600080fd5b82018360208201111561015857600080fd5b8035906020019184600183028401116401000000008311171561017a57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156101dd57600080fd5b8201836020820111156101ef57600080fd5b8035906020019184600183028401116401000000008311171561021157600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610b50565b005b34801561027157600080fd5b5061029e6004803603602081101561028857600080fd5b8101908080359060200190929190505050610b95565b604051808215151515815260200191505060405180910390f35b3480156102c457600080fd5b506104d6600480360360a08110156102db57600080fd5b81019080803590602001909291908035906020019064010000000081111561030257600080fd5b82018360208201111561031457600080fd5b8035906020019184600183028401116401000000008311171561033657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561039957600080fd5b8201836020820111156103ab57600080fd5b803590602001918460018302840111640100000000831117156103cd57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019064010000000081111561045057600080fd5b82018360208201111561046257600080fd5b8035906020019184600183028401116401000000008311171561048457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610bb9565b005b3480156104e457600080fd5b50610531600480360360408110156104fb57600080fd5b8101908080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610f30565b005b34801561053f57600080fd5b50610751600480360360a081101561055657600080fd5b81019080803590602001909291908035906020019064010000000081111561057d57600080fd5b82018360208201111561058f57600080fd5b803590602001918460018302840111640100000000831117156105b157600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561061457600080fd5b82018360208201111561062657600080fd5b8035906020019184600183028401116401000000008311171561064857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156106cb57600080fd5b8201836020820111156106dd57600080fd5b803590602001918460018302840111640100000000831117156106ff57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506110ba565b005b34801561075f57600080fd5b5061078c6004803603602081101561077657600080fd5b8101908080359060200190929190505050611431565b604051808215151515815260200191505060405180910390f35b3480156107b257600080fd5b506107df600480360360208110156107c957600080fd5b8101908080359060200190929190505050611441565b6040518085815260200180602001806020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001838103835286818151815260200191508051906020019080838360005b8381101561085b578082015181840152602081019050610840565b50505050905090810190601f1680156108885780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b838110156108c15780820151818401526020810190506108a6565b50505050905090810190601f1680156108ee5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390f35b34801561090c57600080fd5b506109596004803603604081101561092357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001909291905050506115c1565b604051808215151515815260200191505060405180910390f35b34801561097f57600080fd5b50610ada6004803603606081101561099657600080fd5b8101908080359060200190929190803590602001906401000000008111156109bd57600080fd5b8201836020820111156109cf57600080fd5b803590602001918460018302840111640100000000831117156109f157600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190640100000000811115610a5457600080fd5b820183602082011115610a6657600080fd5b80359060200191846001830284011164010000000083111715610a8857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506115cd565b005b6000806000808481526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415915050919050565b610b906080604051908101604052808581526020018481526020018381526020013373ffffffffffffffffffffffffffffffffffffffff16815250611612565b505050565b6000610ba082611431565b8015610bb25750610bb082610adc565b155b9050919050565b60606040805190810160405280600481526020017febc1b8ff0000000000000000000000000000000000000000000000000000000081525090506060868686604051602001808481526020018060200180602001838103835285818151815260200191508051906020019080838360005b83811015610c45578082015181840152602081019050610c2a565b50505050905090810190601f168015610c725780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015610cab578082015181840152602081019050610c90565b50505050905090810190601f168015610cd85780820380516001836020036101000a031916815260200191505b5095505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b83811015610db4578082015181840152602081019050610d99565b50505050905090810190601f168015610de15780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b83811015610e1a578082015181840152602081019050610dff565b50505050905090810190601f168015610e475780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015610e80578082015181840152602081019050610e65565b50505050905090810190601f168015610ead5780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b158015610ecf57600080fd5b505af4158015610ee3573d6000803e3d6000fd5b50505050610f276080604051908101604052808981526020018881526020018781526020018673ffffffffffffffffffffffffffffffffffffffff16815250611826565b50505050505050565b8160006001028114151515610fd3576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f456d707479206e616d65206973206e6f74206f776e656420627920616e796f6e81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff1660008083815260200190815260200160002060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415156110ab576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260198152602001807f53656e64657220646f6573206e6f74206f776e206e616d652e0000000000000081525060200191505060405180910390fd5b6110b58383611abb565b505050565b60606040805190810160405280600481526020017febc1b8ff0000000000000000000000000000000000000000000000000000000081525090506060868686604051602001808481526020018060200180602001838103835285818151815260200191508051906020019080838360005b8381101561114657808201518184015260208101905061112b565b50505050905090810190601f1680156111735780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156111ac578082015181840152602081019050611191565b50505050905090810190601f1680156111d95780820380516001836020036101000a031916815260200191505b5095505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b838110156112b557808201518184015260208101905061129a565b50505050905090810190601f1680156112e25780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b8381101561131b578082015181840152602081019050611300565b50505050905090810190601f1680156113485780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015611381578082015181840152602081019050611366565b50505050905090810190601f1680156113ae5780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b1580156113d057600080fd5b505af41580156113e4573d6000803e3d6000fd5b505050506114286080604051908101604052808981526020018881526020018781526020018673ffffffffffffffffffffffffffffffffffffffff16815250611612565b50505050505050565b6000806001028214159050919050565b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156114f35780601f106114c8576101008083540402835291602001916114f3565b820191906000526020600020905b8154815290600101906020018083116114d657829003601f168201915b505050505090806002018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156115915780601f1061156657610100808354040283529160200191611591565b820191906000526020600020905b81548152906001019060200180831161157457829003601f168201915b5050505050908060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905084565b60006001905092915050565b61160d6080604051908101604052808581526020018481526020018381526020013373ffffffffffffffffffffffffffffffffffffffff16815250611826565b505050565b6000600102816000015114151515611692576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260168152602001807f4e616d65206d757374206265206e6f6e2d7a65726f2e0000000000000000000081525060200191505060405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff16816060015173ffffffffffffffffffffffffffffffffffffffff161415151561173b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f4f776e65722061646472657373206d757374206265206e6f6e2d7a65726f2e0081525060200191505060405180910390fd5b8060008083600001518152602001908152602001600020600082015181600001556020820151816001019080519060200190611778929190611b4a565b506040820151816002019080519060200190611795929190611b4a565b5060608201518160030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050507f63c9969310f1091dd9bcfe2b7dc93e34f3e02e4c02d6e8890c7b05800491935b816000015142604051808381526020018281526020019250505060405180910390a150565b60006001028160000151141515156118a6576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260168152602001807f4e616d65206d757374206265206e6f6e2d7a65726f2e0000000000000000000081525060200191505060405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff16816060015173ffffffffffffffffffffffffffffffffffffffff161415151561194f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f4f776e65722061646472657373206d757374206265206e6f6e2d7a65726f2e0081525060200191505060405180910390fd5b61195c8160000151610b95565b15156119d0576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f4e616d6520616c72656164792074616b656e206f7220696e76616c69642e000081525060200191505060405180910390fd5b8060008083600001518152602001908152602001600020600082015181600001556020820151816001019080519060200190611a0d929190611b4a565b506040820151816002019080519060200190611a2a929190611b4a565b5060608201518160030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050507f63c9969310f1091dd9bcfe2b7dc93e34f3e02e4c02d6e8890c7b05800491935b816000015142604051808381526020018281526020019250505060405180910390a150565b8060008084815260200190815260200160002060030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055507f817c7b78adaf39aee990e279b002c658db5af0269244a5bba321a3f03d7c8417826040518082815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f10611b8b57805160ff1916838001178555611bb9565b82800160010185558215611bb9579182015b82811115611bb8578251825591602001919060010190611b9d565b5b509050611bc69190611bca565b5090565b611bec91905b80821115611be8576000816000905550600101611bd0565b5090565b9056fea165627a7a72305820764fed74563eaa37840ae23101c8ccb1fc9480cf3e4551182b61ecbb3d60971e0029";

    public static final String FUNC_USERS = "users";

    public static final String FUNC_NAMEISVALID = "nameIsValid";

    public static final String FUNC_NAMEISTAKEN = "nameIsTaken";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_UPDATE = "update";

    public static final String FUNC_DELEGATEDREGISTER = "delegatedRegister";

    public static final String FUNC_DELEGATEUPDATE = "delegateUpdate";

    public static final String FUNC_TRANSFER = "transfer";

    public static final Event USERREGISTERED_EVENT = new Event("UserRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event USERTRANSFERRED_EVENT = new Event("UserTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1548030495818", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1548179226677", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1543166959423", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542660356610", "0x522dbc32a688dd887bee78ef593b8ba3bdea263c");
        _addresses.put("1538948396210", "0x896d2ea49d60a7d490566c0ff7c0353cae494b80");
        _addresses.put("1547253445583", "0x3e5f9D96aeF514b8A25fc82df83E6C9316BE08b2");
        _addresses.put("1540910817715", "0x39eaaef93bb162bbffb25207ffd8366f097621ba");
        _addresses.put("1540914412800", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547950005495", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547668249195", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1542029033973", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543166567414", "0x10294d2950378c3f811c7be05da0481fc5b64387");
        _addresses.put("1337", "0x5a405272214de85bf865a1e0a3c1352e82faa861");
        _addresses.put("1544647115488", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547669508831", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1542106474778", "0xf300eb5556912ce4aeca505a7d9308fe7c6cd6bd");
        _addresses.put("1548176445685", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1537802983234", "0x641b3fa5e1dfe5dc5fad4c42f2aa1e46ffb52c57");
        _addresses.put("1547753439529", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547948957914", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547124773774", "0x22f30e99d1b2a7fa89824dcb82c675fcb74a200f");
        _addresses.put("1543176208799", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547907420602", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547650799067", "0x213A9432a55a6Fe0129F238bEd7119A7A2b75b94");
        _addresses.put("1543176413402", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1548030691435", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547754447366", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547665181270", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1543180431517", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1544650273884", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1541606582953", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543183353235", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543176596896", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1544646383682", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542554349057", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1541421861694", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542788782705", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547671518090", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547671253677", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1541429234327", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547665092551", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547761267341", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1543173884204", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1543175223552", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1538963171946", "0x5fe12658b58b3a4af2ec8c4a3a2a51805b59056b");
        _addresses.put("1547668297815", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547671600204", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547136692373", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547668545717", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1543180833463", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547650997683", "0x213A9432a55a6Fe0129F238bEd7119A7A2b75b94");
        _addresses.put("1543175929774", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547753664593", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547663533027", "0x213A9432a55a6Fe0129F238bEd7119A7A2b75b94");
        _addresses.put("1543173626032", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547671405647", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1542106226176", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547669054084", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1544646252041", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542729634872", "0x5f00ea380b2027db95ef20f0d0d72ab778daa22f");
        _addresses.put("1547671862981", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1548071284377", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547134394368", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547669426929", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547649230996", "0x213A9432a55a6Fe0129F238bEd7119A7A2b75b94");
        _addresses.put("1547669623311", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("456719", "0x7117983d3be99e1cbe296dfeaf034c91db3cd02b");
        _addresses.put("1547137763380", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547747212786", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1538343946582", "0xc06b03b0871c21550c1222306304c7d2307d9316");
        _addresses.put("1543182664634", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1537881365143", "0x777f6c40d84df9d9e43efd6590c482576d2daaf9");
        _addresses.put("1541353003952", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1548178066194", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1542059718816", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1542059511560", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1541006760161", "0x9adc3881b3858377a82f39a564536fd9a4a2a929");
        _addresses.put("1547949424396", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1543178229634", "0x213a9432a55a6fe0129f238bed7119a7a2b75b94");
        _addresses.put("1547754544246", "0x0e8acA05A8B35516504690Fa97fAEa69bbAFf901");
        _addresses.put("1547563145556", "0x9333DCEE9B37D1DCa270BbC939C07832e98a6Cdb");
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

    public RemoteCall<Tuple4<byte[], byte[], byte[], String>> users(byte[] param0) {
        final Function function = new Function(FUNC_USERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Address>() {}));
        return new RemoteCall<Tuple4<byte[], byte[], byte[], String>>(
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
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UserRegisteredEventResponse>() {
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

    public RemoteCall<Boolean> isOwner(String claimedOwner, byte[] userName) {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(claimedOwner), 
                new org.web3j.abi.datatypes.generated.Bytes32(userName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> register(byte[] name, byte[] agentId, byte[] publicKey) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> update(byte[] name, byte[] agentId, byte[] publicKey) {
        final Function function = new Function(
                FUNC_UPDATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> delegatedRegister(byte[] name, byte[] agentId, byte[] publicKey, String consentee, byte[] consentSignature) {
        final Function function = new Function(
                FUNC_DELEGATEDREGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> delegateUpdate(byte[] name, byte[] agentId, byte[] publicKey, String consentee, byte[] consentSignature) {
        final Function function = new Function(
                FUNC_DELEGATEUPDATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.DynamicBytes(agentId), 
                new org.web3j.abi.datatypes.DynamicBytes(publicKey), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
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

        public BigInteger timestamp;
    }

    public static class UserTransferredEventResponse {
        public Log log;

        public byte[] name;
    }
}
