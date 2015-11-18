package i5.las2peer.tools;

import java.util.Random;

/**
 * Collection of simple tools not large enough to form a separate class.
 * 
 * 
 *
 */
public class SimpleTools {

	/**
	 * get a long hash value from a given String
	 * 
	 * @param s
	 */
	public static long longHash(String s) {
		long h = 1125899906842597L;
		int len = s.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + s.charAt(i);
		}
		return h;
	}

	/**
	 * set with available characters for random string generation via {@link #createRandomString}
	 */
	public static final String sRandomStringCharSet = "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "0123456789";

	/**
	 * create a random string of the given length with the possible characters from {@link #sRandomStringCharSet}
	 * 
	 * @param length
	 * 
	 * @return a random string
	 */
	public static String createRandomString(int length) {
		Random rand = new Random();

		StringBuffer result = new StringBuffer();
		for (int i = 0; i < length; i++) {
			result.append(sRandomStringCharSet.charAt(rand.nextInt(sRandomStringCharSet.length())));
		}

		return result.toString();
	}

	/**
	 * a simple join method to concatenate String representations of the given objects
	 * 
	 * @param objects
	 * @param glue
	 * 
	 * @return join string
	 */
	public static String join(Object[] objects, String glue) {
		if (objects == null || objects.length == 0)
			return "";

		StringBuffer result = new StringBuffer(objects[0].toString());

		for (int lauf = 1; lauf < objects.length; lauf++)
			result.append(glue).append(objects[lauf].toString());

		return result.toString();
	}

	/**
	 * repeat a String <i>count</i> times
	 * 
	 * @param string
	 * @param count
	 * 
	 * @return concatenated string
	 * 
	 */
	public static String repeat(String string, int count) {
		if (string == null)
			return null;
		else if (string.equals("") || count <= 0)
			return "";

		StringBuffer result = new StringBuffer();
		for (int i = 0; i < count; i++)
			result.append(string);

		return result.toString();
	}

	/**
	 * repeat the string representation of the given object <i>count</i> times
	 * 
	 * @param o
	 * @param count
	 * 
	 * @return concatenated string
	 */
	public static String repeat(Object o, int count) {
		return repeat(o.toString(), count);
	}

}
