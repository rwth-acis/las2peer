package i5.las2peer.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoException;

/**
 * Simple Factory class to load Agents from the nested XML files.
 */
public abstract class MockAgentFactory {

	/**
	 * get the contents of a text resource in the classpath
	 * 
	 * @param resourceName
	 * @return contents of a resource as String
	 * @throws IOException
	 */
	public static String getContent(String resourceName) throws IOException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		StringBuffer result = new StringBuffer();
		InputStream is = loader.getResourceAsStream(resourceName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = reader.readLine()) != null) {
			result.append(line);
			result.append(System.getProperty("line.separator"));
		}
		is.close();
		return result.toString();
	}

	/**
	 * Gets Eves agent
	 * 
	 * @return Returns the user agent of Eve
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static UserAgent getEve() throws MalformedXMLException, IOException {
		return UserAgent.createFromXml(getContent("i5/las2peer/testing/eve.xml"));
	}

	/**
	 * Gets Adams agent
	 * 
	 * @return Returns the user agent of Adam
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static UserAgent getAdam() throws MalformedXMLException, IOException {
		return UserAgent.createFromXml(getContent("i5/las2peer/testing/adam.xml"));
	}

	/**
	 * Gets Abels agent
	 * 
	 * @return Returns the user agent of Abel
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static UserAgent getAbel() throws MalformedXMLException, IOException {
		return UserAgent.createFromXml(getContent("i5/las2peer/testing/abel.xml"));
	}

	/**
	 * Gets Anonymous agent
	 * 
	 * @return Returns the user agent to be used for anonymous access
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static UserAgent getAnonymous() throws MalformedXMLException, IOException {
		return UserAgent.createFromXml(getContent("i5/las2peer/testing/anonymous.agent.xml"));
	}

	/**
	 * get ServiceAgent for <i>i5.las2peer.api.TestService</i>
	 * 
	 * The TestServices are placed in the JUnit source tree.
	 * 
	 * @return the ServiceAgent of the TestService
	 * @throws IOException
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent getTestService() throws MalformedXMLException, IOException {
		return ServiceAgent.createFromXml(getContent("i5/las2peer/api/TestService.agent.xml"));
	}

	public static ServiceAgent getCorrectTestService() throws MalformedXMLException, IOException {
		return ServiceAgent.createFromXml(getContent("i5/las2peer/testing/TestService.agent.xml"));
	}

	/**
	 * get a group agent for group1 (containing Adam, eve and Abel)
	 * 
	 * @return a group
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static GroupAgent getGroup1() throws MalformedXMLException, IOException {
		return GroupAgent.createFromXml(getContent("i5/las2peer/testing/group1.xml"));
	}

	/**
	 * get a group agent for group2 (containing Adam, eve and Abel)
	 * 
	 * @return a group
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static GroupAgent getGroup2() throws MalformedXMLException, IOException {
		return GroupAgent.createFromXml(getContent("i5/las2peer/testing/group2.xml"));
	}

	/**
	 * get a group agent for group3 (containing Adam, eve and Abel)
	 * 
	 * @return a group
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static GroupAgent getGroup3() throws MalformedXMLException, IOException {
		return GroupAgent.createFromXml(getContent("i5/las2peer/testing/group3.xml"));
	}

	/**
	 * get a group agent for groupA (containing only Adam and Abel)
	 * 
	 * @return a group
	 * @throws MalformedXMLException
	 * @throws IOException
	 */
	public static GroupAgent getGroupA() throws MalformedXMLException, IOException {
		return GroupAgent.createFromXml(getContent("i5/las2peer/testing/groupA.xml"));
	}

	/**
	 * get ServiceAgent for <i>i5.las2peer.api.TestService2</i>
	 * 
	 * The TestServices are placed in the JUnit source tree.
	 * 
	 * @return the ServiceAgent for TestService2
	 * @throws IOException
	 * @throws MalformedXMLException
	 */
	public static ServiceAgent getTestService2() throws MalformedXMLException, IOException {
		return ServiceAgent.createFromXml(getContent("i5/las2peer/api/TestService2.agent.xml"));
	}

	/**
	 * create an agent and print its XML representation to standard out the first command line argument will be used as
	 * passphrase
	 * 
	 * @param argv
	 * @throws NoSuchAlgorithmException
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws IOException
	 */
	public static void main(String argv[])
			throws NoSuchAlgorithmException, L2pSecurityException, CryptoException, IOException {
		System.out.println(UserAgent.createUserAgent(argv[0]).toXmlString());
	}

}
