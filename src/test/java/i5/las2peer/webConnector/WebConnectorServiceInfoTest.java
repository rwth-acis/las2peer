package i5.las2peer.webConnector;

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
import i5.las2peer.webConnector.services.TestMissingPathService;
import i5.las2peer.webConnector.services.TestVersionService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebConnectorServiceInfoTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

	private static final String testServiceClass1 = TestVersionService.class.getName() + "@1";
	private static final String testServiceClass2 = TestVersionService.class.getName() + "@2.0";
	private static final String testServiceClass3 = TestVersionService.class.getName() + "@2.1";
	private static final String testServiceClass4 = TestVersionService.class.getName() + "@2.2.0-1";
	private static final String testServiceClass5 = TestVersionService.class.getName() + "@2.2.0-2";
	private static final String testServiceClass6 = TestMissingPathService.class.getName() + "@1";

	@BeforeClass
	public static void startServer() throws Exception {
		// init agents
		UserAgent eve = MockAgentFactory.getEve();
		eve.unlockPrivateKey("evespass");
		UserAgent adam = MockAgentFactory.getAdam();
		adam.unlockPrivateKey("adamspass");
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
		ServiceAgent testService6 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass6),
				"a pass");

		testService1.unlockPrivateKey("a pass");
		testService2.unlockPrivateKey("a pass");
		testService3.unlockPrivateKey("a pass");
		testService4.unlockPrivateKey("a pass");
		testService5.unlockPrivateKey("a pass");
		testService6.unlockPrivateKey("a pass");

		node.registerReceiver(testService1);
		node.registerReceiver(testService2);
		node.registerReceiver(testService3);
		node.registerReceiver(testService4);
		node.registerReceiver(testService5);
		node.registerReceiver(testService6); // should not throw an error

		// start connector
		logStream = new ByteArrayOutputStream();
		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);

		// eve is the anonymous agent!
		testAgent = MockAgentFactory.getAdam();
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
	public void testVersions() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		// without version
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "version/test", "");
			assertTrue(result.getResponse().trim().startsWith("2"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// unambiguous version
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v1/test", "");
			assertTrue(result.getResponse().trim().equals("1"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		node.getNodeServiceCache().clear();

		// ambiguous version
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v2/test", "");
			assertTrue(result.getResponse().trim().startsWith("2.2"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// exact version
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v2.2.0-1/test", "");
			assertTrue(result.getResponse().trim().equals("2.2.0-1"));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
