package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;

import i5.las2peer.logging.L2pLogger;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
			TransactionReceipt transactionReceipt = communityTagIndex.create(tagName, "Lorem ipsum dolor sit amet").send();

			Event event = new Event("CommunityTagCreated", Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
			logger.info("created tag, processing receipt:");
			List<Log> logs = transactionReceipt.getLogs();
			for (Log log : logs) {
				logger.info("next log:");

				List<String> topics = log.getTopics();
				for (String topic : topics) {
					logger.info("next topic: " + topic);
				}

				String data = log.getData();
				logger.info("data: " + data);
				List<Type> results = FunctionReturnDecoder.decode(data, event.getNonIndexedParameters());
				for (Type entry : results) {
					logger.info("data entry: " + entry);
					logger.info("attempting decode: " + new String(((Bytes32) entry).getValue(), StandardCharsets.UTF_8));
				}
			}

			return communityTagIndex.viewDescription(tagName).send();
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	public void eventDebug() {
		EthFilter filter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, COMMUNITY_TAG_INDEX_ADDRESS);

		Event event = new Event("CommunityTagCreated", Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
		String topicData = EventEncoder.encode(event);
		filter.addSingleTopic(topicData);
		logger.info(topicData);

		// FIXME: even though the Event signature seems to match the actual
		// logged events (e.g. triggered by tagDebug), the subscription does
		// not trigger

		web3j.ethLogObservable(filter).subscribe(log -> {
			logger.info("got one:");
			logger.info(log.getBlockNumber().toString());
			logger.info(log.getTransactionHash());
			List<String> topics = log.getTopics();
			for (String topic : topics) {
				logger.info(topic);
			}
		});
	}

	public String debug() {
		logger.info("attempting stuff ...");
		try {
			this.eventDebug();
			logger.info("filtering ...");
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
