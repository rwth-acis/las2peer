package i5.las2peer.testing.services;

import i5.las2peer.api.Service;
import i5.las2peer.testing.services.helperClasses.MyException;
import i5.las2peer.testing.services.helperClasses.SomeValue;
import i5.las2peer.tools.FileContentReader;

import java.io.IOException;

/**
 * A simple test service for testing the classloader possibilities of the LAS2peer framework.
 * 
 * 
 *
 */
public class PackageTestService extends Service {

	/**
	 * throw an exception placed in the used library
	 * 
	 * @throws MyException
	 */
	public void throwsOwnException() throws MyException {
		throw new MyException("an exception out of the library to deal with");
	}

	/**
	 * return a value of a class placed in the used library
	 * 
	 * @param aValue
	 * 
	 * @return a test value, containing the negative version of the parameter
	 */
	public SomeValue getValue(int aValue) {
		return new SomeValue(-aValue);
	}

	/**
	 * unpack the given instance of the class SomeValue placed in the helper library and return the doubled value
	 * 
	 * @param aValue
	 * @return double of the given parameter
	 */
	public int getDouble(SomeValue aValue) {
		return aValue.getValue() * 2;
	}

	/**
	 * tries to parse the given parameter into an integer, return -1 on errors
	 * 
	 * @param test
	 * @return integer as described
	 */
	public int intToString(String test) {
		try {
			return Integer.valueOf(test);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * get the content of test.xml in the package folder of this service
	 * 
	 * @return a string from the file test.xml inside the package
	 * @throws IOException
	 */
	public String getResContent() throws IOException {
		return FileContentReader
				.read(getClass().getClassLoader().getResourceAsStream("i5/las2peer/testing/services/test.xml"));
	}

}
