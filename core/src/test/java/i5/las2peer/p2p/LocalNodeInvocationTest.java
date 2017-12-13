package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.classLoaders.ServiceClassLoader;
import i5.las2peer.execution.ExecutionContext;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

public class LocalNodeInvocationTest {

	private LocalNodeManager manager;

	@Before
	public void init() {
		manager = new LocalNodeManager();
		// make results determinant
		manager.setMinMessageWait(100);
		manager.setMaxMessageWait(100);
	}

	@Test
	public void testLocalInvocation() {
		try {
			LocalNode node = manager.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			node.storeAgent(eve);

			node.launch();

			ServiceAgentImpl testServiceAgent = node
					.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");

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
			LocalNode serviceNode = manager.newNode();
			serviceNode.getNodeServiceCache().setWaitForResults(3);
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode.storeAgent(eve);
			serviceNode.launch();

			ServiceAgentImpl testServiceAgent = serviceNode
					.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");

			LocalNode callerNode = manager.launchNode();
			Object result = callerNode.invokeGlobally(eve, testServiceAgent.getIdentifier(),
					testServiceAgent.getRunningAtNode().getNodeId(), "inc", new Serializable[] { new Integer(12) });

			assertEquals(14, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testSubinvocation() {
		try {
			LocalNode serviceNode1 = manager.newNode();
			LocalNode serviceNode2 = manager.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode1.storeAgent(eve);
			serviceNode1.launch();
			serviceNode2.launch();

			serviceNode1.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");

			serviceNode2.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");

			LocalNode callerNode = manager.launchNode();
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
			LocalNode serviceNode2 = manager.newNode();
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode2.storeAgent(eve);
			serviceNode2.launch();

			serviceNode2.startService(ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");

			LocalNode callerNode = manager.launchNode();
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
			LocalNode serviceNode1 = manager.newNode("export/jars/");
			LocalNode serviceNode2 = manager.newNode("export/jars/");
			UserAgentImpl eve = MockAgentFactory.getEve();

			eve.unlock("evespass");
			serviceNode1.storeAgent(eve);
			serviceNode1.launch();
			serviceNode2.launch();

			serviceNode1.startService(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "a pass");

			serviceNode2.startService(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.0"), "a pass");

			serviceNode2.startService(
					ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.1"), "a pass");

			LocalNode callerNode = manager.launchNode();

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

	@Test
	public void testMultiNodeClassLoading() {
		try {
			// service jar is used
			LocalNodeManager manager = new LocalNodeManager();
			LocalNode serviceNode1 = manager.newNode(new String[] { "export/jars/", "core/export/jars/" });
			serviceNode1.launch();
			LocalNode serviceNode2 = manager.newNode(new String[] { "export/jars/", "core/export/jars/" });
			serviceNode2.launch();

			UserAgentImpl eve = MockAgentFactory.getEve();
			eve.unlock("evespass");
			serviceNode1.storeAgent(eve);

			// start two nodes with both powering the service
			ServiceNameVersion snv = new ServiceNameVersion("i5.las2peer.testServices.testPackage3.PackageTestService",
					"1.0");
			ServiceAgentImpl serviceAgent1 = serviceNode1.startService(snv, "a pass");
			Assert.assertEquals(ServiceClassLoader.class,
					serviceAgent1.getServiceInstance().getClass().getClassLoader().getClass());

			ServiceAgentImpl serviceAgent2 = serviceNode2.startService(snv, "a pass");
			Assert.assertEquals(ServiceClassLoader.class,
					serviceAgent2.getServiceInstance().getClass().getClassLoader().getClass());

			ExecutionContext context = new ExecutionContext(serviceAgent1, serviceNode1.getAgentContext(eve),
					serviceNode1);

			// make regular RMI call (here locally)
			Object result = context.invoke(snv, "getValue", new Serializable[] { 42 });
			Assert.assertEquals(ServiceClassLoader.class.getCanonicalName(),
					result.getClass().getClassLoader().getClass().getCanonicalName());
			Class<?> resultCls = result.getClass().getClassLoader()
					.loadClass("i5.las2peer.testServices.testPackage3.helperClasses.SomeValue");
			Assert.assertEquals(resultCls, result.getClass());
			Assert.assertEquals(-42, resultCls.getMethod("getValue").invoke(result));

			// make first node "busy"
			serviceNode1.setCpuLoadThreshold(-1);

			// make RMI call from first to second node (because node1 is busy now)
			result = context.invoke(snv, "getValue", new Serializable[] { 42 });
			Assert.assertEquals(ServiceClassLoader.class.getCanonicalName(),
					result.getClass().getClassLoader().getClass().getCanonicalName());
			resultCls = result.getClass().getClassLoader()
					.loadClass("i5.las2peer.testServices.testPackage3.helperClasses.SomeValue");
			Assert.assertEquals(resultCls, result.getClass());
			Assert.assertEquals(-42, resultCls.getMethod("getValue").invoke(result));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
