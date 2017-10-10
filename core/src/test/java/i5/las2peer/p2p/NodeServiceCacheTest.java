package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.p2p.NodeServiceCache.ServiceInstance;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

public class NodeServiceCacheTest {

	@Test
	public void testLocalServices() {
		try {
			Node node = new LocalNodeManager().launchNode();

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

			assertSame(agent1, cache.getServiceAgentInstance(service1, true, true, null).getServiceAgent());
			assertSame(agent2, cache.getServiceAgentInstance(service2, true, true, null).getServiceAgent());
			assertSame(agent3, cache.getServiceAgentInstance(service3, true, true, null).getServiceAgent());

			assertEquals(service1.getName(),
					cache.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null)
							.getService().getName());
			assertEquals(service3.getName(),
					cache.getServiceAgentInstance(ServiceNameVersion.fromString(service3.getName()), false, true, null)
							.getService().getName());

			assertSame(agent1, cache.getLocalService(service1));
			assertSame(agent2, cache.getLocalService(service2));
			assertSame(agent3, cache.getLocalService(service3));

			cache.unregisterLocalService(agent1);
			assertEquals(service1.getName(),
					cache.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null)
							.getService().getName());

			cache.unregisterLocalService(agent2);
			cache.unregisterLocalService(agent3);

			try {
				cache.getServiceAgentInstance(ServiceNameVersion.fromString(service1.getName()), false, true, null);
				fail("AgentNotRegisteredException expected");
			} catch (AgentNotRegisteredException e) {
				// expected
			}

			try {
				cache.getServiceAgentInstance(ServiceNameVersion.fromString(service3.getName()), false, true, null);
				fail("AgentNotRegisteredException expected");
			} catch (AgentNotRegisteredException e) {
				// expected
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testIntegration() {
		try {
			ServiceNameVersion serviceNameVersion = ServiceNameVersion
					.fromString("i5.las2peer.testServices.testPackage2.UsingService@1.0");

			LocalNode serviceNode = new LocalNodeManager().newNode("export/jars/");
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
				// expected
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

	@Test
	public void testGlobalServices() {
		try {
			// Attention when changing NodeServiceCache parameters
			// You may have to adjust these results afterwards since this may influence the selected versions

			// launch nodes
			LocalNodeManager manager = new LocalNodeManager();
			Node invokingNode = manager.launchNode();
			Node node1 = manager.launchNode();
			Node node2 = manager.launchNode();
			Node node3 = manager.launchNode();

			// generate services
			ServiceAgentImpl service2 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), "a pass");
			System.out.println("NodeServiceCacheTest: service2 instance " + service2.getIdentifier());
			service2.unlock("a pass");
			ServiceAgentImpl service20 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.0"), "a pass");
			System.out.println("NodeServiceCacheTest: service20 instance " + service20.getIdentifier());
			service20.unlock("a pass");
			ServiceAgentImpl service21 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.1"), "a pass");
			System.out.println("NodeServiceCacheTest: service21 instance " + service21.getIdentifier());
			service21.unlock("a pass");
			ServiceAgentImpl service22_1 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), "a pass");
			System.out.println("NodeServiceCacheTest: service22_1 instance " + service22_1.getIdentifier());
			service22_1.unlock("a pass");
			ServiceAgentImpl service22_2 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), "a pass");
			System.out.println("NodeServiceCacheTest: service22_2 instance " + service22_2.getIdentifier());
			service22_2.unlock("a pass");
			ServiceAgentImpl service22_3 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), "a pass");
			System.out.println("NodeServiceCacheTest: service22_3 instance " + service22_3.getIdentifier());
			service22_3.unlock("a pass");
			ServiceAgentImpl service3 = ServiceAgentImpl
					.createServiceAgent(ServiceNameVersion.fromString("i5.las2peer.api.TestService@3"), "a pass");
			service3.unlock("a pass");
			System.out.println("NodeServiceCacheTest: service3 instance " + service3.getIdentifier());

			// start services
			// v2@node1 v20@node1 v21@node1 v22@node1 v22@node2 v22@node3 v22@invokingNode v3@node1
			node1.storeAgent(service2);
			node1.registerReceiver(service2);
			node1.storeAgent(service20);
			node1.registerReceiver(service20);
			node1.storeAgent(service21);
			node1.registerReceiver(service21);
			node2.storeAgent(service22_1);
			node2.registerReceiver(service22_1);
			node3.storeAgent(service22_2);
			node3.registerReceiver(service22_2);
			invokingNode.storeAgent(service22_3);
			invokingNode.registerReceiver(service22_3);
			node1.storeAgent(service3);
			node1.registerReceiver(service3);

			// register user
			UserAgentImpl userAgent = MockAgentFactory.getAdam();
			userAgent.unlock("adamspass");
			invokingNode.storeAgent(userAgent);
			invokingNode.registerReceiver(userAgent);

			invokingNode.getNodeServiceCache().setTimeoutMs(10000);
			// global exact v2 -> v2
			ServiceInstance instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), true, false, userAgent);
			assertEquals(service2.getIdentifier(), instance.getServiceAgentId());
			assertEquals(node1.getNodeId(), instance.getNodeId());

			// local only v2 -> v22@invokingNode
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, true, userAgent);
			assertEquals(service22_3.getIdentifier(), instance.getServiceAgent().getIdentifier());
			assertTrue(instance.local());

			// global * -> v22@invokingNode
			// v3 is not returned since local versions are available
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@*"), false, false, userAgent);
			assertEquals(service22_3, instance.getServiceAgent());
			assertTrue(instance.local());

			// global v2 -> v22@invokingNode
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, false, userAgent);
			assertEquals(service22_3, instance.getServiceAgent());
			assertTrue(instance.local());

			// stop all services v22 except v22@node3, global v2.2 -> v22@node3
			node2.unregisterReceiver(service22_1);
			invokingNode.unregisterReceiver(service22_3);
			invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@2.2"), false, false, userAgent);
			assertEquals(service22_2.getIdentifier(), instance.getServiceAgentId());
			assertEquals(node3.getNodeId(), instance.getNodeId());

			// stop service v22@node3, global v2 -> v21
			invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
			node3.unregisterReceiver(service22_2);
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@2"), false, false, userAgent);
			assertEquals(service21.getIdentifier(), instance.getServiceAgentId());
			assertEquals(node1.getNodeId(), instance.getNodeId());

			// stop service v22@node3, global * -> v3
			invokingNode.getNodeServiceCache().setWaitForResults(5);
			invokingNode.getNodeServiceCache().clear(); // clear cache to force reloading of service index
			instance = invokingNode.getNodeServiceCache().getServiceAgentInstance(
					ServiceNameVersion.fromString("i5.las2peer.api.TestService@*"), false, false, userAgent);
			assertEquals(service3.getIdentifier(), instance.getServiceAgentId());
			assertEquals(node1.getNodeId(), instance.getNodeId());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}

}
