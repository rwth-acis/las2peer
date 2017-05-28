package i5.las2peer.connectors.webConnector;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;

import javax.ws.rs.core.HttpHeaders;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.connectors.webConnector.services.TestClassLoaderService;
import i5.las2peer.connectors.webConnector.services.TestDeepPathService;
import i5.las2peer.connectors.webConnector.services.TestSecurityContextService;
import i5.las2peer.connectors.webConnector.services.TestService;
import i5.las2peer.connectors.webConnector.services.TestSwaggerService;
import i5.las2peer.connectors.webConnector.services.TestVersionService;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

public class WebConnectorTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgentImpl testAgent;
	private static final String testPass = "adamspass";

	private static final String testServiceClass1 = TestSecurityContextService.class.getName() + "@1.0";
	private static final String testServiceClass2 = TestSwaggerService.class.getName() + "@1.0";
	private static final String testServiceClass3 = TestVersionService.class.getName() + "@1.0";
	private static final String testServiceClass4 = TestService.class.getName() + "@1.0";
	private static final String testServiceClass5 = TestClassLoaderService.class.getName() + "@1.0";
	private static final String testServiceClass6 = TestDeepPathService.class.getName() + "@1.0";

	@BeforeClass
	public static void startServer() throws Exception {
		// init agents
		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");
		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
		adam.setLoginName("adam");
		UserAgentImpl abel = MockAgentFactory.getAbel();
		abel.unlock("abelspass");
		GroupAgentImpl group1 = MockAgentFactory.getGroup1();
		group1.unlock(adam);

		// start Node
		node = LocalNode.newNode();
		node.storeAgent(eve);
		node.storeAgent(adam);
		node.storeAgent(abel);
		node.storeAgent(group1);
		node.launch();

		ServiceAgentImpl testService1 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass1), "a pass");
		ServiceAgentImpl testService2 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass2), "a pass");
		ServiceAgentImpl testService3 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass3), "a pass");
		ServiceAgentImpl testService4 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass4), "a pass");
		ServiceAgentImpl testService5 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass5), "a pass");
		ServiceAgentImpl testService6 = ServiceAgentImpl
				.createServiceAgent(ServiceNameVersion.fromString(testServiceClass6), "a pass");

		testService1.unlock("a pass");
		testService2.unlock("a pass");
		testService3.unlock("a pass");
		testService4.unlock("a pass");
		testService5.unlock("a pass");
		testService6.unlock("a pass");

		node.registerReceiver(testService1);
		node.registerReceiver(testService2);
		node.registerReceiver(testService3);
		node.registerReceiver(testService4);
		node.registerReceiver(testService5);
		node.registerReceiver(testService6);

		// start connector
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setCrossOriginResourceDomain("*");
		connector.setCrossOriginResourceSharing(true);
		logStream = new ByteArrayOutputStream();
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);

		testAgent = adam;
		testAgent.unlock("adamspass");
		node.storeAgent(testAgent);
	}

	@AfterClass
	public static void shutDownServer() throws Exception {
		connector.stop();
		node.shutDown();

		connector = null;
		node = null;

		LocalNode.reset();

		System.out.println("Connector-Log:");
		System.out.println("--------------");
		System.out.println(logStream.toString());
	}

	@Test
	public void testNotMethodService() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse response = c.sendRequest("GET", "service1/asdag", "");
			Assert.assertEquals(404, response.getHttpCode());
		} catch (Exception e) {
			Assert.fail("Not existing service caused wrong exception");
		}
	}

	@Test
	public void testLogin() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		// correct, id based
		try {
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("get", "test/ok", "");
			Assert.assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}

		// correct, name based
		try {
			c.setLogin("adam", testPass);

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			Assert.assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}

		// invalid password
		try {
			c.setLogin(testAgent.getIdentifier(), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			Assert.assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
		// invalid user
		try {
			c.setLogin(Long.toString(65464), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			Assert.assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}

		// no authentication, use default (wrong)
		try {
			connector.defaultLoginUser = "Hans";
			connector.defaultLoginPassword = "asdasd";
			c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(Long.toString(65464), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			Assert.assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testExceptions() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);
			// unknown service
			ClientResponse result = c.sendRequest("GET", "doesNotExist", "");
			Assert.assertEquals(404, result.getHttpCode());

			// exception in invocation
			result = c.sendRequest("GET", "test/exception", "");
			Assert.assertEquals(500, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testCrossOriginHeader() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			// this test should work for an unknown function, too
			ClientResponse response = c.sendRequest("GET", "asdag", "");
			Assert.assertEquals(connector.crossOriginResourceDomain, response.getHeader("Access-Control-Allow-Origin"));
			Assert.assertEquals(String.valueOf(connector.crossOriginResourceMaxAge),
					response.getHeader("Access-Control-Max-Age"));
		} catch (Exception e) {
			Assert.fail("Not existing service caused wrong exception");
		}
	}

	@Test
	public void testPath() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "version/path", "");
			Assert.assertTrue(result.getResponse().trim().endsWith("version/"));

			result = c.sendRequest("GET", "version/v1/path", "");
			Assert.assertTrue(result.getResponse().trim().endsWith("version/v1/"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testSwagger() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", "swaggertest/swagger.json", "");

			Assert.assertTrue(result.getResponse().trim().contains("createSomething"));
			Assert.assertTrue(result.getResponse().trim().contains("subresource/content"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testResponseCode() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("PUT", "swaggertest/create/notfound", "");
			Assert.assertEquals(404, result.getHttpCode());

			result = c.sendRequest("PUT", "swaggertest/create/asdf", "");
			Assert.assertEquals(200, result.getHttpCode());

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testSubresource() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "swaggertest/subresource/content", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertEquals("test", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testUploadLimit() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			byte[] testContent = new byte[WebConnector.DEFAULT_MAX_REQUEST_BODY_SIZE];
			new Random().nextBytes(testContent);
			String base64 = Base64.getEncoder().encodeToString(testContent);
			ClientResponse result = c.sendRequest("POST", "test", base64);
			Assert.assertEquals(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testSecurityContextIntegration() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			// unauthenticated request
			c.setLogin(AnonymousAgent.LOGIN_NAME, AnonymousAgent.PASSPHRASE);

			ClientResponse result = c.sendRequest("GET", "security/name", "");
			System.out.println("RESPONSE: " + result.getResponse());
			Assert.assertEquals("no principal", result.getResponse().trim());
			Assert.assertEquals(403, result.getHttpCode());
			result = c.sendRequest("GET", "security/authenticated", "");
			Assert.assertEquals(403, result.getHttpCode());
			result = c.sendRequest("GET", "security/anonymous", "");
			Assert.assertEquals(200, result.getHttpCode());

			// authenticated request
			c.setLogin(testAgent.getIdentifier(), testPass);
			result = c.sendRequest("GET", "security/name", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertEquals("adam", result.getResponse().trim());
			result = c.sendRequest("GET", "security/authenticated", "");
			Assert.assertEquals(200, result.getHttpCode());
			result = c.sendRequest("GET", "security/anonymous", "");
			Assert.assertEquals(200, result.getHttpCode());
			result = c.sendRequest("GET", "security/bot", "");
			Assert.assertEquals(403, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testClassLoading() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "classloader/test", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testEmptyResponse() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "test/empty", "");
			Assert.assertEquals(200, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testAuthParamSanitization() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			// test auth params in GET
			ClientResponse result = c.sendRequest("GET", "test/requesturi?param1=sadf&access_token=secret", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().contains("param1"));
			Assert.assertFalse(result.getResponse().contains("secret"));
			Assert.assertFalse(result.getResponse().contains("access_token"));

			// test auth params in header
			HashMap<String, String> headers = new HashMap<>();
			headers.put("param1", "asdf");
			result = c.sendRequest("GET", "test/headers", "", headers);
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().toLowerCase().contains("param1"));
			Assert.assertFalse(result.getResponse().toLowerCase().contains("authorization"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testEncoding() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "test/encoding", "");
			Assert.assertEquals(200, result.getHttpCode());
			final String header = result.getHeader(HttpHeaders.CONTENT_TYPE);
			System.out.println("header is: " + header);
			Assert.assertNotNull(header);
			Assert.assertTrue(header.toLowerCase().contains("charset=utf-8"));
			final String response = result.getResponse();
			System.out.println("response is: " + response);
			Assert.assertNotNull(response);
			Assert.assertTrue(response.contains("☺"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testBody() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			String body = "This is a test.";
			ClientResponse result = c.sendRequest("POST", "test/body", body);
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().trim().equals(body));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}

	@Test
	public void testPathResolve() {
		try {
			MiniClient c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(testAgent.getIdentifier(), testPass);

			ClientResponse result = c.sendRequest("GET", "deep/path/test", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().trim().endsWith("deep/path/"));

			result = c.sendRequest("GET", "deep/path/v1/test", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().trim().endsWith("deep/path/v1/"));

			result = c.sendRequest("GET", "deep/path", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().trim().endsWith("deep/path/"));

			result = c.sendRequest("GET", "deep/path/v1", "");
			Assert.assertEquals(200, result.getHttpCode());
			Assert.assertTrue(result.getResponse().trim().endsWith("deep/path/v1/"));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception: " + e);
		}
	}
}
