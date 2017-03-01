package i5.las2peer.nodeAdminConnector.handler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.p2p.Node;
import i5.las2peer.tools.SimpleTools;

public class DefaultHandler extends AbstractHandler {

	public DefaultHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			logger.info("Handler: " + getClass().getCanonicalName() + " Request-Path: "
					+ exchange.getRequestURI().getPath());
			exchange.getResponseHeaders().set("Server-Name", "las2peer " + getClass().getSimpleName());
			final Node node = connector.getNode();
			final Path path = Paths.get(exchange.getRequestURI().getPath());
			if (path.getNameCount() > 0) {
				String path0 = path.getName(0).toString();
				if (path0.equalsIgnoreCase("version")) {
					sendPlainResponse(exchange, getCoreVersion());
				} else if (path0.equalsIgnoreCase("nodeinfo")) {
					sendPlainResponse(exchange, node.getNodeInformation().toString());
				} else if (path0.equalsIgnoreCase("cpuload")) {
					sendPlainResponse(exchange, Integer.toString(getCPULoad(node)));
				} else if (path0.equalsIgnoreCase("netinfo")) {
					sendPlainResponse(exchange, SimpleTools.join(node.getOtherKnownNodes(), "\n"));
				} else {
					sendStringResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, "text/plain", "404 (Not Found)\n");
				}
			} else {
				// perm redirect to frontend
				sendRedirect(exchange, FrontendHandler.STATUS_PATH, true);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Unknown connector error", e);
			sendPlainResponse(exchange, e.toString());
		}
	}

}
