package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.tools.SimpleTools;

public abstract class AbstractFilesHandler extends AbstractHandler {

	protected AbstractFilesHandler(NodeAdminConnector connector) {
		super(connector);
	}

	protected Response serveFile(String filename, String myHostname) throws IOException {
		InputStream is = getClass().getResourceAsStream(filename);
		if (is == null) {
			logger.info("File not found: '" + filename + "'");
			return Response.status(Status.NOT_FOUND).build();
		}
		byte[] bytes = SimpleTools.toByteArray(is);
		String strContent = new String(bytes, StandardCharsets.UTF_8);
		strContent = replaceContent(filename, myHostname, strContent);
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
		} else if (lName.endsWith(".gif")) {
			mime = "image/gif";
		} else {
			logger.log(Level.WARNING, "Unknown file type '" + filename + "' using " + mime + " as mime");
		}
		return Response.ok(strContent).type(mime).build();
	}

	protected String replaceContent(String filename, String myHostname, String strContent) {
		return strContent;
	}

}
