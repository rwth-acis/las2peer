package i5.las2peer.tools;

import java.io.IOException;
import java.util.Vector;

import i5.las2peer.api.security.AgentException;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.serialization.MalformedXMLException;
import i5.las2peer.serialization.SerializationException;

/**
 * A simple command line client generating a group agent XML file.
 * 
 * 
 *
 */
public class GroupAgentGenerator {

	/**
	 * command line method printing a group XML file
	 * 
	 * @param argv
	 * @throws MalformedXMLException
	 * @throws IOException
	 * @throws AgentException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static void main(String[] argv, String groupName)
			throws MalformedXMLException, IOException, AgentException, CryptoException, SerializationException {
		if (argv.length == 0 || argv[0].equals("-?")) {
			System.out.println("Just give a liste with xml files of the agents, you want to aggregate in a group");
			System.exit(0);
		}

		Vector<AgentImpl> agents = new Vector<AgentImpl>();

		for (String file : argv) {
			AgentImpl a = AgentImpl.createFromXml(FileContentReader.read(file));
			agents.add(a);
		}

		GroupAgentImpl result = GroupAgentImpl.createGroupAgent(agents.toArray(new AgentImpl[0]), groupName);

		System.out.println(result.toXmlString());
	}

}
