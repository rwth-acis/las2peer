package i5.las2peer.registry;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

/**
 * Signs transactions locally (with EIP-155 replay protection) and takes nonces from {@link NonceManager}, so
 * contract calls and plain ether transfers from the same account share one consistent nonce sequence.
 */
class NonceManagedTransactionManager extends RawTransactionManager {

	private final Web3j web3j;

	NonceManagedTransactionManager(Web3j web3j, Credentials credentials, long chainId,
			TransactionReceiptProcessor receiptProcessor) {
		super(web3j, credentials, chainId, receiptProcessor);
		this.web3j = web3j;
	}

	@Override
	protected BigInteger getNonce() throws IOException {
		return NonceManager.reserve(web3j, getFromAddress());
	}

	@Override
	public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data,
			BigInteger value, boolean constructor) throws IOException {
		try {
			EthSendTransaction response = super.sendTransaction(gasPrice, gasLimit, to, data, value, constructor);
			if (response.hasError()) {
				NonceManager.reset(getFromAddress());
			}
			return response;
		} catch (IOException | RuntimeException e) {
			NonceManager.reset(getFromAddress());
			throw e;
		}
	}
}
