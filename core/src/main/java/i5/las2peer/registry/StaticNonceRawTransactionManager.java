package i5.las2peer.registry;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

import i5.las2peer.logging.L2pLogger;

class StaticNonceRawTransactionManager extends FastRawTransactionManager {

    protected final L2pLogger logger = L2pLogger.getInstance(StaticNonceRawTransactionManager.class);

    private volatile BigInteger nonce = BigInteger.valueOf(-1);
    private String credentialAddress = "";
    private TransactionReceiptProcessor transactionReceiptProcessor;
    

    public StaticNonceRawTransactionManager(Web3j web3j, Credentials credentials,
            TransactionReceiptProcessor transactionReceiptProcessor, BigInteger nonce) {
        super(web3j, credentials, transactionReceiptProcessor);
        this.transactionReceiptProcessor = transactionReceiptProcessor;
        this.credentialAddress = credentials.getAddress();
        logger.info("[TX-NONCE@"+credentialAddress+"] init to " + nonce);
        StaticNonce.Manager().putStaticNonceIfAbsent(credentialAddress, nonce);
    }

    /*@Override
    public EthSendTransaction signAndSend(RawTransaction rawTransaction) throws IOException {
        EthSendTransaction signedTX = super.signAndSend(rawTransaction);

        logger.info("[TXManager]: signing and sending transaction... " );
        String txHash = signedTX.getTransactionHash();
        if ( txHash.length() > 0 )
        {
            logger.info("[TXManager]  > txHash: " + txHash);
            Contracts.addPendingTXHash(txHash);
        }

        return signedTX;
    }
    */
    @Override
    protected synchronized BigInteger getNonce() throws IOException {
        if (StaticNonce.Manager().getStaticNonce(credentialAddress) == BigInteger.valueOf(-1l)) {
            BigInteger parentNonce = super.getNonce();
            logger.info(
                    "[TX-NONCE@"+credentialAddress+"] first transaction: set nonce to " + parentNonce);
            setNonce(parentNonce);
        } else {
            StaticNonce.Manager().incStaticNonce(credentialAddress);
            logger.info("[TX-NONCE@"+credentialAddress+"] consecutive transactions: set nonce to "
                    + StaticNonce.Manager().getStaticNonce(credentialAddress));
        }
        return StaticNonce.Manager().getStaticNonce(credentialAddress);
    }

    @Override
    public BigInteger getCurrentNonce() {
        System.out.println("Calling Static get Current nonce for address: ");
        System.out.println(credentialAddress);
        return StaticNonce.Manager().getStaticNonce(credentialAddress);
    }

    @Override
    public synchronized void resetNonce() throws IOException {
        BigInteger parentNonce = super.getNonce();
        System.out.println("[TX-NONCE@"+credentialAddress+"] resetting nonce (=" + parentNonce + ")");
        System.out.println(credentialAddress);
        logger.info("[TX-NONCE@"+credentialAddress+"] resetting nonce (=" + parentNonce + ")");
        setNonce(parentNonce);
    }

    @Override
    public synchronized void setNonce(BigInteger value) {
        nonce = value;
        System.out.println("[TX-NONCE@"+credentialAddress+"] set nonce to:" + value);
        System.out.println(credentialAddress);
        StaticNonce.Manager().putStaticNonce(credentialAddress, nonce);
        logger.info("[TX-NONCE@"+credentialAddress+"] set nonce to:" + value);
    }

    public TransactionReceipt waitForTxReceipt(String txHash) throws IOException, TransactionException
    {
        return transactionReceiptProcessor.waitForTransactionReceipt(txHash);
    }

}