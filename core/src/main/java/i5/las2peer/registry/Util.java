package i5.las2peer.registry;

import org.web3j.crypto.Hash;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.web3j.utils.Bytes.trimLeadingZeroes;
import static org.web3j.utils.Numeric.hexStringToByteArray;

/**
 * Helper methods mostly for converting data.
 */
public class Util {
	private Util() {
	}

	/**
	 * Converts string to byte array, padded with zero bytes,
	 * using UTF-8 encoding.
	 * @param string input string. Must be no longer than <code>desiredLength</code>
	 * @param desiredLength length of the returned byte array
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
	 * Recovers string from byte-encoded hex string.
	 * (E.g., the hex strings in transactions / event logs.)
	 * Assumes UTF-8 encoding.
	 * @param hexString hexadecimal string containing Unicode
	 * @return decoded string
	 */
	public static String recoverString(String hexString) {
		byte[] byteArray = hexStringToByteArray(hexString);
		byte[] trimmed = trimLeadingZeroes(byteArray);
		return new String(trimmed, StandardCharsets.UTF_8);
	}

	/**
	 * Recovers string from byte array with possible leading zeros.
	 * Assumes UTF-8 encoding.
	 * @param byteArray Unicode byte array. Leading zeros are allowed
	 *                  and will be trimmed before decoding.
	 * @return decoded string
	 */
	public static String recoverString(byte[] byteArray) {
		byte[] trimmed = trimLeadingZeroes(byteArray);
		return new String(trimmed, StandardCharsets.UTF_8);
	}

	public static String bytesToHexString(byte[] byteArray) {
		StringBuilder sb = new StringBuilder();
		for (byte b : byteArray) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}

	/**
	 * Computes Sha3 (= Keccak256) sum, hopefully matching the sum
	 * produced by Solidity's keccak256 and web3's soliditySha3.
	 * @param input arbitrary input string
	 * @return hash of input
	 */
	public static byte[] soliditySha3(String input) {
		return Hash.sha3(input.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Converts version string ("x.y.z") to integers.
	 * If the string has the form "x.y" or "x", the unspecified
	 * components are assumed to be zero. (E.g., "1" = [1,0,0].)
	 * @param versionString version consisting of digits and up to
	 *                      three periods. Components must be parsable
	 *                      as Integers.
	 * @return equivalent version as int array of length three
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
	 * Returns human-readable time string.
	 * @param unixSeconds Unix time in seconds
	 * @return date and time as string
	 */
	public static String unixtimeToString(long unixSeconds) {
		long unixMilliseconds = unixSeconds * 1000L;
		Date date = new java.util.Date(unixMilliseconds);
		SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("Europe/Berlin"));
		return sdf.format(date);
	}

	/***
	 * Returns the value of the first parameter if it is not null and the second param otherwise
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static <T> T getOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}
}
