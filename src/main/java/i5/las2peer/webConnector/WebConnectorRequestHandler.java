package i5.las2peer.webConnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.ServiceInfoAgent;
import i5.las2peer.security.UserAgent;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.util.Json;
import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web Connector. Each request will be distributed to
 * its corresponding session.
 * 
 */
public class WebConnectorRequestHandler implements HttpHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private static final String ACCESS_TOKEN_KEY = "access_token";
	private static final String OIDC_PROVIDER_KEY = "oidc_provider";
	private static final int NO_RESPONSE_BODY = -1; // 0 means chunked transfer encoding, see
													// https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpExchange.html#sendResponseHeaders-int-long-

	private WebConnector connector;
	private Node l2pNode;

	public WebConnectorRequestHandler(WebConnector connector) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();
	}

	/**
	 * set the connector handling this request processor
	 * 
	 * @param connector
	 */
	public void setConnector(WebConnector connector) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();
	}

	/**
	 * Logs in a las2peer user
	 * 
	 * @param exchange
	 * 
	 * @return -1 if no successful login else userId
	 * @throws UnsupportedEncodingException
	 */
	private synchronized PassphraseAgent authenticate(HttpExchange exchange) throws UnsupportedEncodingException {
		String userPass = "";
		String username = "";
		String password = "";

		// Default authentication:
		// check for authentication information in header
		if (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD)
				&& exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).toLowerCase().startsWith("basic ")) {
			// looks like: Authentication Basic <Byte64(name:pass)>
			userPass = exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).substring("BASIC ".length());
			userPass = new String(Base64.decode(userPass), "UTF-8");
			int separatorPos = userPass.indexOf(':');

			// get username and password
			username = userPass.substring(0, separatorPos);
			password = userPass.substring(separatorPos + 1);

			return login(username, password, exchange);
		}
		// OpenID Connect authentication:
		// check for access token in query parameter and headers
		else if (connector.oidcProviderInfos != null
				&& ((exchange.getRequestURI().getRawQuery() != null
						&& exchange.getRequestURI().getRawQuery().contains(ACCESS_TOKEN_KEY + "="))
						|| exchange.getRequestHeaders().containsKey(ACCESS_TOKEN_KEY)
						|| (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD)
								&& exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).toLowerCase()
										.startsWith("bearer ")))) {

			String token = "";
			String oidcProviderURI = connector.defaultOIDCProvider;
			if (exchange.getRequestHeaders().containsKey(ACCESS_TOKEN_KEY)) { // get OIDC parameters from headers
				token = exchange.getRequestHeaders().getFirst(ACCESS_TOKEN_KEY);
				if (exchange.getRequestHeaders().containsKey(OIDC_PROVIDER_KEY))
					oidcProviderURI = URLDecoder.decode(exchange.getRequestHeaders().getFirst(OIDC_PROVIDER_KEY),
							"UTF-8");
			} else if (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD) && exchange.getRequestHeaders()
					.getFirst(AUTHENTICATION_FIELD).toLowerCase().startsWith("bearer ")) { // get BEARER token from
																							// Authentication field
				token = exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).substring("BEARER ".length());
				if (exchange.getRequestHeaders().containsKey(OIDC_PROVIDER_KEY))
					oidcProviderURI = URLDecoder.decode(exchange.getRequestHeaders().getFirst(OIDC_PROVIDER_KEY),
							"UTF-8");
			} else { // get OIDC parameters from GET values
				String[] params = exchange.getRequestURI().getRawQuery().split("&");
				for (int i = 0; i < params.length; i++) {
					String[] keyval = params[i].split("=");
					if (keyval[0].equalsIgnoreCase(ACCESS_TOKEN_KEY)) {
						token = keyval[1];
					} else if (keyval[0].equalsIgnoreCase(OIDC_PROVIDER_KEY)) {
						oidcProviderURI = URLDecoder.decode(keyval[1], "UTF-8");
					}
				}
			}

			// validate given OIDC provider and get provider info
			JSONObject oidcProviderInfo = null;
			if (connector.oidcProviders.contains(oidcProviderURI) == false) {
				sendInternalErrorResponse(exchange, "The given OIDC provider (" + oidcProviderURI
						+ ") is not whitelisted! Please make sure the complete OIDC provider URI is added to the config.");
				return null;
			} else if (connector.oidcProviderInfos.get(oidcProviderURI) == null) {
				sendInternalErrorResponse(exchange, "The OIDC config is not known for the given provider ("
						+ oidcProviderURI + ")! Please make sure the right URI is added to the config.");
				return null;
			} else {
				oidcProviderInfo = connector.oidcProviderInfos.get(oidcProviderURI);
			}

			// send request to OpenID Connect user info endpoint to retrieve complete user information
			// in exchange for access token.
			HTTPRequest hrq;
			HTTPResponse hrs;

			try {
				URI userinfoEndpointUri = new URI(
						(String) ((JSONObject) oidcProviderInfo.get("config")).get("userinfo_endpoint"));
				hrq = new HTTPRequest(Method.GET, userinfoEndpointUri.toURL());
				hrq.setAuthorization("Bearer " + token);

				// TODO: process all error cases that can happen (in particular invalid tokens)
				hrs = hrq.send();
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
				sendInternalErrorResponse(exchange, "Unexpected authentication error: " + e.getMessage());
				return null;
			}

			// process response from OpenID Connect user info endpoint
			UserInfoResponse userInfoResponse;
			try {
				userInfoResponse = UserInfoResponse.parse(hrs);
			} catch (ParseException e) {
				sendInternalErrorResponse(exchange, "Couldn't parse UserInfo response: " + e.getMessage());
				return null;
			}

			// failed request for OpenID Connect user info will result in no agent being returned.
			if (userInfoResponse instanceof UserInfoErrorResponse) {
				UserInfoErrorResponse uier = (UserInfoErrorResponse) userInfoResponse;
				ErrorObject err = uier.getErrorObject();
				String cause = "Session expired?";
				if (err != null) {
					cause = err.getDescription();
				}
				sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED,
						"Open ID Connect UserInfo request failed! Cause: " + cause);
				return null;
			}

			// In case of successful request, map OpenID Connect user info to intern
			UserInfo userInfo = ((UserInfoSuccessResponse) userInfoResponse).getUserInfo();

			try {
				JSONObject ujson = userInfo.toJSONObject();
				// response.println("User Info: " + userInfo.toJSONObject());

				if (!ujson.containsKey("sub") || !ujson.containsKey("email")
						|| !ujson.containsKey("preferred_username")) {
					sendStringResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN,
							"Could not get provider information. Please check your scopes.");
					return null;
				}

				String sub = (String) ujson.get("sub");

				long oidcAgentId = hash(sub);
				username = oidcAgentId + "";
				password = sub;

				synchronized (this.connector) {
					if (this.connector.getOpenUserRequests().containsKey(oidcAgentId)) {
						Integer numReq = this.connector.getOpenUserRequests().get(oidcAgentId);
						this.connector.getOpenUserRequests().put(oidcAgentId, numReq + 1);
					} else {
						this.connector.getOpenUserRequests().put(oidcAgentId, 1);
					}
				}

				try {
					PassphraseAgent pa = (PassphraseAgent) l2pNode.getAgent(oidcAgentId);
					pa.unlockPrivateKey(password);
					if (pa instanceof UserAgent) {
						UserAgent ua = (UserAgent) pa;
						ua.setUserData(ujson.toJSONString());
						return ua;
					}
					return pa;
				} catch (AgentNotKnownException e) {
					UserAgent oidcAgent;
					try {
						// here, we choose the OpenID Connect
						// TODO: choose other scheme for generating agent password.
						oidcAgent = UserAgent.createUserAgent(oidcAgentId, sub);

						oidcAgent.unlockPrivateKey(ujson.get("sub").toString());
						oidcAgent.setEmail((String) ujson.get("email"));
						oidcAgent.setLoginName((String) ujson.get("preferred_username"));
						oidcAgent.setUserData(ujson.toJSONString());

						l2pNode.storeAgent(oidcAgent);
						oidcAgent.unlockPrivateKey(password);
						return oidcAgent;
					} catch (Exception e1) {
						e1.printStackTrace();
						sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
						return null;
					}
				}
			} catch (L2pSecurityException e) {
				e.printStackTrace();
				sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage());
			}
		}
		// no information? check if there is a default account for login
		else if (connector.defaultLoginUser.length() > 0) {
			return login(connector.defaultLoginUser, connector.defaultLoginPassword, exchange);
		} else {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress() + ": No Authentication provided!");
		}
		return null;
	}

	// helper function to create long hash from string
	public static long hash(String string) {
		long h = 1125899906842597L; // prime
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i);
		}
		return h;
	}

	private PassphraseAgent login(String username, String password, HttpExchange exchange) {
		try {
			long userId;
			PassphraseAgent userAgent;

			if (username.matches("-?[0-9].*")) {// username is id?
				try {
					userId = Long.valueOf(username);
				} catch (NumberFormatException e) {
					throw new L2pSecurityException("The given user does not contain a valid agent id!");
				}
			} else {// username is string
				userId = l2pNode.getAgentIdForLogin(username);
			}

			// keep track of active requests
			synchronized (this.connector) {
				if (this.connector.getOpenUserRequests().containsKey(userId)) {
					Integer numReq = this.connector.getOpenUserRequests().get(userId);
					this.connector.getOpenUserRequests().put(userId, numReq + 1);
				} else {
					this.connector.getOpenUserRequests().put(userId, 1);
				}
			}
			userAgent = (PassphraseAgent) l2pNode.getAgent(userId);

			/* if ( ! (userAgent instanceof PassphraseAgent ))
			    throw new L2pSecurityException ("Agent is not passphrase protected!");*/

			userAgent.unlockPrivateKey(password);

			return userAgent;
		} catch (AgentNotKnownException e) {
			sendUnauthorizedResponse(exchange, null,
					exchange.getRemoteAddress() + ": login denied for user " + username);
		} catch (L2pSecurityException e) {
			sendUnauthorizedResponse(exchange, null,
					exchange.getRemoteAddress() + ": unauth access - prob. login problems");
		} catch (Exception e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress()
					+ ": something went horribly wrong. Check your request for correctness.");
		}
		return null;
	}

	/**
	 * Delegates the request data to a service method, which then decides what to do with it (maps it internally)
	 * 
	 * @param userAgent
	 * @param exchange
	 * 
	 * @return
	 */
	private boolean invoke(PassphraseAgent userAgent, HttpExchange exchange) {
		// internal server error unless otherwise specified (errors might occur)
//		int responseCode = STATUS_INTERNAL_SERVER_ERROR;
		String[] requestSplit = exchange.getRequestURI().getPath().split("/", 2);
		// first: empty (string starts with '/')
		// second: URI
		String uri = "";

		try {
			if (requestSplit.length >= 2) {
				int varsstart = requestSplit[1].indexOf('?');
				if (varsstart > 0) {
					uri = requestSplit[1].substring(0, varsstart);
				} else {
					uri = requestSplit[1];
				}
			}

			// http body
			InputStream is = exchange.getRequestBody();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[4096];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}
			buffer.flush();
			byte[] rawContent = buffer.toByteArray();

			// http method
			String httpMethod = exchange.getRequestMethod();

			// extract all get variables from the query
			ArrayList<Pair<String>> variablesList = new ArrayList<Pair<String>>();
			String query = exchange.getRequestURI().getQuery();
			if (query != null) {
				for (String param : query.split("&")) {
					String pair[] = param.split("=");
					if (pair.length > 1) {
						variablesList.add(new Pair<String>(pair[0], pair[1]));
					} else {
						variablesList.add(new Pair<String>(pair[0], ""));
					}
				}
			}
			@SuppressWarnings("unchecked")
			Pair<String>[] variables = variablesList.toArray(new Pair[variablesList.size()]);

			// extract all header fields from the query
			ArrayList<Pair<String>> headersList = new ArrayList<Pair<String>>();
			// default values
			String acceptHeader = "*/*";
			String contentTypeHeader = "text/plain";
			Set<Entry<String, List<String>>> entries = exchange.getRequestHeaders().entrySet();
			for (Entry<String, List<String>> entry : entries) {
				String key = entry.getKey();
				for (String value : entry.getValue()) {
					headersList.add(new Pair<String>(key, value));
					// fetch MIME types
					if (key.equalsIgnoreCase("accept") && !value.isEmpty()) {
						acceptHeader = value;
					} else if (key.equalsIgnoreCase("content-type") && !value.isEmpty()) {
						contentTypeHeader = value;
					}
				}
			}
			@SuppressWarnings("unchecked")
			Pair<String>[] headers = headersList.toArray(new Pair[headersList.size()]);

			Serializable result = "";
			Mediator mediator = l2pNode.getOrRegisterLocalMediator(userAgent);
			boolean gotResult = false;
			String[] returnMIMEType = RESTMapper.DEFAULT_PRODUCES_MIME_TYPE;
			StringBuilder warnings = new StringBuilder();
			InvocationData[] invocation = RESTMapper.parse(this.connector.getMappingTree(), httpMethod, uri, variables,
					rawContent, contentTypeHeader, acceptHeader, headers, warnings);

			if (invocation.length == 0) {
				if (warnings.length() > 0) {
					sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
							warnings.toString().replaceAll("\n", " "));
				} else {
					sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, NO_RESPONSE_BODY);
					// otherwise the client waits till the timeout for an answer
					exchange.getResponseBody().close();
				}
				return false;
			}

			for (InvocationData inv : invocation) {
				try {
					// invoke service method // TODO specify version?
					result = mediator.invoke(inv.getServiceName(), inv.getMethodName(), inv.getParameters(),
							connector.preferLocalServices());
					gotResult = true;
					returnMIMEType = inv.getMIME();
					break;
				} catch (NoSuchServiceException | TimeoutException e) {
					sendNoSuchService(exchange, inv.getServiceName());
				} catch (NoSuchServiceMethodException e) {
					sendNoSuchMethod(exchange);
				} catch (L2pSecurityException e) {
					sendSecurityProblems(exchange, e);
				} catch (ServiceInvocationException e) {
					if (e.getCause() == null) {
						sendResultInterpretationProblems(exchange);
					} else {
						sendInvocationException(exchange, e);
					}
				} catch (InterruptedException e) {
					sendInvocationInterrupted(exchange);
				}
			}

			if (gotResult) {
				sendInvocationSuccess(result, returnMIMEType, exchange);
			}
			return true;
		} catch (NoMethodFoundException | NotSupportedUriPathException e) {
			sendNoSuchMethod(exchange);
		} catch (NumberFormatException e) {
			sendMalformedRequest(exchange, e.toString());
		} catch (Exception e) {
			connector.logError("Error occured:" + exchange.getRequestURI().getPath() + " " + e.getMessage());
			sendInternalErrorResponse(exchange, e.toString());
		}
		return false;
	}

	/**
	 * Logs the user out
	 * 
	 * @param userAgent
	 */
	private void logout(PassphraseAgent userAgent) {
		long userId = userAgent.getId();

		// synchronize across multiple threads
		synchronized (this.connector) {

			if (this.connector.getOpenUserRequests().containsKey(userId)) {
				Integer numReq = this.connector.getOpenUserRequests().get(userId);
				if (numReq <= 1) {
					this.connector.getOpenUserRequests().remove(userId);
					try {
						l2pNode.unregisterReceiver(userAgent);
						userAgent.lockPrivateKey();
						// System.out.println("+++ logout");

					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					this.connector.getOpenUserRequests().put(userId, numReq - 1);
				}
			} else {
				try {
					l2pNode.unregisterReceiver(userAgent);
					userAgent.lockPrivateKey();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Handles a request (login, invoke)
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Server-Name", "LAS2peer WebConnector");
		// check for an OPTIONS request and auto answer it
		// TODO this should become a default reply for OPTIONS-requests,
		// but should be also be available to service developers
		if (exchange.getRequestMethod().equalsIgnoreCase("options")) {
			sendResponse(exchange, HttpURLConnection.HTTP_OK, NO_RESPONSE_BODY);
		} else if (exchange.getRequestMethod().equalsIgnoreCase("get")
				&& exchange.getRequestURI().getPath().equalsIgnoreCase("/swagger.json")) {
			// respond with swagger.json for all known services at this endpoint
			handleSwagger(exchange);
		} else {
			PassphraseAgent userAgent;
			if ((userAgent = authenticate(exchange)) != null) {
				invoke(userAgent, exchange);
				logout(userAgent);
			}
		}
		// otherwise the client waits till the timeout for an answer
		exchange.getResponseBody().close();
	}

	/**
	 * Returns the API documentation of all annotated local resources for purposes of Swagger documentation.
	 * 
	 * @param exchange The origin exchange request for the swagger listing.
	 */
	private void handleSwagger(HttpExchange exchange) {
		try {
			L2pClassManager clsLoader = (L2pClassManager) connector.getL2pNode().getBaseClassLoader();
			ServiceNameVersion[] services = ServiceInfoAgent.getServices();
			Set<Class<?>> serviceClasses = new HashSet<Class<?>>(services.length);
			for (ServiceNameVersion snv : services) {
				try {
					Class<?> cls = clsLoader.getServiceClass(snv.getName(), snv.getVersion());
					serviceClasses.add(cls);
				} catch (IllegalArgumentException | ClassLoaderException e) {
					connector.logError("Class '" + snv + "' not found " + e);
				}
			}
			Swagger swagger = new Reader(new Swagger()).read(serviceClasses);
			if (swagger == null) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
						"Swagger API declaration not available!");
			} else {
				// OpenID Connect integration
				if (connector.oidcProviderInfos != null && connector.defaultOIDCProvider != null
						&& !connector.defaultOIDCProvider.isEmpty()) {
					// add security definition for default provider
					JSONObject infos = connector.oidcProviderInfos.get(connector.defaultOIDCProvider);
					OAuth2Definition scheme = new OAuth2Definition();
					String authUrl = (String) ((JSONObject) infos.get("config")).get("authorization_endpoint");
					scheme.implicit(authUrl);
					scheme.addScope("openid", "Access Identity");
					scheme.addScope("email", "Access E-Mail-Address");
					scheme.addScope("profile", "Access Profile Data");

					swagger.addSecurityDefinition("defaultProvider", scheme);

					// add security requirements to operations
					List<String> scopes = new ArrayList<String>();
					scopes.add("openid");
					scopes.add("email");
					scopes.add("profile");
					Map<String, Path> paths = swagger.getPaths();
					if (paths != null) {
						for (Path path : paths.values()) {
							for (Operation operation : path.getOperations()) {
								operation.addSecurity("defaultProvider", scopes);
							}
						}
					}
				}

				String json = Json.mapper().writeValueAsString(swagger);
				sendStringResponse(exchange, HttpURLConnection.HTTP_OK, json);
			}
		} catch (Exception e) {
			connector.logError("Exception while creating swagger.json output " + e.toString(), e);
			sendInternalErrorResponse(exchange, e.toString());
		}
	}

	/**
	 * send a notification, that the requested service does not exists
	 * 
	 * @param exchange
	 * @param error
	 */
	private void sendMalformedRequest(HttpExchange exchange, String error) {
		// connector.logError("Malformed request: " + error);
		sendStringResponse(exchange, HttpURLConnection.HTTP_BAD_REQUEST, "Malformed Request: wrong datatypes");
	}

	/**
	 * send a notification, that the requested service does not exists
	 * 
	 * @param exchange
	 * @param service
	 */
	private void sendNoSuchService(HttpExchange exchange, String service) {
		connector.logError("Service not found: " + service);
		sendStringResponse(exchange, HttpURLConnection.HTTP_UNAVAILABLE,
				"The service you requested is not known to this server!");
	}

	/**
	 * send a notification, that the requested method does not exists at the requested service
	 * 
	 * @param exchange
	 */
	private void sendNoSuchMethod(HttpExchange exchange) {
		connector.logError("Invocation request " + exchange.getRequestURI().getPath() + " for unknown service method");
		sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
				"The method you requested is not known to this service!");
	}

	/**
	 * send a notification, that security problems occurred during the requested service method
	 * 
	 * @param exchange
	 * @param e
	 */
	private void sendSecurityProblems(HttpExchange exchange, L2pSecurityException e) {
		connector.logError("Security exception in invocation request " + exchange.getRequestURI().getPath());
		sendStringResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN,
				"You don't have access to the method you requested");

		if (System.getProperty("http-connector.printSecException") != null
				&& System.getProperty("http-connector.printSecException").equals("true")) {
			e.printStackTrace();
		}
	}

	/**
	 * send a notification, that the result of the service invocation is not transportable
	 * 
	 * @param exchange
	 */
	private void sendResultInterpretationProblems(HttpExchange exchange) {
		connector.logError("Exception while processing RMI: " + exchange.getRequestURI().getPath());
		// result interpretation problems
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR,
				"The result of the method call is not transferable!");
	}

	/**
	 * send a notification about an exception which occurred inside the requested service method
	 * 
	 * @param exchange
	 * @param e
	 */
	private void sendInvocationException(HttpExchange exchange, ServiceInvocationException e) {
		connector.logError("Exception while processing RMI: " + exchange.getRequestURI().getPath(), e);
		// internal exception in service method
		Object[] ret = new Object[4];
		ret[0] = "Exception during RMI invocation!";
		ret[1] = e.getCause().getCause().getClass().getCanonicalName();
		ret[2] = e.getCause().getCause().getMessage();
		ret[3] = e.getCause().getCause();
		String code = ret[0] + "\n" + ret[1] + "\n" + ret[2] + "\n" + ret[3];
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, code);
	}

	/**
	 * send a notification, that the processing of the invocation has been interrupted
	 * 
	 * @param exchange
	 */
	private void sendInvocationInterrupted(HttpExchange exchange) {
		connector.logError("Invocation has been interrupted!");
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "The invoction has been interrupted!");
	}

	/**
	 * 
	 * @param result
	 * @param contentType
	 * @param exchange
	 */
	private void sendInvocationSuccess(Serializable result, String[] contentType, HttpExchange exchange) {
		try {
			if (result != null) {
				byte[] responseBody = null;
				int statusCode = HttpURLConnection.HTTP_OK;
				// check if the client only accepts textual results
				boolean txtOnly = false;
				for (String type : contentType) {
					if (type.startsWith("text/")) {
						txtOnly = true;
					} else {
						txtOnly = false;
						break;
					}
				}
				exchange.getResponseHeaders().set("content-type",
						RESTMapper.join(contentType, RESTMapper.DEFAULT_MIME_SEPARATOR));
				if (result instanceof HttpResponse) {
					HttpResponse res = (HttpResponse) result;
					Pair<String>[] headers = res.listHeaders();
					for (Pair<String> header : headers) {
						exchange.getResponseHeaders().set(header.getOne(), header.getTwo());
					}
					statusCode = res.getStatus();
					responseBody = res.getResultRaw();
				} else if (txtOnly) {
					// client expects only text, make it happen
					responseBody = result.toString().getBytes();
				} else {
					// serialize result into byte array to get response body length
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ObjectOutput out = null;
					try {
						out = new ObjectOutputStream(bos);
						out.writeObject(result);
						responseBody = bos.toByteArray();
					} finally {
						try {
							if (out != null) {
								out.close();
							}
						} catch (IOException ex) {
							// ignore close exception
						}
						try {
							bos.close();
						} catch (IOException ex) {
							// ignore close exception
						}
					}
				}
				if (responseBody != null) {
					sendResponse(exchange, statusCode, responseBody.length);
					OutputStream os = exchange.getResponseBody();
					os.write(responseBody);
					os.close();
				} else {
					sendResponse(exchange, HttpURLConnection.HTTP_NO_CONTENT, NO_RESPONSE_BODY);
				}
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_NO_CONTENT, NO_RESPONSE_BODY);
			}
		} catch (IOException e) {
			connector.logMessage(e.getMessage());
		}
	}

	/**
	 * send a message about an unauthorized request
	 * 
	 * @param exchange
	 * @param answerMessage
	 * @param logMessage
	 */
	private void sendUnauthorizedResponse(HttpExchange exchange, String answerMessage, String logMessage) {
		connector.logMessage(logMessage);
		exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"LAS2peer WebConnector\"");
		if (answerMessage != null) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, answerMessage);
		} else {
			try {
				sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, NO_RESPONSE_BODY);
				// otherwise the client waits till the timeout for an answer
				exchange.getResponseBody().close();
			} catch (IOException e) {
				connector.logMessage(e.getMessage());
			}
		}
	}

	/**
	 * send a response that an internal error occurred
	 * 
	 * @param exchange
	 * @param message
	 */
	private void sendInternalErrorResponse(HttpExchange exchange, String message) {
		connector.logMessage(message);
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, message);
	}

	private void sendStringResponse(HttpExchange exchange, int responseCode, String response) {
		byte[] content = response.getBytes();
		exchange.getResponseHeaders().set("content-type", "text/plain");
		try {
			sendResponse(exchange, responseCode, content.length);
			OutputStream os = exchange.getResponseBody();
			os.write(content);
			os.close();
		} catch (IOException e) {
			connector.logMessage(e.getMessage());
		}
	}

	private void sendResponse(HttpExchange exchange, int responseCode, long contentLength) throws IOException {
		// add configured headers
		Headers responseHeaders = exchange.getResponseHeaders();
		if (connector.enableCrossOriginResourceSharing) {
			responseHeaders.add("Access-Control-Allow-Origin", connector.crossOriginResourceDomain);
			responseHeaders.add("Access-Control-Max-Age", String.valueOf(connector.crossOriginResourceMaxAge));
			// just reply all requested headers
			String requestedHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");
			if (requestedHeaders != null) {
				responseHeaders.add("Access-Control-Allow-Headers", requestedHeaders);
			}
			responseHeaders.add("Access-Control-Allow-Headers", "Authorization");
			responseHeaders.add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
		}
		exchange.sendResponseHeaders(responseCode, contentLength);
	}

}
