package i5.las2peer.tools;

import i5.las2peer.security.ServiceAgent;

/**
 * A simple command line tool creating a service agent for the given service class.
 * 
 * Provided a passphrase, the tool will generate an XML representation of the required agent and put it to standard out.
 * 
 */
public class ServiceAgentGenerator {

	private static final int PW_MIN_LENGTH = 4;

	/**
	 * command line service agent generator
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {
		if (argv.length != 2) {
			System.err.println("usage: java i5.las2peer.tools.ServiceAgentGenerator [service class] [passphrase]");
			return;
		} else if (argv[0].length() < PW_MIN_LENGTH) {
			System.err.println("the password needs to be at least " + PW_MIN_LENGTH + " signs long, but only "
					+ argv[0].length() + " given");
			return;
		}

		try {
			ServiceAgent agent = ServiceAgent.createServiceAgent(argv[0], argv[1]);
			System.out.print(agent.toXmlString());
		} catch (Exception e) {
			System.err.println("unable to generate new agent: " + e);
		}
	}

}
