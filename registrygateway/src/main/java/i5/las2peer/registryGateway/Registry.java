package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;

import i5.las2peer.logging.L2pLogger;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.BytesType;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.web3j.utils.Numeric.hexStringToByteArray;

public class Registry {
	private final L2pLogger logger = L2pLogger.getInstance(Registry.class);


	// TODO: put stuff in config file
	private static final String ENDPOINT = "http://localhost:8545";
	private static final BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);
	private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);

	// first account using mnemonic "differ employ cook sport clinic wedding melody column pave stuff oak price"
	private static final String ACCOUNT = "0xee5e18b0963126cde89dd2b826f0acdb7e71acdb";
	private static final String PRIVATE_KEY = "0x964d02d3f440a078af46dbc459fc2ac7674e715903fd9f20df737ce26f8bd368";

	private static final String HELLO_WORLD_ADDRESS = "0x3e5f9d96aef514b8a25fc82df83e6c9316be08b2";
	private static final String COMMUNITY_TAG_INDEX_ADDRESS = "0x48c7234741fa9910f9228bdc247a92852d531bcd";

	private Web3j web3j;
	private Credentials credentials;

	public Registry() {
		this.web3j = Web3j.build(new HttpService(ENDPOINT));
		this.credentials = Credentials.create(PRIVATE_KEY);

		// DEBUG
		this.eventDebug();
		logger.info("filtering ...");
	}

	public String getEthClientVersion() throws EthereumException {
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
			return web3ClientVersion.getWeb3ClientVersion();
		} catch (IOException e) {
			throw new EthereumException("failed to get client version", e);
		}
	}

	public String tagDebug(String tag) throws EthereumException {
		CommunityTagIndex communityTagIndex = CommunityTagIndex.load(COMMUNITY_TAG_INDEX_ADDRESS, web3j, credentials, GAS_PRICE, GAS_LIMIT);

		try {
			byte[] tagName = Util.padAndConvertString(tag, 32);
			communityTagIndex.create(tagName, "Lorem ipsum dolor sit amet").send();
			return communityTagIndex.viewDescription(tagName).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	public void eventDebug() {
		EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, COMMUNITY_TAG_INDEX_ADDRESS.substring(2));

		Event event = new Event("CommunityTagCreated", Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
		String topicData = EventEncoder.encode(event);
		filter.addSingleTopic(topicData);
		logger.info(topicData);

		web3j.ethLogObservable(filter).subscribe(log -> {
			logger.fine("got one: " + log);
			String dataHexString = log.getData();
			byte[] dataByteArray = hexStringToByteArray(dataHexString);
			BytesType dataBytes = new BytesType(dataByteArray, "Bytes32");
			String dataValue = new String(dataBytes.getValue(), StandardCharsets.UTF_8);
			logger.info("attempting decode: " + dataValue);
		});
	}

	public String debug() {
		logger.info("attempting stuff ...");
		try {
			this.tagDebug("foo" + System.currentTimeMillis());
			this.tagDebug("bar" + System.currentTimeMillis());
			this.tagDebug("spam" + System.currentTimeMillis());
			logger.info("wrote 3 tags");
		} catch	(EthereumException e) {
			return "failed!" + e;
		}
		return "done.";
	}
}
