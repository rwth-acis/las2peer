package i5.las2peer.p2p;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.execution.NoSuchServiceException;
import i5.las2peer.execution.ServiceInvocationException;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

public class LocalNodeInvocationTest {

	@Before
	public void reset() {
		LocalNode.reset();
	}

	@After
	public void clearThreads() {
		LocalNode.stopCleaner();
	}

	@Test
	public void testLocalInvocation()
			throws SecurityException, IllegalArgumentException, AgentNotKnownException, L2pSecurityException,
			NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException,
			AgentAlreadyRegisteredException, MalformedXMLException, IOException, CryptoException, AgentException, NodeException {
		LocalNode node = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		node.storeAgent(eve);

		node.launch();

		ServiceAgent testServiceAgent = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent);

		Object result = node.invokeLocally(eve.getId(), ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "inc",
				new Serializable[] { new Integer(10) });

		assertEquals(12, result);
	}

	@Test
	public void testGlobalInvocation() throws SecurityException, IllegalArgumentException, AgentNotKnownException,
			L2pSecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
			InterruptedException, AgentAlreadyRegisteredException, MalformedXMLException, IOException, CryptoException,
			AgentException, TimeoutException, NodeException {
		LocalNode serviceNode = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode.storeAgent(eve);
		serviceNode.launch();

		ServiceAgent testServiceAgent = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		serviceNode.registerReceiver(testServiceAgent);

		LocalNode callerNode = LocalNode.launchNode();
		eve.unlockPrivateKey("evespass");
		Object result = callerNode.invokeGlobally(eve, ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "inc",
				new Serializable[] { new Integer(12) });

		assertEquals(14, result);
	}

	@Test
	public void testSubinvocation() throws MalformedXMLException, IOException, L2pSecurityException, CryptoException,
			InterruptedException, AgentAlreadyRegisteredException, AgentException, TimeoutException, NodeException {
		LocalNode serviceNode1 = LocalNode.newNode();
		LocalNode serviceNode2 = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode1.storeAgent(eve);
		serviceNode1.launch();
		serviceNode2.launch();

		ServiceAgent testServiceAgent = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@1.0"), "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		serviceNode1.registerReceiver(testServiceAgent);

		ServiceAgent testServiceAgent2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");
		testServiceAgent2.unlockPrivateKey("a 2nd pass");
		serviceNode2.registerReceiver(testServiceAgent2);

		LocalNode callerNode = LocalNode.launchNode();
		eve.unlockPrivateKey("evespass");
		Object result = callerNode.invokeGlobally(eve, ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "usingOther",
				new Serializable[] { new Integer(12) });

		assertEquals(14, result);
	}

	@Test
	public void testSubinvocationFail() throws MalformedXMLException, IOException, L2pSecurityException,
			CryptoException, InterruptedException, AgentAlreadyRegisteredException, AgentException, TimeoutException, NodeException {
		LocalNode serviceNode2 = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode2.storeAgent(eve);
		serviceNode2.launch();

		ServiceAgent testServiceAgent2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "a 2nd pass");
		testServiceAgent2.unlockPrivateKey("a 2nd pass");
		serviceNode2.registerReceiver(testServiceAgent2);

		LocalNode callerNode = LocalNode.launchNode();
		eve.unlockPrivateKey("evespass");
		Object result = callerNode.invokeGlobally(eve, ServiceNameVersion.fromString("i5.las2peer.api.TestService2@1.0"), "usingOther",
				new Serializable[] { new Integer(12) });

		assertEquals(-200, result);

	}
	
	@Test
	public void testInvocation() throws MalformedXMLException, IOException, L2pSecurityException, AgentException, InterruptedException, TimeoutException, CryptoException, NodeException {
		// start
		LocalNode serviceNode1 = LocalNode.newNode("export/jars/");
		LocalNode serviceNode2 = LocalNode.newNode("export/jars/");
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode1.storeAgent(eve);
		serviceNode1.launch();
		serviceNode2.launch();

		ServiceAgent usingAgent = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "a pass");
		usingAgent.unlockPrivateKey("a pass");
		serviceNode1.registerReceiver(usingAgent);

		ServiceAgent serviceAgent1 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.0"), "a pass");
		serviceAgent1.unlockPrivateKey("a pass");
		serviceNode2.registerReceiver(serviceAgent1);
		
		ServiceAgent serviceAgent2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.1"), "a pass");
		serviceAgent2.unlockPrivateKey("a pass");
		serviceNode2.registerReceiver(serviceAgent2);

		LocalNode callerNode = LocalNode.launchNode();
		eve.unlockPrivateKey("evespass");
				
		// specify exact version
		Object result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.0"), "getVersion", new Serializable[] {});
		assertEquals(100, result);
		
		result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage1.TestService@1.1"), "getVersion", new Serializable[] {});
		assertEquals(110, result);
		
		//  use newest version
		result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService", "getVersion", new Serializable[] {});
		assertEquals(110, result);
		
		// choose appropriate version
		result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1.0", "getVersion", new Serializable[] {});
		assertEquals(100, result);
		
		result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1.1", "getVersion", new Serializable[] {});
		assertEquals(110, result);
		
		result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@1", "getVersion", new Serializable[] {});
		assertEquals(110, result);
		
		// fail on non-existent  version
		boolean failed = false;
		try {
			result = callerNode.invoke(eve, "i5.las2peer.testServices.testPackage1.TestService@2.0", "getVersion", new Serializable[] {});
		}
		catch (NoSuchServiceException e) {
			failed = true;
		}
		assertTrue(failed);
		
		
		// test version selection with RMI
		result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "useTestService", new Serializable[] {"null"});
		assertEquals(110, result);		
		
		result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "useTestService", new Serializable[] {"1.0"});
		assertEquals(100, result);	
		
		result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "useTestService", new Serializable[] {"1.1"});
		assertEquals(110, result);	
		
		failed = false;
		try {
			result = callerNode.invoke(eve, ServiceNameVersion.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0"), "useTestService",  new Serializable[] {"2.0"});
		}
		catch (ServiceInvocationException e) {
			failed = true;
		}
		assertTrue(failed);
	}

}
