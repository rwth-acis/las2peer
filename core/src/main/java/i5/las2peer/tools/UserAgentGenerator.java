package i5.las2peer.tools;

import i5.las2peer.security.UserAgentImpl;

/**
 * A simple command line tool creating a new agent.
 * 
 * Provided a passphrase, the tool will generate an xml representation of the required agent and put it to standard out.
 * 
 */
public class UserAgentGenerator {

	private static final int PW_MIN_LENGTH = 4;

	/**
	 * command line service agent generator
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {
		if (argv.length < 1) {
			System.err.println(SimpleTools.join(argv, "/"));
			System.err.println("usage: java i5.las2peer.tools.UserAgentGenerator [passphrase] [login] [email]\n");
			System.err.println("\n[login] and [email] are optional.\n");
			return;
		} else if (argv[0].length() < PW_MIN_LENGTH) {
			System.err.println("the password needs to be at least " + PW_MIN_LENGTH + " signs long, but only "
					+ argv[0].length() + " given");
			return;
		}

		try {
			UserAgentImpl agent = null;
			if (argv[0].length() > 0)
				agent = UserAgentImpl.createUserAgent(argv[0]);
			else {
				System.err.println("No passphrase is given!");
				return;
			}

			if (argv.length > 1) {
				agent.unlock(argv[0]);

				if (argv[1].length() > 0)
					agent.setLoginName(argv[1]);

				if (argv.length > 2 && argv[2].length() > 0)
					agent.setEmail(argv[2]);
			}
			System.out.print(agent.toXmlString());
		} catch (Exception e) {
			System.err.println("unable to generate new agent: " + e);
		}
	}

}
