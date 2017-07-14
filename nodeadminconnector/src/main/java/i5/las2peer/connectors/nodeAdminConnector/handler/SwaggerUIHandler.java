package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;

@Path("/swagger-ui")
public class SwaggerUIHandler extends AbstractFilesHandler {

	public static final String ROOT_PATH = "/swagger-ui";
	public static final String SWAGGER_UI_JAR_PREFIX = "META-INF/resources/webjars/swagger-ui/2.2.10";

	private static final String INDEX_PATH = ROOT_PATH + "/index.html";

	public SwaggerUIHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@GET
	public Response rootPath(@HeaderParam("Host") String myHostname) throws IOException {
		return serveFile(INDEX_PATH, myHostname);
	}

	@GET
	@Path("{any: .*}")
	public Response processRequest(@Context UriInfo uriInfo, @HeaderParam("Host") String myHostname)
			throws IOException, URISyntaxException {
		final String path = uriInfo.getPath();
		if (path.isEmpty() || path.equalsIgnoreCase("/")) {
			return serveFile(INDEX_PATH, myHostname);
		} else if (path.endsWith("/")) { // do not list directories
			return Response.temporaryRedirect(new URI(INDEX_PATH)).build();
		} else {
			return serveFile("/" + path, myHostname);
		}
	}

}
