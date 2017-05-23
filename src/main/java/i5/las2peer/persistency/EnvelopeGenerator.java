package i5.las2peer.persistency;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Random;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.FileContentReader;

/**
 * A simple command line tool for generating XML envelopes to the standard out.
 */
public class EnvelopeGenerator {

	/**
	 * Prints a usage help message.
	 */
	public static void usage() {
		usage(null);
	}

	/**
	 * Prints a usage help message and some additional information.
	 * 
	 * @param message additional information
	 */
	public static void usage(String message) {
		System.err.println(
				"Usage: java [-cp ...] i5.las2peer.tools.EnvelopeGenerator [xml agent file] [agent passphrase] [nested class name] [String constructor value]");
		if (message != null) {
			System.err.println("\n" + message);
		}
	}

	/**
	 * Loads an agent from the given XML file name.
	 * 
	 * @param filename
	 * @return a PassphraseAgent
	 * @throws IOException
	 * @throws MalformedXMLException
	 */
	public static PassphraseAgentImpl loadAgent(String filename) throws MalformedXMLException, IOException {
		return (PassphraseAgentImpl) AgentImpl.createFromXml(FileContentReader.read(filename));
	}

	/**
	 * Command line script for generating a simple envelope.
	 * 
	 * Arguments:
	 * <ol>
	 * <li>xml file with owner agent</li>
	 * <li>passphrase of the owner for unlocking the key</li>
	 * <li>name of the (serializable or XmlAble) class to be nested in the envelope<br>
	 * needs a string constructor</li>
	 * <li>String constructor value</li>
	 * </ol>
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {
		if (argv.length != 4) {
			usage();
			return;
		}
		try {
			PassphraseAgentImpl owner = loadAgent(argv[0]);
			owner.unlock(argv[1]);
			Serializable temp = createSerializable(argv[2], argv[3]);
			EnvelopeVersion env = new EnvelopeVersion(Long.toString(new Random().nextLong()), owner.getPublicKey(),
					temp, Arrays.asList(new AgentImpl[] { owner }));
			System.out.println(env.toXmlString());
		} catch (SecurityException e) {
			usage("Unable to call constructor of nested class: " + e);
		} catch (IllegalArgumentException e) {
			usage();
		} catch (MalformedXMLException e) {
			usage("malformed agent XML file");
		} catch (IOException e) {
			usage("unable to read contents of given agent file (" + argv[0] + ")");
		} catch (AgentAccessDeniedException | AgentOperationFailedException e) {
			usage("unable to unlock agent!");
		} catch (ClassNotFoundException e) {
			usage("content class does not exist!");
		} catch (NoSuchMethodException e) {
			usage("no constructor for target class");
		} catch (InstantiationException e) {
			usage("unable to create target class instance");
		} catch (IllegalAccessException e) {
			usage("access problems with constructor of target class");
		} catch (InvocationTargetException e) {
			usage("invocation problems with constructor of target class!");
		} catch (CryptoException e) {
			usage("envelope problems while generating xml output: " + e);
		} catch (SerializationException e) {
			usage("unable to serialize content into envelope!");
		}
	}

	/**
	 * Tries to create an instance of the given class providing a String constructor with the given value.
	 * 
	 * @param classname
	 * @param value
	 * @return a serializable
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static Serializable createSerializable(String classname, String value)
			throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> cls = Class.forName(classname);
		Constructor<?> cons = cls.getConstructor(String.class);
		return (Serializable) cons.newInstance(value);
	}

}
