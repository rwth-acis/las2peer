package i5.las2peer.testing;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.execution.ServiceAccessDeniedException;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentAlreadyExistsException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.serialization.MalformedXMLException;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

/**
 * Simple test service for connectors to have a service with methods to call.
 */
public class TestService extends Service {

	private int iCounter = 0;
	private String sStorage = "";

	/**
	 * a simple counter, returns the number fo calls within each session.
	 *
	 * @return an int
	 */
	public int counter() {
		iCounter++;
		return iCounter;
	}

	/**
	 * returns the stored string
	 *
	 * @return a String
	 */
	public String getStoredString() {
		return sStorage;
	}

	/**
	 * Stores a String in the service instance
	 *
	 * @param s a String
	 */
	public void setStoredString(String s) {
		sStorage = s;
	}

	/**
	 * Throws an Exception
	 *
	 * @exception Exception on each call
	 */
	public void exceptionThrower() throws Exception {
		throw new Exception("An Exception to deal with!");
	}

	/**
	 * Throws a RuntimeException
	 *updateAgent
	 * @exception RuntimeException on each call
	 */
	public void runtimeExceptionThrower() throws RuntimeException {
		throw new RuntimeException("A RuntimeException to deal with!");
	}

	/**
	 * throws an exception that shouldn't be kown at the client.
	 *
	 * @exception MyOwnException
	 */
	public void myExceptionThrower() throws MyOwnException {
		throw new MyOwnException("This is an exception not kown to the client!");
	}

	/**
	 * returns the sum of an bytearray
	 *
	 * @param bytes a byte[]
	 * @return a long
	 */
	public long byteAdder(byte[] bytes) {
		long result = 0;

		for (byte b : bytes) {
			result += b;
		}

		return result;
	}

	/**
	 * returns a Hashtable with one entry to test complex result types
	 *
	 * @return a Hashtable
	 */
	public Hashtable<String, String> getAHash() {
		Hashtable<String, String> hsResult = new Hashtable<String, String>();
		hsResult.put("x", "y");

		return hsResult;
	}

	/**
	 * Returns a fixed array with strings.
	 *
	 * @return a String[]
	 */
	public String[] stringArrayReturner() {
		return new String[] { "This", "is an", "array", "with Strings" };
	}

	/**
	 * Returns a reverted version of the given string array
	 *
	 * @param ar a String[]
	 * @return a String[]
	 */
	public String[] stringArrayReturner(String[] ar) {
		String[] asResult = new String[ar.length];

		for (int i = 0; i < asResult.length; i++) {
			asResult[asResult.length - i - 1] = ar[i];
		}

		return asResult;
	}

	/**
	 * @return an empty array of strings
	 */
	public String[] emptyStringArrayReturner() {
		return new String[0];
	}

	/**
	 * A method with multiple arguments returning a string summary over the argument
	 *
	 * @param i an int
	 * @param s a String
	 * @param l a long
	 * @param b a boolean
	 * @return a String
	 */
	public String multipleArguments(int i, String s, long l, boolean b) {
		return "The method has been called with the following arguments:\n" + "an Integer: " + i + "\n" + "a String: "
				+ s + "\n" + "a long: " + l + "\n" + "a boolean: " + b + "\n";
	}

	/**
	 * A method with multiple arguments returning a string summary over the argument
	 *
	 * @param i an int
	 * @param s a String
	 * @param l a long
	 * @param b a boolean
	 * @return a String
	 */
	public String multipleArguments2(Integer i, String s, Long l, Boolean b) {
		return "The method has been called with the following arguments:\n" + "an Integer: " + i + "\n" + "a String: "
				+ s + "\n" + "a long: " + l + "\n" + "a boolean: " + b + "\n";
	}

	/**
	 * throws an Exception indication, that the access (for the current user) has been denied.
	 *
	 * @exception ServiceAccessDeniedException
	 */
	public void accessForbidden() throws ServiceAccessDeniedException {
		throw new ServiceAccessDeniedException("access forbidden!");
	}

	/**
	 * concats an array of String to one single String. Basically for testing array of Strings as invocation parameter.
	 *
	 * @param strings a String[]
	 * @return a String
	 */
	public String concatStrings(String[] strings) {
		if (strings == null) {
			return "";
		}

		StringBuffer buffer = new StringBuffer();
		for (String string : strings) {
			buffer.append(string);
		}

		return buffer.toString();
	}

	/**
	 * simply returns the given byte array for connector coding tests
	 *
	 * @param ab a byte[]
	 * @return a byte[]
	 */
	public byte[] byteArrayReturner(byte[] ab) {
		return ab;
	}

	/**
	 * simply returns the given long array for connector coding testing
	 *
	 * @param al a long[]
	 * @return a long[]
	 */
	public long[] longArrayReturner(long[] al) {
		return al;
	}

	/**
	 * simply returns the given long value for connector coding testing
	 *
	 * @param l a long
	 * @return a long
	 */
	public long longReturner(long l) {
		return l;
	}

	/**
	 * Returns the given date increased by one day. This may be used as test for delivering Serializables in connectors.
	 *
	 * @param input a Date
	 * @return a Date
	 */
	public java.util.Date addADay(java.util.Date input) {
		long time = input.getTime();
		time += 1000 * 60 * 60 * 24;
		java.util.Date result = new java.util.Date(time);

		return result;
	}

	/**
	 * access to the property file
	 * 
	 * @return hashtable with properties
	 */
	public Hashtable<String, String> getProps() {
		return getProperties();
	}

	private String envelopeId = null;

	/**
	 * test for envelopes: store a string in an envelope
	 * 
	 * @return previously stored string
	 * @throws EnvelopeOperationFailedException
	 * @throws EnvelopeNotFoundException
	 * @throws EnvelopeAccessDeniedException
	 */
	public String getEnvelopeString() throws EnvelopeAccessDeniedException, EnvelopeNotFoundException,
			EnvelopeOperationFailedException {
		if (envelopeId == null) {
			return "nothing stored!";
		}
		String result = (String) Context.get().requestEnvelope(envelopeId).getContent();
		return result;
	}

	/**
	 * test for envelopes: get stored String
	 * 
	 * @param s
	 * @throws IllegalArgumentException
	 * @throws EnvelopeOperationFailedException
	 * @throws EnvelopeAccessDeniedException
	 */
	public void storeEnvelopeString(String s) throws EnvelopeOperationFailedException, EnvelopeAccessDeniedException {
		Envelope env = Context.getCurrent().createEnvelope(Long.toString(new Random().nextLong()));
		env.setContent(s);
		Context.get().storeEnvelope(env);
		envelopeId = env.getIdentifier();
	}

	private String groupEnvelopeId;

	/**
	 * get a string stored for the Group1
	 * 
	 * @return a simple stored string
	 * @throws EnvelopeOperationFailedException
	 * @throws EnvelopeNotFoundException
	 * @throws EnvelopeAccessDeniedException
	 */
	public String getGroupEnvelopeString() throws EnvelopeAccessDeniedException, EnvelopeNotFoundException,
			EnvelopeOperationFailedException {
		if (groupEnvelopeId == null) {
			return "nothing stored";
		}
		String result = (String) Context.get().requestEnvelope(groupEnvelopeId).getContent();
		return result;
	}

	/**
	 * store a simple string encrypted for the group
	 * 
	 * @param store a string to store for the group
	 * @throws EnvelopeOperationFailedException
	 * @throws EnvelopeAccessDeniedException
	 * @throws IOException
	 * @throws MalformedXMLException
	 * @throws AgentOperationFailedException
	 * @throws AgentAlreadyExistsException
	 * @throws AgentAccessDeniedException
	 */
	public void storeGroupEnvelopeString(String store) throws EnvelopeAccessDeniedException,
			EnvelopeOperationFailedException, AgentAccessDeniedException, AgentAlreadyExistsException,
			AgentOperationFailedException, MalformedXMLException, IOException {
		Context.get().storeAgent(MockAgentFactory.getGroup1());
		Envelope env = Context.get().createEnvelope(Long.toString(new Random().nextLong()));
		env.setContent(store);
		Context.get().storeEnvelope(env);
		groupEnvelopeId = env.getIdentifier();
	}
}
