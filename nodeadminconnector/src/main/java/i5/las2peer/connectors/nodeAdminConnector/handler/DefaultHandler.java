package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.connectors.nodeAdminConnector.KeystoreManager;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.tools.L2pNodeLauncher;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

@Path("/")
public class DefaultHandler extends AbstractHandler {

	public DefaultHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@GET
	public Response rootPath() throws URISyntaxException {
		return Response.temporaryRedirect(new URI(AppHandler.DEFAULT_ROUTE)).build();
	}

	@GET
	@Path("/version")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCoreVersion() {
		return L2pNodeLauncher.getVersion();
	}

	@GET
	@Path("/favicon.ico")
	@Produces("image/x-icon")
	public Response getFavicon() throws IOException {
		byte[] bytes = null;
		try {
			FileInputStream fis = new FileInputStream("etc/favicon.ico");
			bytes = SimpleTools.toByteArray(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			// use fallback from classpath
			InputStream is = getClass().getResourceAsStream("/favicon.ico");
			if (is != null) {
				bytes = SimpleTools.toByteArray(is);
				is.close();
			}
		}
		return Response.ok(bytes, "image/x-icon").build();
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
		long localStorageSize = node.getLocalStorageSize();
		response.put("storageSize", localStorageSize);
		response.put("storageSizeStr", humanReadableByteCount(localStorageSize, true));
		long maxLocalStorageSize = node.getLocalMaxStorageSize();
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

	private String getUptime(PastryNodeImpl node) {
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
				JSONObject json = new JSONObject();
				json.put("name", serviceName);
				json.put("version", version.toString());
				// use host header, so browsers do not block subsequent ajax requests to an unknown host
				json.put("swagger", "https://" + hostHeader + RMIHandler.RMI_PATH + "/" + serviceName + "/"
						+ version.toString() + "/swagger.json");
				result.add(json);
			}
		}
		return result;
	}

	private JSONArray getOtherNodes(PastryNodeImpl node) {
		JSONArray result = new JSONArray();
		for (Object other : node.getOtherKnownNodes()) {
			result.add(other.toString());
		}
		return result;
	}

}
