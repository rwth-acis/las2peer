package i5.las2peer.registry;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.web3j.utils.Numeric.hexStringToByteArray;

public class SignatureUtils {
	/**
	 * Returns signature of encoded Ethereum function call.
	 *
	 * The message is derived from the function selector (a.k.a. method ID)
	 * and the arguments. This is hashed. Then prefixed with an Ethereum
	 * specific prefix. Then hashed again. Then signed.
	 *
	 * (The last three steps happen in Web3J:
	 * {@link org.web3j.crypto.Sign#signPrefixedMessage(byte[], ECKeyPair)}.)
	 *
	 * @see <a href="https://solidity.readthedocs.io/en/v0.5.0/abi-spec.html">Encoding spec</a>
	 *
	 * @param functionCall Ethereum contract function call representation,
	 *                     including arguments
	 * @param credentials credentials with key pair used for signing
	 * @return signature as a single byte array, as used for ECRecover
	 */
	public static byte[] signFunctionCall(Function functionCall, Credentials credentials) {
		String callHexString = FunctionEncoder.encode(functionCall);
		byte[] callAsBytes = hexStringToByteArray(callHexString);
		byte[] hashOfCall = Hash.sha3(callAsBytes);

		// in this method, the hash is enveloped with the Ethereum signed message prefix,
		// then that envelope is hashed again (!)
		// (this is to make recreating the signed message in the verifying smart contract a bit easier)
		Sign.SignatureData sigData = Sign.signPrefixedMessage(hashOfCall, credentials.getEcKeyPair());
		return signatureToBytes(sigData);
	}

	/** Concatenates signature components (R, S, V) */
	private static byte[] signatureToBytes(Sign.SignatureData signature) {
		ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
		try {
			byteOS.write(signature.getR());
			byteOS.write(signature.getS());
			byteOS.write(signature.getV());
		} catch (IOException e) {
			throw new IllegalStateException("Totally can't happen, promise.", e);
		}
		return byteOS.toByteArray();
	}
}
