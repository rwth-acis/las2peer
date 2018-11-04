package i5.las2peer.registryGateway;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;

public class Registry {
    public static String foo() {
        Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
        try {
            Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
            return web3ClientVersion.getWeb3ClientVersion();
        } catch (IOException e) {
            return "[Eth client failed to connect]";
        }
    }
}
