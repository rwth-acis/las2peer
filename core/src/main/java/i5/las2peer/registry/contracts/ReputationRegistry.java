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
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Int256;
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
import org.web3j.tuples.generated.Tuple6;
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
public class ReputationRegistry extends Contract {
    public static final String BINARY = "0x60806040526005600055600060015560026000540260025560026001540260035534801561002c57600080fd5b50604051611bc9380380611bc98339818101604052602081101561004f57600080fd5b810190808051906020019092919050505050611b59806100706000396000f3fe608060405234801561001057600080fd5b50600436106100b45760003560e01c80636dae0a56116100715780636dae0a561461044a5780637740f92f146104f15780637a2bba61146105ac578063a787c80b1461061a578063bbe1562714610676578063d9cf86551461071d576100b4565b806306220d88146100b95780632fe1167514610111578063447e0ac61461016957806344a9f676146101b75780635e4177f2146103d457806367c9b51a1461042c575b600080fd5b6100fb600480360360208110156100cf57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610775565b6040518082815260200191505060405180910390f35b6101676004803603606081101561012757600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff1690602001909291908035906020019092919080359060200190929190505050610842565b005b6101b56004803603604081101561017f57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610a97565b005b6103d2600480360360c08110156101cd57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190803590602001909291908035906020019064010000000081111561021e57600080fd5b82018360208201111561023057600080fd5b8035906020019184600183028401116401000000008311171561025257600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290803590602001906401000000008111156102b557600080fd5b8201836020820111156102c757600080fd5b803590602001918460018302840111640100000000831117156102e957600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192908035906020019064010000000081111561034c57600080fd5b82018360208201111561035e57600080fd5b8035906020019184600183028401116401000000008311171561038057600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610aab565b005b610416600480360360208110156103ea57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610ac2565b6040518082815260200191505060405180910390f35b610434610b8f565b6040518082815260200191505060405180910390f35b61048c6004803603602081101561046057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610b9c565b604051808773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001868152602001858152602001848152602001838152602001828152602001965050505050505060405180910390f35b6105aa6004803603602081101561050757600080fd5b810190808035906020019064010000000081111561052457600080fd5b82018360208201111561053657600080fd5b8035906020019184600183028401116401000000008311171561055857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f820116905080830192505050505050509192919290505050610de7565b005b6105d8600480360360208110156105c257600080fd5b8101908080359060200190929190505050610f25565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b61065c6004803603602081101561063057600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff169060200190929190505050610f66565b604051808215151515815260200191505060405180910390f35b6106b86004803603602081101561068c57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061102e565b604051808773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001868152602001858152602001848152602001838152602001828152602001965050505050505060405180910390f35b61075f6004803603602081101561073357600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919050505061108a565b6040518082815260200191505060405180910390f35b600061078082610f66565b6107f2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260118152602001807f70726f66696c65206e6f7420666f756e6400000000000000000000000000000081525060200191505060405180910390fd5b6000600560008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002090508060030154915050919050565b61084b33610f66565b61088e5761088d6040518060400160405280601681526020017f73656e6465722070726f66696c6520756e6b6e6f776e00000000000000000000815250610de7565b5b61089783610f66565b6108da576108d96040518060400160405280601a81526020017f636f6e74726168656e742070726f66696c6520756e6b6e6f776e000000000000815250610de7565b5b600054821380156108ec575060015482125b1561091357610912604051806060016040528060398152602001611aec60399139610de7565b5b8273ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415610986576109856040518060400160405280601481526020017f43616e6e6f74207261746520796f757273656c66000000000000000000000000815250610de7565b5b600080610994338686611157565b915091506109a3338683611259565b506109ad3361142e565b506109b785611565565b5080600560008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206002018190555081600560003373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060060160008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550610a90338685878561169c565b5050505050565b610aa682826000806000611711565b505050565b610aba338787878787876118e3565b505050505050565b6000610acd82610f66565b610b3f576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260118152602001807f70726f66696c65206e6f7420666f756e6400000000000000000000000000000081525060200191505060405180910390fd5b6000600560008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002090508060020154915050919050565b6000600480549050905090565b600080600080600080610bae87610f66565b610c20576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260118152602001807f70726f66696c65206e6f7420666f756e6400000000000000000000000000000081525060200191505060405180910390fd5b600560008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16600560008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060010154600560008a73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060020154600560008b73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060030154600560008c73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060040154600560008d73ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206005015495509550955095509550955091939550919395565b7f6222dc10afcca3c8cec10b1b08a9bff096c30eb574a3233be051aa5440fc41bb816040518080602001828103825283818151815260200191508051906020019080838360005b83811015610e49578082015181840152602081019050610e2e565b50505050905090810190601f168015610e765780820380516001836020036101000a031916815260200191505b509250505060405180910390a1806040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825283818151815260200191508051906020019080838360005b83811015610eea578082015181840152602081019050610ecf565b50505050905090810190601f168015610f175780820380516001836020036101000a031916815260200191505b509250505060405180910390fd5b600060048281548110610f3457fe5b9060005260206000200160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050919050565b6000806004805490501415610f7e5760009050611029565b8173ffffffffffffffffffffffffffffffffffffffff166004600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206005015481548110610fe457fe5b9060005260206000200160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff161490505b919050565b60056020528060005260406000206000915090508060000160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff16908060010154908060020154908060030154908060040154908060050154905086565b600061109582610f66565b611107576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260118152602001807f70726f66696c65206e6f7420666f756e6400000000000000000000000000000081525060200191505060405180910390fd5b6000600560008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002090508060040154915050919050565b600080600083600560008773ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060020154019050600084600560008973ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060060160008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020540190506002548113156112395760025490505b6003548112156112495760035490505b8082935093505050935093915050565b600061126483610f66565b6112a7576112a66040518060400160405280601381526020017f726563697069656e74206e6f7420666f756e6400000000000000000000000000815250610de7565b5b6112b084610f66565b6112f3576112f26040518060400160405280601081526020017f73656e646572206e6f7420666f756e6400000000000000000000000000000000815250610de7565b5b81600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206002018190555081600560008673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060060160008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167f74c9e96f7615ed2accc025c19a2fc4eac63e018762c879dd73307716932285ed846040518082815260200191505060405180910390a3600190509392505050565b600061143982610f66565b61147c5761147b6040518060400160405280601181526020017f70726f66696c65206e6f7420666f756e64000000000000000000000000000000815250610de7565b5b60006001600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206003015401905080600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600301819055508273ffffffffffffffffffffffffffffffffffffffff167fec0fb6207e4b2f6bf5fde406d30ca3449f7f717795efa8efc08aef272ef6265d826040518082815260200191505060405180910390a26001915050919050565b600061157082610f66565b6115b3576115b26040518060400160405280601181526020017f70726f66696c65206e6f7420666f756e64000000000000000000000000000000815250610de7565b5b60006001600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206004015401905080600560008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020600401819055508273ffffffffffffffffffffffffffffffffffffffff167fec0fb6207e4b2f6bf5fde406d30ca3449f7f717795efa8efc08aef272ef6265d826040518082815260200191505060405180910390a26001915050919050565b828473ffffffffffffffffffffffffffffffffffffffff168673ffffffffffffffffffffffffffffffffffffffff167f9b441bc0b57d4dd8d299920aac8beed8314e766a0ce7a41e5d6255b89a5868ec8585604051808381526020018281526020019250505060405180910390a45050505050565b600061171c86610f66565b156117605761175f6040518060400160405280601681526020017f70726f66696c6520616c72656164792065786973747300000000000000000000815250610de7565b5b6040518060c001604052808773ffffffffffffffffffffffffffffffffffffffff168152602001868152602001858152602001848152602001838152602001600160048990806001815401808255809150509060018203906000526020600020016000909192909190916101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555003815250600560008873ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008201518160000160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055506020820151816001015560408201518160020155606082015181600301556080820151816004015560a082015181600501559050506118cf8587611a99565b600160048054905003905095945050505050565b838673ffffffffffffffffffffffffffffffffffffffff168873ffffffffffffffffffffffffffffffffffffffff167f4eea43eaa97f3f895b534e3eec9db8e2a6adbc482962d585cface499479e815a8486888b60405180806020018060200180602001858152602001848103845288818151815260200191508051906020019080838360005b8381101561198557808201518184015260208101905061196a565b50505050905090810190601f1680156119b25780820380516001836020036101000a031916815260200191505b50848103835287818151815260200191508051906020019080838360005b838110156119eb5780820151818401526020810190506119d0565b50505050905090810190601f168015611a185780820380516001836020036101000a031916815260200191505b50848103825286818151815260200191508051906020019080838360005b83811015611a51578082015181840152602081019050611a36565b50505050905090810190601f168015611a7e5780820380516001836020036101000a031916815260200191505b5097505050505050505060405180910390a450505050505050565b8073ffffffffffffffffffffffffffffffffffffffff167f97ac5d16ba6a936f2c55834f0a22fa19a40cb24e9629f714bc2c63890139cb2f836040518082815260200191505060405180910390a2505056fe526174696e67206d75737420626520616e20696e74206265747765656e205f5f616d6f756e744d696e20616e64205f5f616d6f756e744d6178a265627a7a723158201a10311befca1995918bf11d54eafe36b717dd19bbe9954b33ad00f624daf1a864736f6c63430005100032";

    public static final String FUNC_PROFILES = "profiles";

    public static final String FUNC_GETNOTRANSACTIONSSENT = "getNoTransactionsSent";

    public static final String FUNC_GETNOTRANSACTIONSRECEIVED = "getNoTransactionsReceived";

    public static final String FUNC_GETCUMULATIVESCORE = "getCumulativeScore";

    public static final String FUNC_HASPROFILE = "hasProfile";

    public static final String FUNC__GETPROFILE = "_getProfile";

    public static final String FUNC__GETUSERATINDEX = "_getUserAtIndex";

    public static final String FUNC__GETUSERCOUNT = "_getUserCount";

    public static final String FUNC_CREATEPROFILE = "createProfile";

    public static final String FUNC_ADDTRANSACTION = "addTransaction";

    public static final String FUNC_ADDGENERICTRANSACTION = "addGenericTransaction";

    public static final String FUNC__REVERT = "_revert";

    public static final Event ERROREVENT_EVENT = new Event("ErrorEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    public static final Event GENERICTRANSACTIONADDED_EVENT = new Event("GenericTransactionAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRANSACTIONADDED_EVENT = new Event("TransactionAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Int256>() {}, new TypeReference<Int256>() {}));
    ;

    public static final Event TRANSACTIONCOUNTCHANGED_EVENT = new Event("TransactionCountChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRANSACTIONSCORECHANGED_EVENT = new Event("TransactionScoreChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Int256>() {}));
    ;

    public static final Event USERPROFILECREATED_EVENT = new Event("UserProfileCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}, new TypeReference<Address>(true) {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("5777", "0xA72991770D8244D318079e70a38F1d63B98359a9");
    }

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ReputationRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ReputationRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ErrorEventEventResponse> getErrorEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ERROREVENT_EVENT, transactionReceipt);
        ArrayList<ErrorEventEventResponse> responses = new ArrayList<ErrorEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ErrorEventEventResponse typedResponse = new ErrorEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ErrorEventEventResponse> errorEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ErrorEventEventResponse>() {
            @Override
            public ErrorEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ERROREVENT_EVENT, log);
                ErrorEventEventResponse typedResponse = new ErrorEventEventResponse();
                typedResponse.log = log;
                typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ErrorEventEventResponse> errorEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ERROREVENT_EVENT));
        return errorEventEventFlowable(filter);
    }

    public List<GenericTransactionAddedEventResponse> getGenericTransactionAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(GENERICTRANSACTIONADDED_EVENT, transactionReceipt);
        ArrayList<GenericTransactionAddedEventResponse> responses = new ArrayList<GenericTransactionAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GenericTransactionAddedEventResponse typedResponse = new GenericTransactionAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.transactionType = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.message = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.txHash = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.weiAmount = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<GenericTransactionAddedEventResponse> genericTransactionAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, GenericTransactionAddedEventResponse>() {
            @Override
            public GenericTransactionAddedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(GENERICTRANSACTIONADDED_EVENT, log);
                GenericTransactionAddedEventResponse typedResponse = new GenericTransactionAddedEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.transactionType = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.message = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.txHash = (String) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.weiAmount = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<GenericTransactionAddedEventResponse> genericTransactionAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GENERICTRANSACTIONADDED_EVENT));
        return genericTransactionAddedEventFlowable(filter);
    }

    public List<TransactionAddedEventResponse> getTransactionAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSACTIONADDED_EVENT, transactionReceipt);
        ArrayList<TransactionAddedEventResponse> responses = new ArrayList<TransactionAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransactionAddedEventResponse typedResponse = new TransactionAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.grade = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.recipientNewScore = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransactionAddedEventResponse> transactionAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransactionAddedEventResponse>() {
            @Override
            public TransactionAddedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSACTIONADDED_EVENT, log);
                TransactionAddedEventResponse typedResponse = new TransactionAddedEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.timestamp = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.grade = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.recipientNewScore = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransactionAddedEventResponse> transactionAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSACTIONADDED_EVENT));
        return transactionAddedEventFlowable(filter);
    }

    public List<TransactionCountChangedEventResponse> getTransactionCountChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSACTIONCOUNTCHANGED_EVENT, transactionReceipt);
        ArrayList<TransactionCountChangedEventResponse> responses = new ArrayList<TransactionCountChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransactionCountChangedEventResponse typedResponse = new TransactionCountChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransactionCountChangedEventResponse> transactionCountChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransactionCountChangedEventResponse>() {
            @Override
            public TransactionCountChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSACTIONCOUNTCHANGED_EVENT, log);
                TransactionCountChangedEventResponse typedResponse = new TransactionCountChangedEventResponse();
                typedResponse.log = log;
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransactionCountChangedEventResponse> transactionCountChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSACTIONCOUNTCHANGED_EVENT));
        return transactionCountChangedEventFlowable(filter);
    }

    public List<TransactionScoreChangedEventResponse> getTransactionScoreChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSACTIONSCORECHANGED_EVENT, transactionReceipt);
        ArrayList<TransactionScoreChangedEventResponse> responses = new ArrayList<TransactionScoreChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransactionScoreChangedEventResponse typedResponse = new TransactionScoreChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransactionScoreChangedEventResponse> transactionScoreChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransactionScoreChangedEventResponse>() {
            @Override
            public TransactionScoreChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSACTIONSCORECHANGED_EVENT, log);
                TransactionScoreChangedEventResponse typedResponse = new TransactionScoreChangedEventResponse();
                typedResponse.log = log;
                typedResponse.sender = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.recipient = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.newScore = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransactionScoreChangedEventResponse> transactionScoreChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSACTIONSCORECHANGED_EVENT));
        return transactionScoreChangedEventFlowable(filter);
    }

    public List<UserProfileCreatedEventResponse> getUserProfileCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(USERPROFILECREATED_EVENT, transactionReceipt);
        ArrayList<UserProfileCreatedEventResponse> responses = new ArrayList<UserProfileCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UserProfileCreatedEventResponse typedResponse = new UserProfileCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UserProfileCreatedEventResponse> userProfileCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, UserProfileCreatedEventResponse>() {
            @Override
            public UserProfileCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(USERPROFILECREATED_EVENT, log);
                UserProfileCreatedEventResponse typedResponse = new UserProfileCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.name = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UserProfileCreatedEventResponse> userProfileCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(USERPROFILECREATED_EVENT));
        return userProfileCreatedEventFlowable(filter);
    }

    public RemoteFunctionCall<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>> profiles(String param0) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROFILES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Int256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue());
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> getNoTransactionsSent(String profileID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETNOTRANSACTIONSSENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(profileID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getNoTransactionsReceived(String profileID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETNOTRANSACTIONSRECEIVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(profileID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> getCumulativeScore(String profileID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETCUMULATIVESCORE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(profileID)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Int256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> hasProfile(String userAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_HASPROFILE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>> _getProfile(String userAddress) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC__GETPROFILE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userAddress)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Int256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<String, byte[], BigInteger, BigInteger, BigInteger, BigInteger>(
                                (String) results.get(0).getValue(), 
                                (byte[]) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> _getUserAtIndex(BigInteger index) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC__GETUSERATINDEX, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(index)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> _getUserCount() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC__GETUSERCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> createProfile(String sender, byte[] userName) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEPROFILE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(sender), 
                new org.web3j.abi.datatypes.generated.Bytes32(userName)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> addTransaction(String contrahent, BigInteger amount, BigInteger timestamp) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADDTRANSACTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(contrahent), 
                new org.web3j.abi.datatypes.generated.Int256(amount), 
                new org.web3j.abi.datatypes.generated.Uint256(timestamp)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> addGenericTransaction(String contrahent, BigInteger weiAmount, BigInteger timestamp, String txHash, String message, String transactionType) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ADDGENERICTRANSACTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(contrahent), 
                new org.web3j.abi.datatypes.generated.Uint256(weiAmount), 
                new org.web3j.abi.datatypes.generated.Uint256(timestamp), 
                new org.web3j.abi.datatypes.Utf8String(txHash), 
                new org.web3j.abi.datatypes.Utf8String(message), 
                new org.web3j.abi.datatypes.Utf8String(transactionType)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> _revert(String message) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC__REVERT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(message)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ReputationRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ReputationRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ReputationRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ReputationRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String userRegistryAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(userRegistryAddress)));
        return deployRemoteCall(ReputationRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class ErrorEventEventResponse extends BaseEventResponse {
        public String message;
    }

    public static class GenericTransactionAddedEventResponse extends BaseEventResponse {
        public String sender;

        public String recipient;

        public BigInteger timestamp;

        public String transactionType;

        public String message;

        public String txHash;

        public BigInteger weiAmount;
    }

    public static class TransactionAddedEventResponse extends BaseEventResponse {
        public String sender;

        public String recipient;

        public BigInteger timestamp;

        public BigInteger grade;

        public BigInteger recipientNewScore;
    }

    public static class TransactionCountChangedEventResponse extends BaseEventResponse {
        public String recipient;

        public BigInteger newScore;
    }

    public static class TransactionScoreChangedEventResponse extends BaseEventResponse {
        public String sender;

        public String recipient;

        public BigInteger newScore;
    }

    public static class UserProfileCreatedEventResponse extends BaseEventResponse {
        public String owner;

        public byte[] name;
    }
}
