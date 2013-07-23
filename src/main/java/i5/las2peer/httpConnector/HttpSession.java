package i5.las2peer.httpConnector;

import i5.httpServer.HttpRequest;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.security.Mediator;
import i5.las2peer.tools.SimpleTools;

import java.util.Date;
import java.util.HashSet;

/**
 * a session object storing all session relevant information for an HTTP connector session
 * 
 * @author Holger Jan&szlig;en
 *
 */
public class HttpSession {
	
	public static int SESSION_KEY_LENGTH = 20;
	

	private String sessionId;
	
	private long lastAccess = new Date().getTime();
	
	private boolean bPersistent = false;
	
	private boolean bIsAttached = true;
	
	
	private String remoteAddress = null;
	
	private Mediator agentMediator;
	
	private long timeout = -1;
	
	private long persistentTimeout = -1;
	
	
	private String storedPassphrase = "";
	
	
	/**
	 * create a new session object
	 * 
	 * @param mediator
	 * @param remoteAddress
	 */
	public HttpSession ( Mediator mediator, String remoteAddress, String pass ) {
		this.sessionId = createNewSessionId();
		this.remoteAddress = remoteAddress;
		
		agentMediator = mediator;
		this.storedPassphrase = pass;
	}
	
	/**
	 * create a new session object
	 * 
	 * @param mediator
	 * @param request
	 */
	public HttpSession ( Mediator mediator, HttpRequest  request, String pass ) {
		this ( mediator, getRemoteHost ( request ), pass);
	}
	
	/**
	 * set the timestamp of the last usage of this session
	 */
	public void touch() {
		lastAccess = new Date().getTime();
	}
	
	
	/**
	 * get the timestamp of the last usage 
	 * @return (unix) timestamp of the last session usage
	 */
	public long getLastAccess() {
		return lastAccess;
	}
	
	/**
	 * get the id of the agent
	 * @return logged in agent
	 */
	public long getAgentId() {
		return agentMediator.getResponsibleForAgentId();
	}
	
	/**
	 * get the mediator representing the remote user at the las2peer Node
	 * @return mediator at the las2peer node
	 */
	public Mediator getMediator () {
		return agentMediator;
	}
	
	/**
	 * is this session expired?
	 * 
	 * @return true, if this session is expired (not used for the timeout timespan)
	 */
	public boolean isExpired () {
		if ( timeout <= 0 )
			return false;
		
		return (new Date().getTime() - lastAccess) > timeout;
	}
	
	
	/**
	 * is the persistent part of this session timed out?
	 * @return true, if the persistent session is timed out
	 */
	public boolean isOutdated () {
		if ( persistentTimeout <= 0)
			return false;
		
		//System.out.println( new Date().getTime()  - lastAccess);
		
		return ( new Date().getTime() - lastAccess) > persistentTimeout;
	}
	
	/**
	 * set the timeout in milliseconds for the standard (non persistent part)
	 * @param timeout
	 */
	public void setTimeout ( long timeout ) {
		this.timeout = timeout;
	}
	
	/**
	 * set the timeout in ms for the persistent part
	 * @param timeout
	 */
	public void setPersistentTimeout ( long timeout ) {
		this.persistentTimeout = timeout;
	}
	
	/**
	 * get the session's expiration time in ms 
	 * @return expiration timeout
	 */
	public long getTimeout () {
		return timeout;
	}
	
	/**
	 * get the expiration time of the persistent session part
	 * @return timeout for persistent mode
	 */
	public long getPersistentTimeout () {
		return persistentTimeout;
	}
	
	
	/**
	 * has this session been initialized as persistent session
	 * (i.e. a session, where the remote client may detach from and the agent at the node will 
	 * still be working and collecting all incoming messages)
	 * 
	 * @return true, if this session may use the persistent mode
	 */
	public boolean isPersistent () {
		return bPersistent;
	}
	
	/**
	 * mark this session as persistent
	 */
	public void setPersistent () {
		bPersistent = true;
	}
	
	
	/**
	 * is the remote client currently attached to this session
	 * @return true if this session is currently attached 
	 */
	public boolean isAttached ( ){
		return bIsAttached;
	}
	
	/**
	 * notify the session, that the remote client has been detached
	 */
	public void detach () throws ConnectorException {
		if ( ! isPersistent() )
			throw new ConnectorException ( "This is not a persistent session - detaching is not possible!");
		bIsAttached = false;
	}
	
	/**
	 * notify this session, that the remote client has (re-)attached
	 */
	public void attach () {
		bIsAttached = true;
	}
	
	
	/**
	 * get the id of this session
	 * @return id of this session
	 */
	public String getId () {
		return sessionId;
	}
	
	/**
	 * kind of destructor, notify the session about the logout of the remote client
	 */
	public void endSession () {
		hsExistingSessionId.remove( this.getId() );
	}
	
	/**
	 * check, if the given address corresponds to the stored local address of this session
	 * 
	 * @param newRemoteAddress
	 * 
	 * @throws AddressNotAllowedException
	 */
	public void checkRemoteAccess ( String newRemoteAddress ) throws AddressNotAllowedException {
		if ( ! newRemoteAddress.equals( remoteAddress ) || remoteAddress == null )
			throw new  AddressNotAllowedException ( sessionId, newRemoteAddress);
	}
	
	/**
	 * check, if the given http request (it's ip resp.) may access this session
	 * @param request
	 * @throws AddressNotAllowedException
	 */
	public void checkRemoteAccess ( HttpRequest request ) throws AddressNotAllowedException {
		checkRemoteAccess ( getRemoteHost ( request ));
	}
	
	
	/**
	 * get the original passphrase which opened this session
	 */
	public String getStoredPass () {
		return storedPassphrase;
	}
	
	
	/**
	 * get a XML representation of this session
	 * @return XML string representing this session
	 */
	public String toXmlString() {
		StringBuffer result = new StringBuffer ();
				
		if ( isPersistent() )
			result.append ("<session persistent=\"true\">\n" );
		else
			result.append("<session>\n");
		result.append("\t<id>" + getId() + "</id>\n")
			.append( "\t<timeout>" + getTimeout() + "</timeout>\n" )
			.append ( "\t<user id=\"" + getAgentId() + "\" login=\"" + getAgentId() + "\" />\n" );
				
		if ( isPersistent() )
			result.append ("\t<outdate>" + getPersistentTimeout() + "</outdate>\n");
		
		result.append( "</session>\n" );
		
		return result.toString();
	}
	
	
	/******************** statics **********************************/
	
	private static HashSet<String> hsExistingSessionId = new HashSet<String> ();
	
	
	/**
	 * get the remote (ip) address of the client
	 * (e.g. to check next request)
	 * 
	 * @param request
	 * 
	 * @return remote host of this session  
	 */
	private static String getRemoteHost ( HttpRequest request ) {
		String result = request.getRemoteAddress().substring ( 0, request.getRemoteAddress().lastIndexOf(":") -1 );
		
		return result;
	}
	
	/**
	 * create a new Session id
	 * @return a new (not used) session id
	 */
	private static String createNewSessionId () {
		synchronized ( hsExistingSessionId ) {
			String suggestion;
			do {
				suggestion = SimpleTools.createRandomString(SESSION_KEY_LENGTH);
			} while ( hsExistingSessionId.contains(suggestion ));

			hsExistingSessionId.add( suggestion );
			return suggestion;
		}
	}
	
		
	
	
}
