package i5.las2peer.connectors.nodeAdminConnector.handler;

import java.util.Date;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import i5.las2peer.api.p2p.ServiceVersion;
import i5.las2peer.connectors.nodeAdminConnector.NodeAdminConnector;
import i5.las2peer.connectors.nodeAdminConnector.ParameterFilter.ParameterMap;
import i5.las2peer.p2p.Node;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class StatusHandler extends AbstractHandler {

	public StatusHandler(NodeAdminConnector connector) {
		super(connector);
	}

	@Override
	protected void handleSub(HttpExchange exchange, PastryNodeImpl node, ParameterMap parameters, PassphraseAgentImpl activeAgent,
			byte[] requestBody) throws Exception {
		// gather status values and return as JSON string
		JSONObject response = new JSONObject();
		response.put("nodeid", node.getNodeId().toString());
		response.put("cpuload", getCPULoad(node));
		response.put("publicKey", node.getPublicNodeKey().toString());
		long localStorageSize = node.getLocalStorageSize();
		long maxLocalStorageSize = node.getLocalMaxStorageSize();
		response.put("storageSize", localStorageSize);
		response.put("maxStorageSize", maxLocalStorageSize);
		response.put("storageSizeStr", humanReadableByteCount(localStorageSize, true));
		response.put("maxStorageSizeStr", humanReadableByteCount(maxLocalStorageSize, true));
		response.put("uptime", getUptime(node));
		response.put("localServices", getLocalServices(node));
		response.put("otherNodes", getOtherNodes(node));
		sendJSONResponse(exchange, response);
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

	private JSONArray getLocalServices(Node node) {
		List<String> serviceNames = node.getNodeServiceCache().getLocalServiceNames();
		JSONArray result = new JSONArray();
		for (String serviceName : serviceNames) {
			List<ServiceVersion> serviceVersions = node.getNodeServiceCache().getLocalServiceVersions(serviceName);
			for (ServiceVersion version : serviceVersions) {
				JSONObject json = new JSONObject();
				json.put("name", serviceName);
				json.put("version", version.toString());
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
