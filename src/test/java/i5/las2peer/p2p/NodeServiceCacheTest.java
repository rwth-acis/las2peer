package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.p2p.NodeServiceCache.ServiceInstance;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.tools.CryptoException;

import java.io.IOException;

import org.junit.Test;

public class NodeServiceCacheTest {

	@Test
	public void testLocalServices() throws CryptoException, L2pSecurityException, AgentNotRegisteredException {
		Node node = LocalNode.launchNode();

		NodeServiceCache cache = new NodeServiceCache(node, 0, 0);

		ServiceNameVersion service1 = ServiceNameVersion.fromString("test.service@1.0.5-12");
		ServiceAgentImpl agent1 = ServiceAgentImpl.createServiceAgent(service1, "test");

		ServiceNameVersion service2 = ServiceNameVersion.fromString("test.service@2");
		ServiceAgentImpl agent2 = ServiceAgentImpl.createServiceAgent(service2, "test");

		ServiceNameVersion service3 = ServiceNameVersion.fromString("test.service3@1.0");
		ServiceAgentImpl agent3 = ServiceAgentImpl.createServiceAgent(service3, "test");

		cache.registerLocalService(agent1);
		cache.registerLocalService(agent2);
		cache.registerLocalService(agent3);

		assertSame(cache.getServiceAgentInstance(service1, true, true, null).getServiceAgent(), agent1);
		assertSame(cache.getServiceAgentInstance(service2, true, true, null).getServiceAgent(), agent2);
		assertSame(cache.getServiceAgentInstance(service3, true, true, null).getServiceAgent(), agent3);

		assertEquals(cache
				.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null)
				.getService().getName(), service1.getName());
		assertEquals(cache
				.getServiceAgentInstance(ServiceNameVersion.fromString(service3.getName()), false, true, null)
				.getService().getName(), service3.getName());

		assertSame(cache.getLocalService(service1), agent1);
		assertSame(cache.getLocalService(service2), agent2);
		assertSame(cache.getLocalService(service3), agent3);

		cache.unregisterLocalService(agent1);
		assertEquals(cache
				.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null)
				.getService().getName(), service1.getName());

		cache.unregisterLocalService(agent2);
		cache.unregisterLocalService(agent3);

		try {
			cache.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null);
			fail("AgentNotRegisteredException expected");
		} catch (AgentNotRegisteredException e) {
		}

		try {
			cache.getServiceAgentInstance(ServiceNameVersion.fromString(service3.getName()), false, true, null);
			fail("AgentNotRegisteredException expected");
		} catch (AgentNotRegisteredException e) {
		}
	}

	@Test
	public void testIntegration() throws CryptoException, L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException, NodeException, AgentAccessDeniedException {
		ServiceNameVersion serviceNameVersion = ServiceNameVersion
				.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0");

		LocalNode serviceNode = LocalNode.newNode("export/jars/");
		serviceNode.launch();

		ServiceAgentImpl serviceAgent = ServiceAgentImpl.createServiceAgent(serviceNameVersion, "a pass");
		serviceAgent.unlock("a pass");
		serviceNode.registerReceiver(serviceAgent);

		ServiceAgentImpl localServiceAgent = serviceNode.getNodeServiceCache()
				.getServiceAgentInstance(serviceNameVersion, true, true, null).getServiceAgent();

		assertSame(serviceAgent, localServiceAgent);

		serviceNode.unregisterReceiver(serviceAgent);

		try {
			serviceNode.getNodeServiceCache().getServiceAgentInstance(serviceNameVersion, true, true, null);
			fail("AgentNotRegisteredException exptected!");
		} catch (AgentNotRegisteredException e) {
		}
	}

	@Test
	public void testGlobalServices() throws CryptoException, L2pSecurityException, AgentAlreadyRegisteredException,
			AgentException, CloneNotSupportedException, MalformedXMLException, IOException, NodeException,
			AgentAccessDeniedException {
		// Attention when changing NodeServiceCache parameters
		// You may have to adjust these results afterwards since this may influence the selected versions

		// launch nodes
		LocalNode invokingNode = LocalNode.launchNode();
		LocalNode node1 = LocalNode.launchNode();
		LocalNode node2 = LocalNode.launchNode();
		LocalNode node3 = LocalNode.launchNode();

		// generate services
		ServiceAgentImpl service2 = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), "a pass");
		service2.unlock("a pass");
		ServiceAgentImpl service20 = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.0"), "a pass");
		service20.unlock("a pass");
		ServiceAgentImpl service21 = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.1"), "a pass");
		service21.unlock("a pass");
		ServiceAgentImpl service22 = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), "a pass");
		service22.unlock("a pass");
		ServiceAgentImpl service22_1 = (ServiceAgentImpl) service22.cloneLocked();
		service22_1.unlock("a pass");
		ServiceAgentImpl service22_2 = (ServiceAgentImpl) service22.cloneLocked();
		service22_2.unlock("a pass");
		ServiceAgentImpl service22_3 = (ServiceAgentImpl) service22.cloneLocked();
		service22_3.unlock("a pass");
		ServiceAgentImpl service3 = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@3"), "a pass");
		service3.unlock("a pass");

		// start services
		// v2@node1 v20@node1 v21@node1 v22@node1 v22@node2 v22@node3 v22@invokingNode v3@node1
		node1.registerReceiver(service2);
		node1.registerReceiver(service20);
		node1.registerReceiver(service21);
		node1.registerReceiver(service22);
		node2.registerReceiver(service22_1);
		node3.registerReceiver(service22_2);
		invokingNode.registerReceiver(service22_3);
		node1.registerReceiver(service3);

		// register user
		UserAgentImpl userAgent = MockAgentFactory.getAdam();
		userAgent.unlock("adamspass");
		invokingNode.registerReceiver(userAgent);

		// global exact v2 -> v2
		ServiceInstance instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), true, false, userAgent);
		assertEquals(instance.getServiceAgentId(), service2.getSafeId());
		assertEquals(instance.getNodeId(), node1.getNodeId());

		// local only v2 -> v22@invokingNode
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, true, userAgent);
		assertEquals(instance.getServiceAgent(), service22);
		assertTrue(instance.local());

		// global * -> v22@invokingNode
		// v3 is not returned since local versions are available
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@*"), false, false, userAgent);
		assertEquals(instance.getServiceAgent(), service22);
		assertTrue(instance.local());

		// global v2 -> v22@invokingNode
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, false, userAgent);
		assertEquals(instance.getServiceAgent(), service22_3);
		assertTrue(instance.local());

		// stop all services v22 except v22@node3, global v2.2 -> v22@node3
		node1.unregisterReceiver(service22);
		node2.unregisterReceiver(service22_1);
		invokingNode.unregisterReceiver(service22_3);
		invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), false, false, userAgent);
		assertEquals(instance.getServiceAgentId(), service22.getSafeId());
		assertEquals(instance.getNodeId(), node3.getNodeId());

		// stop service v22@node3, global v2 -> v21
		invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
		node3.unregisterReceiver(service22_2);
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, false, userAgent);
		assertEquals(instance.getServiceAgentId(), service21.getSafeId());
		assertEquals(instance.getNodeId(), node1.getNodeId());

		// stop service v22@node3, global * -> v3
		invokingNode.getNodeServiceCache().setWaitForResults(5);
		invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
		instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
				ServiceNameVersion.fromString("i5.las2peer.api.TestService@*"), false, false, userAgent);
		assertEquals(instance.getServiceAgentId(), service3.getSafeId());
		assertEquals(instance.getNodeId(), node1.getNodeId());
	}

}
