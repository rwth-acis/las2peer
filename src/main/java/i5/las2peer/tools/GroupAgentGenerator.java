package i5.las2peer.tools;

import java.io.IOException;
import java.util.Vector;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.L2pSecurityException;


/**
 * a simple command line client generating a group agent xml file
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/02/27 16:23:14 $
 *
 */
public class GroupAgentGenerator {


	/**
	 * command line method printing a group XML file 
	 * @param argv
	 * @throws MalformedXMLException
	 * @throws IOException
	 * @throws L2pSecurityException
	 * @throws CryptoException
	 * @throws SerializationException
	 */
	public static void main ( String[] argv ) throws MalformedXMLException, IOException, L2pSecurityException, CryptoException, SerializationException {
		if ( argv.length == 0 || argv[0].equals ( "-?") ) {
			System.out.println ( "Just give a liste with xml files of the agents, you want to aggregate in a group");
			System.exit(0);
		}
		
		Vector<Agent> agents = new Vector<Agent> ();
		
		for ( String file : argv ) {
			Agent a = Agent.createFromXml( FileContentReader.read( file ));
			agents.add ( a );
		}
		
		
		GroupAgent result = GroupAgent.createGroupAgent( agents.toArray ( new Agent[0]));
		
		System.out.println(result.toXmlString());
	}
	
}
