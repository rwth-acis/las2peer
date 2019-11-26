package i5.las2peer.connectors.webConnector.handler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.connectors.webConnector.util.MimeTypes;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.tools.SimpleTools;

public abstract class AbstractFileHandler {

	private final L2pLogger logger = L2pLogger.getInstance(AbstractFileHandler.class);

	protected Response serveFile(String relativeUri) throws IOException {
		InputStream is = getClass().getResourceAsStream(relativeUri);
		if (is == null) {
			logger.info("File not found: '" + relativeUri + "'");
			return Response.status(Status.NOT_FOUND).build();
		}
		byte[] bytes = SimpleTools.toByteArray(is);

		// get metadata for header
		Path filePath = Paths.get(relativeUri);
		String mime = Files.probeContentType(filePath);
		if (mime == null) {
			mime = MimeTypes.get(relativeUri);
		}
		Path fileName = filePath.getFileName();

		return Response.ok(bytes, mime).header("Content-Disposition", "inline; filename=\"" + fileName + "\"").build();
	}

}
