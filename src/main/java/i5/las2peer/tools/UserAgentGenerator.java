package i5.las2peer.tools;

import i5.las2peer.security.UserAgent;


/**
 * A simple command line tool creating a new agent.
 * 
 * Provided a passphrase, the tool will generate an xml representation of the
 * required agent and put it to standard out.
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class UserAgentGenerator {

	/**
	 * command line service agent generator
	 * 
	 * @param argv
	 */
	public static void main ( String argv[] ) {
		if ( argv.length < 1 || argv[0].length() < 4 ) {
			System.err.println ( SimpleTools.join(argv,  "/"));
			System.err.println ("usage: java i5.las2peer.tools.UserAgentGenerator [passphrase] [login] [email]\n\n[login] and [email] are optional.\n");
			return;
		}
		
		UserAgent agent = null;
		try {
			if ( argv[0].length() > 0 )
				agent = UserAgent.createUserAgent(argv[0]);
			else {
				System.err.println( "No passphrase is given!");
				return;
			}
				
			
			if ( argv.length > 1  )  {
				agent.unlockPrivateKey(argv[0]);
				
				if ( argv[1].length() > 0 )
					agent.setLoginName( argv[1]);
				
				if ( argv.length > 2 && argv[2].length() > 0 )
					agent.setEmail(argv[2]);
			}
			System.out.print( agent.toXmlString());
		} catch (Exception e) {
			System.err.println ("unable to generate new agent: " + e);
		}		
	}
	
}
