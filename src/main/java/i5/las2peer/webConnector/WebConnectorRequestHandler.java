package i5.las2peer.webConnector;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * A HttpServer RequestHandler for handling requests to the LAS2peer Web Connector.
 * Each request will be distributed to its corresponding session.
 */

public class WebConnectorRequestHandler implements HttpHandler {

	private static final String AUTHENTICATION_FIELD = "Authorization";
	private WebConnector connector;
	private Node l2pNode;

	public static final int STATUS_CONTINUE = 100; // Continue (Http/1.1)
	public static final int STATUS_SWITCHING = 101; // Switching Protocols (Http/1.1)
	// 2XX ? Success
	public static final int STATUS_OK = 200; // OK
	public static final int STATUS_CREATED = 201; // Created
	public static final int STATUS_ACCEPTED = 202; // Accepted
	public static final int STATUS_NON_AUTH = 203; // Non-Authoritative Information (HTTP/1.1)
	public static final int STATUS_NO_CONTENT = 204; // No Content
	public static final int STATUS_RESET_CONTENT = 205; // Reset Content (HTTP/1.1)
	public static final int STATUS_PARTIAL_CONTENT = 206; // Partial Content (HTTP/1.1)
	// 3XX ? Redirection ? the requested document is to be found at some other location
	public static final int STATUS_MULTIPLE_CHOICES = 300; // Multiple Choices (HTTP/1.1)
	public static final int STATUS_MOVED_PERM = 301; // Moved Permanently
	public static final int STATUS_FOUND = 302; // Found
	public static final int STATUS_SEE_OTHER = 303; // See Other (HTTP/1.1)
	public static final int STATUS_NOT_MODIFIED = 304; // Not Modified
	public static final int STATUS_USE_PROXY = 305; // Use Proxy (HTTP/1.1)
	public static final int STATUS_TEMP_REDIRECT = 307; // Temporary Redirect (HTTP/1.1)
	// 4XX ? Error of the client - e.g. errornous requests
	public static final int STATUS_BAD_REQUEST = 400; // Bad Request
	public static final int STATUS_UNAUTHORIZED = 401; // Unauthorized
	public static final int STATUS_PAYMENT_REQUIRED = 402; // Payment Required (Unused) (HTTP/1.1)
	public static final int STATUS_FORBIDDEN = 403; // Forbidden
	public static final int STATUS_NOT_FOUND = 404; // Not Found
	public static final int STATUS_METHOD_NOT_ALLOWED = 405; // Method Not Allowed (HTTP/1.1)
	public static final int STATUS_NOT_ACCEPTABLE = 406; // Not Acceptable (HTTP/1.1)
	public static final int STATUS_PROXY_AUTH_REQUIRED = 407; // Proxy Authentication Required (HTTP/1.1)
	public static final int STATUS_REQUEST_TIMEOUT = 408; // Request Timeout (HTTP/1.1)
	public static final int STATUS_CONFLICT = 409; // Conflict (HTTP/1.1)
	public static final int STATUS_GONE = 410; // Gone (HTTP/1.1)
	public static final int STATUS_LENGTH_REQUIRED = 411; // Length Required (HTTP/1.1)
	public static final int STATUS_PRECONDITION_FAILED = 412; // Precondition Failed (HTTP/1.1)
	public static final int STATUS_REQUEST_ENTITY_TOO_LONG = 413; // Request Entity Too Long (HTTP/1.1)
	public static final int STATUS_REQUEST_URI_TOO_LONG = 414; // Request-URI Too Long (HTTP/1.1)
	public static final int STATUS_UNSUPPORTED_MEDIA_TYPE = 415; // Unsupported Media Type (HTTP/1.1)
	public static final int STATUS_REQUEST_RANGE_NOT_SATISFIABLE = 416; // Requested Range Not Satisfiable (HTTP/1.1)
	public static final int STATUS_EXPECTATION_FAILED = 417; // Expectation Failed (HTTP/1.1)
	// 5XX ? Error of the server
	public static final int STATUS_INTERNAL_SERVER_ERROR = 500; // Internal Server Error
	public static final int STATUS_NOT_IMPLEMENTED = 501; // Not Implemented
	public static final int STATUS_BAD_GATEWAY = 502; // Bad Gateway
	public static final int STATUS_SERVICE_UNAVAILABLE = 503; // Service Unavailable
	public static final int STATUS_GATEWAY_TIMEOUT = 504; // Gateway Timeout (HTTP/1.1)
	public static final int STATUS_HTTP_VERSION_NOT_SUPPORTED = 505; // HTTP Version Not Supported (HTTP/1.1)
	/** HTTP Status Messages **/
	public static final String CODE_202_MESSAGE = "OK";
	public static final String CODE_404_MESSAGE = "forbidden";
	public static final String CODE_500_MESSAGE = "Internal Server Error";

	public WebConnectorRequestHandler(WebConnector connector) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();
	}

	/**
	 * set the connector handling this request processor
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
		// check for access token in query parameter

		// IMPORTANT NOTE: doing the same thing with authorization header and bearer token results in client-side
		// cross-domain errors despite correct config for CORS in LAS2peer Web Connector!
		else if (connector.oidcProviderInfo != null && exchange.getRequestURI().getRawQuery() != null
				&& exchange.getRequestURI().getRawQuery().contains("access_token=")) {
			String[] params = exchange.getRequestURI().getRawQuery().split("&");
			String token = "";
			for (int i = 0; i < params.length; i++) {
				String[] keyval = params[i].split("=");
				if (keyval[0].equals("access_token")) {
					token = keyval[1];
				}
			}

			// send request to OpenID Connect user info endpoint to retrieve complete user information
			// in exchange for access token.
			HTTPRequest hrq;
			HTTPResponse hrs;

			try {
				URI userinfoEndpointUri = new URI(
						(String) ((JSONObject) connector.oidcProviderInfo.get("config")).get("userinfo_endpoint"));
				hrq = new HTTPRequest(Method.GET, userinfoEndpointUri.toURL());
				hrq.setAuthorization("Bearer " + token);

				// TODO: process all error cases that can happen (in particular invalid tokens)
				hrs = hrq.send();
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
				sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR,
						"Unexpected authentication error: " + e.getMessage());
				return null;
			}

			// process response from OpenID Connect user info endpoint
			UserInfoResponse userInfoResponse;
			try {
				userInfoResponse = UserInfoResponse.parse(hrs);
			} catch (ParseException e) {
				sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR,
						"Couldn't parse UserInfo response: " + e.getMessage());
				return null;
			}

			// failed request for OpenID Connect user info will result in no agent being returned.
			if (userInfoResponse instanceof UserInfoErrorResponse) {
				UserInfoErrorResponse uier = (UserInfoErrorResponse) userInfoResponse;
				sendStringResponse(exchange, STATUS_UNAUTHORIZED, "Open ID Connect UserInfo request failed! Cause: "
						+ uier.getErrorObject().getDescription());
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
				sendStringResponse(exchange, STATUS_UNAUTHORIZED, e.getMessage());
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
				// TODO this only returns the first header value for this key
				String value = entry.getValue().get(0).trim();
				headersList.add(new Pair<String>(key, value));
				// fetch MIME types
				if (key.toLowerCase().equals("accept") && !value.isEmpty()) {
					acceptHeader = value;
				} else if (key.toLowerCase().equals("content-type") && !value.isEmpty()) {
					contentTypeHeader = value;
				}
			}
			@SuppressWarnings("unchecked")
			Pair<String>[] headers = headersList.toArray(new Pair[headersList.size()]);

			Serializable result = "";
			Mediator mediator = l2pNode.getOrRegisterLocalMediator(userAgent);
			boolean gotResult = false;
			String returnMIMEType = "text/plain";
			StringBuilder warnings = new StringBuilder();
			InvocationData[] invocation = RESTMapper.parse(this.connector.getMappingTree(), httpMethod, uri, variables,
					content, contentTypeHeader, acceptHeader, headers, warnings);

			if (invocation.length == 0) {
				if (warnings.length() > 0) {
					sendStringResponse(exchange, STATUS_NOT_FOUND, warnings.toString().replaceAll("\n", " "));
				} else {
					exchange.sendResponseHeaders(STATUS_NOT_FOUND, 0);
				}
				return false;
			}

			for (int i = 0; i < invocation.length; i++) {
				try {
					result = mediator.invoke(invocation[i].getServiceName(), invocation[i].getMethodName(),
							invocation[i].getParameters(), connector.preferLocalServices());// invoke service method
					gotResult = true;
					returnMIMEType = invocation[i].getMIME();
					break;
				} catch (NoSuchServiceException | TimeoutException e) {
					sendNoSuchService(exchange, invocation[i].getServiceName());
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
		exchange.getResponseHeaders().set("Server-Name", "LAS2peer");
		exchange.getResponseHeaders().set("content-type", "text/xml");

		PassphraseAgent userAgent;
		if ((userAgent = authenticate(exchange)) != null) {
			invoke(userAgent, exchange);
			logout(userAgent);
		}
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

	/**
	 * send a notification, that the requested service does not exists
	 * @param request
	 * @param response
	 * @param service
	 */
	private void sendNoSuchService(HttpExchange exchange, String service) {
		connector.logError("Service not found: " + service);
		sendStringResponse(exchange, STATUS_SERVICE_UNAVAILABLE,
				"The service you requested is not known to this server!");
	}

	/**
	 * send a notification, that the requested method does not exists at the requested service
	 * @param request
	 * @param response
	 */
	private void sendNoSuchMethod(HttpExchange exchange) {
		connector.logError("Invocation request " + exchange.getRequestURI().getPath() + " for unknown service method");
		sendStringResponse(exchange, STATUS_NOT_FOUND, "The method you requested is not known to this service!");
	}

	/**
	 * send a notification, that security problems occurred during the requested service method
	 * @param request
	 * @param response
	 * @param e
	 */
	private void sendSecurityProblems(HttpExchange exchange, L2pSecurityException e) {
		connector.logError("Security exception in invocation request " + exchange.getRequestURI().getPath());
		sendStringResponse(exchange, STATUS_FORBIDDEN, "You don't have access to the method you requested");

		if (System.getProperty("http-connector.printSecException") != null
				&& System.getProperty("http-connector.printSecException").equals("true")) {
			e.printStackTrace();
		}
	}

	/**
	 * send a notification, that the result of the service invocation is
	 * not transportable 
	 * 
	 * @param request
	 * @param response
	 */
	private void sendResultInterpretationProblems(HttpExchange exchange) {
		connector.logError("Exception while processing RMI: " + exchange.getRequestURI().getPath());
		// result interpretation problems
		sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR, "The result of the method call is not transferable!");
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
		sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR, code);
	}

	/**
	 * send a notification, that the processing of the invocation has been interrupted
	 * 
	 * @param request
	 * @param response
	 */
	private void sendInvocationInterrupted(HttpExchange exchange) {
		connector.logError("Invocation has been interrupted!");
		sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR, "The invoction has been interrupted!");
	}

	/**
	 * 
	 * @param result
	 * @param contentType
	 * @param response
	 */
	private void sendInvocationSuccess(Serializable result, String contentType, HttpExchange exchange) {
		try {
			if (result != null) {
				String msg = null;
				int statusCode = STATUS_OK;
				exchange.getResponseHeaders().set("content-type", contentType);
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
					exchange.sendResponseHeaders(statusCode, content.length);
					OutputStream os = exchange.getResponseBody();
					os.write(content);
					os.close();
				} else {
					exchange.sendResponseHeaders(STATUS_NO_CONTENT, 0);
				}
			} else {
				exchange.sendResponseHeaders(STATUS_NO_CONTENT, 0);
			}
		} catch (IOException e) {
			connector.logMessage(e.getMessage());
		}
	}

	/**
	 * send a message about an unauthorized request
	 * @param response
	 * @param logMessage
	 */
	private void sendUnauthorizedResponse(HttpExchange exchange, String answerMessage, String logMessage) {
		connector.logMessage(logMessage);
		exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"LAS2peer WebConnector\"");
		if (answerMessage != null) {
			sendStringResponse(exchange, STATUS_UNAUTHORIZED, answerMessage);
		} else {
			try {
				exchange.sendResponseHeaders(STATUS_UNAUTHORIZED, 0);
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
	private void sendInternalErrorResponse(HttpExchange exchange, String answerMessage, String logMessage) {
		connector.logMessage(logMessage);
		sendStringResponse(exchange, STATUS_INTERNAL_SERVER_ERROR, answerMessage);
	}

	private void sendStringResponse(HttpExchange exchange, int responseCode, String response) {
		byte[] content = response.getBytes();
		exchange.getResponseHeaders().set("content-type", "text/plain");
		try {
			exchange.sendResponseHeaders(responseCode, content.length);
			OutputStream os = exchange.getResponseBody();
			os.write(content);
			os.close();
		} catch (IOException e) {
			connector.logMessage(e.getMessage());
		}
	}

}
