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

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.0.1.
 */
public class CommunityTagIndex extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b5061080f806100206000396000f3fe608060405260043610610062576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680633a4e04e51461006757806357760f311461012257806389b6f027146101d6578063ec9c5f8b14610229575b600080fd5b34801561007357600080fd5b506100a06004803603602081101561008a57600080fd5b81019080803590602001909291905050506102fb565b6040518083815260200180602001828103825283818151815260200191508051906020019080838360005b838110156100e65780820151818401526020810190506100cb565b50505050905090810190601f1680156101135780820380516001836020036101000a031916815260200191505b50935050505060405180910390f35b34801561012e57600080fd5b5061015b6004803603602081101561014557600080fd5b81019080803590602001909291905050506103b7565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561019b578082015181840152602081019050610180565b50505050905090810190601f1680156101c85780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b3480156101e257600080fd5b5061020f600480360360208110156101f957600080fd5b810190808035906020019092919050505061046e565b604051808215151515815260200191505060405180910390f35b34801561023557600080fd5b506102f96004803603604081101561024c57600080fd5b81019080803590602001909291908035906020019064010000000081111561027357600080fd5b82018360208201111561028557600080fd5b803590602001918460018302840111640100000000831117156102a757600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610498565b005b6000602052806000526040600020600091509050806000015490806001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156103ad5780601f10610382576101008083540402835291602001916103ad565b820191906000526020600020905b81548152906001019060200180831161039057829003601f168201915b5050505050905082565b60606000808381526020019081526020016000206001018054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156104625780601f1061043757610100808354040283529160200191610462565b820191906000526020600020905b81548152906001019060200180831161044557829003601f168201915b50505050509050919050565b60008060008084815260200190815260200160002090506000600102816000015414915050919050565b60006001028214151515610514576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601a8152602001807f546167206e616d65206d757374206265206e6f6e2d7a65726f2e00000000000081525060200191505060405180910390fd5b6040518060000190506040518091039020816040516020018082805190602001908083835b60208310151561055e5780518252602082019150602081019050602083039250610539565b6001836020036101000a038019825116818451168082178552505050505050905001915050604051602081830303815290604052805190602001201415151561060f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040180806020018281038252601d8152602001807f4465736372697074696f6e206d757374206265206e6f6e2d656d70747900000081525060200191505060405180910390fd5b6106188261046e565b15156106b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f412074616720776974682074686973206e616d6520616c72656164792065786981526020017f7374732e0000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b6040805190810160405280838152602001828152506000808481526020019081526020016000206000820151816000015560208201518160010190805190602001906106ff92919061073e565b509050507fea4be58b856928e4f8780ed245d8e090228cb3599447d8e96f6c765214cb6215826040518082815260200191505060405180910390a15050565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061077f57805160ff19168380011785556107ad565b828001600101855582156107ad579182015b828111156107ac578251825591602001919060010190610791565b5b5090506107ba91906107be565b5090565b6107e091905b808211156107dc5760008160009055506001016107c4565b5090565b9056fea165627a7a72305820e737dae575b43f62918914e9a86a76f761c91530a9a3326a5271387a5ed6b9ae0029";

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
        _addresses.put("1543166959423", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542660356610", "0xcbc2808bcdb3db67dbfb93b5e385ce9a429604b2");
        _addresses.put("1538948396210", "0xc0b52c5d5d0bc6611c54d186fc1e7f16c37bf84b");
        _addresses.put("1547253445583", "0x213A9432a55a6Fe0129F238bEd7119A7A2b75b94");
        _addresses.put("1540910817715", "0x578fb8da8cb7cd8aabdce388035caa828e09408a");
        _addresses.put("1540914412800", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542029033973", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543166567414", "0x2571f11ce15d40d5b23c5d8ea2f5470aa6f98c75");
        _addresses.put("1337", "0x2304f9d9912f7465922667e0e992486c0da76298");
        _addresses.put("1544647115488", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542106474778", "0xfa2c31156cfa5c7267e70a4c84452e85a667e01d");
        _addresses.put("1537802983234", "0xeac2f5d02687e27268d8b8f0b55446a16e311135");
        _addresses.put("1547124773774", "0x9c13028f57ff68e54c02ba1977118510fb8d46fd");
        _addresses.put("1543176208799", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1547650799067", "0x3e5f9D96aeF514b8A25fc82df83E6C9316BE08b2");
        _addresses.put("1543176413402", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543180431517", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1544650273884", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1541606582953", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543183353235", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543176596896", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1544646383682", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542554349057", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1541421861694", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542788782705", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1541429234327", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543173884204", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543175223552", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1538963171946", "0x1ead3da213d39d614b843dab4c29a726b1294b8b");
        _addresses.put("1547136692373", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543180833463", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1547650997683", "0x3e5f9D96aeF514b8A25fc82df83E6C9316BE08b2");
        _addresses.put("1543175929774", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1543173626032", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542106226176", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1544646252041", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542729634872", "0xd58b28e3cf6a1e2ee59b45761e7632decdbba85d");
        _addresses.put("1547134394368", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1547649230996", "0x3e5f9D96aeF514b8A25fc82df83E6C9316BE08b2");
        _addresses.put("456719", "0x8d8d5ebf3f1a60c476d265fc558393ab2cb36e87");
        _addresses.put("1547137763380", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1538343946582", "0x0019b802d23775b233dd14af1b9f905eacd9c903");
        _addresses.put("1543182664634", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1537881365143", "0xe0257295d2745c42d983d4a3b575178ddf6d10ff");
        _addresses.put("1541353003952", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542059718816", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1542059511560", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1541006760161", "0xb9d6d78c8080b46384c77935f0087aed324e06b8");
        _addresses.put("1543178229634", "0x48c7234741fa9910f9228bdc247a92852d531bcd");
        _addresses.put("1547563145556", "0x1431d5e400d96b12970a2CD49a3f597DED1d8BCA");
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

    public Flowable<CommunityTagCreatedEventResponse> communityTagCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, CommunityTagCreatedEventResponse>() {
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

    public static class CommunityTagCreatedEventResponse {
        public Log log;

        public byte[] name;
    }
}
