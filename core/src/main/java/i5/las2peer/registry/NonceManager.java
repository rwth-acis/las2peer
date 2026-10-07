package i5.las2peer.registry;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import i5.las2peer.logging.L2pLogger;

/**
 * Hands out transaction nonces per account for this node.
 *
 * The next nonce is the larger of the chain's pending transaction count and the last nonce this node
 * handed out + 1. The chain is always consulted, so a reset chain, transactions sent by other tools, or a
 * restarted node can never leave the node stuck with a nonce gap. Handing out is serialized per address, so
 * concurrent requests on one node never reuse a nonce. After a failed send the local counter is dropped and
 * the next transaction resynchronizes with the chain.
 *
 * Nodes that share an account still race each other; give every node its own operator account.
 */
public final class NonceManager {

	private static final L2pLogger logger = L2pLogger.getInstance(NonceManager.class);
	private static final Map<String, BigInteger> nextNonce = new ConcurrentHashMap<>();
	private static final Map<String, Object> locks = new ConcurrentHashMap<>();

	private NonceManager() {
	}

	/** Reserves the next nonce for the address. */
	public static BigInteger reserve(Web3j web3j, String address) throws IOException {
		String key = address.toLowerCase();
		synchronized (locks.computeIfAbsent(key, k -> new Object())) {
			BigInteger pending = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send()
					.getTransactionCount();
			BigInteger local = nextNonce.getOrDefault(key, BigInteger.ZERO);
			BigInteger nonce = pending.max(local);
			nextNonce.put(key, nonce.add(BigInteger.ONE));
			logger.fine("[nonce] " + address + " -> " + nonce + " (chain pending " + pending + ")");
			return nonce;
		}
	}

	/** Forgets the local counter, e.g. after a transaction was rejected, so the chain is trusted again. */
	public static void reset(String address) {
		nextNonce.remove(address.toLowerCase());
	}
}
