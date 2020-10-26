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
import org.web3j.abi.FunctionEncoder;
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
 * <p>Generated with web3j version 4.6.1.
 */
@SuppressWarnings("rawtypes")
public class ServiceRegistry extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b5060405161269f38038061269f8339818101604052602081101561003357600080fd5b8101908080519060200190929190505050806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505061260b806100946000396000f3fe608060405234801561001057600080fd5b50600436106100b45760003560e01c80635c7460d6116100715780635c7460d6146106e6578063656afdee14610730578063bddbd3a7146107f5578063df975418146109fc578063e193070014610aa3578063ea23ea2b14610c1d576100b4565b806309cf8201146100b95780630a790eb7146102c05780630b0e7f151461036e5780630cbc2a151461043d57806317c523d4146105b957806335341fb41461068c575b600080fd5b6102be600480360360c08110156100cf57600080fd5b81019080803590602001906401000000008111156100ec57600080fd5b8201836020820111156100fe57600080fd5b8035906020019184600183028401116401000000008311171561012057600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561018357600080fd5b82018360208201111561019557600080fd5b803590602001918460018302840111640100000000831117156101b757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019092919080359060200190929190803590602001909291908035906020019064010000000081111561023857600080fd5b82018360208201111561024a57600080fd5b8035906020019184600183028401116401000000008311171561026c57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610e4f565b005b6102ec600480360360208110156102d657600080fd5b8101908080359060200190929190505050611028565b6040518080602001838152602001828103825284818151815260200191508051906020019080838360005b83811015610332578082015181840152602081019050610317565b50505050905090810190601f16801561035f5780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b6104276004803603602081101561038457600080fd5b81019080803590602001906401000000008111156103a157600080fd5b8201836020820111156103b357600080fd5b803590602001918460018302840111640100000000831117156103d557600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506110e4565b6040518082815260200191505060405180910390f35b6105b76004803603608081101561045357600080fd5b810190808035906020019064010000000081111561047057600080fd5b82018360208201111561048257600080fd5b803590602001918460018302840111640100000000831117156104a457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019064010000000081111561053157600080fd5b82018360208201111561054357600080fd5b8035906020019184600183028401116401000000008311171561056557600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061115f565b005b610672600480360360208110156105cf57600080fd5b81019080803590602001906401000000008111156105ec57600080fd5b8201836020820111156105fe57600080fd5b8035906020019184600183028401116401000000008311171561062057600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061154c565b604051808215151515815260200191505060405180910390f35b6106c2600480360360408110156106a257600080fd5b810190808035906020019092919080359060200190929190505050611579565b60405180848152602001838152602001828152602001935050505060405180910390f35b6106ee6115bd565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6107f36004803603604081101561074657600080fd5b810190808035906020019064010000000081111561076357600080fd5b82018360208201111561077557600080fd5b8035906020019184600183028401116401000000008311171561079757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001909291905050506115e2565b005b6109fa600480360360c081101561080b57600080fd5b810190808035906020019064010000000081111561082857600080fd5b82018360208201111561083a57600080fd5b8035906020019184600183028401116401000000008311171561085c57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156108bf57600080fd5b8201836020820111156108d157600080fd5b803590602001918460018302840111640100000000831117156108f357600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019092919080359060200190929190803590602001909291908035906020019064010000000081111561097457600080fd5b82018360208201111561098657600080fd5b803590602001918460018302840111640100000000831117156109a857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050611726565b005b610a2860048036036020811015610a1257600080fd5b81019080803590602001909291905050506118ff565b6040518080602001828103825283818151815260200191508051906020019080838360005b83811015610a68578082015181840152602081019050610a4d565b50505050905090810190601f168015610a955780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b610c1b600480360360c0811015610ab957600080fd5b8101908080359060200190640100000000811115610ad657600080fd5b820183602082011115610ae857600080fd5b80359060200191846001830284011164010000000083111715610b0a57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019092919080359060200190929190803590602001909291908035906020019092919080359060200190640100000000811115610b9557600080fd5b820183602082011115610ba757600080fd5b80359060200191846001830284011164010000000083111715610bc957600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506119b7565b005b610e4d6004803603610100811015610c3457600080fd5b8101908080359060200190640100000000811115610c5157600080fd5b820183602082011115610c6357600080fd5b80359060200191846001830284011164010000000083111715610c8557600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019092919080359060200190929190803590602001909291908035906020019092919080359060200190640100000000811115610d1057600080fd5b820183602082011115610d2257600080fd5b80359060200191846001830284011164010000000083111715610d4457600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190640100000000811115610dc757600080fd5b820183602082011115610dd957600080fd5b80359060200191846001830284011164010000000083111715610dfb57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050611b03565b005b856000801b60016000610e61846110e4565b8152602001908152602001600020600101541415610ee7576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206d75737420626520726567697374657265642e000000000081525060200191505060405180910390fd5b6000610ef2886110e4565b9050807feab4e2e4c52c208cb1ee041fd1151e46f2c1847fce1f4ef741bf3bfe77536e29888888888842604051808060200187815260200186815260200185815260200180602001848152602001838103835289818151815260200191508051906020019080838360005b83811015610f78578082015181840152602081019050610f5d565b50505050905090810190601f168015610fa55780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b83811015610fde578082015181840152602081019050610fc3565b50505050905090810190601f16801561100b5780820380516001836020036101000a031916815260200191505b509850505050505050505060405180910390a25050505050505050565b6001602052806000526040600020600091509050806000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156110d45780601f106110a9576101008083540402835291602001916110d4565b820191906000526020600020905b8154815290600101906020018083116110b757829003601f168201915b5050505050908060010154905082565b6000816040516020018082805190602001908083835b6020831061111d57805182526020820191506020810190506020830392506110fa565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051602081830303815290604052805190602001209050919050565b60606040518060400160405280600481526020017f656afdee000000000000000000000000000000000000000000000000000000008152509050606085856040516020018080602001838152602001828103825284818151815260200191508051906020019080838360005b838110156111e65780820151818401526020810190506111cb565b50505050905090810190601f1680156112135780820380516001836020036101000a031916815260200191505b509350505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff1660e01b81526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b838110156112d15780820151818401526020810190506112b6565b50505050905090810190601f1680156112fe5780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b8381101561133757808201518184015260208101905061131c565b50505050905090810190601f1680156113645780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b8381101561139d578082015181840152602081019050611382565b50505050905090810190601f1680156113ca5780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b1580156113ec57600080fd5b505af4158015611400573d6000803e3d6000fd5b505050506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663e96b462a85876040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019250505060206040518083038186803b1580156114aa57600080fd5b505afa1580156114be573d6000803e3d6000fd5b505050506040513d60208110156114d457600080fd5b810190808051906020019092919050505061153a576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260308152602001806125816030913960400191505060405180910390fd5b6115448686611f7d565b505050505050565b60008060001b6001600061155f856110e4565b815260200190815260200160002060010154149050919050565b6002602052816000526040600020818154811061159257fe5b9060005260206000209060030201600091509150508060000154908060010154908060020154905083565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663e96b462a33836040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019250505060206040518083038186803b15801561168857600080fd5b505afa15801561169c573d6000803e3d6000fd5b505050506040513d60208110156116b257600080fd5b8101908080519060200190929190505050611718576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260308152602001806124e06030913960400191505060405180910390fd5b6117228282611f7d565b5050565b856000801b60016000611738846110e4565b81526020019081526020016000206001015414156117be576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206d75737420626520726567697374657265642e000000000081525060200191505060405180910390fd5b60006117c9886110e4565b9050807f488efddc026b80be7290e78ebae8f6804d46e82cf6cbb8fce219d6f1239ed9e3888888888842604051808060200187815260200186815260200185815260200180602001848152602001838103835289818151815260200191508051906020019080838360005b8381101561184f578082015181840152602081019050611834565b50505050905090810190601f16801561187c5780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b838110156118b557808201518184015260208101905061189a565b50505050905090810190601f1680156118e25780820380516001836020036101000a031916815260200191505b509850505050505050505060405180910390a25050505050505050565b6060600160008381526020019081526020016000206000018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156119ab5780601f10611980576101008083540402835291602001916119ab565b820191906000526020600020905b81548152906001019060200180831161198e57829003601f168201915b50505050509050919050565b6000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663e96b462a33876040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019250505060206040518083038186803b158015611a5d57600080fd5b505afa158015611a71573d6000803e3d6000fd5b505050506040513d6020811015611a8757600080fd5b8101908080519060200190929190505050611aed576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260278152602001806125376027913960400191505060405180910390fd5b611afb86868686868661218a565b505050505050565b60606040518060400160405280600481526020017fe19307000000000000000000000000000000000000000000000000000000000081525090506060898989898989604051602001808060200187815260200186815260200185815260200184815260200180602001838103835289818151815260200191508051906020019080838360005b83811015611ba4578082015181840152602081019050611b89565b50505050905090810190601f168015611bd15780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b83811015611c0a578082015181840152602081019050611bef565b50505050905090810190601f168015611c375780820380516001836020036101000a031916815260200191505b5098505050505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff1660e01b81526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b83811015611cfa578082015181840152602081019050611cdf565b50505050905090810190601f168015611d275780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b83811015611d60578082015181840152602081019050611d45565b50505050905090810190601f168015611d8d5780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015611dc6578082015181840152602081019050611dab565b50505050905090810190601f168015611df35780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b158015611e1557600080fd5b505af4158015611e29573d6000803e3d6000fd5b505050506000809054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1663e96b462a858b6040518363ffffffff1660e01b8152600401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020018281526020019250505060206040518083038186803b158015611ed357600080fd5b505afa158015611ee7573d6000803e3d6000fd5b505050506040513d6020811015611efd57600080fd5b8101908080519060200190929190505050611f63576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260278152602001806125106027913960400191505060405180910390fd5b611f718a8a8a8a8a8a61218a565b50505050505050505050565b81611f96604051806020016040528060008152506110e4565b611f9f826110e4565b1415612013576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260188152602001807f537472696e67206d757374206265206e6f6e2d7a65726f2e000000000000000081525060200191505060405180910390fd5b816000801b811415612070576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001806125b16026913960400191505060405180910390fd5b6120798461154c565b6120eb576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601b8152602001807f53657276696365206e616d6520616c72656164792074616b656e2e000000000081525060200191505060405180910390fd5b60006120f6856110e4565b905060405180604001604052808681526020018581525060016000838152602001908152602001600020600082015181600001908051906020019061213c92919061243a565b506020820151816001015590505083817fdbcd7b865e4bbd2885b269251a7a2ac22db95d1479bdba1ebffdd9b0f14c1f15426040518082815260200191505060405180910390a35050505050565b856121a3604051806020016040528060008152506110e4565b6121ac826110e4565b1415612220576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260188152602001807f537472696e67206d757374206265206e6f6e2d7a65726f2e000000000000000081525060200191505060405180910390fd5b856000801b81141561227d576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001806125b16026913960400191505060405180910390fd5b6000612288896110e4565b9050876001600083815260200190815260200160002060010154146122f8576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602381526020018061255e6023913960400191505060405180910390fd5b6002600082815260200190815260200160002060405180606001604052808981526020018881526020018781525090806001815401808255809150509060018203906000526020600020906003020160009091929091909150600082015181600001556020820151816001015560408201518160020155505050807ffe3ffc15bade6f3ec6539c01fd0de509fd35fea0b61cee9c50a2988abb22c76f88888888426040518086815260200185815260200184815260200180602001838152602001828103825284818151815260200191508051906020019080838360005b838110156123f15780820151818401526020810190506123d6565b50505050905090810190601f16801561241e5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390a2505050505050505050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061247b57805160ff19168380011785556124a9565b828001600101855582156124a9579182015b828111156124a857825182559160200191906001019061248d565b5b5090506124b691906124ba565b5090565b6124dc91905b808211156124d85760008160009055506001016124c0565b5090565b9056fe53656e646572206d757374206f776e20617574686f72206e616d6520746f20726567697374657220736572766963652e5369676e6572206d757374206f776e20617574686f72206e616d6520746f2072656c656173652e53656e646572206d757374206f776e20617574686f72206e616d6520746f2072656c656173652e50617373656420617574686f7220646f6573206e6f74206f776e20736572766963652e5369676e6572206d757374206f776e20617574686f72206e616d6520746f20726567697374657220736572766963652e576861746576657220746869732069732c206974206d757374206265206e6f6e2d7a65726f2ea265627a7a723158207f060e0b0be0c8dbe2eac4e213a4ca977856476ac53b8b19038c5a398e56b68464736f6c63430005100032";

    public static final String FUNC_SERVICEVERSIONS = "serviceVersions";

    public static final String FUNC_SERVICES = "services";

    public static final String FUNC_USERREGISTRY = "userRegistry";

    public static final String FUNC_STRINGHASH = "stringHash";

    public static final String FUNC_NAMEISAVAILABLE = "nameIsAvailable";

    public static final String FUNC_HASHTONAME = "hashToName";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DELEGATEDREGISTER = "delegatedRegister";

    public static final String FUNC_RELEASE = "release";

    public static final String FUNC_DELEGATEDRELEASE = "delegatedRelease";

    public static final String FUNC_ANNOUNCEDEPLOYMENT = "announceDeployment";

    public static final String FUNC_ANNOUNCEDEPLOYMENTEND = "announceDeploymentEnd";

    public static final Event SERVICECREATED_EVENT = new Event("ServiceCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SERVICEDEPLOYMENT_EVENT = new Event("ServiceDeployment", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SERVICEDEPLOYMENTEND_EVENT = new Event("ServiceDeploymentEnd", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SERVICERELEASED_EVENT = new Event("ServiceReleased", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<Uint256>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("5777", "0x65756290e77c9aFe69d4A788E94a85918e3ae08a");
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

    public List<ServiceCreatedEventResponse> getServiceCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICECREATED_EVENT, transactionReceipt);
        ArrayList<ServiceCreatedEventResponse> responses = new ArrayList<ServiceCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceCreatedEventResponse> serviceCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ServiceCreatedEventResponse>() {
            @Override
            public ServiceCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICECREATED_EVENT, log);
                ServiceCreatedEventResponse typedResponse = new ServiceCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.author = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceCreatedEventResponse> serviceCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICECREATED_EVENT));
        return serviceCreatedEventFlowable(filter);
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
            typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceDeploymentEventResponse> serviceDeploymentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ServiceDeploymentEventResponse>() {
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
                typedResponse.nodeId = (String) eventValues.getNonIndexedValues().get(4).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
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
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceDeploymentEndEventResponse> serviceDeploymentEndEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ServiceDeploymentEndEventResponse>() {
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
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceDeploymentEndEventResponse> serviceDeploymentEndEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICEDEPLOYMENTEND_EVENT));
        return serviceDeploymentEndEventFlowable(filter);
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
            typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceReleasedEventResponse> serviceReleasedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ServiceReleasedEventResponse>() {
            @Override
            public ServiceReleasedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICERELEASED_EVENT, log);
                ServiceReleasedEventResponse typedResponse = new ServiceReleasedEventResponse();
                typedResponse.log = log;
                typedResponse.nameHash = (byte[]) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.versionMajor = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.versionMinor = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.versionPatch = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.hash = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceReleasedEventResponse> serviceReleasedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICERELEASED_EVENT));
        return serviceReleasedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>> serviceVersions(byte[] param0, BigInteger param1) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SERVICEVERSIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>>(function,
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

    public RemoteFunctionCall<Tuple2<String, byte[]>> services(byte[] param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SERVICES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}));
        return new RemoteFunctionCall<Tuple2<String, byte[]>>(function,
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

    public RemoteFunctionCall<String> userRegistry() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_USERREGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> stringHash(String name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_STRINGHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<Boolean> nameIsAvailable(String serviceName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_NAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<String> hashToName(byte[] hashOfName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_HASHTONAME, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(hashOfName)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> register(String serviceName, byte[] authorName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> delegatedRegister(String serviceName, byte[] authorName, String consentee, byte[] consentSignature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELEGATEDREGISTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> release(String serviceName, byte[] authorName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] hash) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RELEASE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch), 
                new org.web3j.abi.datatypes.DynamicBytes(hash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> delegatedRelease(String serviceName, byte[] authorName, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, byte[] hash, String consentee, byte[] consentSignature) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DELEGATEDRELEASE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.generated.Bytes32(authorName), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch), 
                new org.web3j.abi.datatypes.DynamicBytes(hash), 
                new org.web3j.abi.datatypes.Address(consentee), 
                new org.web3j.abi.datatypes.DynamicBytes(consentSignature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> announceDeployment(String serviceName, String className, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, String nodeId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ANNOUNCEDEPLOYMENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(serviceName), 
                new org.web3j.abi.datatypes.Utf8String(className), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMajor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionMinor), 
                new org.web3j.abi.datatypes.generated.Uint256(versionPatch), 
                new org.web3j.abi.datatypes.Utf8String(nodeId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> announceDeploymentEnd(String serviceName, String className, BigInteger versionMajor, BigInteger versionMinor, BigInteger versionPatch, String nodeId) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
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

    public static class ServiceCreatedEventResponse extends BaseEventResponse {
        public byte[] nameHash;

        public byte[] author;

        public BigInteger timestamp;
    }

    public static class ServiceDeploymentEventResponse extends BaseEventResponse {
        public byte[] nameHash;

        public String className;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public String nodeId;

        public BigInteger timestamp;
    }

    public static class ServiceDeploymentEndEventResponse extends BaseEventResponse {
        public byte[] nameHash;

        public String className;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public String nodeId;

        public BigInteger timestamp;
    }

    public static class ServiceReleasedEventResponse extends BaseEventResponse {
        public byte[] nameHash;

        public BigInteger versionMajor;

        public BigInteger versionMinor;

        public BigInteger versionPatch;

        public byte[] hash;

        public BigInteger timestamp;
    }
}
