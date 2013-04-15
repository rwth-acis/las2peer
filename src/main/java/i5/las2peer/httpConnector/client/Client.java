package i5.las2peer.httpConnector.client;

import i5.las2peer.httpConnector.coder.InvalidCodingException;
import i5.las2peer.httpConnector.coder.ParamCoder;
import i5.las2peer.httpConnector.coder.ParamDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The connector client is the basic class for accessing a remote LAS server
 * via the http connector within any java application.
 *
 * @author Holger Jan�en
 * @version $Revision: 1.3 $, $Date: 2013/02/12 17:53:23 $
 */

public class Client {
	public static final int DEFAULT_PORT = 8080;
	public static final long DEFAULT_TIMEOUT_MS = 60*60*10; // 10 minutes
	public static final long DEFAULT_OUTDATE_S  = 60 * 60 * 24; // 1 day
	
	
	public static final int WAIT_INTERVAL = 200;
	public static final int MAX_WAIT = 10*1000;
	
	public static final String DEFAULT_CODER = "i5.las2peer.httpConnector.coder.XmlCoder";
	public static final String DEFAULT_DECODER = "i5.las2peer.httpConnector.coder.XmlDecoder";
	
	private String coderClass = DEFAULT_CODER;
	private String decoderClass = DEFAULT_DECODER;
	
	private String sHost;
	private int iPort = DEFAULT_PORT;
	
	private String sUser = null;
	private String sPasswd = null;
	
	private long lTimeOutMs = DEFAULT_TIMEOUT_MS;
	
	private boolean bConnected = false;
	private String sSessionId = null;
	
	
	//private boolean bUsePersistent = false;
	private long lOutdateS = DEFAULT_OUTDATE_S;
	
	private boolean bTryPersistent = false;
	private boolean bIsPersistent = false;
	
	private boolean bUseHttps = false;
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 *
	 */
	public Client ( String host ) {
		sHost = host;
	}
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 * @param    port                http connector port on the target host
	 *
	 */
	public Client ( String host, int port ) {
		sHost = host;
		iPort = port;
	}
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 * @param    port                http connector port on the target host
	 * @param    user                login name
	 * @param    passwd              password
	 *
	 */
	public Client ( String host, int port, String user, String passwd ) {
		this ( host, port );
		sUser = user;
		sPasswd = passwd;
	}
	
	
	
	/**
	 * Constructor for reataching to a remote session
	 *
	 * @param    host                a  String
	 * @param    port                an int
	 * @param    user                a  String
	 * @param    passwd              a  String
	 * @param    session             a  String
	 *
	 */
	public Client ( String host, int port, String user, String passwd, String session ) {
		this ( host, port, user, passwd );
		
		this.sSessionId = session;
		this.bTryPersistent = true;
	}
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 * @param    port                http connector port on the target host
	 * @param    timeout             timeout of the generated session in milliseconds
	 *
	 */
	public Client ( String host, int port, long timeout ) {
		sHost = host;
		iPort = port;
		lTimeOutMs = timeout;
	}
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 * @param    timeout             timeout of the generated session in milliseconds
	 *
	 */
	public Client ( String host, long timeout ) {
		sHost = host;
		lTimeOutMs = timeout;
	}
	
	/**
	 * Constructor
	 *
	 * @param    host                target host name
	 * @param    port                http connector port on the target host
	 * @param    timeout             timeout of the generated session in milliseconds
	 * @param    user                login name
	 * @param    passwd              password
	 *
	 */
	public Client ( String host, int port, long timeout, String user, String passwd ) {
		sHost = host;
		iPort = port;
		lTimeOutMs = timeout;
		sUser = user;
		sPasswd = passwd;
	}
	
	
	/**
	 * set the password for the current user
	 *
	 * @param    passwd                 new password to use
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setPasswd ( String passwd ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );
		
		sPasswd = passwd;
	}
	
	
	/**
	 *
	 * @return   name of the las user
	 *
	 */
	public String getUser () {
		return sUser;
	}
	
	
	/**
	 * set the user login name for connecting to the remote las server
	 *
	 * @param    user                a  String
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setUser ( String user ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );

		sUser = user;
	}
	
	/**
	 *
	 * @return   name or ip of the las server host
	 *
	 */
	public String getHost () {
		return sHost;
	}
	
	/**
	 * Set the target host of the las server to connect to
	 *
	 * @param    host                new hostname
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setHost ( String host ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );

		sHost = host;
	}
	
	/**
	 *
	 * @return   the number of the used port at the las server
	 *
	 */
	public int getPort () {
		return iPort;
	}
	
	
	/**
	 * Set the port of the connector
	 *
	 * @param    port                new port number
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setPort ( int port ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );
		
		iPort = port;
	}
	
	
	
	/**
	 * set the flag if the client is to try to open persistent sessions
	 *
	 * @param    tryP                a  boolean
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setTryPersistent ( boolean tryP ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );

		bTryPersistent = tryP;
	}
	
	
	/**
	 * returns of the client tries to open persistent sessions
	 *
	 * @return   a boolean
	 *
	 */
	public boolean getTryPersistent ()  {
		return bTryPersistent;
	}
	
	
	/**
	 * returns true if the client is connected and the session is persistent
	 *
	 * @return   a boolean
	 *
	 */
	public boolean isPersistent () {
		return bConnected && bIsPersistent;
	}
	
	
	/**
	 * return the (tried or real) session timeout in ms
	 *
	 * @return   a long
	 *
	 */
	public long getSessionTimeout () {
		return lTimeOutMs;
	}
	

	/**
	 * Set the session timeout value to be requested on the next connection opening
	 *
	 * @param    time                timespan in ms
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setSessionTimeout ( long time ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );
		
		lTimeOutMs = time;
	}
	
	/**
	 * return the (tried or real) session outdate time in s
	 *
	 * @return   a long
	 *
	 */
	public long getSessionOutdate () {
		return lOutdateS;
	}
	
	/**
	 * set the outedate time to use for opening new sessions
	 *
	 * @param    outdate             timespan in s
	 *
	 * @exception   ConnectorClientException 	client is currently connected
	 *
	 */
	public void setSessionOutdate ( long outdate ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change the client setting during a session!" );

		lOutdateS = outdate;
	}

	
	/**
	 * returns if the client uses an https connection to the remote las
	 *
	 * @return   a boolean
	 *
	 */
	public boolean isUsingHttps() {
		return bUseHttps;
	}
	
	
	/**
	 * change the setting for the usage of the https protocol
	 *
	 * @param    use                 a  boolean
	 *
	 * @exception   ConnectorClientException
	 *
	 */
	public void setUseHttps ( boolean use ) throws ConnectorClientException {
		if ( bConnected )
			throw new ConnectorClientException ( "Don't change client settings during a session!" );
		
		bUseHttps = use;
	}
	
	/**
	 * Tries to connect to the las server with the current connection data.
	 *
	 * This method will be implicitly called on an attepmt to use a not existing connection.
	 *
	 * @exception   AuthenticationFailedException
	 * @exception   UnableToConnectException
	 *
	 */
	public void connect () throws AuthenticationFailedException, UnableToConnectException {
		if ( bConnected )
			return;
		
		if ( sSessionId != null )
			reattachToSession();
		else
			createSession();
	}
		
	
	/**
	 * Tries to open a new session with the current connection data
	 *
	 * @exception   UnableToConnectException
	 *
	 */
	private void createSession () throws UnableToConnectException, AuthenticationFailedException {
		if ( bConnected ) return;
		bIsPersistent = false;
		
		String sProtocol = (bUseHttps) ? "https://" : "http://";
		String sTimeout = "timeout=" + lTimeOutMs;
		
		try {
			String sUrl = "";
			if ( sUser == null )
				sUrl = sProtocol + sHost + ":" + iPort + "/createsession?" + sTimeout ;
			else
				sUrl = sProtocol + sHost + ":" + iPort + "/createsession?user=" + sUser + "&passwd=" + sPasswd + "&" + sTimeout;

			if ( bTryPersistent ) {
				if ( sUser == null )
					sUrl += "?";
				else
					sUrl += "&";
				sUrl += "persistent=1&outdate=" + lOutdateS;
			}

			URL url = new URL ( sUrl );
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			int length = conn.getContentLength();
			
			if ( conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED )
				throw new AuthenticationFailedException();
			
			String type = conn.getContentType();
			
			if ( ! "text/xml".equals ( type ) )
				throw new UnableToConnectException ( "Invalid Server answer: " + type + " - is this a las server?" );
			
			String content = readHttpContent ( (InputStream) conn.getContent(), length );
			try {
				interpretSessionContent ( content );
			} catch (InvalidServerAnswerException e) {
				throw new UnableToConnectException ( "Problems interpreting server response to create session request!", e );
			}
			bConnected = true;
		} catch (IOException e) {
			throw new UnableToConnectException ( e );
		} catch ( UnableToConnectException e ) {
			if ( bTryPersistent ) {
				// try a non-persistent session!
				bTryPersistent = false;
				createSession();
			} else {
				throw e;
			}
			
		}
	}
	
	
	/**
	 * tries to reattach to an existing (persitent) session using the current
	 * connection data
	 *
	 */
	private void reattachToSession () throws UnableToConnectException {
		if ( bConnected ) return;
		
		String sProtocol = (bUseHttps) ? "https://" : "http://";
		
		if ( sUser == null || sPasswd == null )
			throw new UnableToConnectException ( "No user / Password given for reattaching" );
		if ( sSessionId == null )
			throw new UnableToConnectException ( "No session id given for reattaching" );
		
		try {
			URL url = new URL ( sProtocol + sHost + ":" + iPort + "/attachsession?user=" + sUser + "&passwd=" + sPasswd + "&SESSION=" + sSessionId );
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK )
				throw new UnableToConnectException ( "Unable to connect to remote session - response code: " + conn.getResponseCode() ) ;
			
			bConnected = true;
			bIsPersistent = true;
		} catch (IOException e) {
			throw new UnableToConnectException ( "IOException during connection attempt!", e );
		}
	}
	
	
	
	/**
	 * disconnects an open connection
	 *
	 * @exception   InvalidServerAnswerException
	 * @exception   UnableToConnectException
	 *
	 */
	public void disconnect () throws InvalidServerAnswerException, UnableToConnectException {
		if ( ! bConnected ) return;

		String sProtocol = (bUseHttps) ? "https://" : "http://";
		
		try {
			URL url = new URL ( sProtocol + sHost + ":" + iPort + "/closesession?SESSION=" + sSessionId );
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//int length = conn.getContentLength();
			
			if ( conn.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED ) {
				// session expired
			} else if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK ) {
				throw new InvalidServerAnswerException ( "Returncode from server: " + conn.getResponseCode() );
			}
			
			bConnected = false;
			sSessionId = null;
		} catch (IOException e) {
			bConnected = false;
			throw new UnableToConnectException ( e );
		}
	}
	
	/**
	 * tries to detach from the current session
	 *
	 * @exception   InvalidServerAnswerException
	 * @exception   ConnectorClientException
	 *
	 */
	public void detach () throws InvalidServerAnswerException, ConnectorClientException {
		if ( ! bConnected ) return;

		String sProtocol = (bUseHttps) ? "https://" : "http://";

		try {
			URL url = new URL ( sProtocol + sHost + ":" + iPort + "/detachsession?SESSION=" + sSessionId );
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			if ( conn.getResponseCode() != HttpURLConnection.HTTP_OK )
				throw new ConnectorClientException ("Unable to detach - response code: " + conn.getResponseCode() );
			else
				bConnected = false;
		} catch (IOException e) {
			throw new ConnectorClientException ("Unable to detach - IOException in connection",  e );
		}
	}
	
	
	
	
	/**
	 * reads the content of an http answer into a resulting string
	 * the length of the expected content if given by the length parameter
	 *
	 * @param    content             an InputStream
	 * @param    length              an int
	 *
	 * @return   a String
	 *
	 */
	private String readHttpContent(InputStream content, int length) throws UnableToConnectException, IOException {
		
		int iWait = 0;
		while ( content.available() < length ) {
			iWait += WAIT_INTERVAL;
			try {
				Thread.sleep ( WAIT_INTERVAL );
			} catch (InterruptedException e) {}
			
			if ( iWait >= MAX_WAIT )
				throw new UnableToConnectException ( "Timeout at " + iWait + " milliseconds!" );
		}
		
		InputStreamReader isr = new InputStreamReader ( content );
		char[] con = new char[length];
		isr.read ( con );
		String sContent = new String ( con );
		
		return sContent;
	}
	
	
	/**
	 * Set the code class to be used for encoding message parameters
	 *
	 * @param    className           a  String
	 *
	 */
	public void setCoderClass ( String className ) {
		coderClass = className;
	}
	
	/**
	 * returns the currently used coder class
	 *
	 * @return   a String
	 *
	 */
	public String getCoder () {
		return coderClass;
	}
	
	/**
	 * returns if the client is currently connected
	 *
	 * @return   a boolean
	 *
	 */
	public boolean isConnected () {
		return isConnected( false );
	}
	
	
	/**
	 * returns if the client is currently connected
	 *
	 * depending on the tryTouch parameter a touchSession is invoked before
	 * returning the connective flag
	 *
	 * @param    tryTouch            a  boolean
	 *
	 * @return   a boolean
	 *
	 */
	public boolean isConnected ( boolean tryTouch ) {
		try {
			if ( tryTouch )
				touchSession();
		} catch (ConnectorClientException e) {
			bConnected = false;
			return false;
		}
		
		return bConnected;
	}
	
	/**
	 * returns the id of the currently used session at the server
	 *
	 * @return   a String
	 *
	 */
	public String getSessionId () {
		return sSessionId;
	}
	
	/**
	 * returns the timeout in milliseconds of the currently open session.
	 *
	 * @return   a long
	 *
	 */
	public long getTimeoutMs () {
		return lTimeOutMs;
	}
	
	
	/**
	 * Invokes a service method at the server. If not connected a connections
	 * attempt will be performed. The result of the call will be returned as an
	 * object.
	 *
	 *
	 * @param    service             a  String
	 * @param    method              a  String
	 * @param    params              an Object[]
	 *
	 * @return   an Object
	 *
	 * @exception   UnableToConnectException
	 * @exception   AuthenticationFailedException
	 * @exception   TimeoutException
	 * @exception   ParameterTypeNotImplementedException
	 * @exception   ServerErrorException
	 * @exception   AccessDeniedException
	 * @exception   NotFoundException
	 * @exception   ConnectorClientException
	 *
	 */
	public Object invoke ( String service, String method, Object... params )
		throws UnableToConnectException, AuthenticationFailedException, TimeoutException,
				ServerErrorException, AccessDeniedException,
				NotFoundException, ConnectorClientException
	{
		if ( ! bConnected )
			connect();

		try {
			URL url = new URL ( (bUseHttps) ? "https" : "http" , sHost, iPort, "/"+ service + "/" + method + "?SESSION=" + sSessionId );
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty( "Content-Type", "text/xml" );

			if ( params != null && params.length > 0 ) {
				String paramCode = getParameterCoding( params );

				// ok, code the parameters into a post call
				connection.setDoOutput( true );
				
				OutputStreamWriter osw = new OutputStreamWriter ( connection.getOutputStream() );
				osw.write( paramCode );
				
				osw.flush ();
				osw.close();
			}
			
			int responseCode = connection.getResponseCode();
			
			if ( responseCode == HttpURLConnection.HTTP_FORBIDDEN )
				throw new AccessDeniedException() ;
			
			if ( responseCode == HttpURLConnection.HTTP_NOT_FOUND // method unavailable
				|| responseCode == 503 )   // service unavailable
				throw new NotFoundException ();
			
			if ( responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR ) {
				String mess = "Remote Exception during invokation";
				try {
					if ( "text/xml".equals ( connection.getContentType() ) ) {
						// we have a object array response describing the exception
						Object[] result = (Object[]) interpretInvokationResult( (InputStream) connection.getErrorStream() );
						throw new ServerErrorException ( (Exception) result[3] );
					} else
						// simple text message (to stay compatible to older versions of the connector
						mess = readHttpContent( (InputStream) connection.getErrorStream(), connection.getContentLength() );
				} catch ( ServerErrorException e ) {
					throw e;
				} catch ( Exception e ) {
					e.printStackTrace();
					mess += "Unable to create cause Exception: " + e;
				}
				
				throw new ServerErrorException (mess);
			}
			
			if ( responseCode == HttpURLConnection.HTTP_PRECON_FAILED )
				throw new TimeoutException ();
			
			if ( responseCode == HttpURLConnection.HTTP_NO_CONTENT )
				return null;
			
			if ( responseCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE )
				throw new ConnectorClientException ( "The server could no read the invokation parameters!" );
			
			if ( responseCode == HttpURLConnection.HTTP_NOT_IMPLEMENTED ) {
				//String mess = ""; //readHttpContent( (InputStream) connection.getContent(), connection.getContentLength() );
				throw new ReturnTypeNotImplementedException ();
			}
			
			String type = connection.getContentType();
			//int length = connection.getContentLength();
			if ( ! "text/xml".equals ( type ) )
				throw new ConnectorClientException ( "Problems to interpret the server's answer - content type not text/xml!");

			Object result = interpretInvokationResult( (InputStream) connection.getContent() );
			
			return result;
		} catch ( IOException e ) {
			bConnected = false;
			throw new UnableToConnectException ( "IOException with the connection!", e );
		}

	}
	
	
	
	/**
	 * writes an encoding of the object parameter array to the outputStream
	 *
	 * @param    params              an Object[]
	 *
	 * @exception   ParameterTypeNotImplementedException
	 * @exception   UnableToLoadCoderException
	 * @exception   IOException
	 *
	 */
	public String getParameterCoding ( Object[] params )
		throws ParameterTypeNotImplementedException, IOException, ConnectorClientException
	{
		try {
			ParamCoder coder = null;
			
			StringWriter sw = new StringWriter ();
			
			try {
				@SuppressWarnings("rawtypes")
				Constructor constr = Class.forName ( coderClass ).getConstructor ( new Class[] { Writer.class } ) ;
				coder = (ParamCoder) constr.newInstance( new Object[]{ sw } ) ;
			} catch (Exception e) {
				throw new ConnectorClientException ( "Unable to loadecoader!", e );
			}
			
			coder.header( params.length );
			
			for ( int i=0; i<params.length; i++ ) {
				coder.write ( params[i] );
			}
			
			coder.footer();
			
			return sw.toString();
		} catch (i5.las2peer.httpConnector.coder.ParameterTypeNotImplementedException e) {
			throw new ParameterTypeNotImplementedException ( "One or more of the invokation parameters could not be coded for transfer!", e );
		}
	}
	
	
	/**
	 * tries to touch the session at the server
	 *
	 * @exception   ConnectorClientException
	 *
	 */
	public void touchSession () throws ConnectorClientException {
		if ( ! bConnected )
			return;
		
		try {
			URL url = new URL ( bUseHttps ? "https": "http", sHost, iPort, "/touchsession?SESSION=" + sSessionId );
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			//int length = conn.getContentLength();
			
			int response = conn.getResponseCode() ;
			
			if ( response == 401 ) { // 401 = UNAUTHORIZED
				throw new ConnectorClientException ( "The Session I tried to access does not exist!!" );
			} else if ( response != HttpURLConnection.HTTP_NO_CONTENT ) {
				throw new ConnectorClientException ( "Unkown Answer!" );
			}
			
			// ok, touch was successfull
		} catch (IOException e) {
			bConnected = false;
			throw new ConnectorClientException ( "I/O-Exception in the http connection!", e );
		}
	}
		
	
	/**
	 * interprets the content of a open session request an sets the attributes of the connection
	 *
	 * @param    content             a  String
	 *
	 * @exception   InvalidServerAnswerException
	 *
	 */
	private void interpretSessionContent ( String content ) throws InvalidServerAnswerException {
		
		String[] lines = content.split ( "\\s*\r?\n\\s*" );
		
		if ( ! lines[0].matches( "<\\?xml\\s+version=\"1.0\"\\s*\\?>" ) )
			throw new InvalidServerAnswerException ( "answer is not xml conform ( lacking header )" );
		
		if ( lines[1].matches ( "<session persistent=\"true\">" ) )
			bIsPersistent = true;
		else if ( ! lines[1].trim().matches( "<session>" ))
			throw new InvalidServerAnswerException ( "answer has not the expected root node (<session>)" + lines [1] + "..." + content );
					
		Matcher m = Pattern.compile ( "<id>([^>]+)</id>").matcher ( lines[2] );
		if ( m.matches () ) {
			sSessionId = m.group(1);
		} else
			throw new InvalidServerAnswerException ( "first element of session is not the id!" );

		m = Pattern.compile ( "<timeout>([0-9]+)</timeout>").matcher ( lines[3] ) ;
		if ( m.matches () ) {
			lTimeOutMs = Long.valueOf( m.group(1)).longValue();
		} else
			throw new InvalidServerAnswerException ( "Second element of session is not the timeout!" );
		
		m = Pattern.compile ( "<outdate>([0-9]+)</outdate>").matcher ( lines[4] );
		if ( m.matches () ) {
			lOutdateS = Long.valueOf ( m.group(1)).longValue();
		}
	}
		
	
	/**
	 * tries to interpret the content of the urlConnections (given as InputStream) either as a
	 * single object or an array.
	 *
	 * @param    content             an InputStream
	 *
	 * @return   an Object
	 *
	 * @exception   ConnectorClientException
	 * @exception   IOException
	 * @exception   InvalidCodingException
	 *
	 */
	private Object interpretInvokationResult ( InputStream content ) throws ConnectorClientException {
		ParamDecoder decoder = null;
		try {
			@SuppressWarnings("rawtypes")
			Constructor constr = Class.forName( decoderClass).getConstructor ( new Class[] { InputStream.class } );
			decoder = (ParamDecoder) constr.newInstance( new Object[] { content } );
		} catch (Exception e) {
			throw new ConnectorClientException ( "Unable to instanciate decoder class " + decoderClass + "!" , e );
		}
		
		Object result = null;
		try {
			int count = decoder.checkHeader();
			if ( count != 1)
				result = decoder.decodeArray();
			else
				result = decoder.decodeSingle();
			decoder.checkFooter();
		} catch (IOException e) {
			throw new ConnectorClientException ( "Error with the connections input stream", e );
		} catch ( InvalidCodingException e ) {
			throw new ConnectorClientException ( "Response of the server is not interpretable as an Object!", e );
		}
		
		return result;
	}
	
		
	
		
}


