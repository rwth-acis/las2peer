package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.logging.L2pLogger;

@Path(SwaggerUIHandler.RESOURCE_PATH)
public class SwaggerUIHandler extends AbstractFileHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/swagger-ui";

	private final L2pLogger logger = L2pLogger.getInstance(SwaggerUIHandler.class);

	private final WebConnector connector;

	public SwaggerUIHandler(WebConnector connector) {
		this.connector = connector;
	}

	@GET
	public Response rootPath() throws IOException, URISyntaxException {
		return Response.temporaryRedirect(new URI(RESOURCE_PATH + "/index.html")).build();
	}

	@GET
	@Path("{any: .*}")
	public Response processRequest(@PathParam("any") String path) throws IOException, URISyntaxException {
		if (path.isEmpty() || path.endsWith("/")) { // don't list directories
			return rootPath();
		} else {
			return serveFile(path);
		}
	}

	@Override
	protected Response serveFile(String relativeUri) throws IOException {
		String resourceName = "/META-INF/resources/webjars/swagger-ui/3.6.1/" + relativeUri;
		if (relativeUri.equalsIgnoreCase("index.html")) {
			InputStream is = getClass().getResourceAsStream(resourceName);
			if (is == null) {
				logger.info("File not found: '" + resourceName + "'");
				return Response.status(Status.NOT_FOUND).build();
			}
			String strContent = IOUtils.toString(is, StandardCharsets.UTF_8);
			String oidcClientId = connector.getOidcClientId();
			if (oidcClientId != null) {
				strContent = strContent.replace("clientId: \"your-client-id\",", "clientId: \"" + oidcClientId + "\",");
			}
			String oidcClientSecret = connector.getOidcClientSecret();
			if (oidcClientSecret != null) {
				strContent = strContent.replace("clientSecret: \"your-client-secret-if-required\",",
						"clientSecret: \"" + oidcClientSecret + "\",");
			}
			return Response.ok(strContent, MediaType.TEXT_HTML).build();
		} else {
			return super.serveFile(resourceName);
		}
	}
}
