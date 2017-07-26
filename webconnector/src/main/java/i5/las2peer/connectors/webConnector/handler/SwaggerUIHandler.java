package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.SimpleTools;

@Path(SwaggerUIHandler.RESOURCE_PATH)
public class SwaggerUIHandler {

	private final L2pLogger logger = L2pLogger.getInstance(SwaggerUIHandler.class);

	public static final String RESOURCE_PATH = DefaultHandler.ROOT_RESOURCE_PATH + "/swagger-ui";
	public static final String BASE_PATH = RESOURCE_PATH + "/";

	private static final String SWAGGER_UI_JAR_PREFIX = "/META-INF/resources/webjars/swagger-ui/2.2.10/";
	private static final String INDEX_FILE = RESOURCE_PATH + "/index.html";

	@GET
	public Response rootPath() throws IOException, URISyntaxException {
		return Response.temporaryRedirect(new URI(INDEX_FILE)).build();
	}

	@GET
	@Path("{any: .*}")
	public Response processRequest(@Context UriInfo uriInfo) throws IOException, URISyntaxException {
		final String path = uriInfo.getPath();
		if (!path.startsWith(BASE_PATH)) {
			logger.info("File request blocked: '" + path + "'");
			return Response.status(Status.FORBIDDEN).build();
		} else {
			// serve files from swagger-ui jar
			String filename = SWAGGER_UI_JAR_PREFIX + path.substring(BASE_PATH.length());
			return serveFile(filename);
		}
	}

	protected Response serveFile(String filename) throws IOException {
		InputStream is = getClass().getResourceAsStream(filename);
		if (is == null) {
			logger.info("File not found: '" + filename + "'");
			return Response.status(Status.NOT_FOUND).build();
		}
		byte[] bytes = SimpleTools.toByteArray(is);
		return Response.ok(bytes).build();
	}

}
