package i5.las2peer.registryGateway;

import org.web3j.abi.datatypes.BytesType;

import java.nio.charset.StandardCharsets;

import static org.web3j.utils.Bytes.trimLeadingZeroes;
import static org.web3j.utils.Numeric.hexStringToByteArray;

public class Util {
	private Util() {
	}

	/**
	 * Convert string to to byte array, padded with zero bytes.
	 * Uses UTF-8 encoding.
	 */
	public static byte[] padAndConvertString(String string, int desiredLength) {
		if (string.length() > desiredLength) {
			throw new IllegalArgumentException("string length must be <= desired length");
		}

		byte[] output = new byte[desiredLength];
		byte[] possiblyTooShort = string.getBytes(StandardCharsets.UTF_8);

		int inputLength = possiblyTooShort.length;
		int offset = desiredLength - inputLength;
		System.arraycopy(possiblyTooShort, 0, output, offset, inputLength);
		return output;
	}

	/**
	 * Recover string from byte-encoded hex string.
	 * (E.g., the hex strings in transactions / event logs.)
	 * Assumes UTF-8 encoding.
	 */
	public static String recoverString(String hexString) {
		byte[] byteArray = hexStringToByteArray(hexString);
		byte[] trimmed = trimLeadingZeroes(byteArray);
		return new String(trimmed, StandardCharsets.UTF_8);
	}

	public static String recoverString(byte[] byteArray) {
		byte[] trimmed = trimLeadingZeroes(byteArray);
		return new String(trimmed, StandardCharsets.UTF_8);
	}
}
