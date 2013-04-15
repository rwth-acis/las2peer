package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalNodeInvokationTest {

	@Before
	public void reset() {
		LocalNode.reset();
	}
	
	@After 
	public void clearThreads () {
		LocalNode.stopCleaner();
	}
	
	@Test
	public void testLocalInvokation() throws SecurityException, IllegalArgumentException, AgentNotKnownException, L2pSecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException, AgentAlreadyRegisteredException, MalformedXMLException, IOException, CryptoException, AgentException {		
		LocalNode node = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();
		
		node.storeAgent ( eve );
		
		node.launch();
		
		ServiceAgent testServiceAgent = ServiceAgent.generateNewAgent("i5.las2peer.api.TestService", "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		node.registerReceiver(testServiceAgent);
		
		Object result = node.invokeLocally(eve.getId(), "i5.las2peer.api.TestService", "inc", new Serializable [] { new Integer ( 10)});
		
		assertEquals ( 12, result);
	}
	
	@Test
	public void testGlobalInvokation() throws SecurityException, IllegalArgumentException, AgentNotKnownException, L2pSecurityException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InterruptedException, AgentAlreadyRegisteredException, MalformedXMLException, IOException, CryptoException, AgentException, TimeoutException {		
		LocalNode serviceNode = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();
		
		serviceNode.storeAgent ( eve );
		serviceNode.launch();
		
		ServiceAgent testServiceAgent = ServiceAgent.generateNewAgent("i5.las2peer.api.TestService", "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		serviceNode.registerReceiver(testServiceAgent);
		
		LocalNode callerNode = LocalNode.launchNode ();
		eve.unlockPrivateKey("evespass");
				Object result = callerNode.invokeGlobally(eve, "i5.las2peer.api.TestService", "inc", new Serializable[]{ new Integer (12)}); 
		
		assertEquals ( 14, result);
	}
	
	
	
	@Test
	public void testSubinvokation ()  throws MalformedXMLException, IOException, L2pSecurityException, CryptoException, InterruptedException, AgentAlreadyRegisteredException, AgentException, TimeoutException {
		LocalNode serviceNode1 = LocalNode.newNode();
		LocalNode serviceNode2 = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode1.storeAgent ( eve );
		serviceNode1.launch();
		serviceNode2.launch();
		
		ServiceAgent testServiceAgent = ServiceAgent.generateNewAgent("i5.las2peer.api.TestService", "a pass");
		testServiceAgent.unlockPrivateKey("a pass");
		serviceNode1.registerReceiver(testServiceAgent);

		ServiceAgent testServiceAgent2 = ServiceAgent.generateNewAgent("i5.las2peer.api.TestService2", "a 2nd pass");
		testServiceAgent2.unlockPrivateKey("a 2nd pass");
		serviceNode2.registerReceiver(testServiceAgent2);
		
		
		LocalNode callerNode = LocalNode.launchNode ();
		eve.unlockPrivateKey("evespass");
		Object result = callerNode.invokeGlobally(eve, "i5.las2peer.api.TestService2", "usingOther", new Serializable[] { new Integer (12) }); 
		
		assertEquals ( 14, result);
	}
	
	
	@Test 
	public void testSubinvokationFail () throws MalformedXMLException, IOException, L2pSecurityException, CryptoException, InterruptedException, AgentAlreadyRegisteredException, AgentException, TimeoutException {
		LocalNode serviceNode2 = LocalNode.newNode();
		UserAgent eve = MockAgentFactory.getEve();

		serviceNode2.storeAgent ( eve );
		serviceNode2.launch();
		
		ServiceAgent testServiceAgent2 = ServiceAgent.generateNewAgent("i5.las2peer.api.TestService2", "a 2nd pass");
		testServiceAgent2.unlockPrivateKey("a 2nd pass");
		serviceNode2.registerReceiver(testServiceAgent2);
		
		
		LocalNode callerNode = LocalNode.launchNode ();
		eve.unlockPrivateKey("evespass");
		Object result = callerNode.invokeGlobally(eve, "i5.las2peer.api.TestService2", "usingOther", new Serializable[] {new Integer (12)}); 
		
		assertEquals ( -200, result);
		
	}
	

}
