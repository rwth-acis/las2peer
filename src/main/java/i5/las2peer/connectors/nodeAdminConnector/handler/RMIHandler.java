package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.restMapper.RESTResponse;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.tools.SimpleTools;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

public class RMIHandler extends AbstractHandler {

	public static final String RMI_PATH = "/rmi";

	public RMIHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters,
			PassphraseAgentImpl activeAgent, byte[] requestBody) throws Exception {
		final String path = exchange.getRequestURI().getPath();
		final String subPathString = path.substring(RMI_PATH.length());
		final Path subPath = Paths.get(path.substring(RMI_PATH.length()));
		final int pathCount = subPath.getNameCount();
		if (subPathString.isEmpty() || pathCount < 1) {
			sendPlainResponse(exchange, "Please specify a service name");
			return;
		}
		// resolve service name and search for local running versions
		String serviceName = subPath.getName(0).toString();
		if (pathCount == 1) { // show local running instances
			String versions = "Service '" + serviceName + "' not running locally";
			List<ServiceVersion> versionList = node.getNodeServiceCache().getLocalServiceVersions(serviceName);
			if (versionList != null && !versionList.isEmpty()) {
				versions = SimpleTools.join(versionList, "\n");
			}
			sendPlainResponse(exchange, "Service versions running locally:\n" + versions);
			return;
		}
		// validate service version
		String version = subPath.getName(1).toString();
		ServiceNameVersion snv = new ServiceNameVersion(serviceName, version);
		Mediator mediator = node.createMediatorForAgent(activeAgent);
		if (pathCount == 3 && subPath.getName(2).toString().equalsIgnoreCase("swagger.json")) {
			sendSwaggerListing(exchange, node, mediator, snv, path);
			return;
		} else {
			handleServiceInvocation(exchange, node, mediator, snv, requestBody);
		}
	}

	private void sendSwaggerListing(HttpExchange exchange, Node node, Mediator mediator,
			ServiceNameVersion serviceNameVersion, String path) throws Exception {
		String basePath = path.substring(0, path.length() - "swagger.json".length());
		Serializable swagResult = mediator.invoke(serviceNameVersion, "getSwagger", new Serializable[0], false);
		if (swagResult == null) {
			sendInternalErrorResponse(exchange, "Method invocation 'getSwagger' returned null");
			return;
		}
		if (!(swagResult instanceof String)) {
			sendInternalErrorResponse(exchange,
					"Expected type String got '" + swagResult.getClass().getCanonicalName() + "' instead");
			return;
		}
		// deserialize Swagger
		Swagger swagger;
		try {
			swagger = Json.mapper().readerFor(Swagger.class).readValue((String) swagResult);
		} catch (Exception e) {
			sendInternalErrorResponse(exchange, "Swagger API declaration not available!", e);
			return;
		}
		swagger.setBasePath(basePath);
		// serialize Swagger API listing into a JSON String
		try {
			String swaggerJson = Json.mapper().writeValueAsString(swagger);
			sendStringResponse(exchange, HttpURLConnection.HTTP_OK, "application/json", swaggerJson);
		} catch (JsonProcessingException e) {
			sendInternalErrorResponse(exchange, "Swagger documentation could not be serialized to JSON", e);
			return;
		}
	}

	private void handleServiceInvocation(HttpExchange exchange, Node node, Mediator mediator,
			ServiceNameVersion serviceNameVersion, byte[] requestBody) throws Exception {
		final URI requestUri = exchange.getRequestURI();
		Path requestPath = Paths.get(requestUri.getPath());
		URI baseUri = new URI(
				"/" + requestPath.getName(0) + "/" + requestPath.getName(1) + "/" + requestPath.getName(2) + "/");
		HashMap<String, List<String>> headers = new HashMap<>();
		for (Entry<String, List<String>> headerEntry : exchange.getRequestHeaders().entrySet()) {
			headers.put(headerEntry.getKey(), headerEntry.getValue());
		}
		// invoke
		Serializable[] params = new Serializable[] { baseUri, requestUri, exchange.getRequestMethod(), requestBody,
				headers };
		Serializable result = null;
		try {
			result = mediator.invoke(serviceNameVersion, "handle", params, false);
		} catch (Exception e) {
			sendInternalErrorResponse(exchange, "Service method invocation failed", e);
			return;
		}
		if (result == null) {
			sendInternalErrorResponse(exchange, "Service method invocation returned null response");
		} else if (result instanceof RESTResponse) {
			RESTResponse response = (RESTResponse) result;
			exchange.getResponseHeaders().putAll(response.getHeaders());
			try {
				final byte[] responseBody = response.getBody();
				exchange.sendResponseHeaders(response.getHttpCode(), getResponseLength(responseBody.length));
				OutputStream os = exchange.getResponseBody();
				System.out.println("Writting " + responseBody.length + " bytes");
				if (responseBody.length > 0) {
					os.write(responseBody);
				}
				os.close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Communication with client failed", e);
			}
		} else {
			sendInternalErrorResponse(exchange, "Expected " + RESTResponse.class.getCanonicalName() + ", but got "
					+ result.getClass().getCanonicalName() + " instead");
		}
	}

	private long getResponseLength(final long contentLength) {
		if (contentLength == 0) {
			return -1;
		}
		if (contentLength < 0) {
			return 0;
		}
		return contentLength;
	}

}
