package i5.las2peer.tools;

import i5.las2peer.security.ServiceAgent;


/**
 * a simple command line tool creating a service agent for the given service class
 * 
 * Provided a passphrase, the tool will generate an xml representation of the
 * required agent and put it to standard out.
 * 
 * @author Holger Janssen
 * @version $Revision: 1.1 $, $Date: 2013/01/09 10:25:14 $
 *
 */
public class ServiceAgentGenerator {

	/**
	 * command line service agent generator
	 * 
	 * @param argv
	 */
	public static void main ( String argv[] ) {
		if ( argv.length != 2) {
			System.err.println ("usage: java i5.las2peer.tools.ServiceAgentGenerator [service class] [passphrase]");
			return;
		}
		
		ServiceAgent agent;
		try {
			agent = ServiceAgent.generateNewAgent(argv[0], argv[1]);
			System.out.print( agent.toXmlString());
		} catch (Exception e) {
			System.err.println ("unable to generate new agent: " + e);
		}		
	}
	
}
