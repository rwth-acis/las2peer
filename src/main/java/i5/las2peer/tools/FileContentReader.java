package i5.las2peer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A simple reader for string contents of a file or input stream.
 * 
 * 
 *
 */
public class FileContentReader {

	/**
	 * get the contents of a file as String
	 * 
	 * @param filename
	 * @return contents of the given file as String
	 * @throws IOException
	 */
	public static String read(String filename) throws IOException {
		return read(new File(filename));
	}

	/**
	 * get the contents of a file as String
	 * 
	 * @param file
	 * @return contents of the given file as String
	 * @throws IOException
	 */
	public static String read(File file) throws IOException {
		if (!file.isFile()) {
			throw new IOException("File " + file.getName() + " does not exist");
		}

		return read(new FileInputStream(file));
	}

	/**
	 * read the content of a Stream and return it as String
	 * 
	 * @param is
	 * @return contents of the given Stream as String
	 * @throws IOException
	 */
	public static String read(InputStream is) throws IOException {
		StringBuffer result = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line);
			result.append(System.getProperty("line.separator"));
		}

		is.close();

		return result.toString();
	}
}
