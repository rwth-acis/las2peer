package i5.las2peer.connectors.webConnector;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.connectors.webConnector.handler.WebappHandler;
import i5.las2peer.connectors.webConnector.util.AuthenticationManager;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentAlreadyRegisteredException;
import i5.las2peer.p2p.AliasNotFoundException;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.ServiceAliasManager.AliasResolveResponse;
import i5.las2peer.restMapper.RESTResponse;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.Mediator;
import i5.las2peer.tools.SimpleTools;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.util.Json;
import net.minidev.json.JSONObject;

/**
 * A HttpServer RequestHandler for handling requests to the las2peer Web Connector. Each request will be distributed to
 * its corresponding session.
 * 
 */
@Path("/")
public class WebConnectorRequestHandler {

	private static final List<String> INGORE_HEADERS = Arrays.asList(HttpHeaders.AUTHORIZATION.toLowerCase(),
			AuthenticationManager.ACCESS_TOKEN_KEY.toLowerCase(),
			AuthenticationManager.OIDC_PROVIDER_KEY.toLowerCase());
	private static final List<String> IGNORE_QUERY_PARAMS = Arrays.asList(
			AuthenticationManager.ACCESS_TOKEN_KEY.toLowerCase(),
			AuthenticationManager.OIDC_PROVIDER_KEY.toLowerCase());

	private WebConnector connector;
	private Node l2pNode;

	private final L2pLogger logger = L2pLogger.getInstance(WebConnectorRequestHandler.class.getName());

	public WebConnectorRequestHandler(WebConnector connector) {
		this.connector = connector;
		l2pNode = connector.getL2pNode();
	}

	@GET
	public Response rootPath() throws URISyntaxException {
		return Response.temporaryRedirect(new URI(WebappHandler.DEFAULT_ROUTE)).build();
	}

	@GET
	@Path("/favicon.ico")
	@Produces("image/x-icon")
	public Response getFavicon() throws IOException {
		byte[] bytes = null;
		try {
			FileInputStream fis = new FileInputStream("etc/favicon.ico");
			bytes = SimpleTools.toByteArray(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			// use fallback from classpath
			InputStream is = getClass().getResourceAsStream("/favicon.ico");
			if (is != null) {
				bytes = SimpleTools.toByteArray(is);
				is.close();
			}
		}
		return Response.ok(bytes, "image/x-icon").build();
	}

	@GET
	@Path("/{paths: .+}")
	public Response handleGET(@PathParam("paths") List<PathSegment> paths, @Context UriInfo uriInfo,
			@Context HttpHeaders requestHeaders) throws Exception {
		return handle(HttpMethod.GET, uriInfo, paths, null, requestHeaders);
	}

	@POST
	@Path("/{paths: .+}")
	public Response handlePOST(@PathParam("paths") List<PathSegment> paths, @Context UriInfo uriInfo,
			@Context HttpHeaders requestHeaders, InputStream requestBody) throws Exception {
		return handle(HttpMethod.POST, uriInfo, paths, requestBody, requestHeaders);
	}

	@PUT
	@Path("/{paths: .+}")
	public Response handlePUT(@PathParam("paths") List<PathSegment> paths, @Context UriInfo uriInfo,
			@Context HttpHeaders requestHeaders, InputStream requestBody) throws Exception {
		return handle(HttpMethod.PUT, uriInfo, paths, requestBody, requestHeaders);
	}

	@DELETE
	@Path("/{paths: .+}")
	public Response handleDELETE(@PathParam("paths") List<PathSegment> paths, @Context UriInfo uriInfo,
			@Context HttpHeaders requestHeaders, InputStream requestBody) throws Exception {
		return handle(HttpMethod.DELETE, uriInfo, paths, requestBody, requestHeaders);
	}

	private Response handle(String requestMethod, UriInfo uriInfo, List<PathSegment> paths, InputStream requestBody,
			HttpHeaders httpHeaders) throws Exception {
		MultivaluedMap<String, String> requestHeaders = httpHeaders.getRequestHeaders();
		try {
			Mediator mediator = authenticate(requestHeaders,
					uriInfo.getQueryParameters().getFirst(AuthenticationManager.ACCESS_TOKEN_KEY));
			if (mediator != null) {
				ArrayList<String> strPaths = new ArrayList<>(paths.size());
				for (PathSegment seg : paths) {
					strPaths.add(seg.getPath());
				}
				return resolveServiceAndInvoke(mediator, strPaths, requestMethod, uriInfo, requestBody, requestHeaders);
			} else {
				// XXX refactor: is this necesssary?
				throw new InternalServerErrorException("Authorization failed! Agent is null");
			}
		} catch (NotFoundException e) {
			connector.logError("not found: " + e.getMessage(), e);
			return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
		} catch (NotAuthorizedException e) {
			connector.logError("authorization error: " + e.getMessage(), e);
			logger.warning("authorization error: " + e.getMessage());
			return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
		} catch (InternalServerErrorException e) {
			connector.logError("Internal Server Error: " + e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}

	private Mediator authenticate(MultivaluedMap<String, String> requestHeaders, String accessTokenQueryParam) {
		AgentImpl agent = connector.authenticateAgent(requestHeaders, accessTokenQueryParam);
		try {
			return l2pNode.createMediatorForAgent(agent);
		} catch (AgentAlreadyRegisteredException | AgentLockedException e) {
			// should not occur, since agent is known and unlocked at this point
			throw new InternalServerErrorException("Mediator could not be created", e);
		}
	}

	private Response resolveServiceAndInvoke(Mediator mediator, ArrayList<String> pathSplit, String requestMethod,
			UriInfo uriInfo, InputStream requestBody, MultivaluedMap<String, String> requestHeaders)
			throws URISyntaxException {
		String requestPath = "/" + String.join("/", pathSplit);
		// resolve service name
		String serviceName;
		int serviceAliasLength;
		try {
			AliasResolveResponse response = l2pNode.getServiceAliasManager().resolvePathToServiceName(requestPath);
			serviceName = response.getServiceName();
			serviceAliasLength = response.getNumMatchedParts();
		} catch (AliasNotFoundException e1) {
			throw new NotFoundException("Could not resolve " + requestPath + " to a service name.", e1);
		}
		// get service version
		ServiceVersion serviceVersion;
		boolean versionSpecified = false;
		if (pathSplit.size() > serviceAliasLength && pathSplit.get(serviceAliasLength).startsWith("v")) {
			try {
				serviceVersion = new ServiceVersion(pathSplit.get(serviceAliasLength).substring(1));
				versionSpecified = true;
			} catch (IllegalArgumentException e) {
				serviceVersion = new ServiceVersion("*");
			}
		} else {
			serviceVersion = new ServiceVersion("*");
		}
		// required service
		ServiceNameVersion requiredService = new ServiceNameVersion(serviceName, serviceVersion);
		// construct base path
		String basePath = "/";
		for (int i = 0; i < serviceAliasLength; i++) {
			basePath += pathSplit.get(i) + "/";
		}
		if (versionSpecified) {
			basePath += pathSplit.get(serviceAliasLength) + "/";
		}
		// invoke
		if (requestMethod.equalsIgnoreCase("get") && requestPath.equals(basePath + "swagger.json")) {
			return invokeSwagger(mediator, requiredService, basePath);
		} else {
			return invokeRestService(mediator, requiredService, basePath, requestMethod, uriInfo, requestBody,
					requestHeaders);
		}
	}

	private Response invokeSwagger(Mediator mediator, ServiceNameVersion requiredService, String basePath) {
		// get definitions
		Serializable result = callServiceMethod(mediator, requiredService, "getSwagger", new Serializable[] {});
		if (!(result instanceof String)) {
			throw new InternalServerErrorException("Swagger API declaration not available!");
		}
		// deserialize Swagger
		Swagger swagger;
		try {
			swagger = Json.mapper().readerFor(Swagger.class).readValue((String) result);
		} catch (Exception e) {
			throw new NotFoundException("Swagger API declaration not available!", e);
		}
		// modify Swagger
		swagger.setBasePath(basePath);
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
			List<String> scopes = new ArrayList<>();
			scopes.add("openid");
			scopes.add("email");
			scopes.add("profile");
			Map<String, io.swagger.models.Path> paths = swagger.getPaths();
			if (paths != null) {
				for (io.swagger.models.Path path : paths.values()) {
					for (Operation operation : path.getOperations()) {
						operation.addSecurity("defaultProvider", scopes);
					}
				}
			}
		}
		// serialize Swagger
		String json;
		try {
			json = Json.mapper().writeValueAsString(swagger);
		} catch (JsonProcessingException e) {
			throw new InternalServerErrorException("Swagger documentation could not be serialized to JSON", e);
		}
		return Response.ok(json, MediaType.APPLICATION_JSON).build();
	}

	private Response invokeRestService(Mediator mediator, ServiceNameVersion requiredService, String basePath,
			String requestMethod, UriInfo uriInfo, InputStream requestBody,
			MultivaluedMap<String, String> requestHeaders) throws URISyntaxException {
		// URIs
		String baseUriStr = uriInfo.getBaseUri().toString();
		URI baseUri = new URI(baseUriStr.substring(0, baseUriStr.length() - 1) + basePath);
		URI requestUri = uriInfo.getRequestUri();
		UriBuilder cleanRequestUriBuilder = UriBuilder.fromUri(requestUri);
		if (!requestUri.getPath().endsWith("/")) { // make sure URI ends with "/"
			cleanRequestUriBuilder.path("/");
		}
		for (String param : IGNORE_QUERY_PARAMS) { // remove auth params from uri
			cleanRequestUriBuilder.replaceQueryParam(param);
		}
		URI cleanRequestUri = cleanRequestUriBuilder.build();

		// content
		byte[] requestContent;
		try {
			requestContent = getRequestContent(requestBody);
		} catch (IOException e1) {
			throw new InternalServerErrorException("An error occurred: " + e1, e1);
		}
		if (requestContent == null) {
			throw new ClientErrorException(
					"Given request body exceeds limit of " + connector.maxRequestBodySize + " bytes",
					Status.REQUEST_ENTITY_TOO_LARGE);
		}

		// headers
		HashMap<String, ArrayList<String>> cleanHeaders = new HashMap<>();
		for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
			// exclude some headers for security reasons
			if (!INGORE_HEADERS.contains(entry.getKey().toLowerCase())) {
				ArrayList<String> list = new ArrayList<>();
				list.addAll(entry.getValue());
				cleanHeaders.put(entry.getKey(), list);
			}
		}

		// invoke
		Serializable[] params = new Serializable[] { baseUri, cleanRequestUri, requestMethod, requestContent,
				cleanHeaders };
		Serializable result = callServiceMethod(mediator, requiredService, "handle", params);
		if (result instanceof RESTResponse) {
			RESTResponse restResponse = (RESTResponse) result;
			ResponseBuilder responseBuilder = Response.status(restResponse.getHttpCode());
			for (Entry<String, List<String>> entry : restResponse.getHeaders().entrySet()) {
				// don't add possible CORS headers set by service
				final String key = entry.getKey();
				if (!key.equalsIgnoreCase("Access-Control-Allow-Origin")
						&& !key.equalsIgnoreCase("Access-Control-Max-Age")
						&& !key.equalsIgnoreCase("Access-Control-Allow-Headers")
						&& !key.equalsIgnoreCase("Access-Control-Allow-Methods")) {
					for (String value : entry.getValue()) {
						responseBuilder.header(key, value);
					}
				} else {
					// XXX logging
				}
			}
			// add CORS headers
			if (connector.enableCrossOriginResourceSharing) {
				// just reply all requested headers, other values are set by filter
				String requestedHeaders = requestHeaders.getFirst("Access-Control-Request-Headers");
				if (requestedHeaders != null) {
					if (!requestedHeaders.toLowerCase().contains("authorization")) {
						if (!requestedHeaders.trim().equals("")) {
							requestedHeaders += ", ";
						}
						requestedHeaders += "Authorization";
					}
					responseBuilder.header("Access-Control-Allow-Headers", requestedHeaders);
				}
			}
			// add response body
			responseBuilder.entity(restResponse.getBody());
			return responseBuilder.build();
		} else {
			throw new InternalServerErrorException(
					"Service method response is not an " + RESTResponse.class.getSimpleName());
		}
	}

	private byte[] getRequestContent(InputStream is) throws IOException {
		if (is == null) {
			return new byte[0];
		}
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
			return null;
		}
		return buffer.toByteArray();
	}

	private Serializable callServiceMethod(Mediator mediator, ServiceNameVersion service, String method,
			Serializable[] params) {
		try {
			Serializable result = mediator.invoke(service, method, params, connector.onlyLocalServices());
			if (result == null) {
				throw new InternalServerErrorException("Service method invocation returned null response");
			}
			return result;
		} catch (AgentException e) {
			connector.logError("No service found matching " + service + ".", e);
			throw new NotFoundException("No service found matching " + service + ".");
		} catch (ServiceInvocationException e) {
			throw new InternalServerErrorException("Exception during RMI invocation!", e);
		} catch (Exception e) {
			throw new InternalServerErrorException("Service method invocation failed", e);
		}
	}

}
