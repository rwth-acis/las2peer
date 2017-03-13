package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

import java.io.Serializable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LocalNodeInvocationTest {

	@Before
	public void reset() {
		LocalNode.reset();

		// make results determinant
		LocalNode.setMinMessageWait(100);
		LocalNode.setMaxMessageWait(100);
	}

	@After
	public void clearThreads() {
		LocalNode.stopCleaner();
	}

	@Test
	public void testLocalInvocation() {
		try {
			LocalNode node = LocalNode.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			node.storeAgent(eve);

			node.launch();

			ServiceAgentImpl testServiceAgent = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
			testServiceAgent.unlock("a pass");
			node.registerReceiver(testServiceAgent);

			Object result = node.invokeLocally(eve, testServiceAgent, "inc", new Serializable[] { new Integer(10) });

			assertEquals(12, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGlobalInvocation() {
		try {
			LocalNode serviceNode = LocalNode.newNode();
			serviceNode.getNodeServiceCache().setWaitForResults(3);
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode.storeAgent(eve);
			serviceNode.launch();

			ServiceAgentImpl testServiceAgent = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
			testServiceAgent.unlock("a pass");
			serviceNode.registerReceiver(testServiceAgent);

			LocalNode callerNode = LocalNode.launchNode();
			Object result = callerNode.invokeGlobally(eve, testServiceAgent.getIdentifier(), testServiceAgent
					.getRunningAtNode().getNodeId(), "inc", new Serializable[] { new Integer(12) });

			assertEquals(14, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSubinvocation() {
		try {
			LocalNode serviceNode1 = LocalNode.newNode();
			LocalNode serviceNode2 = LocalNode.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode1.storeAgent(eve);
			serviceNode1.launch();
			serviceNode2.launch();

			ServiceAgentImpl testServiceAgent = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
			testServiceAgent.unlock("a pass");
			serviceNode1.registerReceiver(testServiceAgent);

			ServiceAgentImpl testServiceAgent2 = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");
			testServiceAgent2.unlock("a 2nd pass");
			serviceNode2.registerReceiver(testServiceAgent2);

			LocalNode callerNode = LocalNode.launchNode();
			Object result = callerNode.invoke(eve, "i5.las2peer.api.TestService2@1.0", "usingOther",
					new Serializable[] { new Integer(12) });

			assertEquals(14, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSubinvocationFail() {
		try {
			LocalNode serviceNode2 = LocalNode.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode2.storeAgent(eve);
			serviceNode2.launch();

			ServiceAgentImpl testServiceAgent2 = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");
			testServiceAgent2.unlock("a 2nd pass");
			serviceNode2.registerReceiver(testServiceAgent2);

			LocalNode callerNode = LocalNode.launchNode();
			Object result = callerNode.invoke(eve, "i5.las2peer.api.TestService2@1.0", "usingOther",
					new Serializable[] { new Integer(12) });

			assertEquals(-200, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testInvocation() {
		try {
			// start
			LocalNode serviceNode1 = LocalNode.newNode("export/jars/");
			LocalNode serviceNode2 = LocalNode.newNode("export/jars/");
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode1.storeAgent(eve);
			serviceNode1.launch();
			serviceNode2.launch();

			ServiceAgentImpl usingAgent = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "a pass");
			usingAgent.unlock("a pass");
			serviceNode1.registerReceiver(usingAgent);

			ServiceAgentImpl serviceAgent1 = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.0"), "a pass");
			serviceAgent1.unlock("a pass");
			serviceNode2.registerReceiver(serviceAgent1);

			ServiceAgentImpl serviceAgent2 = ServiceAgentImpl.createServiceAgent(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.1"), "a pass");
			serviceAgent2.unlock("a pass");
			serviceNode2.registerReceiver(serviceAgent2);
			
			LocalNode callerNode = LocalNode.launchNode();

			// specify exact version
			Object result = callerNode.invoke(eve,
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.0"),
					"getVersion", new Serializable[] {}, true);
			assertEquals(100, result);

			result = callerNode.invoke(eve,
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.1"),
					"getVersion", new Serializable[] {}, true);
			assertEquals(110, result);

			// use newest version
			result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService", "getVersion",
					new Serializable[] {});
			assertEquals(110, result);

			// choose appropriate version
			result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1.0", "getVersion",
					new Serializable[] {});
			assertEquals(100, result);

			result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1.1", "getVersion",
					new Serializable[] {});
			assertEquals(110, result);

			result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1", "getVersion",
					new Serializable[] {});
			assertEquals(110, result);

			// fail on non-existent version
			boolean failed = false;
			try {
				result = callerNode.invoke(eve,
						ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@2.0"),
						"getVersion", new Serializable[] {}, true);
			} catch (ServiceNotFoundException e) {
				failed = true;
			}
			assertTrue(failed);

			// test version selection with RMI
			result = callerNode.invoke(eve,
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"),
					"useTestService", new Serializable[] { "null" });
			assertEquals(110, result);

			result = callerNode.invoke(eve,
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"),
					"useTestService", new Serializable[] { "1.0" });
			assertEquals(100, result);

			result = callerNode.invoke(eve,
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"),
					"useTestService", new Serializable[] { "1.1" });
			assertEquals(110, result);

			failed = false;
			try {
				result = callerNode.invoke(eve,
						ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"),
						"useTestService", new Serializable[] { "2.0" });
			} catch (ServiceInvocationException e) {
				failed = true;
			}
			assertTrue(failed);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
