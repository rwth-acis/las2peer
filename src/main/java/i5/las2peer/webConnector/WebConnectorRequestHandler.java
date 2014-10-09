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
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;

import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.UserAgent;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;

import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;


/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web Connector.
 * Each request will be distributed to its corresponding session.
<<<<<<< HEAD
 *
 * 
=======
>>>>>>> origin/oidc
 */



public class WebConnectorRequestHandler implements RequestHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private WebConnector connector;
	private Node l2pNode;

	/**
	 * Standard Constructor
	 * @throws URISyntaxException 
	 *
	 */
	public WebConnectorRequestHandler () throws URISyntaxException {

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
	 * @throws SerializeException 
	 * @throws Exception 
	 */
	private PassphraseAgent authenticate (HttpRequest request, HttpResponse response) throws UnsupportedEncodingException, SerializeException
	{

		final int BASIC_PREFIX_LENGTH="BASIC ".length();
		String userPass="";
		String username="";
		String password="";


		// Default authentication: 
		// check for authentication information in header
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

		} 
		// OpenID Connect authentication:
		// check for access token in query parameter
		
		// IMPORTANT NOTE: doing the same thing with authorization header and bearer token results in client-side 
		//                 cross-domain errors despite correct config for CORS in LAS2peer Web Connector!
		else if(request.getQueryString() != null && request.getQueryString().contains("access_token=")){
			String[] params = request.getQueryString().split("&");
			String token = "";
			for(int i=0;i<params.length; i++){
				String[] keyval = params[i].split("=");
				if(keyval[0].equals("access_token")){
					token = keyval[1];
				}
			}

			// send request to OpenID Connect user info endpoint to retrieve complete user information 
			// in exchange for access token.
			HTTPRequest hrq;
			HTTPResponse hrs;

			try {
				URI userinfoEndpointUri = new URI((String)((JSONObject) connector.oidcProviderInfo.get("config")).get("userinfo_endpoint"));
				hrq = new HTTPRequest(Method.GET,userinfoEndpointUri.toURL());
				hrq.setAuthorization("Bearer "+token);
				
				//TODO: process all error cases that can happen (in particular invalid tokens)
				hrs = hrq.send();

			} catch (IOException|URISyntaxException e) {
				e.printStackTrace();
				response.println("Unexpected authentication error: " + e.getMessage());
				response.setStatus(500);
				return null;
			}


			// process response from OpenID Connect user info endpoint
			UserInfoResponse userInfoResponse;
			try {
				userInfoResponse = UserInfoResponse.parse(hrs);
			} catch (ParseException e) {
				response.println("Couldn't parse UserInfo response: " + e.getMessage());
				response.setStatus(500);
				return null;
			}


			// failed request for OpenID Connect user info will result in no agent being returned.
			if (userInfoResponse instanceof UserInfoErrorResponse) {


				UserInfoErrorResponse uier = (UserInfoErrorResponse) userInfoResponse;
				
				response.println("Open ID Connect UserInfo request failed! Cause: " + uier.getErrorObject().getDescription());
				response.setStatus(401);
				
				return null;
			}

			// In case of successful request, map OpenID Connect user info to intern
			UserInfo userInfo = ((UserInfoSuccessResponse)userInfoResponse).getUserInfo();

			try {

				JSONObject ujson = userInfo.toJSONObject();
				//response.println("User Info: " + userInfo.toJSONObject());

				String sub = (String) ujson.get("sub");
				String mail = (String) ujson.get("mail");
				String name = (String) ujson.get("name");

				long oidcAgentId = hash(sub);
				username = oidcAgentId+"";
				password = sub;

				PassphraseAgent pa;
				try {
					pa = (PassphraseAgent)l2pNode.getAgent(oidcAgentId);
					pa.unlockPrivateKey(password);
					return pa;
				} catch (AgentNotKnownException e) {
					UserAgent oidcAgent;
					try {
						// here, we choose the OpenID Connect 
						// TODO: choose other scheme for generating agent password.
						oidcAgent = UserAgent.createUserAgent(oidcAgentId,sub);
						
						
						oidcAgent.unlockPrivateKey(ujson.get("sub").toString());
						oidcAgent.setEmail((String) ujson.get("email"));
						oidcAgent.setLoginName((String) ujson.get("preferred_username"));

						l2pNode.storeAgent(oidcAgent);
						oidcAgent.unlockPrivateKey(password);
						return oidcAgent;
					} catch (Exception e1) {
						return null;
					} 
				}

			} catch (L2pSecurityException e) {
				response.println(e.getMessage());
				response.setStatus(401);
				e.printStackTrace();
			}
		}

		//no information? check if there is a default account for login
		else if(connector.defaultLoginUser.length()>0)
		{
			response.print("");
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

			//keep track of active requests
			synchronized (this.connector)
			{

				if(this.connector.getOpenUserRequests().containsKey(userId))
				{
					Integer numReq = this.connector.getOpenUserRequests().get(userId);
					this.connector.getOpenUserRequests().put(userId,numReq+1);
					//System.out.println("### numreq " +numReq);
				}
				else
				{
					this.connector.getOpenUserRequests().put(userId,1);
					//System.out.println("### numreq 0" );
				}
			}
			userAgent = (PassphraseAgent)l2pNode.getAgent(userId);

			/* if ( ! (userAgent instanceof PassphraseAgent ))
                throw new L2pSecurityException ("Agent is not passphrase protected!");*/


			userAgent.unlockPrivateKey(password);

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
		long userId=userAgent.getId();


		//synchronize across multiple threads
		synchronized (this.connector)
		{

			if(this.connector.getOpenUserRequests().containsKey(userId))
			{
				Integer numReq = this.connector.getOpenUserRequests().get(userId);
				if(numReq<=1)
				{
					this.connector.getOpenUserRequests().remove(userId);
					try {
						l2pNode.unregisterAgent(userAgent);
						userAgent.lockPrivateKey();
						//System.out.println("+++ logout");

					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				else
				{
					this.connector.getOpenUserRequests().put(userId,numReq-1);

				}
			}
			else
			{
				try {
					l2pNode.unregisterAgent(userAgent);
					userAgent.lockPrivateKey();
					//System.out.println("+++ logout");

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		//userAgent.lockPrivateKey(); //lock local copy again

		/*try {


			//if(userAgent!=null)

			//TODO check
			//l2pNode.unregisterAgent(userAgent);
			userAgent.lockPrivateKey();//don't know if really necessary
                //TODO check
              //  l2pNode.unregisterAgent(userAgent);
              //  userAgent.lockPrivateKey();//don't know if really necessary


		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	/**
	 * Handles a request (login, invoke)
	 */
	@Override
	public void processRequest(HttpRequest request, HttpResponse response) throws Exception {
		response.setHeaderField( "Server-Name", "LAS2peer" );
		response.setContentType( "text/xml" );

		PassphraseAgent userAgent;
		if((userAgent=authenticate(request,response))!= null)
		{
			invoke(userAgent,request,response);
			logout(userAgent);
		}

	}

	/*
	private void handleOIDCLoginRequest(HttpRequest request, HttpResponse response){

		try {

			// construct HTML response, presenting a login page to the user
			String html = "<!doctype html>";
			html += "<html>";
			html += "  <head><title>LAS2peer Open ID Connect Logins</title></head>";
			html += "  <body>\n    <h1>LAS2peer Login</h1>";

			String name = (String) connector.oidcProvider.get("name");
			String logoUri = (String) connector.oidcProvider.get("logo");
			String clientId = (String) connector.oidcProvider.get("client_id");

			JSONObject config = (JSONObject) connector.oidcProvider.get("config");

			URI authEndpointUri = new URI((String) config.get("authorization_endpoint")); 
			URI redirectUri = new URI((String) connector.oidcProvider.get("redirect_uri"));
			URI authRequestURI = composeAuthzRequestURL(authEndpointUri, clientId, redirectUri);

			html += "    <div class='login' style='border: 1 pt solid black;'><a href='" + authRequestURI + "'><img src='" + logoUri + "' height='100' />Login via " + name + "</a></div>";
			html +="  </body>\n</html>";

			// return response
			response.setStatus(200);
			response.setContentType(MediaType.TEXT_HTML);
			response.println(html);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(500);
			response.setContentType(MediaType.TEXT_PLAIN);
			response.println("Unexpected Error: " + e.getMessage());
		}

	}
	

	private void handleOIDCRedirectRequest(HttpRequest request, HttpResponse response){

		JSONObject oidcProviderConfig = (JSONObject) connector.oidcProvider.get("config");

		//response.println("OIDC Provider Configuration: " + oidcProviderConfig.toJSONString());

		URI userinfoEndpointUri, tokenEndpointUri, clientRedirectUri;

		try {
			tokenEndpointUri = new URI((String) oidcProviderConfig.get("token_endpoint"));
			//response.println("Token Endpoint: " + tokenEndpointUri);
		} catch (URISyntaxException e1) {
			throw new IllegalArgumentException("Invalid Token Endpoint URI " + oidcProviderConfig.get("token_endpoint"));
		}

		try {
			userinfoEndpointUri = new URI((String) oidcProviderConfig.get("userinfo_endpoint"));
			//response.println("Userinfo Endpoint: " + userinfoEndpointUri);

		} catch (URISyntaxException e1) {
			throw new IllegalArgumentException("Invalid Userinfo Endpoint URI " + oidcProviderConfig.get("userinfo_endpoint"));
		}

		try {
			clientRedirectUri = new URI((String) connector.oidcProvider.get("redirect_uri"));
			//response.println("Client Redirect URI: " + clientRedirectUri);

		} catch (URISyntaxException e1) {
			throw new IllegalArgumentException("Invalid Client Redirect URI " + connector.oidcProvider.get("redirect_uri"));
		}


		// *** *** *** Process the authorisation response *** *** *** //

		// Get the URL query string which contains the encoded 
		// authorisation response
		String queryString = request.getQueryString();

		if (queryString == null || queryString.trim().isEmpty()) {

			response.println("Missing URL query string");
			return;
		}

		// Parse the authentication response
		AuthenticationResponse authResponse;

		try {

			URI redirectQuery = new URI(clientRedirectUri + "?" + queryString);
			authResponse = AuthenticationResponseParser.parse(redirectQuery);

		} catch (Exception e) {
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(500);
			response.println("Couldn't parse Open ID Connect authentication response: " + e.getMessage());
			return;
		}



		if (authResponse instanceof AuthenticationErrorResponse) {

			// The authorisation response indicates an error, print
			// it and return immediately
			AuthenticationErrorResponse authzError = (AuthenticationErrorResponse)authResponse;
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(401);
			response.println("Authentication error: " + authzError.getErrorObject());
			return;
		}

		// Authentication success, retrieve the authorisation code
		AuthenticationSuccessResponse authzSuccess = (AuthenticationSuccessResponse)authResponse;

		//		response.println("Authorization success:");
		//		response.println("\tAuthorization code: " + authzSuccess.getAuthorizationCode());
		//		response.println("\tState: " + authzSuccess.getState());
		//		response.println("\tRedirection URI: " + authzSuccess.getRedirectionURI());

		AuthorizationCode code = authzSuccess.getAuthorizationCode();

		if (code == null) {
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(401);
			response.println("Missing authorization code");
			return;
		}

		// *** *** *** Make a token endpoint request *** *** *** //

		//response.println("Sending access token request to " + tokenEndpointUri + "\n\n");

		ClientID clientID = new ClientID((String) connector.oidcProvider.get("client_id"));
		Secret clientSecret = new Secret((String) connector.oidcProvider.get("client_secret"));

		//response.println("Client ID: " + clientID.getValue());
		//response.println("Client Secret : " + clientSecret.getValue());

		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		TokenRequest accessTokenRequest = new TokenRequest(
				tokenEndpointUri,
				clientAuth,
				new AuthorizationCodeGrant(code, authzSuccess.getRedirectionURI(), clientID));

		com.nimbusds.oauth2.sdk.http.HTTPRequest httpRequest;

		try {
			httpRequest = accessTokenRequest.toHTTPRequest();
			// Modify request to become OIDC compliant: strip client ID query parameter. Not part of specification!
			String s = httpRequest.getQuery().split("&client_id")[0];
			httpRequest.setQuery(s);

		} catch (SerializeException e) {
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(401);
			response.println("Couldn't create access token request: " + e.getMessage());
			return;
		}

		com.nimbusds.oauth2.sdk.http.HTTPResponse httpResponse;

		try {
			httpResponse = httpRequest.send();

		} catch (IOException e) {

			// The URL request failed
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(401);
			response.println("Couldn't send HTTP request to token endpoint: " + e.getMessage());
			return;
		}

		TokenResponse tokenResponse;

		try {
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

			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(401);
			response.println("Token error: " + tokenError.getErrorObject() + " " + tokenError.getErrorObject().getDescription());
			return;
		}

		OIDCAccessTokenResponse tokenSuccess = (OIDCAccessTokenResponse)tokenResponse;

		BearerAccessToken accessToken = (BearerAccessToken)tokenSuccess.getAccessToken();
		RefreshToken refreshToken = tokenSuccess.getRefreshToken();
		SignedJWT idToken = (SignedJWT)tokenSuccess.getIDToken();

		//		response.println("Token response:");
		//		response.println("\tAccess token: " + accessToken.toJSONObject().toString());
		//		response.println("\tRefresh token: " + refreshToken);
		//		response.println("\n\n");

		// as soon as tokens are available, construct response in form of an HTML document
		// that writes accessToken to HTML5 local storage
		String redirectHtmlFile = "./etc/redirect.html";

		try {

			String html = new Scanner(new File(redirectHtmlFile)).useDelimiter("\\A").next();
			html = html.replaceAll("_ACCESSTOKEN_",accessToken.getValue());

			response.setStatus(200);
			response.setContentType(MediaType.TEXT_HTML);
			response.println(html);
			return;

		} catch (FileNotFoundException e) {
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setStatus(500);
			response.println("Could not find valid redirect page template at " + redirectHtmlFile);
			return;
		}

		// 
		//
		//		// *** *** *** Process ID token which contains user auth information *** *** *** //
		//		if (idToken != null) {
		//
		//			response.println("ID token [raw]: " + idToken.getParsedString());
		//			response.println("ID token JWS header: " + idToken.getHeader());
		//
		//
		//			try {
		//
		//				// Validate the ID token by checking its HMAC;
		//
		//				//				MACVerifier hmacVerifier = new MACVerifier(clientSecret.getValue().getBytes());
		//				//				final boolean valid = idToken.verify(hmacVerifier);
		//				//				response.println("ID token is valid: " + valid);
		//
		//				JSONObject jsonObject = idToken.getJWTClaimsSet().toJSONObject();
		//
		//				response.println("ID token [claims set]: \n" + jsonObject.toJSONString());
		//				response.println("\n\n");
		//
		//			} catch (Exception e) {
		//				response.setStatus(401);
		//				response.println("Couldn't process ID token: " + e.getMessage());
		//			}
		//		}
		//
		//
		//
		//		// *** *** *** Make a UserInfo endpoint request *** *** *** //
		//
		//
		//
		//
		//		// Append the access token to form actual request
		//		UserInfoRequest userInfoRequest = new UserInfoRequest(userinfoEndpointUri, accessToken);
		//
		//		try {
		//			HTTPRequest http = userInfoRequest.toHTTPRequest();
		//			response.println("Userinfo Request: ");
		//			response.println("Method: " + http.getMethod());
		//			response.println("Authz: " + http.getAuthorization());
		//			response.println("Query: " + http.getQuery());
		//			response.println("Content Type: " + http.getContentType());
		//
		//			httpResponse = userInfoRequest.toHTTPRequest().send();
		//
		//		} catch (Exception e) {
		//
		//			// The URL request failed
		//			response.setStatus(401);
		//			response.println("Couldn't send HTTP request to UserInfo endpoint: " + e.getMessage());
		//			return;
		//		}
		//
		//
		//		UserInfoResponse userInfoResponse;
		//
		//		try {
		//			userInfoResponse = UserInfoResponse.parse(httpResponse);
		//		} catch (ParseException e) {
		//			response.setStatus(401);
		//			response.println("Couldn't parse UserInfo response: " + e.getMessage());
		//			return;
		//		}
		//
		//
		//		if (userInfoResponse instanceof UserInfoErrorResponse) {
		//
		//			response.setStatus(401);
		//			response.println("UserInfo request failed");
		//			return;
		//		}
		//
		//
		//		UserInfo userInfo = ((UserInfoSuccessResponse)userInfoResponse).getUserInfo();
		//
		//
		//		response.println("UserInfo:");
		//
		//
		//		try {
		//			JSONObject ujson = userInfo.toJSONObject();
		//
		//			// important: mapping from OIDC id token to LAS2peer agent id
		//			// use a hash of OIDC id token fields "sub" and "iss" 
		//			JSONObject ijson = idToken.getJWTClaimsSet().toJSONObject();
		//
		//			String sub = (String) ijson.get("sub");
		//			String iss = (String) ijson.get("iss");
		//
		//			long oidcAgentId = hash(iss+sub);
		//
		//			// lookup agent. if agent exists, fetch it and display information 
		//			if(l2pNode.hasAgent(oidcAgentId)){
		//				Agent a = l2pNode.getAgent(oidcAgentId);
		//				if(a instanceof UserAgent){
		//					response.println("OIDC user agent exists.");
		//					UserAgent u = (UserAgent) a;
		//					//u.unlockPrivateKey(sub);
		//					response.println("ID: " + u.getLoginName());
		//					response.println("Login name: " + u.getLoginName());
		//					response.println("Email : " + u.getEmail());
		//
		//				} else {
		//					response.println("Error: found agent is not User Agent!");
		//				}
		//
		//			} 
		//			// if agent does not exist, create new one
		//			else {
		//				// use sub field of userinfo
		//				UserAgent oidcAgent = UserAgent.createUserAgent(oidcAgentId,sub);
		//				oidcAgent.unlockPrivateKey(ujson.get("sub").toString());
		//				oidcAgent.setEmail((String) ujson.get("email"));
		//				oidcAgent.setLoginName((String) ujson.get("preferred_username"));
		//
		//				l2pNode.storeAgent(oidcAgent);
		//				response.println("Stored new OIDC agent.");
		//			}
		//
		//			response.println(userInfo.toJSONObject().toJSONString());
		//
		//		} catch (Exception e) {
		//
		//			response.println("Couldn't parse UserInfo JSON object: " + e.getMessage());
		//		}
	}

*/
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

//	private URI composeAuthzRequestURL(URI authEndpointUri, String clientId, URI redirectUri) throws SerializeException {
//
//		// Set the requested response_type (code, token and / or 
//		// id_token):
//		// Use CODE for authorisation code flow
//		// Use TOKEN for implicit flow
//		ResponseType rt = new ResponseType("code");
//
//		// Set the requested scope of access
//		Scope scope = new Scope("openid", "email", "profile");
//
//		// Generate random state value. It's used to link the
//		// authorisation response back to the original request, also to
//		// prevent replay attacks
//		State state = new State();
//
//		// Generate random nonce value.
//		Nonce nonce = new Nonce();
//
//		// Create the actual OIDC authorisation request object
//
//		ClientID clientID = new ClientID(clientId);
//
//		AuthenticationRequest authRequest = new AuthenticationRequest(authEndpointUri, rt, scope, clientID, redirectUri, state, nonce);
//
//		// Construct and output the final OIDC authorisation URL for
//		// redirect
//		return authRequest.toURI();
//
//	}
}



