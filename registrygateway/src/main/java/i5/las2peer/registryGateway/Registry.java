package i5.las2peer.registryGateway;

import i5.las2peer.registryGateway.contracts.CommunityTagIndex;
import i5.las2peer.registryGateway.contracts.UserRegistry;

import i5.las2peer.logging.L2pLogger;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple4;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class Registry {
	/**
	 * Map of currently known tags and their descriptions.
	 *
	 * Do not change; it is continuously updated to reflect the current
	 * known state of the blockchain. (With the expected delay of
	 * blocks being mined and broadcast.)
	 */
	public Map<String, String> tags;

	private final L2pLogger logger = L2pLogger.getInstance(Registry.class);

	// TODO: put stuff in config file
	private static final String ENDPOINT = "http://localhost:8545";
	private static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
	private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6_721_975L);

	// first account using mnemonic "differ employ cook sport clinic wedding melody column pave stuff oak price"
	private static final String ACCOUNT = "0xee5e18b0963126cde89dd2b826f0acdb7e71acdb";
	private static final String PRIVATE_KEY = "0x964d02d3f440a078af46dbc459fc2ac7674e715903fd9f20df737ce26f8bd368";

	private static final String COMMUNITY_TAG_INDEX_ADDRESS = "0x48c7234741fa9910f9228bdc247a92852d531bcd";
	private static final String USER_REGISTRY_ADDRESS = "0x213a9432a55a6fe0129f238bed7119a7a2b75b94";
	private static final String SERVICE_REGISTRY_ADDRESS = "0xdd934d1dfb15be4f3e5a50199963ead449392bae";

	private static final String COMMUNITY_TAG_CREATE_EVENT_NAME = "CommunityTagCreated";

	private Web3j web3j;
	private Credentials credentials;

	private CommunityTagIndex communityTagIndex;
	private UserRegistry userRegistry;

	/**
	 * Connect to Ethereum node, initialize contracts, and start
	 * updating state to mirror the blockchain.
	 */
	public Registry() {
		this.web3j = Web3j.build(new HttpService(ENDPOINT));
		this.credentials = Credentials.create(PRIVATE_KEY);

		this.initContracts();

		try {
			this.keepTagsUpToDate();
		} catch (EthereumException e) {
			logger.severe("bad stuff happened FIXME");
		}
	}

	private void initContracts() {
		this.communityTagIndex = CommunityTagIndex.load(COMMUNITY_TAG_INDEX_ADDRESS, web3j, credentials, GAS_PRICE, GAS_LIMIT);
		this.userRegistry = UserRegistry.load(USER_REGISTRY_ADDRESS, web3j, credentials, GAS_PRICE, GAS_LIMIT);
	}

	/**
	 * Return version string of connected Ethereum client.
	 */
	public String getEthClientVersion() throws EthereumException {
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
			return web3ClientVersion.getWeb3ClientVersion();
		} catch (IOException e) {
			throw new EthereumException("failed to get client version", e);
		}
	}

	/**
	 * Create new tag on blockchain.
	 * @param tagName String with <= 32 UTF-8 characters
	 * @param tagDescription String of arbitrary (!?) length
	 * @throws EthereumException if transaction failed for some reason (gas? networking?)
	 */
	public void createTag(String tagName, String tagDescription) throws EthereumException {
		try {
			communityTagIndex.create(Util.padAndConvertString(tagName, 32), tagDescription).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Read description string of tag from blockchain.
	 */
	private String getTagDescription(String tagName) throws EthereumException {
		try {
			return communityTagIndex.viewDescription(Util.padAndConvertString(tagName, 32)).send();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("tag name invalid (too long?)", e);
		} catch (Exception e) {
			throw new EthereumException(e);
		}
	}

	/**
	 * Create a blockchain observer that reacts to all (past and future)
	 * tag creation events by putting them in the map.
	 */
	// it *may* be possible to simplify this a bit with the generated
	// event observable methods, but I don't know how
	// https://docs.web3j.io/smart_contracts.html#invoking-transactions-and-events
	private void keepTagsUpToDate() throws EthereumException {
		this.tags = new HashMap<>();

		// create a filter "topic" based on event signature
		List<TypeReference<?>> eventArguments = Arrays.asList(new TypeReference<Bytes32>() {});
		Event event = new Event(COMMUNITY_TAG_CREATE_EVENT_NAME, eventArguments);
		String topicData = EventEncoder.encode(event);

		// apply filter to all blocks (from the very beginning to all future blocks)
		EthFilter tagContractFilter = new EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, COMMUNITY_TAG_INDEX_ADDRESS.substring(2));
		tagContractFilter.addSingleTopic(topicData);

		// asynchronously put tags in the Map as soon as such a Tx is in a mined block
		web3j.ethLogObservable(tagContractFilter).subscribe(logEntry -> {
			String tagName = Util.recoverString(logEntry.getData());
			try {
				String tagDescription = getTagDescription(tagName);
				this.tags.put(tagName, tagDescription);
			} catch (EthereumException e) {
				// actually handling this is apparently tricky in Java:
				// https://stackoverflow.com/questions/31270759/a-better-approach-to-handling-exceptions-in-a-functional-way/31270760#31270760
				logger.severe("FIXME exception in lambda, oh no, good luck");
			}
		});
	}

	private UserData getUser(String name) throws EthereumException {
		try {
			Tuple4<byte[], byte[], String, byte[]> t = this.userRegistry.users(Util.padAndConvertString(name, 32)).send();
			return new UserData(t.getValue1(), t.getValue2(), t.getValue3(), t.getValue4());
		} catch (Exception e) {
			throw new EthereumException("Could not get user", e);
		}
	}

	/**
	 * Output some (changing) debug info.
	 */
	public String debug() {
		try {
			//this.userRegistry.register(Util.padAndConvertString("alice", 32), Util.padAndConvertString("some-agent-id", 32)).send();
			return getUser("alice").toString();
		} catch (Exception e) {
			return "error: " + e;
		}
	}
}
