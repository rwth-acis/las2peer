package i5.las2peer.p2p;

import static org.junit.Assert.*;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;

import org.junit.Test;

public class NodeServiceCacheTest {

	@Test
	public void testLocalServices() throws CryptoException, L2pSecurityException {
		NodeServiceCache cache = new NodeServiceCache(null, 0);

		ServiceNameVersion service1 = ServiceNameVersion.fromString("test.service@1.0.5-12");
		ServiceAgent agent1 = ServiceAgent.createServiceAgent(service1, "test");

		ServiceNameVersion service2 = ServiceNameVersion.fromString("test.service@2");
		ServiceAgent agent2 = ServiceAgent.createServiceAgent(service2, "test");

		ServiceNameVersion service3 = ServiceNameVersion.fromString("test.service3@1.0");
		ServiceAgent agent3 = ServiceAgent.createServiceAgent(service3, "test");

		cache.registerLocalService(agent1);
		cache.registerLocalService(agent2);
		cache.registerLocalService(agent3);

		assertSame(cache.getLocalServiceAgent(service1), agent1);
		assertSame(cache.getLocalServiceAgent(service2), agent2);
		assertSame(cache.getLocalServiceAgent(service3), agent3);

		assertEquals(cache.getLocalVersions(service1.getName()).length, 2);
		assertEquals(cache.getLocalVersions(service3.getName()).length, 1);

		cache.unregisterLocalService(agent1);
		assertEquals(cache.getLocalVersions(service1.getName()).length, 1);

		cache.unregisterLocalService(agent2);
		cache.unregisterLocalService(agent3);

		assertTrue(cache.getLocalVersions(service1.getName()) == null);
		assertTrue(cache.getLocalVersions(service3.getName()) == null);
	}

	@Test
	public void testIntegration() throws CryptoException, L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException, NodeException {
		ServiceNameVersion serviceNameVersion = ServiceNameVersion
				.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0");

		LocalNode serviceNode = LocalNode.newNode("export/jars/");
		serviceNode.launch();

		ServiceAgent serviceAgent = ServiceAgent.createServiceAgent(serviceNameVersion, "a pass");
		serviceAgent.unlockPrivateKey("a pass");
		serviceNode.registerReceiver(serviceAgent);

		ServiceAgent localServiceAgent = serviceNode.getNodeServiceCache().getLocalServiceAgent(serviceNameVersion);

		assertSame(serviceAgent, localServiceAgent);
	}
}
