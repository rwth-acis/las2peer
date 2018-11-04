package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.EthereumException;
import i5.las2peer.registryGateway.contracts.HelloWorld;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;

public class Registry {
	// TODO: put stuff in config file
	private static final String ENDPOINT = "http://localhost:8545";
	private static final BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);
	private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);

	// first account using mnemonic "differ employ cook sport clinic wedding melody column pave stuff oak price"
	private static final String ACCOUNT = "0xee5e18b0963126cde89dd2b826f0acdb7e71acdb";
	private static final String PRIVATE_KEY = "0x964d02d3f440a078af46dbc459fc2ac7674e715903fd9f20df737ce26f8bd368";

	private Web3j web3j;
	private Credentials credentials;

	public Registry() {
		this.web3j = Web3j.build(new HttpService(ENDPOINT));
		this.credentials = Credentials.create(PRIVATE_KEY);
	}

	public String getEthClientVersion() throws EthereumException {
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
			return web3ClientVersion.getWeb3ClientVersion();
		} catch (IOException e) {
			throw new EthereumException("failed to get client version", e);
		}
	}

	public String invokeContract() throws EthereumException {
		String CONTRACT_ADDRESS = "0x3e5f9d96aef514b8a25fc82df83e6c9316be08b2";
		HelloWorld helloWorld = HelloWorld.load(CONTRACT_ADDRESS, web3j, credentials, GAS_PRICE, GAS_LIMIT);

		try {
			BigInteger v = helloWorld.pureF(BigInteger.valueOf(56)).send();
			return "Result was: " + v.toString();
		} catch (Exception e) {
			throw new EthereumException("call failed", e);
		}
	}

	public String debug() {
		try {
			return this.invokeContract();
		} catch	(EthereumException e) {
			return "failed!";
		}
	}
}
