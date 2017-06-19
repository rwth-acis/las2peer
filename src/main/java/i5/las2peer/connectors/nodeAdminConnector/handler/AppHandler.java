package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.tools.SimpleTools;

@Path(AppHandler.ROOT_ROUTE)
public class AppHandler extends AbstractHandler {

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

	protected Response serveFile(String filename, String myHostname) throws IOException {
		InputStream is = getClass().getResourceAsStream(filename);
		if (is == null) {
			logger.info("File not found: '" + filename + "'");
			return Response.status(Status.NOT_FOUND).build();
		}
		byte[] bytes = SimpleTools.toByteArray(is);
		String strContent = new String(bytes, StandardCharsets.UTF_8);
		if (filename.equalsIgnoreCase(ROOT_ROUTE + "/index.html")) {
			strContent = strContent.replace("<base href=\"/\">", "<base href=\"" + ROOT_ROUTE + "/\">");
			// just return host header, so browsers do not block subsequent ajax requests to an possible insecure host
			strContent = strContent.replace("$connector_address$", myHostname);
		}
		String lName = filename.toLowerCase();
		String mime = "application/octet-stream";
		if (lName.endsWith(".html") || lName.endsWith(".htm")) {
			mime = "text/html";
		} else if (lName.endsWith(".css")) {
			mime = "text/css";
		} else if (lName.endsWith(".js")) {
			mime = "text/javascript";
		} else if (lName.endsWith(".json")) {
			mime = "application/json";
		} else if (lName.endsWith(".ico")) {
			return Response.ok(bytes).type("image/x-icon").build(); // don't use stringContent, return raw bytes instead
		} else if (lName.endsWith(".png")) {
			mime = "image/png";
		} else {
			logger.log(Level.WARNING, "Unknown file type '" + filename + "' using " + mime + " as mime");
		}
		return Response.ok(strContent).type(mime).build();
	}

}
