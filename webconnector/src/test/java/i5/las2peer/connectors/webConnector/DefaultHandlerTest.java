package i5.las2peer.connectors.webConnector;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.connectors.webConnector.handler.DefaultHandler;
import i5.las2peer.connectors.webConnector.handler.WebappHandler;
import i5.las2peer.tools.L2pNodeLauncher;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class DefaultHandlerTest extends AbstractTestHandler {

	@Test
	public void testRootPath() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint());
			Response response = target.request().get();
			Assert.assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
			Assert.assertEquals(connector.getHttpEndpoint() + WebappHandler.DEFAULT_ROUTE,
					response.getHeaderString(HttpHeaders.LOCATION));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRootPathWithSlash() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint() + "/");
			Response response = target.request().get();
			Assert.assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
			Assert.assertEquals(connector.getHttpEndpoint() + WebappHandler.DEFAULT_ROUTE,
					response.getHeaderString(HttpHeaders.LOCATION));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRootResourcePath() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint() + DefaultHandler.ROOT_RESOURCE_PATH);
			Response response = target.request().get();
			Assert.assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
			Assert.assertEquals(connector.getHttpEndpoint() + WebappHandler.DEFAULT_ROUTE,
					response.getHeaderString(HttpHeaders.LOCATION));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testRootResourcePathWithSlash() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint() + DefaultHandler.ROOT_RESOURCE_PATH + "/");
			Response response = target.request().get();
			Assert.assertEquals(Status.TEMPORARY_REDIRECT.getStatusCode(), response.getStatus());
			Assert.assertEquals(connector.getHttpEndpoint() + WebappHandler.DEFAULT_ROUTE,
					response.getHeaderString(HttpHeaders.LOCATION));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetFavicon() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint() + "/favicon.ico");
			Response response = target.request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(new MediaType("image", "x-icon"), response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertTrue(bytes.length > 0);
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetCoreVersion() {
		try {
			WebTarget target = webClient
					.target(connector.getHttpEndpoint() + DefaultHandler.ROOT_RESOURCE_PATH + "/version");
			Response response = target.request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			String responseBody = new String(bytes, StandardCharsets.UTF_8);
			Assert.assertEquals(L2pNodeLauncher.getVersion(), responseBody);
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetNodeStatus() {
		try {
			WebTarget target = webClient
					.target(connector.getHttpEndpoint() + DefaultHandler.ROOT_RESOURCE_PATH + "/status");
			Response response = target.request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(nodes.get(0).getNodeId().toString(), result.getAsString("nodeId"));
			Assert.assertNotNull(result.get("cpuLoad"));
			Assert.assertNotNull(result.get("ramLoad"));
			Assert.assertNotNull(result.get("ramLoadStr"));
			Assert.assertNotNull(result.get("maxRamLoad"));
			Assert.assertNotNull(result.get("maxRamLoadStr"));
			Assert.assertNotNull(result.get("storageSize"));
			Assert.assertNotNull(result.get("storageSizeStr"));
			Assert.assertNotNull(result.get("maxStorageSize"));
			Assert.assertNotNull(result.get("maxStorageSizeStr"));
			Assert.assertNotNull(result.get("uptime"));
			Assert.assertNotNull(result.get("localServices"));
			Assert.assertNotNull(result.get("otherNodes"));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
