package i5.las2peer.connectors.webConnector;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.connectors.webConnector.services.TestMissingPathService;
import i5.las2peer.connectors.webConnector.services.TestVersionService;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.GroupAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

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
		UserAgentImpl eve = MockAgentFactory.getEve();
		eve.unlock("evespass");
		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock("adamspass");
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
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", "version/test", "");
			assertTrue(result.getResponse().trim().startsWith("2"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// unambiguous version
		try {
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v1/test", "");
			assertTrue(result.getResponse().trim().equals("1"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		node.getNodeServiceCache().clear();

		// ambiguous version
		try {
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v2/test", "");
			assertTrue(result.getResponse().trim().startsWith("2.2"));
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// exact version
		try {
			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", "version/v2.2.0-1/test", "");
			assertTrue(result.getResponse().trim().equals("2.2.0-1"));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
