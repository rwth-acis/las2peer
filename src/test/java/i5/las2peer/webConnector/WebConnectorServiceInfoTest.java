package i5.las2peer.webConnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

public class WebConnectorServiceInfoTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

	private static final String testServiceClass = "i5.las2peer.webConnector.TestService@0.1";
	private static final String testServiceClass2 = "i5.las2peer.webConnector.TestService2@0.1";
	private static final String testServiceClass3 = "i5.las2peer.webConnector.Calculator3@0.1";
	private static final String testServiceClass4 = "i5.las2peer.webConnector.Calculator2CompatibilityService@0.1";
	private static final String testServiceClass5 = "i5.las2peer.webConnector.Calculator2@0.1";

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

		ServiceAgent testService = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass), "a pass");
		ServiceAgent testService2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass2), "a pass");
		ServiceAgent testService3 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass3), "a pass");
		ServiceAgent testService4 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass4), "a pass");
		ServiceAgent testService5 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString(testServiceClass5), "a pass");

		testService.unlockPrivateKey("a pass");
		testService2.unlockPrivateKey("a pass");
		testService3.unlockPrivateKey("a pass");
		testService4.unlockPrivateKey("a pass");
		testService5.unlockPrivateKey("a pass");

		node.registerReceiver(testService);
		node.registerReceiver(testService2);
		node.registerReceiver(testService3);
		node.registerReceiver(testService4);
		node.registerReceiver(testService5);

		// start connector
		logStream = new ByteArrayOutputStream();
		connector = new WebConnector(true, HTTP_PORT, false, 1000, "./XMLCompatibility");
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
	public void testServices() {
		connector.updateServiceList();

		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		// Calculator3, only known by XML in ./XMLCompatibility
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "5/add/6", "");
			assertEquals("11.0", result.getResponse().trim());
		} catch (Exception e) {
			fail(e.getMessage());
		}

		// TestService, only known by XML in ./XMLCompatibility AND getRESTMapping()
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("PUT", "add/7/6", "");
			assertEquals("13", result.getResponse().trim());
		} catch (Exception e) {
			fail(e.getMessage());
		}
		// Calculator2 only indirectly known by Calculator2CompatibilityService
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", "mul/7/6", "");
			assertEquals("42.0", result.getResponse().trim());
		} catch (Exception e) {
			fail(e.getMessage());
		}
		// TestService2, only known by getRESTMapping()
		try {
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("POST", "do/h/i", "!");
			assertEquals("hi!", result.getResponse().trim());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
