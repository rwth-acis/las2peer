package i5.las2peer.connectors.nodeAdminConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestService;
import i5.las2peer.tools.PackageUploader;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class ServicesHandlerTest extends AbstractTestHandler {

	@Test
	public void testGetSearchNonExisting() {
		try {
			WebTarget target = sslClient.target(connector.getHostname() + "/services/search");
			final String serviceName = "i5.las2peer.services.nonExistingService.NonExistingService";
			Response response = target.queryParam("searchname", serviceName).request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals("'" + serviceName + "' not found in network", result.getAsString("msg"));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetSearchExisting() {
		try {
			final String serviceName = TestService.class.getCanonicalName();
			final String serviceVersion = "1.2.3";
			// upload TestService
			PastryNodeImpl serviceNode = nodes.get(0);
			HashMap<String, byte[]> depHashes = new HashMap<>();
			HashMap<String, byte[]> jarFiles = new HashMap<>();
			PassphraseAgentImpl devAgent = MockAgentFactory.getAdam();
			devAgent.unlock("adamspass");
			PackageUploader.uploadServicePackage(serviceNode, TestService.class.getPackage().getName(), serviceVersion,
					depHashes, jarFiles, devAgent);
			// start actual test
			WebTarget target = sslClient.target(connector.getHostname() + "/services/search");
			Response response = target.queryParam("searchname", serviceName).request().get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			JSONArray instances = (JSONArray) result.get("instances");
			Assert.assertEquals(1, instances.size());
			JSONObject inst = (JSONObject) instances.get(0);
			Assert.assertEquals(serviceName, inst.getAsString("name"));
			Assert.assertEquals(serviceVersion, inst.getAsString("version"));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostServicePackageUploadNoLogin() {
		try {
			WebTarget target = sslClient.target(connector.getHostname() + "/services/upload");
			MultiPart multiPart = new MultiPart();
			multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			File tmpJarFile = File.createTempFile("TestService", ".jar");
			tmpJarFile.deleteOnExit();
			new JarOutputStream(new FileOutputStream(tmpJarFile)).close();
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("jarfile", tmpJarFile,
					MediaType.APPLICATION_OCTET_STREAM_TYPE);
			multiPart.bodyPart(fileDataBodyPart);
			Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
			Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals("You have to be logged in to upload", result.getAsString("msg"));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostServicePackageUpload() {
		try {
			// login with useragent
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			WebTarget targetLogin = sslClient.target(connector.getHostname() + "/auth/login");
			Response responseLogin = targetLogin.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), responseLogin.getStatus());
			NewCookie cookie = responseLogin.getCookies().get(NodeAdminConnector.COOKIE_SESSIONID_KEY);
			Assert.assertNotNull(cookie);
			Assert.assertEquals(NodeAdminConnector.COOKIE_SESSIONID_KEY, cookie.getName());
			// start actual test
			WebTarget target = sslClient.target(connector.getHostname() + "/services/upload");
			File tmpJarFile = File.createTempFile("TestService", ".jar");
			tmpJarFile.deleteOnExit();
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			manifest.getMainAttributes().putValue("las2peer-service-name", TestService.class.getPackage().getName());
			manifest.getMainAttributes().putValue("las2peer-service-version", "1.2.3");
			new JarOutputStream(new FileOutputStream(tmpJarFile), manifest).close();
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("jarfile", tmpJarFile,
					MediaType.APPLICATION_OCTET_STREAM_TYPE);
			MultiPart multiPart = new MultiPart();
			multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			multiPart.bodyPart(fileDataBodyPart);
			Response response = target.request().cookie(cookie)
					.post(Entity.entity(multiPart, multiPart.getMediaType()));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Service package upload successful",
					result.getAsString("text"));
			Assert.assertEquals("Service package upload successful", result.getAsString("msg"));
			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
