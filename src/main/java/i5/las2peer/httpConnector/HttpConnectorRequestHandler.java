package i5.las2peer.httpConnector;

import i5.httpServer.HttpRequest;
import i5.httpServer.HttpResponse;
import i5.httpServer.RequestHandler;
import i5.las2peer.api.ConnectorException;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.execution.UnlockNeededException;
import i5.las2peer.httpConnector.coder.CodingException;
import i5.las2peer.httpConnector.coder.InvalidCodingException;
import i5.las2peer.httpConnector.coder.ParamCoder;
import i5.las2peer.httpConnector.coder.ParamDecoder;
import i5.las2peer.httpConnector.coder.ParameterTypeNotImplementedException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Hashtable;


/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer HTTP connector.
 * Each request will be distributed to its corresponding session.
 *
 * Current Problem (LAS related, maybe out-dated..):
 * This class will be used by an library (the HttpServer), so it has to be provided
 * as an library as well. To gain access to the configuration parameters the way
 * back to the service will be needed, but this is not allowed by the las class loaders.
 *
 * @author Holger Jan&szlig;en
 */



public class HttpConnectorRequestHandler implements RequestHandler {

	private static final String DEFAULT_DECODER_CLASS = i5.las2peer.httpConnector.coder.XmlDecoder.class.getName() ;
	//private static String decoderClass = DEFAULT_DECODER_CLASS;
	
	private static final String DEFAULT_CODER_CLASS = i5.las2peer.httpConnector.coder.XmlCoder.class.getName();
	//private static String coderClass = DEFAULT_CODER_CLASS;
	
	//private static final long DEFAULT_ANONYMOUS_ID = -1722613621014065292l; // anonymous.agent.xml
	//private long lAnonymousId = DEFAULT_ANONYMOUS_ID;
	
	//private static final String DEFAULT_ANONYMOUS_PASS = "anonymous";
	//private String sAnonymousPass = DEFAULT_ANONYMOUS_PASS;
	
	
	private Agent anonymous = null;
	
	// enable access to file system
	private static boolean enableFileAccess= false;
	
	// root directory for file access
	private static String fileAccessDirectory = null;
	
	
	private Hashtable<String, HttpSession> htSessions = null;
	
	private static final String SESSSION_GET_VAR = "SESSION";
	
	
	private HttpConnector connector = null;
	private Node l2pNode;
	
	/**
	 * Standard Constructor
	 *
	 */
	public HttpConnectorRequestHandler () {		
		htSessions = new Hashtable<String, HttpSession>();
		
		enableFileAccess = "true".equals (System.getProperty( "las.http.fileAccess" ));
		fileAccessDirectory = System.getProperty ( "las.http.fileAccessDir" );
	}
	
	
	/**
	 * set the connector handling this request processor
	 * @param connector
	 */
	public void setConnector ( HttpConnector connector ) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();
		
		anonymous = l2pNode.getAnonymous();
	}
	
	/**
	 * set the login data for the anonymous user
	 * (agent t use on an unauthenticated create session request)
	 * 
	 * @param agentId
	 * @param passphrase
	 */
	//public void setAnonymousLogin ( long agentId, String passphrase ) {
	//	lAnonymousId = agentId;
	//	sAnonymousPass = passphrase;
	//}
	
		
	/**
	 * The core method of a RequestHandler. For each request, this method will be called.
	 *
	 * @param    request                  a  HttpRequest
	 * @param    response                 a  HttpResponse
	 *
	 * @exception   Exception
	 *
	 */
	public void processRequest(HttpRequest request, HttpResponse response) throws Exception {
		response.setHeaderField( "Server-Name", "Las2peer 0.1" );
		
		connector.logMessage("request: " + request.getPath()+"?"+request.getQueryString());
		
		try
		{
			if ( request.getPath().equals ( "/createsession" ) ) {
				// open session request
				createSessionRequest ( request, response );
			} else if ( request.getPath().equals ( "/closesession" ) ) {
				// close session request
				closeSessionRequest ( request, response );
			} else if ( request.getPath().equals ( "/touchsession" ) ) {
				// request for session touching
				touchSessionRequest ( request, response );
			} else if ( request.getPath().equals( "/detachsession" ) ) {
				detachSessionRequest ( request, response );
			} else if ( request.getPath().equals ( "/attachsession" ) ) {
				attachSessionRequest ( request, response );
			} else if ( request.getPath().matches ( "/[^/]+/.*" ) )	{
				// invoke request
				invokeRequest ( request, response );
			} else {
				checkFileRequest ( request, response );
			}
		} catch (AddressNotAllowedException e) {
			response.setStatus ( HttpResponse.STATUS_FORBIDDEN );
			response.setContentType( "text/plain" );
			response.print ( "Access to this session not allowed from your host!" );
			connector.logMessage( "Access to session " + request.getGetVar ( SESSSION_GET_VAR ) + " not allowed from address " + request.getRemoteAddress() + "!" );
		}
		
	}
	
	/**
	 * Method checkFileRequest
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 */
	private void checkFileRequest(HttpRequest request, HttpResponse response) {
		if ( enableFileAccess && fileAccessDirectory != null ) {
			if ( request.getMethod() != HttpRequest.METHOD_GET ) {
				response.setStatus( HttpResponse.STATUS_NOT_FOUND );
				response.setContentType( "text/plain");
				response.print ( "This server supports only GET requests for the base directory!" );
				return;
			}
				
			
			File requested = new File ( fileAccessDirectory + File.separator + request.getPath() );
			if ( requested.exists() ) {
				// ending
				String sEnding = request.getPath().substring( request.getPath().lastIndexOf( "." ) +1);
				if ( sEnding.equals ( "xml" ) ) {
					response.setContentType( "text/xml" );
				} else if ( sEnding.equals ( "html" ) || sEnding.equals ( "htm" ) ) {
					response.setContentType ( "text/html" );
				} else {
					response.setContentType( "text/plain" );
				}
			
				try {
					// write contents to out
					BufferedReader contentReader = new BufferedReader ( new FileReader ( requested ));
					String line;
					while ( (line = contentReader.readLine()) != null ) {
						response.println( line );
					}
					contentReader.close();
					return;
				} catch ( IOException e ) {
					response.clearContent();
					response.setContentType( "text/plain" );
					response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
					response.println ( "Error reading file contents!" );
					return;
				}
			} else {
				response.setStatus( HttpResponse.STATUS_NOT_FOUND );
				response.setContentType( "text/plain");
				response.print ( "The document you requested cannot be found at this server!" );
				
				connector.logMessage( "File not Found: " + requested, HttpConnector.LOGLEVEL_NORMAL);
				return;
			}
		} else {
			// unknown or not processable request!
			response.setStatus( HttpResponse.STATUS_NOT_FOUND );
			response.setContentType( "text/plain" );
			response.println ( "File delivery is not enabled at this server. This is just an application server." );
			connector.logMessage( request.getRemoteAddress() + ": unknown request - " + request.getPath() );
		}
				
	}
	
	/**
	 * handles the request for a session touch
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 */
	private void touchSessionRequest(HttpRequest request, HttpResponse response) throws AddressNotAllowedException
	{
		String sid = request.getGetVar( SESSSION_GET_VAR );
		HttpSession session = htSessions.get ( sid ) ;
						
		if ( session == null ) {
			response.setStatus( HttpResponse.STATUS_UNAUTHORIZED );
			response.setContentType( "text/plain" );
			response.print ( "No corresponding session available!" );
			
			connector.logMessage ( request.getRemoteAddress() + ": touchsession request for unknown session " + sid );
			
			return;
		}

		session.checkRemoteAccess ( request );
		
		if( session.isExpired() ) {
			response.setStatus( HttpResponse.STATUS_PRECONDITION_FAILED );
			return;
		} else {
			session.touch();
			response.setStatus( HttpResponse.STATUS_NO_CONTENT );
		}
	}
	
	/**
	 * The method handling a request for closing a session
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 * @exception   AddressNotAllowedException 		access to the session not allowed (host mismatch)
	 *
	 */
	private void closeSessionRequest(HttpRequest request, HttpResponse response) throws AddressNotAllowedException {
		String sid = request.getGetVar ( SESSSION_GET_VAR );
		
		HttpSession session = htSessions.get ( sid ) ;
						
		if ( session == null ) 
			sendUnauthorizedResponse(
					response, 
					"No corresponding session available!", 
					request.getRemoteAddress() + ": closesession request for unknown session " + sid);
			
		session.checkRemoteAccess ( request );
		
		try {
			l2pNode.unregisterAgent(session.getAgentId());
		} catch (AgentNotKnownException e) {
			sendInternalErrorResponse(
					response, 
					"I have a session, but the corresponding user is not logged in to the las2peer net?!?!", 
					"strange problem: session exists, but agent not loaded at p2p node?!?!");			
		}
		
		session.endSession();
		htSessions.remove ( sid );
		response.println ( "Ok, session is closed!" );
		
		connector.logMessage ( request.getRemoteAddress() + ": closed session " + sid );
	}
	
	
	
	
	/**
	 * 
	 * @param result
	 * @param response
	 * @throws CodingException 
	 */
	private void sendInvocationSuccess ( Serializable result, HttpResponse response  ) throws CodingException {
		if ( result != null ) {
			response.setContentType( "text/xml" );
			String resultCode = getResultCode ( result );
			response.println ( resultCode );
		} else {
			response.setStatus( HttpResponse.STATUS_NO_CONTENT );
		}
	}
	

	/**
	 * method handling a request for a service method invocation
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 * @exception   AddressNotAllowedException 		access to the session not allowed (host mismatch)
	 *
	 */
	private void invokeRequest(HttpRequest request, HttpResponse response) throws AddressNotAllowedException {
		invokeRequest ( request, response, true );
	}
	
	/**
	 * method handling a request for a service method invocation
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 * @param	 retry				 retry (once, recursively) in specific conditions
	 *
	 * @exception   AddressNotAllowedException 		access to the session not allowed (host mismatch)
	 *
	 */
	private void invokeRequest(HttpRequest request, HttpResponse response, boolean retry) throws AddressNotAllowedException {
		String sid = request.getGetVar ( SESSSION_GET_VAR );
			
		HttpSession sess = null;
		if ( sid != null )
			sess = htSessions.get ( sid );
		
		if ( sess == null ) {
			sendUnauthorizedResponse(
					response, 
					"No corresponding session available!", 
					request.getRemoteAddress() + ": closesession request for unknown session " + sid);
			return;
		}
		
		// check whether the access to the session comes from the correct ip
		sess.checkRemoteAccess(request);
		
		if ( sess.isExpired() ) {
			response.setStatus( HttpResponse.STATUS_PRECONDITION_FAILED );
			return;
		}
		
		sess.touch();
	
		String[] sRequest = request.getPath().split ( "/", 3 );
		// first: empty (string starts with '/'
		// second: service name
		// third: method name
		
		
		try {
			try {
				Serializable result;				
				if ( request.getContentLength() > 0 )
					result= sess.getMediator ().invoke( sRequest[1], sRequest[2], decodeInvocationParameters ( request ), connector.preferLocalServices());
				else
					result= sess.getMediator ().invoke( sRequest[1], sRequest[2], new Serializable[0], connector.preferLocalServices() );
				
				sendInvocationSuccess ( result, response );
			} catch ( UnlockNeededException e ) {
				// k, unlocking of the agent's secret key at the target node is needed!
				if ( retry ) {
					l2pNode.sendUnlockRequest(sess.getAgentId(), sess.getStoredPass(), e.getRemoteNode(), e.getNodeKey());
					invokeRequest(request, response, false);
				} else
					sendSecurityProblems(request, response, sid, new L2pSecurityException ( "Mediation seems to have failed", e));
			}
		} catch ( NoSuchServiceException e ) {
			sendNoSuchService(request, response, sRequest);			
		} catch ( TimeoutException e ) {
			sendNoSuchService(request, response, sRequest);
		} catch ( NoSuchServiceMethodException e ) {
			sendNoSuchMethod(request, response, sid);
		} catch ( L2pSecurityException e ) {
			sendSecurityProblems(request, response, sid, e);					
		} catch ( ServiceInvocationException e ) {
			if ( e.getCause() == null )
				sendResultInterpretationProblems(request, response, sid);
			else
				sendInvocationException(request, response, sid, e);								
		} catch ( InterruptedException e ) {
			sendInvocationInterrupted(request, response);
		} catch ( InvalidCodingException e ) {
			sendConnectorProblems(request, response, sid, e);
		} catch ( ConnectorException e ) {
			sendConnectorProblems(request, response, sid, e);
		} catch ( CodingException e ) {
			sendResultInterpretationProblems(request, response, sid);
		}
	}

	
	/**
	 * send a response, that the connector has problems to interpret the 
	 * incoming invocation request
	 * 
	 * @param request
	 * @param response
	 * @param sid
	 * @param e
	 */
	private void sendConnectorProblems(HttpRequest request,
			HttpResponse response, String sid, Exception e) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_NOT_ACCEPTABLE );
		response.setContentType( "text/plain" );
		response.println ( "The invokation parameters could not be read!" );
		response.println ( "Exception-Message: " + e.getMessage() );
		connector.logMessage ( request.getRemoteAddress() + ": request coding exception in invokation request " + request.getPath() + " for session " + sid);
	}

	
	/**
	 * send a notification, that the processing of the invocation has been interrupted
	 * 
	 * @param request
	 * @param response
	 */
	private void sendInvocationInterrupted(HttpRequest request,
			HttpResponse response) {
		response.clearContent();
		response.setStatus (HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType ( "text/plain");
		response.println ( "The invoction has been interrupted!");
		connector.logMessage ( request.getRemoteAddress() + ": invocation has been interrupted!" );
	}


	/**
	 * send a notification about an exception which occurred inside the requested service method
	 * 
	 * @param request
	 * @param response
	 * @param sid
	 * @param e
	 */
	private void sendInvocationException(HttpRequest request,
			HttpResponse response, String sid, ServiceInvocationException e) {
		// internal exception in service method
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		connector.logMessage ( request.getRemoteAddress() + ": exception while processing RMI: " + request.getPath() + " in session " + sid );
		
		Object[] ret = new Object[4];
		ret[0] = "Exception during RMI invocation!";
		
		ret[1] = e.getCause().getCause().getClass().getCanonicalName();
		ret[2] = e.getCause().getCause().getMessage();
		ret[3] = e.getCause().getCause();
		try {
			String code = getResultCode( ret );
			response.println ( code );
		} catch (CodingException ce) {
			response.setContentType( "text/plain" );
			response.println ( "Unable to create exception xml answer!" + ce );
		}
	}


	/**
	 * send a notification, that the result of the service invocation is
	 * not transportable 
	 * 
	 * @param request
	 * @param response
	 * @param sid
	 */
	private void sendResultInterpretationProblems(HttpRequest request,
			HttpResponse response, String sid) {
		// result interpretation problems
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		response.println ("the result of the method call is not transferable!");
		connector.logMessage ( request.getRemoteAddress() + ": exception while processing RMI: " + request.getPath() + " in session " + sid );
	}


	/**
	 * send a notification, that security problems occurred during the requested service method
	 * @param request
	 * @param response
	 * @param sid
	 * @param e
	 */
	private void sendSecurityProblems(HttpRequest request,
			HttpResponse response, String sid, L2pSecurityException e) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_FORBIDDEN );
		response.setContentType( "text/plain" );
		response.println ( "You don't have access to the method you requested" );
		connector.logMessage ( request.getRemoteAddress() + ": security exception in invokation request " + request.getPath() + " in session " + sid );
		
		if ( System.getProperty("http-connector.printSecException") != null
				&& System.getProperty( "http-connector.printSecException").equals ( "true" ) )
			e.printStackTrace();
	}


	/**
	 * send a notification, that the requested method does not exists at the requested service
	 * @param request
	 * @param response
	 * @param sid
	 */
	private void sendNoSuchMethod(HttpRequest request, HttpResponse response,
			String sid) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_NOT_FOUND );
		response.setContentType( "text/plain" );
		response.println ( "The method you requested is not known to this service!" );
		connector.logMessage ( request.getRemoteAddress() + ": invocation request " + request.getPath() + " for unknown service method in session " + sid );
	}


	/**
	 * send a notification, that the requested service does not exists
	 * @param request
	 * @param response
	 * @param sRequest
	 */
	private void sendNoSuchService(HttpRequest request, HttpResponse response,
			String[] sRequest) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_SERVICE_UNAVAILABLE );
		response.setContentType( "text/plain" );
		response.println ( "The service you requested is not known to this server!" );
		
		connector.logMessage ( request.getRemoteAddress() + ": service not found: " + sRequest[1]);
	}


	
	/**
	 * encapsulation of decoding the invocation parameters from the content stream
	 * of the HTTP request.
	 *
	 * @param    request             a  HttpRequest
	 *
	 * @return   an Object[]
	 */
	private Serializable[] decodeInvocationParameters(HttpRequest request) throws ConnectorException, InvalidCodingException {
		String requestString = request.getContentString();

		// in the decoder a \n before each important tag is assumed
		// requestString = requestString..replaceAll( "<", "\n<" ).replaceAll ( "\n\n<", "\n<");
		
		//StringBufferInputStream requestBuffer = new StringBufferInputStream ( requestString );
		
		StringReader requestBuffer = new StringReader ( requestString );
		
		ParamDecoder decoder = null;
		try {
			@SuppressWarnings("rawtypes")
			Constructor constr = Class.forName( DEFAULT_DECODER_CLASS ).getConstructor ( new Class[] { Reader.class } );
			decoder = (ParamDecoder) constr.newInstance( new Object[] {(Reader) requestBuffer} );
		} catch (Exception e) {
			throw new ConnectorException ( "Unable to instanciate decoder class " + DEFAULT_DECODER_CLASS + "!" , e );
		}
		
		Serializable[] result = null;
		try {
			decoder.checkHeader();
			result = decoder.decodeArray();
			decoder.checkFooter();
		} catch (IOException e) {
			throw new ConnectorException ( "Error with the connections input stream", e );
		} catch ( NumberFormatException e ) {
			throw new ConnectorException ( "Error in with the input content", e);
		}
		
		return result;
	}
	
	/**
	 * method handling the request for a new session
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 */
	private void createSessionRequest(HttpRequest request, HttpResponse response) {
		String user = request.getGetVar ( "user" );
		String passwd = request.getGetVar ( "passwd" );
		String timeout = request.getGetVar ( "timeout" );
		String outDate = request.getGetVar ( "outdate" );
		String persistent = request.getGetVar ( "persistent" );
		
		try {
			boolean bPersistent = persistent != null && (persistent.equals ( "1" ) || persistent.equals ( "true" ));
			
			long userId;
			Agent userAgent;
			if ( user == null) {
				userAgent = anonymous;
			} else {
				if ( user.matches ("-?[0-9].*") ) {
					try {
						userId = Long.valueOf(user);
					} catch ( NumberFormatException e ) {
						throw new L2pSecurityException ("the given user does not contain a valid agent id!");
					}
				} else {
					userId = l2pNode.getAgentIdForLogin(user);
				}
				
				userAgent = l2pNode.getAgent(userId);
				if ( ! (userAgent instanceof PassphraseAgent ))
					throw new L2pSecurityException ( "Agent is not passphrase protected!");
				((PassphraseAgent)userAgent).unlockPrivateKey(passwd);
			}
			
			long lTimeout = connector.getDefaultSessionTimeout();
			if ( timeout != null)
				lTimeout = connector.getSesstionTimeout( Long.valueOf(timeout) );
			
			long persistentTimeout = connector.getDefaultPersistentTimeout();
			if ( outDate != null)
				persistentTimeout = connector.getPersistentSessionTimeout(Long.valueOf( outDate));

			
			Mediator mediator = l2pNode.getOrRegisterLocalMediator(userAgent);
			
			HttpSession session = new HttpSession (mediator, request, passwd );
			session.setTimeout(lTimeout);
			if (bPersistent) {
				session.setPersistent();
				session.setPersistentTimeout(persistentTimeout);
			}
			
			htSessions.put( session.getId(), session);
			
						
			// write answer
			response.setContentType( "text/xml" );
			response.println ( "<?xml version=\"1.0\"?>");
			response.println( session.toXmlString());
			connector.logMessage ( request.getRemoteAddress() + ": created session " + session.getId() );
		} catch (AgentNotKnownException e) {
			sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": login denied for user " + user);
		} catch (L2pSecurityException e) {
			sendUnauthorizedResponse( response, null, request.getRemoteAddress() + ": unauth access - prob. login problems");
		} catch (Exception e) {
			sendInternalErrorResponse(
					response, 
					"The server was unable to process your request because of an internal exception!", 
					"Exception in processing create session request: " + e);
		}
		
	}


	/**
	 * send a response that an internal error occurred
	 * 
	 * @param response
	 * @param answerMessage
	 * @param logMessage
	 */
	private void sendInternalErrorResponse(HttpResponse response,
			String answerMessage, String logMessage) {
		response.clearContent();
		response.setContentType( "text/plain" );
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.println ( answerMessage );
		connector.logMessage ( logMessage );
	}


	/**
	 * send a message about an unauthorized request
	 * @param response
	 * @param logMessage
	 */
	private void sendUnauthorizedResponse(HttpResponse response, String answerMessage,
			String logMessage) {
		response.clearContent();
		response.setContentType( "text/plain" );
		if ( answerMessage != null)
			response.println ( answerMessage );
		response.setStatus( HttpResponse.STATUS_UNAUTHORIZED );
		connector.logMessage ( logMessage  );
	}
	
		
	/**
	 * handles a detach session request
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 */
	private void detachSessionRequest(HttpRequest request, HttpResponse response) throws AddressNotAllowedException {
		String sid = request.getGetVar ( SESSSION_GET_VAR );
		HttpSession session = htSessions.get (sid );
		
		session.checkRemoteAccess(request);
		
		try {
			session.detach();
			session.touch();
		} catch ( ConnectorException e  ) {
			response.setStatus( HttpResponse.STATUS_BAD_REQUEST );
			response.setContentType( "test/plain" );
			response.println( "Your session is not persistent, so you cannot detach!" );
			connector.logMessage( request.getRemoteAddress() + ": attempt to detach from nonpersistent session!" );
			return;			
		}
		
		response.setStatus( HttpResponse.STATUS_OK );
		response.setContentType( "text/plain" );
		response.println( "You detached from your session!" );
		
		connector.logMessage ( "user detached from session "  +sid );
	}

	/**
	 * handles a attach session request
	 *
	 * @param    request             a  HttpRequest
	 * @param    response            a  HttpResponse
	 *
	 */
	private void attachSessionRequest(HttpRequest request, HttpResponse response) {
		String user = request.getGetVar ( "user" );
		String passwd = request.getGetVar ( "passwd" );
		String sid = request.getGetVar ( SESSSION_GET_VAR );
		
		HttpSession session = htSessions.get(sid);

		long agentId = Long.valueOf(user);
		
		if ( session == null) {
			response.setStatus (HttpResponse.STATUS_NOT_FOUND);
			connector.logMessage ( "Session " + sid + " could not be found!");
		} else if ( session.getAgentId() != agentId ) {
			response.setStatus ( HttpResponse.STATUS_UNAUTHORIZED );
			connector.logMessage ( "Incorrect user tried to attach to session " + sid );
		} else if ( session.isAttached() ) {
			// session is already open
			response.setStatus ( HttpResponse.STATUS_UNAUTHORIZED );
			connector.logMessage( "Attempt to open session " + sid + " a second time!" );
		} else if ( session.isOutdated ()  ) {
			response.setStatus ( HttpResponse.STATUS_PRECONDITION_FAILED  );
			connector.logMessage( "reattaching to session " + sid + " failed - session is outdated!");
			
			
		} else {
			try {
				Agent a = l2pNode.getAgent( Long.valueOf ( user ));
				((PassphraseAgent) a).unlockPrivateKey(passwd);
			} catch ( Exception e) {
				response.setStatus( HttpResponse.STATUS_UNAUTHORIZED );
				return;			
			}

			session.attach();
			session.touch();
			
			response.setStatus( HttpResponse.STATUS_OK );
			response.setContentType( "text/xml" );
			
			response.println( session.toXmlString());
			connector.logMessage( "Remote user attached successfully to session " + sid );
		}		
	}
	
	/**
	 * encapsulates the coding of a service method invocation result in terms
	 * of this protocol.
	 *
	 * @param    result         an Object
	 *
	 * @return   a String 		The coding of the resulting object as String to be
	 * 							send as http response content.
	 *
	 * @exception   ParameterTypeNotImplementedException 	The class of the result cannot be coded via this protocol
	 * @exception   ConnectorException 						Internal problems
	 *
	 */
	private String getResultCode ( Object result )
		throws CodingException
	{
		ParamCoder coder = null;
		
		StringWriter sw = new StringWriter ();
		
		try {
			@SuppressWarnings("rawtypes")
			Constructor constr = Class.forName( DEFAULT_CODER_CLASS ).getConstructor ( new Class[]{ Writer.class});
			coder = (ParamCoder) constr.newInstance ( new Object[] { sw } );
		} catch (Exception e) {
			throw new CodingException ( "Unable to instanciate coder " + DEFAULT_CODER_CLASS + "!", e );
		}
		
		try	{
			coder.header ( 1 );
			coder.write ( result );
			coder.footer ();
		} catch (IOException e) {
			throw new CodingException ( "Unable to transform coding into String", e );
		}
		
		return sw.toString();
	}
	
	
	
}



