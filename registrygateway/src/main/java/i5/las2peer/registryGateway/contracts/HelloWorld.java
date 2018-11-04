package i5.las2peer.registryGateway.contracts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
public class HelloWorld extends Contract {
    private static final String BINARY = "0x608060405234801561001057600080fd5b50610442806100206000396000f3006080604052600436106100a4576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806345319745146100a95780634d43bec9146100d65780634e4715c4146101035780635542c8b8146101445780635c358c531461018557806367190395146101c65780637f6e692d146101f35780637f95173214610234578063cb68953114610277578063e01adb36146102a2575b600080fd5b3480156100b557600080fd5b506100d4600480360381019080803590602001909291905050506102e3565b005b3480156100e257600080fd5b50610101600480360381019080803590602001909291905050506102ed565b005b34801561010f57600080fd5b5061012e60048036038101908080359060200190929190505050610327565b6040518082815260200191505060405180910390f35b34801561015057600080fd5b5061016f60048036038101908080359060200190929190505050610368565b6040518082815260200191505060405180910390f35b34801561019157600080fd5b506101b060048036038101908080359060200190929190505050610372565b6040518082815260200191505060405180910390f35b3480156101d257600080fd5b506101f16004803603810190808035906020019092919050505061037d565b005b3480156101ff57600080fd5b5061021e60048036038101908080359060200190929190505050610380565b6040518082815260200191505060405180910390f35b34801561024057600080fd5b506102616004803603810190808035151590602001909291905050506103c3565b6040518082815260200191505060405180910390f35b34801561028357600080fd5b5061028c610406565b6040518082815260200191505060405180910390f35b3480156102ae57600080fd5b506102cd6004803603810190808035906020019092919050505061040c565b6040518082815260200191505060405180910390f35b8060008190555050565b7f12d199749b3f4c44df8d9386c63d725b7756ec47204f3aa0bf05ea832f89effb816040518082815260200191505060405180910390a150565b60007f12d199749b3f4c44df8d9386c63d725b7756ec47204f3aa0bf05ea832f89effb826040518082815260200191505060405180910390a1819050919050565b6000819050919050565b600080549050919050565b50565b60007f12d199749b3f4c44df8d9386c63d725b7756ec47204f3aa0bf05ea832f89effb602a6040518082815260200191505060405180910390a1602a9050919050565b60007f12d199749b3f4c44df8d9386c63d725b7756ec47204f3aa0bf05ea832f89effb60176040518082815260200191505060405180910390a160179050919050565b60005481565b60008190509190505600a165627a7a72305820e7bbc795feae161c5a2377f8bbe9796d24aa1f11722cfe3de921be85821d53a00029";

    public static final String FUNC_MEMBERVAR = "memberVar";

    public static final String FUNC_NOOP = "noop";

    public static final String FUNC_UNMARKEDPUREF = "unmarkedPureF";

    public static final String FUNC_PUREF = "pureF";

    public static final String FUNC_VIEWF = "viewF";

    public static final String FUNC_SETTERF = "setterF";

    public static final String FUNC_EMITEVENT = "emitEvent";

    public static final String FUNC_EMITANDRETURN = "emitAndReturn";

    public static final String FUNC_OVERLOADED = "overloaded";

    public static final Event SIMPLEEVENT_EVENT = new Event("SimpleEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    protected static final HashMap<String, String> _addresses;

    static {
        _addresses = new HashMap<String, String>();
        _addresses.put("1537881365143", "0x6faaa3ae331079d56b233b47a7a3df377ddac89d");
        _addresses.put("1538948396210", "0xfee6c0bf334f330119d19e3fbd8de6bef8b6c6ce");
        _addresses.put("1540910817715", "0x348e24953ef069c236c36ba53c9c88c5a4ba33ac");
        _addresses.put("1540914412800", "0x3e5f9d96aef514b8a25fc82df83e6c9316be08b2");
        _addresses.put("1537802983234", "0x25437af88cc3b6de723cf71bfd2674a1e081beb1");
        _addresses.put("456719", "0xb73b1bbbc56b52a85335dd4cbd351b2e701af089");
        _addresses.put("5777", "0xbfcce5161c8330748ab46835d02cf7788a056e48");
        _addresses.put("1538963171946", "0x14c7c2285e55df040b42059b484c1c58a3b12e9e");
        _addresses.put("1538343946582", "0x6243927de4148490828ae7180ad29e0840561e5a");
        _addresses.put("1537449761607", "0xbe44ac9c6ec5675062d9c1c708dc0441ceb1ea48");
    }

    @Deprecated
    protected HelloWorld(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected HelloWorld(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected HelloWorld(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected HelloWorld(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<BigInteger> memberVar() {
        final Function function = new Function(FUNC_MEMBERVAR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public List<SimpleEventEventResponse> getSimpleEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SIMPLEEVENT_EVENT, transactionReceipt);
        ArrayList<SimpleEventEventResponse> responses = new ArrayList<SimpleEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SimpleEventEventResponse typedResponse = new SimpleEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.name = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<SimpleEventEventResponse> simpleEventEventObservable(EthFilter filter) {
        return web3j.ethLogObservable(filter).map(new Func1<Log, SimpleEventEventResponse>() {
            @Override
            public SimpleEventEventResponse call(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SIMPLEEVENT_EVENT, log);
                SimpleEventEventResponse typedResponse = new SimpleEventEventResponse();
                typedResponse.log = log;
                typedResponse.name = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Observable<SimpleEventEventResponse> simpleEventEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SIMPLEEVENT_EVENT));
        return simpleEventEventObservable(filter);
    }

    public RemoteCall<TransactionReceipt> noop(BigInteger v) {
        final Function function = new Function(
                FUNC_NOOP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> unmarkedPureF(BigInteger v) {
        final Function function = new Function(
                FUNC_UNMARKEDPUREF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> pureF(BigInteger v) {
        final Function function = new Function(FUNC_PUREF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> viewF(BigInteger v) {
        final Function function = new Function(FUNC_VIEWF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> setterF(BigInteger v) {
        final Function function = new Function(
                FUNC_SETTERF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> emitEvent(BigInteger v) {
        final Function function = new Function(
                FUNC_EMITEVENT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> emitAndReturn(BigInteger v) {
        final Function function = new Function(
                FUNC_EMITANDRETURN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> overloaded(BigInteger v) {
        final Function function = new Function(
                FUNC_OVERLOADED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> overloaded(Boolean v) {
        final Function function = new Function(
                FUNC_OVERLOADED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Bool(v)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<HelloWorld> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(HelloWorld.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<HelloWorld> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(HelloWorld.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<HelloWorld> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(HelloWorld.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<HelloWorld> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(HelloWorld.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static HelloWorld load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new HelloWorld(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static HelloWorld load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new HelloWorld(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static HelloWorld load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new HelloWorld(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static HelloWorld load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new HelloWorld(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    protected String getStaticDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static String getPreviouslyDeployedAddress(String networkId) {
        return _addresses.get(networkId);
    }

    public static class SimpleEventEventResponse {
        public Log log;

        public BigInteger name;
    }
}
