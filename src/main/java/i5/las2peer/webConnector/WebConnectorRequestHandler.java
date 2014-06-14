package i5.las2peer.webConnector;

import i5.httpServer.HttpRequest;
import i5.httpServer.HttpResponse;
import i5.httpServer.RequestHandler;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
//import rice.p2p.util.Base64;
import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.tools.CryptoTools;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;

import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCAccessTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;


/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web connector.
 * Each request will be distributed to its corresponding session.
 *
 * @author Holger Jan&szlig;en
 * @Author Alexander Ruppert
 * @author Dominik Renzel
 * 
 */



public class WebConnectorRequestHandler implements RequestHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private WebConnector connector;
	private Node l2pNode;

	// some static Open ID Connect URIs for testing purposes
	// should be rather configured via Web Connector properties

	// URIs of OIDC server endpoints (authorization, token, userinfo)
	private URI authEndpointUri, tokenEndpointUri, userinfoEndpointUri;

	// Registered client ID, secret and redirect URI
	private ClientID clientID;
	private Secret clientSecret;
	private URI redirectURI;


	/**
	 * Standard Constructor
	 * @throws URISyntaxException 
	 *
	 */
	public WebConnectorRequestHandler () throws URISyntaxException {		

		authEndpointUri = new URI("http://137.226.58.15:9085/openid-connect-server-webapp/authorize/");
		tokenEndpointUri = new URI("http://137.226.58.15:9085/openid-connect-server-webapp/token");
		userinfoEndpointUri = new URI("http://137.226.58.15:9085/openid-connect-server-webapp/userinfo");
		redirectURI = new URI("http://localhost:8080/oidc/redirect");

		// Registered client ID, secret and redirect URI
		clientID = new ClientID("51dc0d91-57dd-4706-88b8-022808915ca0");
		clientSecret = new Secret("clobC9RK79bp3gBwWhUNdOft0husFHDQb8S4EEaCclXanXRUuSb3EhvV41V6XOW4K-RKnCRJ-J8XS93RwS3SQg");

	}


	/**
	 * set the connector handling this request processor
	 * @param connector
	 */
	public void setConnector ( WebConnector connector ) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();		
	}

	/**
	 * Logs in a las2peer user
	 * @param request
	 * @param response
	 * @return -1 if no successful login else userId
	 * @throws UnsupportedEncodingException
	 */
	private PassphraseAgent authenticate (HttpRequest request, HttpResponse response) throws UnsupportedEncodingException
	{

		final int BASIC_PREFIX_LENGTH="BASIC ".length();
		String userPass="";
		String username="";
		String password="";

		//Check for authentication information in header
		if(request.hasHeaderField(AUTHENTICATION_FIELD)
				&&(request.getHeaderField(AUTHENTICATION_FIELD).length()>BASIC_PREFIX_LENGTH))
		{
			//looks like: Authentication Basic <Byte64(name:pass)>
			userPass=request.getHeaderField(AUTHENTICATION_FIELD).substring(BASIC_PREFIX_LENGTH);
			userPass=new String(Base64.decode(userPass), "UTF-8");
			int separatorPos=userPass.indexOf(':');

			//get username and password
			username=userPass.substring(0,separatorPos);
			password=userPass.substring(separatorPos+1);


			return login(username,password,request,response);

		}//no information? check if there is a default account for login
		else if(connector.defaultLoginUser.length()>0)
		{
			return login(connector.defaultLoginUser,connector.defaultLoginPassword,request,response);
		}
		else
		{
			sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": No Authentication provided!");
		}
		return null;
	}

	private PassphraseAgent login(String username, String password, HttpRequest request, HttpResponse response)
	{
		try
		{

			long userId;
			PassphraseAgent userAgent;

			if ( username.matches ("-?[0-9].*") ) {//username is id?
				try {
					userId = Long.valueOf(username);
				} catch ( NumberFormatException e ) {
					throw new L2pSecurityException ("The given user does not contain a valid agent id!");
				}
			} else {//username is string
				userId = l2pNode.getAgentIdForLogin(username);
			}

			userAgent = (PassphraseAgent)l2pNode.getAgent(userId);

			/* if ( ! (userAgent instanceof PassphraseAgent ))
                throw new L2pSecurityException ("Agent is not passphrase protected!");*/

			userAgent.unlockPrivateKey(password);

			//connector.logMessage("Login: "+username);
			//connector.logMessage("successful login");
			// Thread.sleep(10); //TODO: find out how to avoid this 'hack'


			return userAgent;

		}catch (AgentNotKnownException e) {
			sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": login denied for user " + username);
		} catch (L2pSecurityException e) {
			sendUnauthorizedResponse( response, null, request.getRemoteAddress() + ": unauth access - prob. login problems");
		} catch (Exception e) {

			sendUnauthorizedResponse(response, null, request.getRemoteAddress() + ": something went horribly wrong. Check your request for correctness.");
		}
		return null;
	}
	/**
	 * Delegates the request data to a service method, which then decides what to do with it (maps it internally)
	 * @param request
	 * @param response
	 * @return
	 */
	private boolean invoke(PassphraseAgent userAgent, HttpRequest request, HttpResponse response) {


		response.setStatus(HttpResponse.STATUS_INTERNAL_SERVER_ERROR); //internal server error unless otherwise specified (errors might occur)
		String[] requestSplit=request.getPath().split("/",2);
		// first: empty (string starts with '/')
		// second: URI



		String uri="";
		String content="";

		try {



			if(requestSplit.length>=2)
			{
				int varsstart=requestSplit[1].indexOf('?');
				if(varsstart>0)
					uri=requestSplit[1].substring(0,varsstart);
				else
					uri=requestSplit[1];
			}

			//http body
			content=request.getContentString();

			if(content==null)
				content="";
			//http method
			int httpMethodInt=request.getMethod();
			String httpMethod="get";

			switch (httpMethodInt) 
			{
			case HttpRequest.METHOD_GET:
				httpMethod="get";
				break;
			case HttpRequest.METHOD_HEAD:
				httpMethod="head";
				break;
			case HttpRequest.METHOD_DELETE:
				httpMethod="delete";
				break;
			case HttpRequest.METHOD_POST:
				httpMethod="post";
				break;
			case HttpRequest.METHOD_PUT:
				httpMethod="put";
				break;
			default:
				break;
			}

			ArrayList<Pair<String>> variablesList=new ArrayList<Pair<String>>();
			@SuppressWarnings("rawtypes")
			Enumeration en = request.getGetVarNames();		

			while(en.hasMoreElements())
			{				
				String param = (String) en.nextElement();
				String val= request.getGetVar(param);
				Pair<String> pair= new Pair<String>(param,val);	
				variablesList.add(pair);				
			}
			@SuppressWarnings("unchecked")
			Pair<String>[] variables=variablesList.toArray(new Pair[variablesList.size()]);

			ArrayList<Pair<String>> headersList=new ArrayList<Pair<String>>();


			en = request.getHeaderFieldNames();

			String acceptHeader="*/*";
			String contentTypeHeader="text/plain";
			while(en.hasMoreElements())
			{
				String param = (String) en.nextElement();

				String val= request.getHeaderField(param);
				Pair<String> pair= new Pair<String>(param,val);
				headersList.add(pair);

				//fetch MIME types
				if(param.equals("accept") && !val.trim().isEmpty())
					acceptHeader=val.trim();
				if(param.equals("content-type")&& !val.trim().isEmpty())
				{
					contentTypeHeader=val.trim();

				}

			}


			@SuppressWarnings("unchecked")
			Pair<String>[] headers=headersList.toArray(new Pair[headersList.size()]);

			//connector.logMessage(httpMethod+" "+request.getUrl());

			//Serializable[] parameters={httpMethod,restURI,variables,content};

			Serializable result="";

			Mediator mediator = l2pNode.getOrRegisterLocalMediator(userAgent);

			boolean gotResult=false;

			String returnMIMEType="text/plain";
			StringBuilder warnings = new StringBuilder();
			InvocationData[] invocation =RESTMapper.parse(this.connector.getMappingTree(), httpMethod, uri, variables, content,contentTypeHeader,acceptHeader,headers,warnings);

			if(invocation.length==0)
			{
				response.setStatus(404);
				if(warnings.length()>0)
				{
					response.setContentType( "text/plain" );

					response.println(warnings.toString().replaceAll("\n"," "));
				}
				return false;
			}

			for (int i = 0; i < invocation.length; i++) {
				try
				{

					result= mediator.invoke(invocation[i].getServiceName(),invocation[i].getMethodName(), invocation[i].getParameters(), connector.preferLocalServices());// invoke service method
					gotResult=true;
					returnMIMEType=invocation[i].getMIME();
					break;

				} catch ( NoSuchServiceException | TimeoutException e ) {

					sendNoSuchService(request, response, invocation[i].getServiceName());
				}
				catch ( NoSuchServiceMethodException e ) {

					sendNoSuchMethod(request, response);
				} catch ( L2pSecurityException e ) {
					sendSecurityProblems(request, response, e);					
				} catch ( ServiceInvocationException e ) {

					if ( e.getCause() == null ){
						sendResultInterpretationProblems(request, response);
					}else{
						sendInvocationException(request, response, e);}
				} catch ( InterruptedException e ) {
					sendInvocationInterrupted(request, response);						

				}
			}


			if (gotResult)
				sendInvocationSuccess ( result, returnMIMEType, response );



			//}
			return true;

		} catch ( NoMethodFoundException | NotSupportedUriPathException e ) {
			sendNoSuchMethod(request, response);	
		}
		catch (Exception e){


			// System.out.println(((UserAgent) userAgent).getLoginName());


			//e.printStackTrace();
			connector.logError("Error occured:" + request.getPath()+" "+e.getMessage() );
		}
		return false;
	}

	/**
	 * Logs the user out	 
	 * @param userAgent
	 */
	private void logout(PassphraseAgent userAgent)
	{
		try {


			//if(userAgent!=null)

			//TODO check
			//l2pNode.unregisterAgent(userAgent);
			userAgent.lockPrivateKey();//don't know if really necessary


		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Handles a request (login, invoke)
	 */
	@Override
	public void processRequest(HttpRequest request, HttpResponse response) throws Exception {
		response.setHeaderField( "Server-Name", "LAS2peer" );
		response.setContentType( "text/xml" );

		// Process Open ID Connect login 
		if(request.getMethod() == HttpRequest.METHOD_GET && request.getPath().equals("/oidc/login")){
			handleOIDCLoginRequest(request, response);
		}

		// Process Open ID Connect redirect
		else if(request.getMethod() == HttpRequest.METHOD_GET && request.getPath().equals("/oidc/redirect")){
			handleOIDCRedirectRequest(request, response);
		}

		else{

			PassphraseAgent userAgent;
			if((userAgent=authenticate(request,response))!= null)
			{
				invoke(userAgent,request,response);
				logout(userAgent);
			}
		}
	}

	private void handleOIDCLoginRequest(HttpRequest request, HttpResponse response){
		try {
			URI u = composeAuthzRequestURL();
			response.setStatus(200);
			response.setContentType(MediaType.TEXT_HTML);
			response.println("<html><head><title>Learning Layers Open ID Connect - Login</title></head><body><h1>LAS2peer Login</h1><div id='login' style='border: 1 pt solid black; border-radius: 5px;'><a href='" + u.toString() + "'><img src='http://learning-layers.eu/wp-content/themes/learninglayers/images/logo.png' />Login via Layers Open ID Connect</a></div></body></html>");
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(500);
			response.setContentType(MediaType.TEXT_PLAIN);
			response.println("Unexpected Error: " + e.getMessage());
		}

	}

	private void handleOIDCRedirectRequest(HttpRequest request, HttpResponse response){

		response.setStatus(200);
		response.setContentType(MediaType.TEXT_PLAIN);
		response.println("Contacted OIDC Redirect Page");
		// *** *** *** Process the authorisation response *** *** *** //

		// Get the URL query string which contains the encoded 
		// authorisation response
		String queryString = request.getQueryString();

		response.println("URL query string with encoded authorization response: " + queryString + "\n\n");

		if (queryString == null || queryString.trim().isEmpty()) {

			response.println("Missing URL query string");
			return;
		}


		// Parse the authentication response
		AuthenticationResponse authResponse;

		try {
			URI redirectQuery = new URI(redirectURI.toString()+"?"+queryString);
			response.println("Redirect with Query URI: " + redirectQuery.toASCIIString());
			authResponse = AuthenticationResponseParser.parse(redirectQuery);

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(500);
			response.println("Couldn't parse Open ID Connect authentication response: " + e.getMessage());
			return;
		}

		if (authResponse instanceof AuthenticationErrorResponse) {

			// The authorisation response indicates an error, print
			// it and return immediately
			AuthenticationErrorResponse authzError = (AuthenticationErrorResponse)authResponse;
			response.setStatus(401);
			response.println("Authentication error: " + authzError.getErrorObject());
			return;
		}

		// Authentication success, retrieve the authorisation code
		AuthenticationSuccessResponse authzSuccess = (AuthenticationSuccessResponse)authResponse;

		response.println("Authorization success:");
		response.println("\tAuthorization code: " + authzSuccess.getAuthorizationCode());
		response.println("\tState: " + authzSuccess.getState() + "\n\n");
		response.println("\tRedirection URI: " + authzSuccess.getRedirectionURI());

		AuthorizationCode code = authzSuccess.getAuthorizationCode();

		if (code == null) {
			response.setStatus(401);
			response.println("Missing authorization code");
			return;
		}

		// *** *** *** Make a token endpoint request *** *** *** //

		response.println("Sending access token request to " + tokenEndpointUri + "\n\n");

		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		TokenRequest accessTokenRequest = new TokenRequest(
				tokenEndpointUri,
				clientAuth,
				new AuthorizationCodeGrant(code, redirectURI, clientID));

		com.nimbusds.oauth2.sdk.http.HTTPRequest httpRequest;

		try {
			httpRequest = accessTokenRequest.toHTTPRequest();
			response.println("Token Request: ");
			response.println("Method: " + httpRequest.getMethod());
			response.println("Auth Header: " + httpRequest.getAuthorization());
			response.println("Query: " + httpRequest.getQuery());
			String s = httpRequest.getQuery().split("&client_id")[0];
			response.println("Modified Query: " + s );
			response.println("Content Type: " + httpRequest.getContentType());
			httpRequest.setQuery(s);



		} catch (SerializeException e) {
			response.setStatus(401);
			response.println("Couldn't create access token request: " + e.getMessage());
			return;
		}


		com.nimbusds.oauth2.sdk.http.HTTPResponse httpResponse;

		try {
			httpResponse = httpRequest.send();

		} catch (IOException e) {

			// The URL request failed
			response.setStatus(401);
			response.println("Couldn't send HTTP request to token endpoint: " + e.getMessage());
			return;
		}


		TokenResponse tokenResponse;

		try {
			response.println(httpResponse.getContent().toString());
			tokenResponse = OIDCTokenResponseParser.parse(httpResponse);

		} catch (Exception e) {
			response.setStatus(401);
			response.println("Couldn't parse token response: " + e.getMessage());
			return;
		}


		if (tokenResponse instanceof TokenErrorResponse) {

			// The token response indicates an error, print it out
			// and return immediately
			TokenErrorResponse tokenError = (TokenErrorResponse)tokenResponse;
			response.setStatus(401);

			response.println("Token error: " + tokenError.getErrorObject() + " " + tokenError.getErrorObject().getDescription());
			return;
		}

		OIDCAccessTokenResponse tokenSuccess = (OIDCAccessTokenResponse)tokenResponse;

		BearerAccessToken accessToken = (BearerAccessToken)tokenSuccess.getAccessToken();
		RefreshToken refreshToken = tokenSuccess.getRefreshToken();
		SignedJWT idToken = (SignedJWT)tokenSuccess.getIDToken();


		response.println("Token response:");
		response.println("\tAccess token: " + accessToken.toJSONObject().toString());
		response.println("\tRefresh token: " + refreshToken);
		response.println("\n\n");


		// *** *** *** Process ID token which contains user auth information *** *** *** //
		if (idToken != null) {

			response.println("ID token [raw]: " + idToken.getParsedString());
			response.println("ID token JWS header: " + idToken.getHeader());


			try {

				// Validate the ID token by checking its HMAC;

				//              MACVerifier hmacVerifier = new MACVerifier(clientSecret.getValue().getBytes());
				//				final boolean valid = idToken.verify(hmacVerifier);
				//				response.println("ID token is valid: " + valid);

				JSONObject jsonObject = idToken.getJWTClaimsSet().toJSONObject();

				response.println("ID token [claims set]: \n" + jsonObject.toJSONString());
				response.println("\n\n");

			} catch (Exception e) {
				response.setStatus(401);
				response.println("Couldn't process ID token: " + e.getMessage());
			}
		}



		// *** *** *** Make a UserInfo endpoint request *** *** *** //

		// Append the access token to form actual request
		UserInfoRequest userInfoRequest = new UserInfoRequest(userinfoEndpointUri, accessToken);

		try {
			httpResponse = userInfoRequest.toHTTPRequest().send();

		} catch (Exception e) {

			// The URL request failed
			response.setStatus(401);
			response.println("Couldn't send HTTP request to UserInfo endpoint: " + e.getMessage());
			return;
		}


		UserInfoResponse userInfoResponse;

		try {
			userInfoResponse = UserInfoResponse.parse(httpResponse);
		} catch (ParseException e) {
			response.setStatus(401);
			response.println("Couldn't parse UserInfo response: " + e.getMessage());
			return;
		}


		if (userInfoResponse instanceof UserInfoErrorResponse) {

			response.setStatus(401);
			response.println("UserInfo request failed");
			return;
		}


		UserInfo userInfo = ((UserInfoSuccessResponse)userInfoResponse).getUserInfo();


		response.println("UserInfo:");


		try {
			JSONObject ujson = userInfo.toJSONObject();

			// important: mapping from OIDC id token to LAS2peer agent id
			// use a hash of OIDC id token fields "sub" and "iss" 
			JSONObject ijson = idToken.getJWTClaimsSet().toJSONObject();
			
			String sub = (String) ijson.get("sub");
			String iss = (String) ijson.get("iss");
			
			long oidcAgentId = hash(iss+sub);

			// lookup agent. if agent exists, fetch it and display information 
			if(l2pNode.hasAgent(oidcAgentId)){
				Agent a = l2pNode.getAgent(oidcAgentId);
				if(a instanceof UserAgent){
					response.println("OIDC user agent exists.");
					UserAgent u = (UserAgent) a;
					//u.unlockPrivateKey(sub);
					response.println("ID: " + u.getLoginName());
					response.println("Login name: " + u.getLoginName());
					response.println("Email : " + u.getEmail());
					
				} else {
					response.println("Error: found agent is not User Agent!");
				}
				
			} 
			// if agent does not exist, create new one
			else {
				// use sub field of userinfo
				UserAgent oidcAgent = UserAgent.createUserAgent(oidcAgentId,sub);
				oidcAgent.unlockPrivateKey(ujson.get("sub").toString());
				oidcAgent.setEmail((String) ujson.get("email"));
				oidcAgent.setLoginName((String) ujson.get("preferred_username"));
				
				l2pNode.storeAgent(oidcAgent);
				response.println("Stored new OIDC agent.");
			}
			
			response.println(userInfo.toJSONObject().toJSONString());

		} catch (Exception e) {

			response.println("Couldn't parse UserInfo JSON object: " + e.getMessage());
		}
	}

	// helper function to create long hash from string
	public static long hash(String string) {
		long h = 1125899906842597L; // prime
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31*h + string.charAt(i);
		}
		return h;
	}

	/**
	 * send a notification, that the requested service does not exists
	 * @param request
	 * @param response
	 * @param service
	 */
	private void sendNoSuchService(HttpRequest request, HttpResponse response,
			String service) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_SERVICE_UNAVAILABLE );
		response.setContentType( "text/plain" );
		response.println ( "The service you requested is not known to this server!" );

		connector.logError ("Service not found: " +service);
	}

	/**
	 * send a notification, that the requested method does not exists at the requested service
	 * @param request
	 * @param response
	 */
	private void sendNoSuchMethod(HttpRequest request, HttpResponse response) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_NOT_FOUND );
		response.setContentType( "text/plain" );
		response.println ( "The method you requested is not known to this service!" );
		connector.logError("Invocation request " + request.getPath() + " for unknown service method");
	}

	/**
	 * send a notification, that security problems occurred during the requested service method
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendSecurityProblems(HttpRequest request,
			HttpResponse response, L2pSecurityException e) {
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_FORBIDDEN );
		response.setContentType( "text/plain" );
		response.println ( "You don't have access to the method you requested" );
		connector.logError("Security exception in invocation request " + request.getPath());

		if ( System.getProperty("http-connector.printSecException") != null
				&& System.getProperty( "http-connector.printSecException").equals ( "true" ) )
			e.printStackTrace();
	}

	/**
	 * send a notification, that the result of the service invocation is
	 * not transportable 
	 * 
	 * @param request
	 * @param response
	 */
	private void sendResultInterpretationProblems(HttpRequest request,
			HttpResponse response) {
		// result interpretation problems
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		response.println ("the result of the method call is not transferable!");
		connector.logError("Exception while processing RMI: " + request.getPath());
	}

	/**
	 * send a notification about an exception which occurred inside the requested service method
	 * 
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendInvocationException(HttpRequest request,
			HttpResponse response, ServiceInvocationException e) {
		// internal exception in service method
		response.clearContent();
		response.setStatus( HttpResponse.STATUS_INTERNAL_SERVER_ERROR );
		response.setContentType( "text/xml" );
		connector.logError("Exception while processing RMI: " + request.getPath());

		Object[] ret = new Object[4];
		ret[0] = "Exception during RMI invocation!";

		ret[1] = e.getCause().getCause().getClass().getCanonicalName();
		ret[2] = e.getCause().getCause().getMessage();
		ret[3] = e.getCause().getCause();
		String code = ret[0]+"\n"+ret[1]+"\n"+ret[2]+"\n"+ret[3];
		response.println ( code );
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
		connector.logError("Invocation has been interrupted!");
	}	

	/**
	 * 
	 * @param result
	 * @param contentType
	 * @param response
	 */
	private void sendInvocationSuccess ( Serializable result, String contentType, HttpResponse response  ) {
		if ( result != null ) {
			response.setContentType( contentType );
			response.setStatus(200);
			if(result instanceof i5.las2peer.restMapper.HttpResponse)
			{

				i5.las2peer.restMapper.HttpResponse res=(i5.las2peer.restMapper.HttpResponse)result;
				Pair<String>[] headers= res.listHeaders();
				for(Pair<String> header : headers)
				{
					response.setHeaderField(header.getOne(),header.getTwo());

					if(header.getOne().toLowerCase().equals("content-type"))//speacial case because of the used http server lib
					{
						//response.clearContent();
						response.setContentType(header.getTwo());
					}
				}
				response.setStatus(res.getStatus() );
				response.println ( res.getResult() );

			}
			else
			{
				String resultCode =  (RESTMapper.castToString(result));
				response.println ( resultCode );
			}



		} else {
			response.setStatus(HttpResponse.STATUS_NO_CONTENT);
		}
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
		response.setHeaderField("WWW-Authenticate","Basic realm=\"LAS2peer WebConnector\"");
		if ( answerMessage != null)
			response.println ( answerMessage );
		response.setStatus( HttpResponse.STATUS_UNAUTHORIZED );
		connector.logMessage ( logMessage  );
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

	private URI composeAuthzRequestURL()
			throws Exception {

		// Set the requested response_type (code, token and / or 
		// id_token):
		// Use CODE for authorisation code flow
		// Use TOKEN for implicit flow
		ResponseType rt = new ResponseType("code");

		// Set the requested scope of access
		Scope scope = new Scope("openid", "email", "profile");

		// Generate random state value. It's used to link the
		// authorisation response back to the original request, also to
		// prevent replay attacks
		State state = new State();

		// Generate random nonce value.
		Nonce nonce = new Nonce();

		// Create the actual OIDC authorisation request object

		AuthenticationRequest authRequest = new AuthenticationRequest(authEndpointUri, rt, scope, clientID, redirectURI, state, nonce);

		// Construct and output the final OIDC authorisation URL for
		// redirect
		return authRequest.toURI();

	}
}



