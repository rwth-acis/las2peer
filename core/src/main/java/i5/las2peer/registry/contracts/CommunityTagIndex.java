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
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
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
public class CommunityTagIndex extends Contract {
    public static final String BINARY = "0x608060405234801561001057600080fd5b506107a5806100206000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80633a4e04e51461005157806357760f31146100ff57806389b6f027146101a6578063ec9c5f8b146101ec575b600080fd5b61007d6004803603602081101561006757600080fd5b81019080803590602001909291905050506102b1565b6040518083815260200180602001828103825283818151815260200191508051906020019080838360005b838110156100c35780820151818401526020810190506100a8565b50505050905090810190601f1680156100f05780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b61012b6004803603602081101561011557600080fd5b810190808035906020019092919050505061036d565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561016b578082015181840152602081019050610150565b50505050905090810190601f1680156101985780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6101d2600480360360208110156101bc57600080fd5b8101908080359060200190929190505050610424565b604051808215151515815260200191505060405180910390f35b6102af6004803603604081101561020257600080fd5b81019080803590602001909291908035906020019064010000000081111561022957600080fd5b82018360208201111561023b57600080fd5b8035906020019184600183028401116401000000008311171561025d57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f82011690508083019250505050505050919291929050505061044d565b005b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103635780601f1061033857610100808354040283529160200191610363565b820191906000526020600020905b81548152906001019060200180831161034657829003601f168201915b5050505050905082565b60606000808381526020019081526020016000206001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156104185780601f106103ed57610100808354040283529160200191610418565b820191906000526020600020905b8154815290600101906020018083116103fb57829003601f168201915b50505050509050919050565b60008060008084815260200190815260200160002090506000801b816000015414915050919050565b6000801b8214156104c6576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f546167206e616d65206d757374206265206e6f6e2d7a65726f2e00000000000081525060200191505060405180910390fd5b6040518060000190506040518091039020816040516020018082805190602001908083835b6020831061050e57805182526020820191506020810190506020830392506104eb565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040516020818303038152906040528051906020012014156105bd576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601d8152602001807f4465736372697074696f6e206d757374206265206e6f6e2d656d70747900000081525060200191505060405180910390fd5b6105c682610424565b61061b576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252602481526020018061074d6024913960400191505060405180910390fd5b6040518060400160405280838152602001828152506000808481526020019081526020016000206000820151816000015560208201518160010190805190602001906106689291906106a7565b509050507fea4be58b856928e4f8780ed245d8e090228cb3599447d8e96f6c765214cb6215826040518082815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106106e857805160ff1916838001178555610716565b82800160010185558215610716579182015b828111156107155782518255916020019190600101906106fa565b5b5090506107239190610727565b5090565b61074991905b8082111561074557600081600090555060010161072d565b5090565b9056fe412074616720776974682074686973206e616d6520616c7265616479206578697374732ea265627a7a723158200ef8c413f5e155076f4cd97bfdf2345c378130d452b64c2a81170846cb121b8964736f6c63430005100032";

    public static final String FUNC_TAGINDEX = "tagIndex";

    public static final String FUNC_ISAVAILABLE = "isAvailable";

    public static final String FUNC_CREATE = "create";

    public static final String FUNC_VIEWDESCRIPTION = "viewDescription";

    public static final Event COMMUNITYTAGCREATED_EVENT = new Event("CommunityTagCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("5777", "0xdCBA759ED3aF3804e460F3298C6aa4c7982237bf");
    }

    @Deprecated
    protected CommunityTagIndex(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CommunityTagIndex(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CommunityTagIndex(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CommunityTagIndex(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<CommunityTagCreatedEventResponse> getCommunityTagCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(COMMUNITYTAGCREATED_EVENT, transactionReceipt);
        ArrayList<CommunityTagCreatedEventResponse> responses = new ArrayList<CommunityTagCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommunityTagCreatedEventResponse typedResponse = new CommunityTagCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<CommunityTagCreatedEventResponse> communityTagCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, CommunityTagCreatedEventResponse>() {
            @Override
            public CommunityTagCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(COMMUNITYTAGCREATED_EVENT, log);
                CommunityTagCreatedEventResponse typedResponse = new CommunityTagCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<CommunityTagCreatedEventResponse> communityTagCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMUNITYTAGCREATED_EVENT));
        return communityTagCreatedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple2<byte[], String>> tagIndex(byte[] param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_TAGINDEX, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteFunctionCall<Tuple2<byte[], String>>(function,
                new Callable<Tuple2<byte[], String>>() {
                    @Override
                    public Tuple2<byte[], String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<byte[], String>(
                                (byte[]) results.get(0).getValue(), 
                                (String) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Boolean> isAvailable(byte[] name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> create(byte[] name, String description) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.Utf8String(description)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> viewDescription(byte[] name) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_VIEWDESCRIPTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static CommunityTagIndex load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CommunityTagIndex(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CommunityTagIndex load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CommunityTagIndex(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CommunityTagIndex load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CommunityTagIndex(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CommunityTagIndex load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CommunityTagIndex(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CommunityTagIndex> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CommunityTagIndex.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CommunityTagIndex> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CommunityTagIndex.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<CommunityTagIndex> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CommunityTagIndex.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CommunityTagIndex> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CommunityTagIndex.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class CommunityTagCreatedEventResponse extends BaseEventResponse {
        public byte[] name;
    }
}
