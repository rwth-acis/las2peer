package i5.las2peer.registryGateway.contracts;

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
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
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
public class CommunityTagIndex extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b50610833806100206000396000f300608060405260043610610062576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680633a4e04e51461006757806357760f311461012057806389b6f027146101ca578063ec9c5f8b14610213575b600080fd5b34801561007357600080fd5b50610096600480360381019080803560001916906020019092919050505061028a565b60405180836000191660001916815260200180602001828103825283818151815260200191508051906020019080838360005b838110156100e45780820151818401526020810190506100c9565b50505050905090810190601f1680156101115780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b34801561012c57600080fd5b5061014f6004803603810190808035600019169060200190929190505050610346565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561018f578082015181840152602081019050610174565b50505050905090810190601f1680156101bc5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101d657600080fd5b506101f96004803603810190808035600019169060200190929190505050610405565b604051808215151515815260200191505060405180910390f35b34801561021f57600080fd5b506102886004803603810190808035600019169060200190929190803590602001908201803590602001908080601f016020809104026020016040519081016040528093929190818152602001838380828437820191505050505050919291929050505061043b565b005b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f01602080910402602001604051908101604052809291908181526020018280546001816001161561010002031660029004801561033c5780601f106103115761010080835404028352916020019161033c565b820191906000526020600020905b81548152906001019060200180831161031f57829003601f168201915b5050505050905082565b606060008083600019166000191681526020019081526020016000206001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103f95780601f106103ce576101008083540402835291602001916103f9565b820191906000526020600020905b8154815290600101906020018083116103dc57829003601f168201915b50505050509050919050565b60008060008084600019166000191681526020019081526020016000209050600060010281600001546000191614915050919050565b60006001028260001916141515156104bb576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f546167206e616d65206d757374206265206e6f6e2d7a65726f2e00000000000081525060200191505060405180910390fd5b604051806000019050604051809103902060001916816040516020018082805190602001908083835b60208310151561050957805182526020820191506020810190506020830392506104e4565b6001836020036101000a0380198251168184511680821785525050505050509050019150506040516020818303038152906040526040518082805190602001908083835b602083101515610572578051825260208201915060208101905060208303925061054d565b6001836020036101000a03801982511681845116808217855250505050505090500191505060405180910390206000191614151515610619576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601d8152602001807f4465736372697074696f6e206d757374206265206e6f6e2d656d70747900000081525060200191505060405180910390fd5b61062282610405565b15156106bc576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f412074616720776974682074686973206e616d6520616c72656164792065786981526020017f7374732e0000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b60408051908101604052808360001916815260200182815250600080846000191660001916815260200190815260200160002060008201518160000190600019169055602082015181600101908051906020019061071b929190610762565b509050507fea4be58b856928e4f8780ed245d8e090228cb3599447d8e96f6c765214cb62158260405180826000191660001916815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106107a357805160ff19168380011785556107d1565b828001600101855582156107d1579182015b828111156107d05782518255916020019190600101906107b5565b5b5090506107de91906107e2565b5090565b61080491905b808211156108005760008160009055506001016107e8565b5090565b905600a165627a7a72305820047bedd98ddf456275e84b0c66ef932ee52c74543f9cbd0acfd99cf3df205fb60029";

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
        _addresses.put("1541421861694", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542660356610", "0x81141942acf438c20bea021398396eb56ee50731");
        _addresses.put("1541429234327", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1538948396210", "0xc0b52c5d5d0bc6611c54d186fc1e7f16c37bf84b");
        _addresses.put("1540910817715", "0x578fb8da8cb7cd8aabdce388035caa828e09408a");
        _addresses.put("1540914412800", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("456719", "0x8d8d5ebf3f1a60c476d265fc558393ab2cb36e87");
        _addresses.put("1538963171946", "0x1ead3da213d39d614b843dab4c29a726b1294b8b");
        _addresses.put("1538343946582", "0x0019b802d23775b233dd14af1b9f905eacd9c903");
        _addresses.put("1542029033973", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1537881365143", "0xe0257295d2745c42d983d4a3b575178ddf6d10ff");
        _addresses.put("1337", "0xbee241a07223038399f68bd1efdda0eb647a6fe1");
        _addresses.put("1541353003952", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542059718816", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542059511560", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1541006760161", "0xb9d6d78c8080b46384c77935f0087aed324e06b8");
        _addresses.put("1541606582953", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542106474778", "0xfa2c31156cfa5c7267e70a4c84452e85a667e01d");
        _addresses.put("1542106226176", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1537802983234", "0xeac2f5d02687e27268d8b8f0b55446a16e311135");
        _addresses.put("1542554349057", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
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

    public RemoteCall<Tuple2<byte[], String>> tagIndex(byte[] param0) {
        final Function function = new Function(FUNC_TAGINDEX, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Utf8String>() {}));
        return new RemoteCall<Tuple2<byte[], String>>(
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

    public Observable<CommunityTagCreatedEventResponse> communityTagCreatedEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, CommunityTagCreatedEventResponse>() {
            @Override
            public CommunityTagCreatedEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(COMMUNITYTAGCREATED_EVENT, log);
                CommunityTagCreatedEventResponse typedResponse = new CommunityTagCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<CommunityTagCreatedEventResponse> communityTagCreatedEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMUNITYTAGCREATED_EVENT));
        return communityTagCreatedEventObservable(filter);
    }

    public RemoteCall<Boolean> isAvailable(byte[] name) {
        final Function function = new Function(FUNC_ISAVAILABLE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> create(byte[] name, String description) {
        final Function function = new Function(
                FUNC_CREATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name), 
                new org.web3j.abi.datatypes.Utf8String(description)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> viewDescription(byte[] name) {
        final Function function = new Function(FUNC_VIEWDESCRIPTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(name)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
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

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class CommunityTagCreatedEventResponse {
        public Log log;

        public byte[] name;
    }
}
