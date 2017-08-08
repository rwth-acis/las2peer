package i5.las2peer.connectors.webConnector.handler;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.util.KeystoreManager;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.tools.L2pNodeLauncher;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Path(DefaultHandler.ROOT_RESOURCE_PATH)
public class DefaultHandler {

	public static final String ROOT_RESOURCE_PATH = "/las2peer";

	private final WebConnector connector;
	private final Node node;

	public DefaultHandler(WebConnector connector) {
		this.connector = connector;
		this.node = connector.getL2pNode();
	}

	@GET
	public Response rootPath() throws URISyntaxException {
		return Response.temporaryRedirect(new URI(WebappHandler.DEFAULT_ROUTE)).build();
	}

	@GET
	@Path("/version")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCoreVersion() {
		return L2pNodeLauncher.getVersion();
	}

	@GET
	@Path("/cacert.pem")
	public Response getCACert() throws IOException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos);
			KeystoreManager.writeCertificateToPEMStream(connector.getCACertificate(), osw);
			osw.close();
			return Response.ok(baos.toByteArray(), "application/x-pem-file").build();
		} catch (FileNotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception e) {
			return Response.serverError().entity(e.toString()).build();
		}
	}

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public String getNodeStatus(@HeaderParam("Host") String hostHeader) {
		// gather status values and return as JSON string
		JSONObject response = new JSONObject();
		response.put("nodeid", node.getNodeId().toString());
		response.put("cpuload", getCPULoad(node));
		long localStorageSize = -1;
		long maxLocalStorageSize = -1;
		if (node instanceof PastryNodeImpl) {
			localStorageSize = ((PastryNodeImpl) node).getLocalStorageSize();
			maxLocalStorageSize = ((PastryNodeImpl) node).getLocalMaxStorageSize();
		}
		response.put("storageSize", localStorageSize);
		response.put("storageSizeStr", humanReadableByteCount(localStorageSize, true));
		response.put("maxStorageSize", maxLocalStorageSize);
		response.put("maxStorageSizeStr", humanReadableByteCount(maxLocalStorageSize, true));
		response.put("uptime", getUptime(node));
		response.put("localServices", getLocalServices(node, hostHeader));
		response.put("otherNodes", getOtherNodes(node));
		return response.toJSONString();
	}

	private int getCPULoad(Node node) {
		return (int) (node.getNodeCpuLoad() * 100);
	}

	// Source: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	private static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	private String getUptime(Node node) {
		Date startTime = node.getStartTime();
		if (startTime == null) {
			return "node stopped";
		} else {
			long uptimeInSeconds = (new Date().getTime() - node.getStartTime().getTime()) / 1000;
			long hours = uptimeInSeconds / 3600;
			long minutes = uptimeInSeconds / 60 % 60;
			long seconds = uptimeInSeconds % 60;
			return hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
		}
	}

	private JSONArray getLocalServices(Node node, String hostHeader) {
		List<String> serviceNames = node.getNodeServiceCache().getLocalServiceNames();
		JSONArray result = new JSONArray();
		for (String serviceName : serviceNames) {
			List<ServiceVersion> serviceVersions = node.getNodeServiceCache().getLocalServiceVersions(serviceName);
			for (ServiceVersion version : serviceVersions) {
				try {
					ServiceAgentImpl localServiceAgent = node.getNodeServiceCache()
							.getLocalService(new ServiceNameVersion(serviceName, version));
					JSONObject json = new JSONObject();
					json.put("name", serviceName);
					json.put("version", version.toString());
					// use host header, so browsers do not block subsequent ajax requests to an unknown host
					String serviceAlias = localServiceAgent.getServiceInstance().getAlias();
					json.put("swagger", "https://" + hostHeader + "/" + serviceAlias + "/swagger.json");
					result.add(json);
				} catch (Exception e) {
					// XXX logging
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private JSONArray getOtherNodes(Node node) {
		JSONArray result = new JSONArray();
		for (Object other : node.getOtherKnownNodes()) {
			result.add(other.toString());
		}
		return result;
	}

}
