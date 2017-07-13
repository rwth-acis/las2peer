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

@Path(AppHandler.ROOT_ROUTE)
public class AppHandler extends AbstractFilesHandler {

	public static final String ROOT_ROUTE = "/webapp";
	public static final String DEFAULT_ROUTE = ROOT_ROUTE + "/view-status";
	public static final String INDEX_PATH = ROOT_ROUTE + "/index.html";

	public AppHandler(NodeAdminConnector connector) {
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
			return Response.temporaryRedirect(new URI(DEFAULT_ROUTE)).build();
		} else if (!path.matches(".+/[^/]+\\.[^/]+")) { // not a file request
			// must be some kind of route path
			return serveFile(INDEX_PATH, myHostname);
		} else {
			return serveFile("/" + path, myHostname);
		}
	}

	@Override
	protected String replaceContent(String filename, String myHostname, String strContent) {
		if (filename.equalsIgnoreCase(ROOT_ROUTE + "/index.html")) {
			strContent = strContent.replace("<base href=\"/\">", "<base href=\"" + ROOT_ROUTE + "/\">");
			// just return host header, so browsers do not block subsequent ajax requests to an possible insecure host
			strContent = strContent.replace("$connector_address$", myHostname);
		}
		return strContent;
	}

}
