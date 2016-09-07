package i5.las2peer.webConnector;

import i5.las2peer.classLoaders.ClassLoaderException;
import i5.las2peer.classLoaders.L2pClassManager;
import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.NoSuchServiceMethodException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.p2p.ServiceNotFoundException;
import i5.las2peer.p2p.ServiceVersion;
import i5.las2peer.p2p.TimeoutException;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.data.InvocationData;
import i5.las2peer.restMapper.data.Pair;
import i5.las2peer.restMapper.data.PathTree;
import i5.las2peer.restMapper.exceptions.NoMethodFoundException;
import i5.las2peer.restMapper.exceptions.NotSupportedUriPathException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgent;
import i5.las2peer.security.UserAgent;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.util.Json;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minidev.json.JSONObject;
import rice.p2p.util.Base64;

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

/**
 * A HttpServer RequestHandler for handling requests to the las2peer Web Connector. Each request will be distributed to
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
	 * Handles a request (login, invoke)
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		exchange.getResponseHeaders().set("Server-Name", "las2peer WebConnector");

		if (exchange.getRequestMethod().equalsIgnoreCase("options")) {
			// check for an OPTIONS request and auto answer it
			// TODO this should become a default reply for OPTIONS-requests,
			// but should be also be available to service developers
			sendResponse(exchange, HttpURLConnection.HTTP_OK, NO_RESPONSE_BODY);
		} else if (exchange.getRequestMethod().equalsIgnoreCase("get")
				&& exchange.getRequestURI().getPath().toString().endsWith("/swagger.json")) {
			// respond with swagger.json for the given service
			handleSwagger(exchange);
		} else {
			PassphraseAgent userAgent;
			if ((userAgent = authenticate(exchange)) != null) {
				invoke(userAgent, exchange);
			}
		}
		// otherwise the client waits till the timeout for an answer
		exchange.getResponseBody().close();
	}

	/**
	 * Logs in a las2peer user
	 * 
	 * @param exchange
	 * 
	 * @return null if no successful login else agent id
	 * @throws UnsupportedEncodingException
	 */
	private PassphraseAgent authenticate(HttpExchange exchange) throws UnsupportedEncodingException {
		if (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD)
				&& exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).toLowerCase().startsWith("basic ")) {
			// basic authentication
			return authenticateBasic(exchange);
		} else if (connector.oidcProviderInfos != null
				&& ((exchange.getRequestURI().getRawQuery() != null && exchange.getRequestURI().getRawQuery()
						.contains(ACCESS_TOKEN_KEY + "="))
						|| exchange.getRequestHeaders().containsKey(ACCESS_TOKEN_KEY) || (exchange.getRequestHeaders()
						.containsKey(AUTHENTICATION_FIELD) && exchange.getRequestHeaders()
						.getFirst(AUTHENTICATION_FIELD).toLowerCase().startsWith("bearer ")))) {
			// openid connect
			return authenticateOIDC(exchange);
		} else if (connector.defaultLoginUser.length() > 0) {
			// anonymous login
			return authenticateNamePassword(connector.defaultLoginUser, connector.defaultLoginPassword, exchange);
		} else {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress() + ": No Authentication provided!");
		}
		return null;
	}

	private PassphraseAgent authenticateBasic(HttpExchange exchange) {
		// looks like: Authentication Basic <Byte64(name:pass)>
		String userPass = exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).substring("BASIC ".length());
		userPass = new String(Base64.decode(userPass), StandardCharsets.UTF_8);
		int separatorPos = userPass.indexOf(':');

		// get username and password
		String username = userPass.substring(0, separatorPos);
		String password = userPass.substring(separatorPos + 1);

		return authenticateNamePassword(username, password, exchange);
	}

	private PassphraseAgent authenticateOIDC(HttpExchange exchange) throws UnsupportedEncodingException {
		// extract token
		String token = "";
		String oidcProviderURI = connector.defaultOIDCProvider;
		if (exchange.getRequestHeaders().containsKey(ACCESS_TOKEN_KEY)) {
			// get OIDC parameters from headers
			token = exchange.getRequestHeaders().getFirst(ACCESS_TOKEN_KEY);
			if (exchange.getRequestHeaders().containsKey(OIDC_PROVIDER_KEY))
				oidcProviderURI = URLDecoder.decode(exchange.getRequestHeaders().getFirst(OIDC_PROVIDER_KEY), "UTF-8");
		} else if (exchange.getRequestHeaders().containsKey(AUTHENTICATION_FIELD)
				&& exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).toLowerCase().startsWith("bearer ")) {
			// get BEARER token from Authentication field
			token = exchange.getRequestHeaders().getFirst(AUTHENTICATION_FIELD).substring("BEARER ".length());
			if (exchange.getRequestHeaders().containsKey(OIDC_PROVIDER_KEY))
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
		if (!connector.oidcProviders.contains(oidcProviderURI)
				|| connector.oidcProviderInfos.get(oidcProviderURI) == null) {
			sendInternalErrorResponse(exchange, "The given OIDC provider (" + oidcProviderURI
					+ ") is not whitelisted! Please make sure the complete OIDC provider URI is added to the config.");
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

		// failed request for OpenID Connect user info
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

		// successful request
		UserInfo userInfo = ((UserInfoSuccessResponse) userInfoResponse).getUserInfo();

		JSONObject ujson = userInfo.toJSONObject();

		if (!ujson.containsKey("sub") || !ujson.containsKey("email") || !ujson.containsKey("preferred_username")) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_FORBIDDEN,
					"Could not get provider information. Please check your scopes.");
			return null;
		}

		String sub = (String) ujson.get("sub");

		// TODO: choose other scheme for generating agent password.
		long oidcAgentId = hash(sub);
		String password = sub;

		synchronized (this.connector.getLockOidc()) {
			// TODO lock by agent id for more concurrency
			try {
				try {
					PassphraseAgent pa = (PassphraseAgent) l2pNode.getAgent(oidcAgentId);
					pa.unlockPrivateKey(password);
					if (pa instanceof UserAgent) {
						UserAgent ua = (UserAgent) pa;
						ua.setUserData(ujson.toJSONString());
						return ua;
					} else {
						return pa;
					}
				} catch (L2pSecurityException e) {
					sendStringResponse(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, e.getMessage());
					return null;
				}
			} catch (AgentNotKnownException e) {
				UserAgent oidcAgent;
				try {
					oidcAgent = UserAgent.createUserAgent(oidcAgentId, password);
					oidcAgent.unlockPrivateKey(password);

					oidcAgent.setEmail((String) ujson.get("email"));
					oidcAgent.setLoginName((String) ujson.get("preferred_username"));
					oidcAgent.setUserData(ujson.toJSONString());

					l2pNode.storeAgent(oidcAgent);

					return oidcAgent;
				} catch (Exception e1) {
					e1.printStackTrace();
					sendStringResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, e1.getMessage());
					return null;
				}
			}
		}
	}

	private PassphraseAgent authenticateNamePassword(String username, String password, HttpExchange exchange) {
		try {
			long userId;
			PassphraseAgent userAgent;

			if (username.matches("-?[0-9].*")) { // username is id
				try {
					userId = Long.valueOf(username);
				} catch (NumberFormatException e) {
					throw new L2pSecurityException("The given user does not contain a valid agent id!");
				}
			} else {// username is string
				userId = l2pNode.getAgentIdForLogin(username);
			}

			userAgent = (PassphraseAgent) l2pNode.getAgent(userId);
			userAgent.unlockPrivateKey(password);

			return userAgent;
		} catch (AgentNotKnownException e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress() + ": user " + username + " not found");
		} catch (L2pSecurityException e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress() + ": passphrase invalid for user "
					+ username);
		} catch (Exception e) {
			sendUnauthorizedResponse(exchange, null, exchange.getRemoteAddress()
					+ ": something went horribly wrong. Check your request for correctness.");
		}
		return null;
	}

	// helper function to create long hash from string
	private static long hash(String string) {
		long h = 1125899906842597L; // prime
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i);
		}
		return h;
	}

	/**
	 * Delegates the request data to a service method, which then decides what to do with it (maps it internally)
	 * 
	 * @param agent
	 * @param exchange
	 * 
	 * @return
	 */
	private boolean invoke(PassphraseAgent agent, HttpExchange exchange) {
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
			boolean overflow = false;
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				if (buffer.size() < connector.maxRequestBodySize - data.length) {
					// still space left in local buffer
					buffer.write(data, 0, nRead);
				} else {
					overflow = true;
					// no break allowed otherwise the client gets an exception (like connection closed)
					// so we have to read all given content
				}
			}
			if (overflow) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_ENTITY_TOO_LARGE,
						"Given request body exceeds limit of " + connector.maxRequestBodySize + " bytes");
				return false;
			}
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

			// extract service information from uri
			if (uri.startsWith("/"))
				uri = uri.substring(1);
			String[] uriSplit = uri.split("/");
			String serviceName;
			ServiceNameVersion requiredService;

			try {
				serviceName = l2pNode.getServiceAliasManager().getServiceNameByAlias(uriSplit[0]);
			} catch (ServiceNotFoundException e1) {
				throw new NoSuchServiceException(uriSplit[0], e1);
			}

			try {
				if (uriSplit.length > 1) {
					requiredService = new ServiceNameVersion(serviceName, uriSplit[1].substring(1));

					// remove version info from URI
					uri = uriSplit[0];
					for (int i = 2; i < uriSplit.length; i++)
						uri += "/" + uriSplit[i];
				} else {
					requiredService = new ServiceNameVersion(serviceName, "*");
				}
			} catch (IllegalArgumentException e) {
				requiredService = new ServiceNameVersion(serviceName, "*");
			}

			// invoke
			Serializable result = "";
			boolean gotResult = false;
			String[] returnMIMEType = RESTMapper.DEFAULT_PRODUCES_MIME_TYPE;
			StringBuilder warnings = new StringBuilder();
			PathTree tree;
			try {
				tree = connector.getServiceRepositoryManager().getServiceTree(requiredService, agent,
						connector.onlyLocalServices());
			} catch (Exception e) {
				throw new NoSuchServiceException(requiredService.toString(), e);
			}
			InvocationData[] invocation = RESTMapper.parse(tree, httpMethod, uri, variables, rawContent,
					contentTypeHeader, acceptHeader, headers, warnings);

			if (invocation.length == 0) {
				if (warnings.length() > 0) {
					sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
							warnings.toString().replaceAll("\n", " "));
				} else {
					sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "could not match REST mapping");
				}
				return false;
			}

			for (InvocationData inv : invocation) {
				try {
					// invoke service method
					Mediator mediator = l2pNode.createMediatorForAgent(agent);
					result = mediator.invoke(requiredService.toString(), inv.getMethodName(), inv.getParameters(),
							connector.onlyLocalServices());
					gotResult = true;
					returnMIMEType = inv.getMIME();
					break;
				} catch (NoSuchServiceException | TimeoutException e) {
					sendNoSuchService(exchange, requiredService.toString());
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
		} catch (NoSuchServiceException e) {
			sendNoSuchService(exchange, e.toString());
		} catch (NumberFormatException e) {
			sendMalformedRequest(exchange, e.toString());
		} catch (Exception e) {
			connector.logError("Error occured:" + exchange.getRequestURI().getPath() + " " + e.getMessage(), e);
			sendInternalErrorResponse(exchange, e.toString());
		}
		return false;
	}

	/**
	 * Returns the API documentation of all annotated local resources for purposes of Swagger documentation.
	 * 
	 * @param exchange The origin exchange request for the swagger listing.
	 */
	private void handleSwagger(HttpExchange exchange) {
		// extract service alias from path
		String[] uriSplit = exchange.getRequestURI().getPath().split("/");
		String serviceName;
		ServiceVersion serviceVersion;

		if (uriSplit.length == 3) {
			if (!uriSplit[2].equals("swagger.json")) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Invalid Path!");
				return;
			}

			serviceVersion = null;
		} else if (uriSplit.length == 4) {
			if (!uriSplit[3].equals("swagger.json")) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Invalid Path!");
				return;
			}

			try {
				serviceVersion = new ServiceVersion(uriSplit[2].substring(1));
			} catch (IllegalArgumentException e) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Invalid version information!");
				return;
			}
		} else {
			sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Invalid Path!");
			return;
		}

		try {
			serviceName = l2pNode.getServiceAliasManager().getServiceNameByAlias(uriSplit[1]);
		} catch (ServiceNotFoundException e1) {
			sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Could not find any service for this path!");
			return;
		}

		try {
			L2pClassManager clsLoader = connector.getL2pNode().getBaseClassLoader();
			Class<?> serviceClass;
			try {
				if (serviceVersion == null) {
					serviceClass = clsLoader.getServiceClass(serviceName);
				} else {
					serviceClass = clsLoader.getServiceClass(serviceName, serviceVersion.toString());

				}
			} catch (IllegalArgumentException | ClassLoaderException e) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND,
						"Swagger works only for locally running services!");
				return;
			}
			Swagger swagger = new Reader(new Swagger()).read(serviceClass);
			if (swagger == null) {
				sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Swagger API declaration not available!");
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
						RESTMapper.join(contentType, RESTMapper.DEFAULT_MIME_SEPARATOR) + "; charset=utf-8");
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
		exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"las2peer WebConnector\"");
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
