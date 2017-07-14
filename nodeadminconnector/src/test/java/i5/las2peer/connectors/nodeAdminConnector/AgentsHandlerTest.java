package i5.las2peer.connectors.nodeAdminConnector;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class AgentsHandlerTest extends AbstractTestHandler {

	@Test
	public void testPostCreateAgentMinimal() {
		try {
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("password", "topsecret");
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/createAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Agent created", result.getAsString("text"));
			Assert.assertNotNull(result.getAsString("agentid"));
			Assert.assertNull(result.getAsString("username"));
			Assert.assertNull(result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostCreateAgentFull() {
		try {
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("password", "topsecret");
			formData.field("username", "testuser");
			formData.field("email", "testuser@example.org");
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/createAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Agent created", result.getAsString("text"));
			Assert.assertNotNull(result.getAsString("agentid"));
			Assert.assertEquals("testuser", result.getAsString("username"));
			Assert.assertEquals("testuser@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetAgentById() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("agentid", adam.getIdentifier());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/getAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(adam.getIdentifier(), result.getAsString("agentid"));
			Assert.assertEquals("adam", result.getAsString("username"));
			Assert.assertEquals("adam@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetAgentByUsername() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("username", adam.getLoginName());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/getAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(adam.getIdentifier(), result.getAsString("agentid"));
			Assert.assertEquals("adam", result.getAsString("username"));
			Assert.assertEquals("adam@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetAgentByEmail() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("email", adam.getEmail());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/getAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(adam.getIdentifier(), result.getAsString("agentid"));
			Assert.assertEquals("adam", result.getAsString("username"));
			Assert.assertEquals("adam@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetAgentDouble() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("agentid", adam.getIdentifier());
			formData.field("username", "eva");
			formData.field("email", "testuser@example.org");
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/getAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(adam.getIdentifier(), result.getAsString("agentid"));
			Assert.assertEquals("adam", result.getAsString("username"));
			Assert.assertEquals("adam@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostExportAgentById() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("agentid", adam.getIdentifier());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/exportAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals(adam.toXmlString(), new String(bytes, StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostExportAgentByUsername() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("username", adam.getLoginName());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/exportAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals(adam.toXmlString(), new String(bytes, StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostExportAgentByEmail() {
		try {
			// prepare network
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			adam.setEmail("adam@example.org");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			// start actual test
			FormDataMultiPart formData = new FormDataMultiPart();
			formData.field("email", adam.getEmail());
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/exportAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			Assert.assertEquals(adam.toXmlString(), new String(bytes, StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostUploadAgent() {
		try {
			FormDataMultiPart formData = new FormDataMultiPart();
			File agentFile = new File("src/test/java/i5/las2peer/connectors/nodeAdminConnector/smith.xml");
			formData.bodyPart(new FileDataBodyPart("agentFile", agentFile));
			formData.field("agentPassword", "adamspass");
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/uploadAgent");
			Response response = target.request().post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Agent uploaded", result.getAsString("text"));
			Assert.assertEquals(MockAgentFactory.getAdam().getIdentifier(), result.getAsString("agentid"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostUploadGroupAgent() {
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
			FormDataMultiPart formData = new FormDataMultiPart();
			File agentFile = new File("src/test/java/i5/las2peer/connectors/nodeAdminConnector/group.xml");
			formData.bodyPart(new FileDataBodyPart("agentFile", agentFile));
			formData.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			WebTarget target = sslClient.target(connector.getHostname() + "/agents/uploadAgent");
			Response response = target.request().cookie(cookie)
					.post(Entity.entity(formData, MediaType.MULTIPART_FORM_DATA_TYPE));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Agent uploaded", result.getAsString("text"));
			Assert.assertEquals(MockAgentFactory.getGroup1().getIdentifier(), result.getAsString("agentid"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testChangePassphrase() {
		try {
			// FIXME method stub
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostCreateGroup() {
		try {
			// FIXME method stub
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostLoadGroup() {
		try {
			// FIXME method stub
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostChangeGroup() {
		try {
			// TODO method stub
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
