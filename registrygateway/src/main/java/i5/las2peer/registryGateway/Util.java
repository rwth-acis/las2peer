package i5.las2peer.registryGateway;

import java.nio.charset.StandardCharsets;

public class Util {
	private Util() {
	}

	/**
	 * Convert String to to byte array, padded with zero bytes.
	 *
	 * If the desired length is shorter than the input, arraycopy will
	 * throw ArrayIndexOutOfBoundsException. (Maybe emit warning and
	 * truncate instead?)
	 */
	public static byte[] padAndConvertString(String string, int desiredLength) {
		byte[] output = new byte[desiredLength];
		byte[] possiblyTooShort = string.getBytes(StandardCharsets.UTF_8);

		int inputLength = possiblyTooShort.length;
		int offset = desiredLength - inputLength;
		System.arraycopy(possiblyTooShort, 0, output, offset, inputLength);
		return output;
	}
}
