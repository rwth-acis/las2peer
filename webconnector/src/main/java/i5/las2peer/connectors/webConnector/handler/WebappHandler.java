package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path(WebappHandler.RESOURCE_PATH)
public class WebappHandler extends AbstractFileHandler {

	public static final String RESOURCE_NAME = "/webapp";
	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + RESOURCE_NAME;
	public static final String DEFAULT_ROUTE = RESOURCE_PATH + "/view1";

	@GET
	public Response rootPath() throws IOException, URISyntaxException {
		return Response.temporaryRedirect(new URI(DEFAULT_ROUTE)).build();
	}

	@GET
	@Path("{any: .*}")
	public Response processRequest(@PathParam("any") String path) throws IOException, URISyntaxException {
		if (path.isEmpty() || path.endsWith("/")) { // don't list directories
			return rootPath();
		} else if (!path.matches(".+/[^/]+\\.[^/]+")) { // not a file request
			// must be some kind of route path
			return serveFile(RESOURCE_NAME + "/index.html");
		} else {
			return serveFile(RESOURCE_NAME + "/" + path);
		}
	}

}
