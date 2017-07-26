package i5.las2peer.connectors.webConnector;

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

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.connectors.webConnector.handler.AuthHandler;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.SimpleTools;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class AuthHandlerTest extends AbstractTestHandler {

	@Test
	public void testPostCreateMinimal() {
		try {
			JSONObject jsonBody = new JSONObject();
			jsonBody.put("password", "topsecret");
			WebTarget target = webClient.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/create");
			Response response = target.request().post(Entity.json(jsonBody.toJSONString()));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Login OK", result.getAsString("text"));
			Assert.assertNotNull(result.getAsString("agentid"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testPostCreateFull() {
		try {
			JSONObject jsonBody = new JSONObject();
			jsonBody.put("password", "topsecret");
			jsonBody.put("username", "testuser");
			jsonBody.put("email", "testuser@example.org");
			WebTarget target = webClient.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/create");
			Response response = target.request().post(Entity.json(jsonBody.toJSONString()));
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Login OK", result.getAsString("text"));
			Assert.assertNotNull(result.getAsString("agentid"));
			Assert.assertEquals("testuser", result.getAsString("username"));
			Assert.assertEquals("testuser@example.org", result.getAsString("email"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetLogin() {
		try {
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			WebTarget targetLogin = webClient
					.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/login");
			Response responseLogin = targetLogin.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), responseLogin.getStatus());
			NewCookie cookie = responseLogin.getCookies().get(WebConnector.COOKIE_SESSIONID_KEY);
			Assert.assertNotNull(cookie);
			Assert.assertEquals(-1, cookie.getMaxAge());
			Assert.assertEquals(WebConnector.COOKIE_SESSIONID_KEY, cookie.getName());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetLogout() {
		try {
			// first login
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			WebTarget targetLogin = webClient
					.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/login");
			Response responseLogin = targetLogin.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), responseLogin.getStatus());
			NewCookie cookie = responseLogin.getCookies().get(WebConnector.COOKIE_SESSIONID_KEY);
			Assert.assertNotNull(cookie);
			Assert.assertEquals(WebConnector.COOKIE_SESSIONID_KEY, cookie.getName());
			// start actual test
			WebTarget target = webClient.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/logout");
			Response response = target.request().cookie(cookie).get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Logout successful", result.getAsString("text"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetValidateWithLogin() {
		try {
			// first login
			UserAgentImpl adam = MockAgentFactory.getAdam();
			adam.unlock("adamspass");
			PastryNodeImpl activeNode = nodes.get(0);
			activeNode.storeAgent(adam);
			WebTarget targetLogin = webClient
					.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/login");
			Response responseLogin = targetLogin.request()
					.header(HttpHeaders.AUTHORIZATION, "basic " + Base64.getEncoder()
							.encodeToString((adam.getLoginName() + ":" + "adamspass").getBytes(StandardCharsets.UTF_8)))
					.get();
			Assert.assertEquals(Status.OK.getStatusCode(), responseLogin.getStatus());
			NewCookie cookie = responseLogin.getCookies().get(WebConnector.COOKIE_SESSIONID_KEY);
			Assert.assertNotNull(cookie);
			Assert.assertEquals(WebConnector.COOKIE_SESSIONID_KEY, cookie.getName());
			// start actual test
			WebTarget target = webClient.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/validate");
			Response response = target.request().cookie(cookie).get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Session OK", result.getAsString("text"));
			Assert.assertNotNull(result.getAsString("agentid"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGetValidateWithoutLogin() {
		try {
			WebTarget target = webClient.target(connector.getHttpEndpoint() + AuthHandler.RESOURCE_PATH + "/validate");
			// use some self baked cookie
			NewCookie cookie = new NewCookie(WebConnector.COOKIE_SESSIONID_KEY, "12345678", "/", null, null, 0, true,
					true);
			Response response = target.request().cookie(cookie).get();
			Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
			Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
			byte[] bytes = SimpleTools.toByteArray((InputStream) response.getEntity());
			JSONObject result = (JSONObject) new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes);
			Assert.assertEquals(Status.OK.getStatusCode(), result.getAsNumber("code"));
			Assert.assertEquals(Status.OK.getStatusCode() + " - Session invalid", result.getAsString("text"));
			Assert.assertNull(result.getAsString("agentid"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
