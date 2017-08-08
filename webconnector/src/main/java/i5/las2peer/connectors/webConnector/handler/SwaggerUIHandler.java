package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path(SwaggerUIHandler.RESOURCE_PATH)
public class SwaggerUIHandler extends AbstractFileHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/swagger-ui";

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
			return serveFile("/META-INF/resources/webjars/swagger-ui/2.2.10/" + path);
		}
	}

}
