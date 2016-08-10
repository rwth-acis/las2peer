package i5.las2peer.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.tools.CryptoException;

import org.junit.Test;

public class ServiceAliasManagerTest {
	@Test
	public void test() throws CryptoException, L2pSecurityException, DuplicateServiceAliasException,
			ServiceNotFoundException {
		LocalNode node = LocalNode.launchNode();
		ServiceAgent agentA = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA.unlockPrivateKey("asdf");
		ServiceAgent agentB = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceB@1.0"), "asdf");
		agentB.unlockPrivateKey("asdf");
		ServiceAgent agentA2 = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceA@1.0"), "asdf");
		agentA2.unlockPrivateKey("asdf");
		ServiceAgent agentC = ServiceAgent.createServiceAgent(ServiceNameVersion.fromString("serviceC@1.0"), "asdf");
		agentC.unlockPrivateKey("asdf");

		// regular creation
		node.getServiceAliasManager().registerServiceAlias(agentA, "aliasA");
		assertEquals(node.getServiceAliasManager().getServiceNameByAlias("aliasA"), "serviceA");

		// second alias
		node.getServiceAliasManager().registerServiceAlias(agentB, "aliasB");
		assertEquals(node.getServiceAliasManager().getServiceNameByAlias("aliasB"), "serviceB");

		// register second time, no conflict
		node.getServiceAliasManager().registerServiceAlias(agentA2, "aliasA");

		// duplicate
		try {
			node.getServiceAliasManager().registerServiceAlias(agentC, "aliasA");
			fail("DuplicateServiceAliasException expected");
		} catch (DuplicateServiceAliasException e) {
		}
	}
}
