package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.SimpleTools;

public abstract class AbstractFileHandler {

	private final L2pLogger logger = L2pLogger.getInstance(AbstractFileHandler.class);

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
