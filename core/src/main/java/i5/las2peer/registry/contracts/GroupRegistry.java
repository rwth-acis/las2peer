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
public class GroupRegistry extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b5061130e806100206000396000f3fe60806040526004361061008e576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff168063186a3d44146100935780633c0ba73f146100e657806363949ef61461013957806379ce9fac14610359578063a2c14bd1146103b4578063e96b462a14610407578063ebc1b8ff1461047a578063ef161bcb146105e3575b600080fd5b34801561009f57600080fd5b506100cc600480360360208110156100b657600080fd5b810190808035906020019092919050505061073d565b604051808215151515815260200191505060405180910390f35b3480156100f257600080fd5b5061011f6004803603602081101561010957600080fd5b81019080803590602001909291905050506107b1565b604051808215151515815260200191505060405180910390f35b34801561014557600080fd5b50610357600480360360a081101561015c57600080fd5b81019080803590602001909291908035906020019064010000000081111561018357600080fd5b82018360208201111561019557600080fd5b803590602001918460018302840111640100000000831117156101b757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561021a57600080fd5b82018360208201111561022c57600080fd5b8035906020019184600183028401116401000000008311171561024e57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803573ffffffffffffffffffffffffffffffffffffffff169060200190929190803590602001906401000000008111156102d157600080fd5b8201836020820111156102e357600080fd5b8035906020019184600183028401116401000000008311171561030557600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506107d5565b005b34801561036557600080fd5b506103b26004803603604081101561037c57600080fd5b8101908080359060200190929190803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610b4c565b005b3480156103c057600080fd5b506103ed600480360360208110156103d757600080fd5b8101908080359060200190929190505050610cd6565b604051808215151515815260200191505060405180910390f35b34801561041357600080fd5b506104606004803603604081101561042a57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610ce6565b604051808215151515815260200191505060405180910390f35b34801561048657600080fd5b506105e16004803603606081101561049d57600080fd5b8101908080359060200190929190803590602001906401000000008111156104c457600080fd5b8201836020820111156104d657600080fd5b803590602001918460018302840111640100000000831117156104f857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561055b57600080fd5b82018360208201111561056d57600080fd5b8035906020019184600183028401116401000000008311171561058f57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610d54565b005b3480156105ef57600080fd5b5061061c6004803603602081101561060657600080fd5b8101908080359060200190929190505050610d99565b6040518085815260200180602001806020018473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001838103835286818151815260200191508051906020019080838360005b8381101561069857808201518184015260208101905061067d565b50505050905090810190601f1680156106c55780820380516001836020036101000a031916815260200191505b50838103825285818151815260200191508051906020019080838360005b838110156106fe5780820151818401526020810190506106e3565b50505050905090810190601f16801561072b5780820380516001836020036101000a031916815260200191505b50965050505050505060405180910390f35b6000806000808481526020019081526020016000209050600073ffffffffffffffffffffffffffffffffffffffff168160030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161415915050919050565b60006107bc82610cd6565b80156107ce57506107cc8261073d565b155b9050919050565b60606040805190810160405280600481526020017febc1b8ff0000000000000000000000000000000000000000000000000000000081525090506060868686604051602001808481526020018060200180602001838103835285818151815260200191508051906020019080838360005b83811015610861578082015181840152602081019050610846565b50505050905090810190601f16801561088e5780820380516001836020036101000a031916815260200191505b50838103825284818151815260200191508051906020019080838360005b838110156108c75780820151818401526020810190506108ac565b50505050905090810190601f1680156108f45780820380516001836020036101000a031916815260200191505b5095505050505050604051602081830303815290604052905073__Delegation____________________________63a491459d838387876040518563ffffffff167c01000000000000000000000000000000000000000000000000000000000281526004018080602001806020018573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001848103845288818151815260200191508051906020019080838360005b838110156109d05780820151818401526020810190506109b5565b50505050905090810190601f1680156109fd5780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b83811015610a36578082015181840152602081019050610a1b565b50505050905090810190601f168015610a635780820380516001836020036101000a031916815260200191505b50848103825285818151815260200191508051906020019080838360005b83811015610a9c578082015181840152602081019050610a81565b50505050905090810190601f168015610ac95780820380516001836020036101000a031916815260200191505b5097505050505050505060006040518083038186803b158015610aeb57600080fd5b505af4158015610aff573d6000803e3d6000fd5b50505050610b436080604051908101604052808981526020018881526020018781526020018673ffffffffffffffffffffffffffffffffffffffff16815250610f19565b50505050505050565b8160006001028114151515610bef576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260228152602001807f456d707479206e616d65206973206e6f74206f776e656420627920616e796f6e81526020017f652e00000000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b3373ffffffffffffffffffffffffffffffffffffffff1660008083815260200190815260200160002060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16141515610cc7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260198152602001807f53656e64657220646f6573206e6f74206f776e206e616d652e0000000000000081525060200191505060405180910390fd5b610cd183836111ae565b505050565b6000806001028214159050919050565b60008273ffffffffffffffffffffffffffffffffffffffff1660008084815260200190815260200160002060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1614905092915050565b610d946080604051908101604052808581526020018481526020018381526020013373ffffffffffffffffffffffffffffffffffffffff16815250610f19565b505050565b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610e4b5780601f10610e2057610100808354040283529160200191610e4b565b820191906000526020600020905b815481529060010190602001808311610e2e57829003601f168201915b505050505090806002018054600181600116156101000203166002900480601f016020809104026020016040519081016040528092919081815260200182805460018160011615610100020316600290048015610ee95780601f10610ebe57610100808354040283529160200191610ee9565b820191906000526020600020905b815481529060010190602001808311610ecc57829003601f168201915b5050505050908060030160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905084565b6000600102816000015114151515610f99576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260168152602001807f4e616d65206d757374206265206e6f6e2d7a65726f2e0000000000000000000081525060200191505060405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff16816060015173ffffffffffffffffffffffffffffffffffffffff1614151515611042576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601f8152602001807f4f776e65722061646472657373206d757374206265206e6f6e2d7a65726f2e0081525060200191505060405180910390fd5b61104f81600001516107b1565b15156110c3576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601e8152602001807f4e616d6520616c72656164792074616b656e206f7220696e76616c69642e000081525060200191505060405180910390fd5b806000808360000151815260200190815260200160002060008201518160000155602082015181600101908051906020019061110092919061123d565b50604082015181600201908051906020019061111d92919061123d565b5060608201518160030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055509050507f92830d9cf3b085d86a265821675c547212fe306b621bacbd770731e10db9420c816000015142604051808381526020018281526020019250505060405180910390a150565b8060008084815260200190815260200160002060030160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055507f89f2bf5d023341ba657f8c6b42dc576078396818a58ca87b7ea51e4b96881d75826040518082815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061127e57805160ff19168380011785556112ac565b828001600101855582156112ac579182015b828111156112ab578251825591602001919060010190611290565b5b5090506112b991906112bd565b5090565b6112df91905b808211156112db5760008160009055506001016112c3565b5090565b9056fea165627a7a72305820c83c9596c3a97eb10dc522315db8058599fd81ba2ba7691c0e080443038c388c0029";

    public static final String FUNC_GROUPS = "groups";

    public static final String FUNC_GROUPNAMEISVALID = "groupNameIsValid";

    public static final String FUNC_NAMEISTAKEN = "nameIsTaken";

    public static final String FUNC_GROUPNAMEISAVAILABLE = "groupNameIsAvailable";

    public static final String FUNC_ISOWNER = "isOwner";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_DELEGATEDREGISTER = "delegatedRegister";

    public static final String FUNC_TRANSFER = "transfer";

    public static final Event GROUPREGISTERED_EVENT = new Event("GroupRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event GROUPTRANSFERRED_EVENT = new Event("GroupTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
    }

    @Deprecated
    protected GroupRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected GroupRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected GroupRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected GroupRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<Tuple4<byte[], byte[], byte[], String>> groups(byte[] param0) {
        final Function function = new Function(FUNC_GROUPS, 
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

    public List<GroupRegisteredEventResponse> getGroupRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(GROUPREGISTERED_EVENT, transactionReceipt);
        ArrayList<GroupRegisteredEventResponse> responses = new ArrayList<GroupRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GroupRegisteredEventResponse typedResponse = new GroupRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<GroupRegisteredEventResponse> groupRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, GroupRegisteredEventResponse>() {
            @Override
            public GroupRegisteredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(GROUPREGISTERED_EVENT, log);
                GroupRegisteredEventResponse typedResponse = new GroupRegisteredEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<GroupRegisteredEventResponse> groupRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GROUPREGISTERED_EVENT));
        return groupRegisteredEventFlowable(filter);
    }

    public List<GroupTransferredEventResponse> getGroupTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(GROUPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<GroupTransferredEventResponse> responses = new ArrayList<GroupTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GroupTransferredEventResponse typedResponse = new GroupTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<GroupTransferredEventResponse> groupTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, GroupTransferredEventResponse>() {
            @Override
            public GroupTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(GROUPTRANSFERRED_EVENT, log);
                GroupTransferredEventResponse typedResponse = new GroupTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<GroupTransferredEventResponse> groupTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GROUPTRANSFERRED_EVENT));
        return groupTransferredEventFlowable(filter);
    }

    public RemoteCall<Boolean> groupNameIsValid(byte[] name) {
        final Function function = new Function(FUNC_GROUPNAMEISVALID, 
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

    public RemoteCall<Boolean> groupNameIsAvailable(byte[] name) {
        final Function function = new Function(FUNC_GROUPNAMEISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<Boolean> isOwner(String claimedOwner, byte[] groupName) {
        final Function function = new Function(FUNC_ISOWNER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(claimedOwner), 
                new org.web3j.abi.datatypes.generated.Bytes32(groupName)), 
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

    public RemoteCall<TransactionReceipt> transfer(byte[] name, String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static GroupRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new GroupRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static GroupRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new GroupRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static GroupRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new GroupRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static GroupRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new GroupRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<GroupRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(GroupRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<GroupRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(GroupRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<GroupRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(GroupRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<GroupRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(GroupRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class GroupRegisteredEventResponse {
        public Log log;

        public byte[] name;

        public BigInteger timestamp;
    }

    public static class GroupTransferredEventResponse {
        public Log log;

        public byte[] name;
    }
}
