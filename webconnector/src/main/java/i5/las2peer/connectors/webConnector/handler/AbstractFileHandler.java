package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.MediaType;
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
		String mime = Files.probeContentType(Paths.get(filename));
		if (mime == null) {
			mime = MediaType.APPLICATION_OCTET_STREAM;
		}
		return Response.ok(bytes, mime).build();
	}

}
