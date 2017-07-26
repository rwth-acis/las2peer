package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.SimpleTools;

@Path(WebappHandler.RESOURCE_PATH)
public class WebappHandler {

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/webapp";
	public static final String DEFAULT_ROUTE = RESOURCE_PATH + "/view-status";

	private static final String INDEX_PATH = RESOURCE_PATH + "/index.html";

	private final L2pLogger logger = L2pLogger.getInstance(WebappHandler.class);

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
		if (filename.equalsIgnoreCase(RESOURCE_PATH + "/index.html")) {
			strContent = strContent.replace("<base href=\"/\">", "<base href=\"" + RESOURCE_PATH + "/\">");
			// just return host header, so browsers do not block subsequent ajax requests to an possible insecure host
			strContent = strContent.replace("$connector_address$", myHostname);
			return Response.ok(strContent).build();
		} else {
			return Response.ok(bytes).build();
		}
	}

}
