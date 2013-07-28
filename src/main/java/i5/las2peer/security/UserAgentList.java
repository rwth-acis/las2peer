package i5.las2peer.security;

import i5.las2peer.p2p.AgentNotKnownException;

import java.io.Serializable;
import java.util.Hashtable;


/**
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class UserAgentList implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6774874711710593932L;

	private Hashtable<String, Long> htLogin2UserIds = new Hashtable<String, Long> ();
	
	private Hashtable<String, Long> htEmail2UserIds = new Hashtable<String, Long> ();

	private Hashtable<Long, String[]> htUserEntries = new Hashtable<Long,String[]> ();
	
	
	/**
	 * remove a user from this list (only internally for updating)
	 * 
	 * @param user
	 */
	private void removeUser ( UserAgent user ) {
		String[] userEntry = htUserEntries.get(user.getId());
		
		if ( userEntry != null ) {
			if ( userEntry[0] != null )
				htLogin2UserIds.remove(userEntry[0]);
			if ( userEntry[1] != null )
				htEmail2UserIds.remove(userEntry[1]);
			htUserEntries.remove(user.getId());
		}
	}
	
	
	/**
	 * add or update a user entry
	 * 
	 * @param user
	 * @throws DuplicateEmailException 
	 * @throws DuplicateLoginNameException 
	 */
	public void updateUser ( UserAgent user ) throws DuplicateEmailException, DuplicateLoginNameException {
		removeUser( user );
		
		String[] userEntry = new String[] { user.getLoginName(), user.getEmail() };
		
		if ( userEntry[1] != null) userEntry[1] = userEntry[1].toLowerCase();
		
		if (user.hasLogin() && htLogin2UserIds.containsKey(userEntry[0]))
			throw new DuplicateLoginNameException ();
			
		if ( user.hasEmail() && htEmail2UserIds.containsKey(userEntry[1]))
			throw new DuplicateEmailException();
		
		
		
		htUserEntries.put( user.getId(),  userEntry);
		
		if ( user.hasLogin() )
			htLogin2UserIds.put ( user.getLoginName(), user.getId());
		
		if ( user.hasEmail () )
			htEmail2UserIds.put ( user.getEmail().toLowerCase(), user.getId() );
	}
	
	
	/**
	 * is the given login name free for registration?
	 * @param login
	 * @return if the given login is free for registration
	 */
	public boolean isLoginFree ( String login ) {
		return !htLogin2UserIds.containsKey(login);
	}
	
	/**
	 * is the given email address free for registration?
	 * @param email
	 * @return returns if the given mail is free for registration
	 */
	public boolean isEmailFree ( String email ) {
		return ! htEmail2UserIds.containsKey(email.toLowerCase());
	}


	/**
	 * get the id of the {@link UserAgent} assigned to the given login name
	 * 
	 * @param login
	 * 
	 * @return	UserAgent id
	 * 
	 * @throws AgentNotKnownException
	 */
	public long getLoginId(String login) throws AgentNotKnownException {
		Long result = htLogin2UserIds.get ( login );
		if ( result != null)
			return result;
		
		throw new AgentNotKnownException("No agent registered for login: " + login );
	}

	/**
	 * get the id of the {@link UserAgent} assigned to the given email
	 * 
	 * @param email
	 * 
	 * @return	UserAgent id
	 * 
	 * @throws AgentNotKnownException
	 */
	public long getEmailId(String email) throws AgentNotKnownException {
		Long result = htEmail2UserIds.get ( email.toLowerCase() );
		if ( result != null)
			return result;
		
		throw new AgentNotKnownException("No agent registered for email: " + email );
	}

}
