package i5.las2peer.webConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;
import i5.las2peer.webConnector.services.TestClassLoaderService;
import i5.las2peer.webConnector.services.TestSecurityContextService;
import i5.las2peer.webConnector.services.TestService;
import i5.las2peer.webConnector.services.TestSwaggerService;
import i5.las2peer.webConnector.services.TestVersionService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebConnectorTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

	private static final String testServiceClass1 = TestSecurityContextService.class.getName() + "@1.0";
	private static final String testServiceClass2 = TestSwaggerService.class.getName() + "@1.0";
	private static final String testServiceClass3 = TestVersionService.class.getName() + "@1.0";
	private static final String testServiceClass4 = TestService.class.getName() + "@1.0";
	private static final String testServiceClass5 = TestClassLoaderService.class.getName() + "@1.0";

	@BeforeClass
	public static void startServer() throws Exception {
		// init agents
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		UserAgent adam = MockAgentFactory.getAdam();
		adam.unlockPrivateKey("adamspass");
		adam.setLoginName("adam");
		UserAgent abel = MockAgentFactory.getAbel();
		abel.unlockPrivateKey("abelspass");
		GroupAgent group1 = MockAgentFactory.getGroup1();
		group1.unlockPrivateKey(adam);

		// start Node
		node = LocalNode.newNode();
		node.storeAgent(eve);
		node.storeAgent(adam);
		node.storeAgent(abel);
		node.storeAgent(group1);
		node.launch();

		ServiceAgent testService1 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass1),
				"a pass");
		ServiceAgent testService2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass2),
				"a pass");
		ServiceAgent testService3 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass3),
				"a pass");
		ServiceAgent testService4 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass4),
				"a pass");
		ServiceAgent testService5 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass5),
				"a pass");

		testService1.unlockPrivateKey("a pass");
		testService2.unlockPrivateKey("a pass");
		testService3.unlockPrivateKey("a pass");
		testService4.unlockPrivateKey("a pass");
		testService5.unlockPrivateKey("a pass");

		node.registerReceiver(testService1);
		node.registerReceiver(testService2);
		node.registerReceiver(testService3);
		node.registerReceiver(testService4);
		node.registerReceiver(testService5);

		// start connector
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setCrossOriginResourceDomain("*");
		connector.setCrossOriginResourceSharing(true);
		logStream = new ByteArrayOutputStream();
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);

		testAgent = adam;
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
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			ClientResponse response = c.sendRequest("GET", "service1/asdag", "");
			assertEquals(404, response.getHttpCode());
		} catch (Exception e) {
			fail("Not existing service caused wrong exception");
		}
	}

	@Test
	public void testLogin() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		// correct, id based
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("get", "test/ok", "");
			assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}

		// correct, name based
		try {
			c.setLogin("adam", testPass);

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}

		// invalid password
		try {
			c.setLogin(Long.toString(testAgent.getId()), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		// invalid user
		try {
			c.setLogin(Long.toString(65464), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}

		// no authentication, use default (wrong)
		try {
			connector.defaultLoginUser = "Hans";
			connector.defaultLoginPassword = "asdasd";
			c = new MiniClient();
			c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
			c.setLogin(Long.toString(65464), "aaaaaaaaaaaaa");

			ClientResponse result = c.sendRequest("GET", "test/ok", "");
			assertEquals(401, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testExceptions() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			// unknown service
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "doesNotExist", "");
			assertEquals(404, result.getHttpCode());

			// exception in invocation
			result = c.sendRequest("GET", "test/exception", "");
			assertEquals(500, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testCrossOriginHeader() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			// this test should work for an unknown function, too
			ClientResponse response = c.sendRequest("GET", "asdag", "");
			assertEquals(connector.crossOriginResourceDomain, response.getHeader("Access-Control-Allow-Origin"));
			assertEquals(String.valueOf(connector.crossOriginResourceMaxAge),
					response.getHeader("Access-Control-Max-Age"));
		} catch (Exception e) {
			fail("Not existing service caused wrong exception");
		}
	}

	@Test
	public void testPath() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			ClientResponse result = c.sendRequest("GET", "version/path", "");
			assertTrue(result.getResponse().trim().endsWith("version/"));

			result = c.sendRequest("GET", "version/v1/path", "");
			assertTrue(result.getResponse().trim().endsWith("version/v1/"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testSwagger() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "swaggertest/swagger.json", "");

			assertTrue(result.getResponse().trim().contains("createSomething"));
			assertTrue(result.getResponse().trim().contains("subresource/content"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testResponseCode() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			ClientResponse result = c.sendRequest("PUT", "swaggertest/create/notfound", "");
			assertEquals(404, result.getHttpCode());

			result = c.sendRequest("PUT", "swaggertest/create/asdf", "");
			assertEquals(200, result.getHttpCode());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testSubresource() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			ClientResponse result = c.sendRequest("GET", "swaggertest/subresource/content", "");
			assertEquals(200, result.getHttpCode());
			assertEquals("test", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testUploadLimit() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			byte[] testContent = new byte[WebConnector.DEFAULT_MAX_REQUEST_BODY_SIZE];
			new Random().nextBytes(testContent);
			String base64 = Base64.getEncoder().encodeToString(testContent);
			ClientResponse result = c.sendRequest("POST", "test", base64);
			assertEquals(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testSecurityContextIntegration() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			// unauthenticated request
			c.setLogin(Long.toString(node.getAnonymous().getId()), "anonymous");
			ClientResponse result = c.sendRequest("GET", "security/name", "");
			System.out.println("RESPONSE: " + result.getResponse());
			assertEquals("no principal", result.getResponse().trim());
			assertEquals(403, result.getHttpCode());
			result = c.sendRequest("GET", "security/authenticated", "");
			assertEquals(403, result.getHttpCode());
			result = c.sendRequest("GET", "security/anonymous", "");
			assertEquals(200, result.getHttpCode());

			// authenticated request
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			result = c.sendRequest("GET", "security/name", "");
			assertEquals(200, result.getHttpCode());
			assertEquals("adam", result.getResponse().trim());
			result = c.sendRequest("GET", "security/authenticated", "");
			assertEquals(200, result.getHttpCode());
			result = c.sendRequest("GET", "security/anonymous", "");
			assertEquals(200, result.getHttpCode());
			result = c.sendRequest("GET", "security/bot", "");
			assertEquals(403, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testClassLoading() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "classloader/test", "");
			assertEquals(200, result.getHttpCode());
			assertEquals("OK", result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testEmptyResponse() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "test/empty", "");
			assertEquals(200, result.getHttpCode());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testAuthParamSanitization() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);

			// test auth params in GET
			ClientResponse result = c.sendRequest("GET", "test/requesturi?param1=sadf&access_token=secret", "");
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().contains("param1"));
			assertFalse(result.getResponse().contains("secret"));
			assertFalse(result.getResponse().contains("access_token"));

			// test auth params in header
			HashMap<String, String> headers = new HashMap<>();
			headers.put("param1", "asdf");
			result = c.sendRequest("GET", "test/headers", "", headers);
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().toLowerCase().contains("param1"));
			assertFalse(result.getResponse().toLowerCase().contains("authorization"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testEncoding() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "test/encoding", "");
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().contains("☺"));
			assertTrue(result.getHeaders().get("content-type").contains("charset=utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void testBody() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		try {
			String body = "This is a test.";
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("POST", "test/body", body);
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().trim().equals(body));
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
}
