package i5.las2peer.webConnector;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import i5.las2peer.classLoaders.L2pClassLoader;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.persistency.EnvelopeException;
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
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web Connector. Each request will be distributed to
 * its corresponding session.
 */

public class WebConnectorRequestHandler implements HttpHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private static final String ACCESS_TOKEN_KEY = "access_token";
	private static final String OIDC_PROVIDER_KEY = "oidc_provider";
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
	 * @param request
	 * @param response
	 * @return -1 if no successful login else userId
	 * @throws UnsupportedEncodingException
	 */
	private PassphraseAgent authenticate(HttpExchange exchange) throws UnsupportedEncodingException {
		final int BASIC_PREFIX_LENGTH = "BASIC ".length();
		String userPass = "";
		String username = "";
		String password = "";

		// Default authentication:
		// check for authentication information in header
		if (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD)
				&& (exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).length() > BASIC_PREFIX_LENGTH)) {
			// looks like: Authentication Basic <Byte64(name:pass)>
			userPass = exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).substring(BASIC_PREFIX_LENGTH);
			userPass = new String(Base64.decode(userPass), "UTF-8");
			int separatorPos = userPass.indexOf(':');

			// get username and password
			username = userPass.substring(0, separatorPos);
			password = userPass.substring(separatorPos + 1);

			return login(username, password, exchange);
		}
		// OpenID Connect authentication:
		// check for access token in query parameter and headers

		// IMPORTANT NOTE: doing the same thing with authorization header and bearer token results in client-side
		// cross-domain errors despite correct config for CORS in LAS2peer Web Connector!
		else if (connector.oidcProviderInfos != null
				&& ((exchange.getRequestURI().getRawQuery() != null && exchange.getRequestURI().getRawQuery()
						.contains(ACCESS_TOKEN_KEY + "=")) || exchange.getRequestHeaders()
								.containsKey(ACCESS_TOKEN_KEY))) {
			String token = "";
			String oidcProviderURI = connector.defaultOIDCProvider;
			if (exchange.getRequestHeaders().containsKey(ACCESS_TOKEN_KEY)) { // get OIDC parameters from headers
				token = exchange.getRequestHeaders().getFirst(ACCESS_TOKEN_KEY);
				oidcProviderURI = URLDecoder.decode(exchange.getRequestHeaders().getFirst(OIDC_PROVIDER_KEY), "UTF-8");
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
				sendInternalErrorResponse(exchange,
						"The OIDC config is not known for the given provider (" + oidcProviderURI
								+ ")! Please make sure the right URI is added to the config.");
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

				String sub = (String) ujson.get("sub");

				long oidcAgentId = hash(sub);
				username = oidcAgentId + "";
				password = sub;

				PassphraseAgent pa;
				try {
					pa = (PassphraseAgent) l2pNode.getAgent(oidcAgentId);
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
					// System.out.println("### numreq " +numReq);
				} else {
					this.connector.getOpenUserRequests().put(userId, 1);
					// System.out.println("### numreq 0" );
				}
			}
			userAgent = (PassphraseAgent) l2pNode.getAgent(userId);

			/* if ( ! (userAgent instanceof PassphraseAgent ))
			    throw new L2pSecurityException ("Agent is not passphrase protected!");*/

			userAgent.unlockPrivateKey(password);

			return userAgent;
		} catch (AgentNotKnownException e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress() + ": login denied for user "
					+ username);
		} catch (L2pSecurityException e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress()
					+ ": unauth access - prob. login problems");
		} catch (Exception e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress()
					+ ": something went horribly wrong. Check your request for correctness.");
		}
		return null;
	}

	/**
	 * Delegates the request data to a service method, which then decides what to do with it (maps it internally)
	 * 
	 * @param request
	 * @param response
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
			java.util.Scanner s = new Scanner(is);
			s.useDelimiter("\\A");
			String content = s.hasNext() ? s.next() : "";
			s.close();

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
					content, contentTypeHeader, acceptHeader, headers, warnings);

			if (invocation.length == 0) {
				if (warnings.length() > 0) {
					sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
							warnings.toString().replaceAll("\n", " "));
				} else {
					sendResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, 0);
					// otherwise the client waits till the timeout for an answer
					exchange.getResponseBody().close();
				}
				return false;
			}

			for (InvocationData inv : invocation) {
				try {
					// invoke service method
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
		} catch (Exception e) {
			connector.logError("Error occured:" + exchange.getRequestURI().getPath() + " " + e.getMessage());
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
						l2pNode.unregisterAgent(userAgent);
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
					l2pNode.unregisterAgent(userAgent);
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
			sendResponse(exchange, HttpURLConnection.HTTP_OK, 0);
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
	 * @return HTTP code OK (200) with resource's documentation as String or an error code including String message.
	 */
	private void handleSwagger(HttpExchange exchange) {
		try {
			L2pClassLoader clsLoader = (L2pClassLoader) connector.getL2pNode().getBaseClassLoader();
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
			}
			try {
				String json = Json.mapper().writeValueAsString(swagger);
				sendStringResponse(exchange, HttpURLConnection.HTTP_OK, json);
			} catch (JsonProcessingException e) {
				sendInternalErrorResponse(exchange, e.getMessage());
			}
		} catch (EnvelopeException e) {
			sendInternalErrorResponse(exchange, e.getMessage());
		}
	}

	/**
	 * send a notification, that the requested service does not exists
	 * 
	 * @param request
	 * @param response
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
	 * @param request
	 * @param response
	 */
	private void sendNoSuchMethod(HttpExchange exchange) {
		connector.logError("Invocation request " + exchange.getRequestURI().getPath() + " for unknown service method");
		sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
				"The method you requested is not known to this service!");
	}

	/**
	 * send a notification, that security problems occurred during the requested service method
	 * 
	 * @param request
	 * @param response
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
	 * @param request
	 * @param response
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
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendInvocationException(HttpExchange exchange, ServiceInvocationException e) {
		connector.logError("Exception while processing RMI: " + exchange.getRequestURI().getPath());
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
	 * @param request
	 * @param response
	 */
	private void sendInvocationInterrupted(HttpExchange exchange) {
		connector.logError("Invocation has been interrupted!");
		sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, "The invoction has been interrupted!");
	}

	/**
	 * 
	 * @param result
	 * @param contentType
	 * @param response
	 */
	private void sendInvocationSuccess(Serializable result, String[] contentType, HttpExchange exchange) {
		try {
			if (result != null) {
				String msg = null;
				int statusCode = HttpURLConnection.HTTP_OK;
				exchange.getResponseHeaders().set("content-type",
						RESTMapper.join(contentType, RESTMapper.DEFAULT_MIME_SEPARATOR));
				if (result instanceof i5.las2peer.restMapper.HttpResponse) {
					i5.las2peer.restMapper.HttpResponse res = (i5.las2peer.restMapper.HttpResponse) result;
					Pair<String>[] headers = res.listHeaders();
					for (Pair<String> header : headers) {
						exchange.getResponseHeaders().set(header.getOne(), header.getTwo());
					}
					statusCode = res.getStatus();
					msg = res.getResult();
				} else {
					msg = RESTMapper.castToString(result);
				}
				if (msg != null) {
					byte[] content = msg.getBytes();
					sendResponse(exchange, statusCode, content.length);
					OutputStream os = exchange.getResponseBody();
					os.write(content);
					os.close();
				} else {
					sendResponse(exchange, HttpURLConnection.HTTP_NO_CONTENT, 0);
				}
			} else {
				sendResponse(exchange, HttpURLConnection.HTTP_NO_CONTENT, 0);
			}
		} catch (IOException e) {
			connector.logMessage(e.getMessage());
		}
	}

	/**
	 * send a message about an unauthorized request
	 * 
	 * @param response
	 * @param logMessage
	 */
	private void sendUnauthorizedResponse(HttpExchange exchange, String answerMessage, String logMessage) {
		connector.logMessage(logMessage);
		exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"LAS2peer WebConnector\"");
		if (answerMessage != null) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, answerMessage);
		} else {
			try {
				sendResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, 0);
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
	 * @param response
	 * @param answerMessage
	 * @param logMessage
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
			responseHeaders.add("Access-Control-Max-Age",
					String.valueOf(connector.crossOriginResourceMaxAge));
			// just reply all requested headers
			String requestedHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");
			if (requestedHeaders != null) {
				responseHeaders.add("Access-Control-Allow-Headers", requestedHeaders);
			}
			responseHeaders.add("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
		}
		exchange.sendResponseHeaders(responseCode, contentLength);
	}

}
