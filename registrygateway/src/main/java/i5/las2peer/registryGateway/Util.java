package i5.las2peer.registryGateway;

import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

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

	/**
	 * Compute Sha3 (= Keccak256) sum, hopefully matching the sum
	 * produced by Solidity's keccak256 and web3's soliditySha3.
	 */
	public static byte[] soliditySha3(String input) {
		return Hash.sha3(input.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Convert version string ("x.y.z") to integers.
	 * If the string has the form "x.y" or "x", the unspecified
	 * components are assumed to be zero. (E.g., "1" = [1,0,0].)
	 */
	public static int[] parseVersion(String versionString) {
		String[] components = versionString.split("\\.");

		if (components.length > 3) {
			throw new IllegalArgumentException("Version string must consist of at most three dot-separated integers");
		}

		// fill with zeros
		int[] version = new int[3];
		for (int i = 0; i < components.length; i++) {
			version[i] = Integer.parseInt(components[i]);
		}
		return version;
	}

	/**
	 * Return human-readable time string.
	 */
	public static String unixtimeToString(long unixSeconds) {
		long unixMilliseconds = unixSeconds * 1000L;
		Date date = new java.util.Date(unixMilliseconds);
		SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Berlin"));
		return sdf.format(date);
	}
}
