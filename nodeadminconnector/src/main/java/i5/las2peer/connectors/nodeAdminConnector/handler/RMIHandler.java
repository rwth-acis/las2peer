package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Level;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.connectors.nodeAdminConnector.AgentSession;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.restMapper.RESTResponse;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.SimpleTools;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

@Path(RMIHandler.RMI_PATH)
public class RMIHandler extends AbstractHandler {

	public static final String RMI_PATH = "/rmi";

	public RMIHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@GET
	@Path("/{serviceName}")
	public Response getLocalInstances(@PathParam("serviceName") String serviceName) {
		// resolve service name and search for local running versions
		String versions = "Service '" + serviceName + "' not running locally";
		List<ServiceVersion> versionList = node.getNodeServiceCache().getLocalServiceVersions(serviceName);
		if (versionList != null && !versionList.isEmpty()) {
			versions = SimpleTools.join(versionList, "\n");
		}
		return Response.ok("Service versions running locally:\n" + versions, MediaType.TEXT_PLAIN).build();
	}

	@GET
	@Path("/{serviceName}/{serviceVersion}/swagger.json")
	public Response sendSwaggerListing(@PathParam("serviceName") String serviceName,
			@PathParam("serviceVersion") String versionString) throws Exception {
		ServiceNameVersion serviceNameVersion = new ServiceNameVersion(serviceName, versionString);
		AnonymousAgentImpl anonymous = AnonymousAgentImpl.getInstance();
		Mediator mediator = node.createMediatorForAgent(anonymous);
		Serializable swagResult = invokeServiceMethod(mediator, serviceNameVersion, "getSwagger", new Serializable[0]);
		if (swagResult == null) {
			return Response.serverError().entity("Method invocation 'getSwagger' returned null").build();
		} else if (!(swagResult instanceof String)) {
			return Response.serverError()
					.entity("Expected type String got '" + swagResult.getClass().getCanonicalName() + "' instead")
					.build();
		}
		// deserialize Swagger
		Swagger swagger;
		try {
			swagger = Json.mapper().readerFor(Swagger.class).readValue((String) swagResult);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Could not get Swagger API declaration", e);
			return Response.serverError().entity("Swagger API declaration not available! Reason: " + e.toString())
					.build();
		}
		swagger.setBasePath(RMI_PATH + "/" + serviceName + "/" + versionString + "/");
		// serialize Swagger API listing into a JSON String
		try {
			String swaggerJson = Json.mapper().writeValueAsString(swagger);
			return Response.ok(swaggerJson, MediaType.APPLICATION_JSON).build();
		} catch (JsonProcessingException e) {
			throw new ServerErrorException("Swagger documentation could not be serialized to JSON",
					Status.INTERNAL_SERVER_ERROR, e);
		}
	}

	@POST
	@Path("/{serviceName}/{serviceVersion}/{any: .+}")
	public Response handleServiceInvocationPOST(@PathParam("serviceName") String serviceName,
			@PathParam("serviceVersion") String versionString, @Context UriInfo uriInfo,
			@Context HttpHeaders httpHeaders, @CookieParam(NodeAdminConnector.COOKIE_SESSIONID_KEY) String sessionId,
			InputStream requestBody) throws Exception {
		return handleServiceInvocation(serviceName, versionString, uriInfo, "POST", httpHeaders, sessionId,
				requestBody);
	}

	@PUT
	@Path("/{serviceName}/{serviceVersion}/{any: .+}")
	public Response handleServiceInvocationPUT(@PathParam("serviceName") String serviceName,
			@PathParam("serviceVersion") String versionString, @Context UriInfo uriInfo,
			@Context HttpHeaders httpHeaders, @CookieParam(NodeAdminConnector.COOKIE_SESSIONID_KEY) String sessionId,
			InputStream requestBody) throws Exception {
		return handleServiceInvocation(serviceName, versionString, uriInfo, "PUT", httpHeaders, sessionId, requestBody);
	}

	@GET
	@Path("/{serviceName}/{serviceVersion}/{any: .+}")
	public Response handleServiceInvocationGET(@PathParam("serviceName") String serviceName,
			@PathParam("serviceVersion") String versionString, @Context UriInfo uriInfo,
			@Context HttpHeaders httpHeaders, @CookieParam(NodeAdminConnector.COOKIE_SESSIONID_KEY) String sessionId)
			throws Exception {
		return handleServiceInvocation(serviceName, versionString, uriInfo, "GET", httpHeaders, sessionId, null);
	}

	@DELETE
	@Path("/{serviceName}/{serviceVersion}/{any: .+}")
	public Response handleServiceInvocationDELETE(@PathParam("serviceName") String serviceName,
			@PathParam("serviceVersion") String versionString, @Context UriInfo uriInfo,
			@Context HttpHeaders httpHeaders, @CookieParam(NodeAdminConnector.COOKIE_SESSIONID_KEY) String sessionId)
			throws Exception {
		return handleServiceInvocation(serviceName, versionString, uriInfo, "DELETE", httpHeaders, sessionId, null);
	}

	private Response handleServiceInvocation(String serviceName, String versionString, UriInfo uriInfo,
			String httpMethod, HttpHeaders httpHeaders, String sessionId, InputStream requestBody) throws Exception {
		ServiceNameVersion serviceNameVersion = new ServiceNameVersion(serviceName, versionString);
		AgentSession session = connector.getSessionById(sessionId);
		AgentImpl activeAgent = null;
		if (session == null) {
			final List<String> authorization = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
			if (authorization != null && !authorization.isEmpty()) {
				try {
					final String encodedUserPassword = authorization.get(0).substring("basic ".length());
					String usernameAndPassword = new String(Base64.getDecoder().decode(encodedUserPassword),
							StandardCharsets.UTF_8);
					final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, ":");
					final String username = tokenizer.nextToken();
					final String password = tokenizer.nextToken();
					try {
						AgentImpl agent = node.getAgent(username);
						if (agent instanceof PassphraseAgentImpl) {
							activeAgent = agent;
							((PassphraseAgentImpl) activeAgent).unlock(password);
						} else {
							// just to reach the catch block
							throw new AgentException(
									"Invalid agent type '" + agent.getClass().getCanonicalName() + "'");
						}
					} catch (AgentException e) {
						try {
							String agentId = node.getAgentIdForLogin(username);
							AgentImpl agent = node.getAgent(agentId);
							if (agent instanceof PassphraseAgentImpl) {
								activeAgent = agent;
								((PassphraseAgentImpl) activeAgent).unlock(password);
							} else {
								// just to reach the catch block
								throw new AgentException(
										"Invalid agent type '" + agent.getClass().getCanonicalName() + "'");
							}
						} catch (AgentException e2) {
							try {
								String agentId = node.getAgentIdForLogin(username);
								AgentImpl agent = node.getAgent(agentId);
								if (agent instanceof PassphraseAgentImpl) {
									activeAgent = agent;
									((PassphraseAgentImpl) activeAgent).unlock(password);
								} else {
									// just to reach the catch block
									throw new AgentException(
											"Invalid agent type '" + agent.getClass().getCanonicalName() + "'");
								}
							} catch (AgentException e3) {
								throw new AgentNotFoundException("No match with an agent");
							}
						}
					}
				} catch (AgentNotFoundException e) {
					return Response.status(Status.UNAUTHORIZED).entity(e.toString()).build();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Could not use basic auth header", e);
					throw new InternalError("Could not use basic auth header");
				}
			}
			if (activeAgent == null) {
				activeAgent = AnonymousAgentImpl.getInstance();
			}
		} else {
			activeAgent = session.getAgent();
		}
		Mediator mediator = node.createMediatorForAgent(activeAgent);
		URI baseUri = new URI(uriInfo.getBaseUri().toString() + "rmi/" + serviceName + "/" + versionString + "/");
		URI requestUri = uriInfo.getRequestUri();
		HashMap<String, List<String>> headers = new HashMap<>();
		headers.putAll(httpHeaders.getRequestHeaders());
		byte[] requestBodyData = new byte[0];
		if (requestBody != null) {
			requestBodyData = SimpleTools.toByteArray(requestBody);
		}
		// invoke
		Serializable[] params = new Serializable[] { baseUri, requestUri, httpMethod, requestBodyData, headers };
		Serializable result = null;
		try {
			result = mediator.invoke(serviceNameVersion, "handle", params, false);
		} catch (Exception e) {
			throw new ServerErrorException("Service method invocation failed", Status.INTERNAL_SERVER_ERROR, e);
		}
		if (result == null) {
			return Response.serverError().entity("Service method invocation returned null response").build();
		} else if (result instanceof RESTResponse) {
			RESTResponse restResponse = (RESTResponse) result;
			ResponseBuilder responseBuilder = Response.status(restResponse.getHttpCode())
					.entity(restResponse.getBody());
			for (Entry<String, List<String>> header : restResponse.getHeaders().entrySet()) {
				for (String value : header.getValue()) {
					responseBuilder.header(header.getKey(), value);
				}
			}
			return responseBuilder.build();
		} else {
			return Response.serverError().entity("Expected " + RESTResponse.class.getCanonicalName() + ", but got "
					+ result.getClass().getCanonicalName() + " instead").build();
		}
	}

	private Serializable invokeServiceMethod(Mediator mediator, ServiceNameVersion serviceNameVersion,
			String serviceMethodName, Serializable[] params) throws ServerErrorException {
		Serializable result = null;
		try {
			try {
				result = mediator.invoke(serviceNameVersion, serviceMethodName, params, false);
			} catch (ServiceNotFoundException e) {
				// FIXME autostart service, locally?
				logger.info("Service not reachable in network. Using autostart feature");
				try {
					ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(serviceNameVersion,
							"autostart");
					serviceAgent.unlock("autostart");
					node.registerReceiver(serviceAgent);
					// FIXME persist agent?
				} catch (Exception e2) {
					throw new ServerErrorException("Could not autostart service", Status.INTERNAL_SERVER_ERROR, e2);
				}
				// FIXME repeat invocation
				result = mediator.invoke(serviceNameVersion, serviceMethodName, params, false);
			}
		} catch (Exception e) {
			throw new ServerErrorException("Service method invocation failed", Status.INTERNAL_SERVER_ERROR, e);
		}
		return result;
	}

}
