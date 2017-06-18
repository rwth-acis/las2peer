package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

@Path("/swagger-ui")
public class SwaggerUIHandler extends AbstractHandler {

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

	protected Response serveFile(String path, String myHostname) throws IOException {
		// serve files from swagger-ui jar
		if (!path.startsWith(ROOT_PATH)) {
			logger.info("File request blocked: '" + path + "'");
			return Response.status(Status.FORBIDDEN).build();
		}
		String filename = path.replace(ROOT_PATH + "/", SWAGGER_UI_JAR_PREFIX + "/");
		InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
		if (is == null) {
			logger.info("File not found: '" + filename + "'");
			return Response.status(Status.NOT_FOUND).build();
		}
		byte[] bytes = SimpleTools.toByteArray(is);
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
			mime = "image/x-icon";
		} else if (lName.endsWith(".png")) {
			mime = "image/png";
		} else {
			logger.log(Level.WARNING, "Unknown file type '" + filename + "' using " + mime + " as mime");
		}
		return Response.ok(bytes).type(mime).build();
	}

}
