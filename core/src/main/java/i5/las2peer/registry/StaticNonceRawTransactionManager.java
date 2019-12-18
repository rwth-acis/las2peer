package i5.las2peer.registry;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.ChainId;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

import i5.las2peer.logging.L2pLogger;

class StaticNonceRawTransactionManager extends FastRawTransactionManager {

    protected final L2pLogger logger = L2pLogger.getInstance(StaticNonceRawTransactionManager.class);

    private volatile BigInteger nonce = BigInteger.valueOf(-1);
    private String credentialAddress = "";
    private TransactionReceiptProcessor transactionReceiptProcessor;
    private static ConcurrentHashMap<String, BigInteger> staticNonces = new ConcurrentHashMap<>();// ;

    public StaticNonceRawTransactionManager(Web3j web3j, Credentials credentials,
            TransactionReceiptProcessor transactionReceiptProcessor) {
        super(web3j, credentials, transactionReceiptProcessor);
        this.transactionReceiptProcessor = transactionReceiptProcessor;
        credentialAddress = credentials.getAddress();
        try {
            StaticNonceRawTransactionManager.staticNonces.put(credentialAddress, super.getNonce());
        } catch (IOException e) {
            StaticNonceRawTransactionManager.staticNonces.put(credentialAddress, BigInteger.valueOf(-1));
        }
    }

    public static BigInteger getStaticNonce(String address) {
        return StaticNonceRawTransactionManager.staticNonces.putIfAbsent(address, BigInteger.valueOf(-1));
    }

    public static void setStaticNonce(String address, BigInteger value) {
        StaticNonceRawTransactionManager.staticNonces.put(address, value);
    }

    public static synchronized BigInteger incStaticNonce(String address) {
        BigInteger newValue = StaticNonceRawTransactionManager.staticNonces.get(address).add(BigInteger.ONE);
        StaticNonceRawTransactionManager.setStaticNonce(address, newValue);
        return newValue;
    }

    @Override
    protected synchronized BigInteger getNonce() throws IOException {
        int staticNonce = StaticNonceRawTransactionManager.getStaticNonce(credentialAddress).intValue();
        if (staticNonce == -1) {
            BigInteger parentNonce = super.getNonce();
            logger.info(
                    "[TX-NONCE] first transaction: set nonce to no. of pending transactions (=" + parentNonce + ")");
            setNonce(parentNonce);
        } else {
            logger.info("[TX-NONCE] consecutive transactions: set nonce to no. of pending transactions (="
                    + StaticNonceRawTransactionManager.getStaticNonce(credentialAddress) + " + 1)");
            StaticNonceRawTransactionManager.incStaticNonce(credentialAddress);
        }
        return StaticNonceRawTransactionManager.getStaticNonce(credentialAddress);
    }

    public BigInteger getCurrentNonce() {
        return nonce;
    }

    public synchronized void resetNonce() throws IOException {
        BigInteger parentNonce = super.getNonce();
        logger.info("[TX-NONCE] resetting nonce (=" + parentNonce + ")");
        setNonce(parentNonce);
    }

    public synchronized void setNonce(BigInteger value) {
        nonce = value;
        StaticNonceRawTransactionManager.setStaticNonce(credentialAddress, nonce);
        logger.info("[TX-NONCE] set nonce to:" + value);
    }

    public TransactionReceipt waitForTxReceipt(String txHash) throws IOException, TransactionException
    {
        return transactionReceiptProcessor.waitForTransactionReceipt(txHash);
    }

}